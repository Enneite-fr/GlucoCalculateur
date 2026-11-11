package com.example.glucocalculateur.data.io

import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Lecture de texte importé : tolère le BOM, l'UTF-8 et les encodages Windows / Latin-1. */
object TextDecoding {
    private val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    private val legacyCharset: Charset =
        runCatching { Charset.forName("windows-1252") }.getOrDefault(Charsets.ISO_8859_1)

    fun decode(bytes: ByteArray): String {
        val content = if (bytes.size >= 3 && bytes.copyOfRange(0, 3).contentEquals(utf8Bom)) {
            bytes.copyOfRange(3, bytes.size)
        } else {
            bytes
        }
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content))
                .toString()
        } catch (_: CharacterCodingException) {
            String(content, legacyCharset)
        }
        return text.removePrefix("\uFEFF")
    }
}

/** Nombres dans les fichiers : virgule ou point décimal, espaces de milliers tolérés. */
object FileNumbers {
    private val spaces = Regex("[\\s  ]")

    fun parse(raw: String): Double? =
        raw.trim().replace(spaces, "").replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    /** Écriture sans exposant ni zéros superflus, avec le séparateur décimal demandé. */
    fun format(value: Double, decimalSeparator: Char): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', decimalSeparator)
}
