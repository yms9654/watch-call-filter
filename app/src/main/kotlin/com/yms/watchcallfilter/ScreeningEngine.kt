package com.yms.watchcallfilter

enum class Decision { ALLOW, BLOCK }

class ScreeningEngine(
    private val contacts: ContactRepository,
    private val settings: ScreeningSettings
) {

    fun decide(rawNumber: String?): Decision {
        if (!settings.enabled) return Decision.ALLOW

        val number = NumberUtil.normalize(rawNumber)
        if (number == null) {
            return if (settings.blockPrivate) Decision.BLOCK else Decision.ALLOW
        }

        return if (contacts.isKnown(number)) Decision.ALLOW else Decision.BLOCK
    }
}
