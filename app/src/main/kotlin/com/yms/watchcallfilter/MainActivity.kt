package com.yms.watchcallfilter

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var settings: SharedPrefScreeningSettings
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SharedPrefScreeningSettings(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            text = getString(R.string.status_ready)
        }
        root.addView(statusText)

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
