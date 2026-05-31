package com.shahid.cents

data class ScanResult(
    val scanned: Int = 0,
    val parsed: Int = 0,
    val imported: Int = 0,
    val error: String? = null
) {
    fun message(): String {
        if (error != null) return "SMS scan failed: $error"
        return "Scanned $scanned SMS. Parsed $parsed transactions. Imported $imported new."
    }
}
