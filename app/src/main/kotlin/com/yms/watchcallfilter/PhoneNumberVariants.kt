package com.yms.watchcallfilter

/**
 * Generates lookup-equivalent variants of a phone number to work around
 * PhoneLookup's inconsistent cross-format matching.
 *
 * Today this only covers KR (+82 <-> leading 0). Extend per-country as needed.
 */
object PhoneNumberVariants {

    private const val KR_CC = "+82"

    fun expand(normalized: String): List<String> {
        if (normalized.isBlank()) return emptyList()

        val out = linkedSetOf(normalized)

        when {
            normalized.startsWith(KR_CC) && normalized.length > KR_CC.length -> {
                out.add("0" + normalized.substring(KR_CC.length))
            }
            normalized.startsWith("0") && normalized.length >= 10 && !normalized.contains('+') -> {
                out.add(KR_CC + normalized.substring(1))
            }
        }

        return out.toList()
    }
}
