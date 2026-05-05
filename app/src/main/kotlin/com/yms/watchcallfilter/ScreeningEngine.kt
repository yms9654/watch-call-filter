package com.yms.watchcallfilter

sealed class Decision {
    object Allow : Decision()
    data class Block(val reason: BlockReason) : Decision()
}

enum class BlockReason { UNKNOWN_NUMBER, PRIVATE_NUMBER }

class ScreeningEngine(
    private val contacts: ContactRepository,
    private val settings: ScreeningSettings
) {

    fun decide(rawNumber: String?): Decision {
        if (!settings.enabled) return Decision.Allow

        val number = NumberUtil.normalize(rawNumber)
        if (number == null) {
            return if (settings.blockPrivate) Decision.Block(BlockReason.PRIVATE_NUMBER)
            else Decision.Allow
        }

        return if (contacts.isKnown(number)) Decision.Allow
        else Decision.Block(BlockReason.UNKNOWN_NUMBER)
    }
}
