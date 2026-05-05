package com.yms.watchcallfilter.pairing

import java.security.SecureRandom

object PairingCode {
    private val random = SecureRandom()

    fun generate(): String = (0 until 6)
        .map { random.nextInt(10) }
        .joinToString("")

    fun isValid(code: String): Boolean =
        code.length == 6 && code.all { it.isDigit() }
}
