package com.yms.watchcallfilter

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.yms.watchcallfilter.identity.WatchIdentity
import com.yms.watchcallfilter.pairing.PairingClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private lateinit var settings: SharedPrefScreeningSettings
    private lateinit var statusText: TextView
    private lateinit var pairingText: TextView
    private val pairingClient = PairingClient()
    private var pairingPollJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SharedPrefScreeningSettings(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        statusText = TextView(this).apply {
            text = getString(R.string.status_ready)
            gravity = Gravity.CENTER
        }
        root.addView(statusText)

        pairingText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setPadding(0, 24, 0, 24)
            gravity = Gravity.CENTER
        }
        root.addView(pairingText)

        val enableSwitch = Switch(this).apply {
            text = getString(R.string.toggle_enabled)
            isChecked = settings.enabled
            setOnCheckedChangeListener { _, checked ->
                settings.setEnabled(checked)
                text = if (checked) getString(R.string.toggle_enabled) else getString(R.string.toggle_disabled)
            }
        }
        root.addView(enableSwitch)

        val privateSwitch = Switch(this).apply {
            text = getString(R.string.block_private)
            isChecked = settings.blockPrivate
            setOnCheckedChangeListener { _, checked ->
                settings.setBlockPrivate(checked)
            }
        }
        root.addView(privateSwitch)

        setContentView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        ensurePermissions()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        startPairingFlow()
    }

    override fun onPause() {
        super.onPause()
        pairingPollJob?.cancel()
        pairingPollJob = null
    }

    private fun ensurePermissions() {
        val contactsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!contactsGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQ_CONTACTS
            )
            return
        }

        requestCallScreeningRoleIfNeeded()
    }

    private fun requestCallScreeningRoleIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val rm = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
            && !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            startActivityForResult(intent, REQ_ROLE)
        }
    }

    private fun updateStatus() {
        val contactsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(Context.ROLE_SERVICE) as RoleManager
            rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else true

        statusText.text = when {
            !contactsGranted -> getString(R.string.status_need_contacts)
            !roleHeld -> getString(R.string.status_need_role)
            else -> getString(R.string.status_ready)
        }
    }

    private fun startPairingFlow() {
        if (pairingPollJob?.isActive == true) return
        pairingPollJob = lifecycleScope.launch {
            val watchId = WatchIdentity(this@MainActivity).watchId
            val auth = Firebase.auth
            try {
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                }
                val authUid = auth.currentUser?.uid ?: return@launch

                if (pairingClient.isPaired(watchId)) {
                    pairingText.text = getString(R.string.paired_label)
                    triggerSyncOnce()
                    return@launch
                }

                val code = pairingClient.publishPairingCode(watchId, authUid)
                pairingText.text = getString(R.string.pairing_code_label, formatCode(code))

                while (true) {
                    delay(5_000)
                    if (pairingClient.isPaired(watchId)) {
                        pairingText.text = getString(R.string.paired_label)
                        triggerSyncOnce()
                        break
                    }
                }
            } catch (t: Throwable) {
                pairingText.text = getString(R.string.pairing_error, t.message ?: "")
            }
        }
    }

    private fun triggerSyncOnce() {
        val request = androidx.work.OneTimeWorkRequestBuilder<
            com.yms.watchcallfilter.sync.AllowlistSyncWorker>().build()
        WorkManager.getInstance(this).enqueue(request)
    }

    private fun formatCode(code: String): String =
        code.chunked(3).joinToString(" ")

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CONTACTS
            && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            requestCallScreeningRoleIfNeeded()
        }
        updateStatus()
    }

    companion object {
        private const val REQ_CONTACTS = 1001
        private const val REQ_ROLE = 1002
    }
}
