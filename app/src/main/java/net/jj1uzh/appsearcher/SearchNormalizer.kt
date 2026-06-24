package net.jj1uzh.appsearcher

import android.icu.text.Transliterator
import com.atilika.kuromoji.ipadic.Tokenizer

class SearchNormalizer {
    // Lazy init for tokenizer and transliterators as they can be heavy
    private val tokenizer by lazy { Tokenizer() }
    
    private val fullToHalf by lazy { Transliterator.getInstance("Fullwidth-Halfwidth") }
    private val kataToHira by lazy { Transliterator.getInstance("Katakana-Hiragana") }
    private val toLatin by lazy { Transliterator.getInstance("Any-Latin; Latin-Ascii; Any-Lower") }
    
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
        val halfWidth = fullToHalf.transliterate(original)
        keys.add(halfWidth)
        keys.add(halfWidth.lowercase())
        
        // 2. Kuromoji Kanji to Kana (Katakana)
        var readingKatakana = ""
        try {
            val tokens = tokenizer.tokenize(original)
            readingKatakana = tokens.joinToString("") { token -> 
                // token.reading might be null if it's already kana or alphanumeric, 
                // in that case we use the surface form
                token.reading ?: token.surface
            }
        } catch (e: Exception) {
            readingKatakana = original
        }
        keys.add(readingKatakana)
        
        // 3. Katakana to Hiragana
        val hiragana = kataToHira.transliterate(readingKatakana)
        keys.add(hiragana)
        
        // 4. Hiragana/Katakana to Romaji (Latin)
        val romaji = toLatin.transliterate(hiragana)
        keys.add(romaji)
        keys.add(normalizeRomaji(romaji))
        
        // Also add the romaji version of the halfWidth just in case
        val hwRomaji = toLatin.transliterate(halfWidth)
        keys.add(hwRomaji)
        keys.add(normalizeRomaji(hwRomaji))
        
        return keys.filter { it.isNotBlank() }.toList()
    }
    
    /**
     * Normalizes a search query typed by the user.
     * We typically convert it to lower-case half-width romaji/hiragana to match.
     */
    fun normalizeQuery(query: String): String {
        var q = query.trim()
        q = fullToHalf.transliterate(q)
        q = toLatin.transliterate(q)
        q = q.lowercase()
        return normalizeQueryRomaji(q)
    }

    private fun normalizeRomaji(text: String): String {
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

    private fun normalizeQueryRomaji(query: String): String {
        var s = normalizeRomaji(query)
        if (s.endsWith("sh")) s = s.substring(0, s.length - 2) + "s"
        if (s.endsWith("ch")) s = s.substring(0, s.length - 2) + "t"
        if (s.endsWith("ts")) s = s.substring(0, s.length - 2) + "t"
        if (s.endsWith("j")) s = s.substring(0, s.length - 1) + "z"
        if (s.endsWith("f")) s = s.substring(0, s.length - 1) + "h"
        if (s.endsWith("c")) s = s.substring(0, s.length - 1) + "t"
        return s
    }

    /**
     * Converts a string to its consonant skeleton to facilitate matching 
     * English words to their Katakana romaji approximations.
     * Example: "print" -> "prnt", "purinto" -> "prnt"
     */
    fun getConsonants(text: String): String {
        return text.lowercase()
            .replace(Regex("[aeiouy\\-]"), "")
            .replace("c", "k")
            .replace("l", "r")
    }
}
