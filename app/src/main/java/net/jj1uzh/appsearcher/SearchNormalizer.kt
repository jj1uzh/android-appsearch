package net.jj1uzh.appsearcher

import android.icu.text.Transliterator
import com.atilika.kuromoji.ipadic.Tokenizer

fun interface TransliteratorService {
    fun transliterate(id: String, text: String): String
}

private class AndroidTransliteratorService : TransliteratorService {
    private val map = mutableMapOf<String, Transliterator>()
    override fun transliterate(id: String, text: String): String {
        val t = map.getOrPut(id) { Transliterator.getInstance(id) }
        return t.transliterate(text)
    }
}

class SearchNormalizer(
    private val transliteratorService: TransliteratorService = AndroidTransliteratorService()
) {
    // Lazy init for tokenizer as it can be heavy
    private val tokenizer by lazy { Tokenizer() }

    private fun fullToHalf(text: String): String =
        transliteratorService.transliterate("Fullwidth-Halfwidth", text)

    private fun kataToHira(text: String): String =
        transliteratorService.transliterate("Katakana-Hiragana", text)

    private fun toLatin(text: String): String =
        transliteratorService.transliterate("Any-Latin; Latin-Ascii; Any-Lower", text)

    private val romajiMoraRegex = Regex(
        """(?i)(?:""" +
        """(?:([b-df-hj-np-tv-z])\1y?|t?ch|s?sh|[b-df-hj-np-tv-z]y|[b-df-hj-np-tv-z])?[aeiou]""" +
        """|n(?![aeiouy])""" +
        """|[b-df-hj-np-tv-z]+$""" +
        """|[^a-z\s]+""" +
        """)"""
    )

    /**
     * Splits a hiragana string into morae (sound units).
     * Sokuon (っ) is combined with the following kana, and small kana (ゃ, ゅ, ょ, etc.)
     * are combined with the preceding kana.
     */
    fun splitHiraganaToMorae(text: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        val smallKana = setOf('ゃ', 'ゅ', 'ょ', 'ゎ', 'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ')

        while (i < text.length) {
            val c = text[i]

            // Consecutive ASCII alphanumeric
            if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9') {
                val start = i
                while (i < text.length && (text[i] in 'a'..'z' || text[i] in 'A'..'Z' || text[i] in '0'..'9')) {
                    i++
                }
                result.add(text.substring(start, i))
                continue
            }
            if (c.isWhitespace()) {
                i++
                continue
            }

            if (c == 'っ') {
                if (i + 1 < text.length) {
                    if (i + 2 < text.length && smallKana.contains(text[i + 2])) {
                        result.add(text.substring(i, i + 3))
                        i += 3
                    } else {
                        result.add(text.substring(i, i + 2))
                        i += 2
                    }
                    continue
                }
            }

            if (i + 1 < text.length && smallKana.contains(text[i + 1])) {
                result.add(text.substring(i, i + 2))
                i += 2
                continue
            }

            result.add(c.toString())
            i++
        }
        return result
    }

    /**
     * Splits a romaji string into morae (sound units) based on syllables.
     */
    fun splitRomajiToMorae(text: String): List<String> {
        return romajiMoraRegex.findAll(text)
            .map { it.value.lowercase() }
            .filter { it.isNotBlank() }
            .toList()
    }

    /**
     * Given an app name (e.g. "設定", "カメラ"), returns a set of normalized
     * strings that can be matched against a user query.
     */
    fun normalize(text: String): List<String> {
        val keys = mutableSetOf<String>()
        val original = text.trim()
        keys.add(original)
        keys.add(original.lowercase())

        // 1. Full-width to Half-width
        val halfWidth = fullToHalf(original)
        keys.add(halfWidth)
        keys.add(halfWidth.lowercase())

        // 2. Kuromoji Kanji to Kana (Katakana)
        var readingKatakana = ""
        try {
            val tokens = tokenizer.tokenize(original)
            readingKatakana = tokens.joinToString("") { token ->
                val r = token.reading
                if (r == null || r == "*") token.surface else r
            }
        } catch (e: Exception) {
            readingKatakana = original
        }
        keys.add(readingKatakana)

        // 3. Katakana to Hiragana
        val hiragana = kataToHira(readingKatakana)
        keys.add(hiragana)

        // 4. Mora-delimited Romaji (Hepburn & Kunrei)
        val morae = splitHiraganaToMorae(hiragana)
        if (morae.isNotEmpty()) {
            val romajiMorae = morae.map { mora ->
                toLatin(mora).trim()
            }.filter { it.isNotBlank() }

            if (romajiMorae.isNotEmpty()) {
                val hepburn = romajiMorae.joinToString(" ")
                keys.add("mora:$hepburn")

                val kunrei = romajiMorae.map { normalizeRomaji(it) }.joinToString(" ")
                keys.add("mora:$kunrei")
            }
        }

        return keys.filter { it.isNotBlank() }.toList()
    }

    /**
     * Normalizes a search query typed by the user.
     */
    fun normalizeQuery(query: String): String {
        var q = query.trim()
        q = fullToHalf(q)
        q = toLatin(q)
        q = q.lowercase()
        return normalizeQueryRomaji(q)
    }

    /**
     * Checks if the app matches the query.
     */
    fun matches(app: AppInfo, query: String): Boolean {
        return matches(app.searchKeys, app.packageName, query)
    }

    /**
     * Checks if the app is a prefix match for the query.
     */
    fun isPrefixMatch(app: AppInfo, query: String): Boolean {
        return isPrefixMatch(app.searchKeys, app.packageName, query)
    }

    fun matches(searchKeys: List<String>, packageName: String, query: String): Boolean {
        val qRaw = query.trim()
        if (qRaw.isBlank()) return false

        val qHalf = fullToHalf(qRaw).lowercase()
        val qHira = kataToHira(qHalf).lowercase()

        // 1. Check package name (prefix match on segments, e.g. "settings" in "com.android.settings")
        val pkgMatches = packageName.lowercase()
            .split(".", "_", "-")
            .filter { it.isNotBlank() && it !in setOf("com", "org", "net", "jp", "io", "android") }
            .any { it.startsWith(qHalf) }
        if (pkgMatches) return true

        // 2. Check standard search keys (original, kana, etc.)
        for (key in searchKeys) {
            if (key.startsWith("mora:")) continue
            val lowerKey = key.lowercase()
            if (lowerKey.contains(qRaw.lowercase()) ||
                lowerKey.contains(qHalf) ||
                lowerKey.contains(qHira)) {
                return true
            }
        }

        // 3. Check mora-delimited romaji keys
        val qMorae = splitRomajiToMorae(qHalf)
        if (qMorae.isNotEmpty()) {
            val normalizedQMorae = qMorae.map { normalizeQueryRomaji(it) }

            for (key in searchKeys) {
                if (!key.startsWith("mora:")) continue
                val keyMoraString = key.removePrefix("mora:")
                val keyMorae = keyMoraString.split(" ")
                val normalizedKeyMorae = keyMorae.map { normalizeRomaji(it) }
                if (isMoraMatch(normalizedKeyMorae, normalizedQMorae)) return true
                if (isMoraMatch(keyMorae, qMorae)) return true
            }
        }

        return false
    }

    fun isPrefixMatch(searchKeys: List<String>, packageName: String, query: String): Boolean {
        val qRaw = query.trim()
        if (qRaw.isBlank()) return false

        val qHalf = fullToHalf(qRaw).lowercase()
        val qHira = kataToHira(qHalf).lowercase()

        // Check package name
        val pkgPrefixMatch = packageName.lowercase()
            .split(".", "_", "-")
            .filter { it.isNotBlank() && it !in setOf("com", "org", "net", "jp", "io", "android") }
            .any { it.startsWith(qHalf) }
        if (pkgPrefixMatch) return true

        // Check standard search keys
        for (key in searchKeys) {
            if (key.startsWith("mora:")) continue
            val lowerKey = key.lowercase()
            if (lowerKey.startsWith(qRaw.lowercase()) ||
                lowerKey.startsWith(qHalf) ||
                lowerKey.startsWith(qHira)) {
                return true
            }
        }

        // Check mora-delimited romaji keys
        val qMorae = splitRomajiToMorae(qHalf)
        if (qMorae.isNotEmpty()) {
            val normalizedQMorae = qMorae.map { normalizeQueryRomaji(it) }

            for (key in searchKeys) {
                if (!key.startsWith("mora:")) continue
                val keyMoraString = key.removePrefix("mora:")
                val keyMorae = keyMoraString.split(" ")
                val normalizedKeyMorae = keyMorae.map { normalizeRomaji(it) }
                if (isMoraPrefixMatch(normalizedKeyMorae, normalizedQMorae)) return true
                if (isMoraPrefixMatch(keyMorae, qMorae)) return true
            }
        }

        return false
    }

    private fun isMoraMatch(keyMorae: List<String>, queryMorae: List<String>): Boolean {
        if (queryMorae.isEmpty()) return false
        for (k in keyMorae.indices) {
            if (k + queryMorae.size > keyMorae.size) break
            var match = true
            for (m in queryMorae.indices) {
                val kMora = keyMorae[k + m]
                val qMora = queryMorae[m]
                if (m == queryMorae.size - 1) {
                    if (!kMora.startsWith(qMora)) {
                        match = false
                        break
                    }
                } else {
                    if (kMora != qMora) {
                        match = false
                        break
                    }
                }
            }
            if (match) return true
        }
        return false
    }

    private fun isMoraPrefixMatch(keyMorae: List<String>, queryMorae: List<String>): Boolean {
        if (queryMorae.isEmpty() || keyMorae.size < queryMorae.size) return false
        for (m in queryMorae.indices) {
            val kMora = keyMorae[m]
            val qMora = queryMorae[m]
            if (m == queryMorae.size - 1) {
                if (!kMora.startsWith(qMora)) return false
            } else {
                if (kMora != qMora) return false
            }
        }
        return true
    }

    fun normalizeRomaji(text: String): String {
        var s = text
        s = s.replace("shi", "si")
        s = s.replace("sha", "sya")
        s = s.replace("shu", "syu")
        s = s.replace("sho", "syo")
        s = s.replace("chi", "ti")
        s = s.replace("cha", "tya")
        s = s.replace("chu", "tyu")
        s = s.replace("cho", "tyo")
        s = s.replace("tsu", "tu")
        s = s.replace("fu", "hu")
        s = s.replace("ji", "zi")
        s = s.replace("ja", "zya")
        s = s.replace("ju", "zyu")
        s = s.replace("jo", "zyo")
        return s
    }

    fun normalizeQueryRomaji(query: String): String {
        var s = normalizeRomaji(query)
        if (s.endsWith("sh")) s = s.substring(0, s.length - 2) + "s"
        if (s.endsWith("ch")) s = s.substring(0, s.length - 2) + "t"
        if (s.endsWith("ts")) s = s.substring(0, s.length - 2) + "t"
        if (s.endsWith("j")) s = s.substring(0, s.length - 1) + "z"
        if (s.endsWith("f")) s = s.substring(0, s.length - 1) + "h"
        if (s.endsWith("c")) s = s.substring(0, s.length - 1) + "t"
        return s
    }
}
