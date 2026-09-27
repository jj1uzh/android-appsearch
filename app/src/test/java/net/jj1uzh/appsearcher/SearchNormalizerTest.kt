package net.jj1uzh.appsearcher

import com.ibm.icu.text.Transliterator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchNormalizerTest {

    private lateinit var normalizer: SearchNormalizer

    @Before
    fun setUp() {
        val testTransliterator = TransliteratorService { id, text ->
            Transliterator.getInstance(id).transliterate(text)
        }
        normalizer = SearchNormalizer(testTransliterator)
    }

    @Test
    fun testNormalizeShouContainsMoraDelimitedRomaji() {
        val keys = normalizer.normalize("しょう")
        // Should contain mora-delimited romaji
        assertTrue("Contains mora:sho u", keys.contains("mora:sho u"))
        assertTrue("Contains mora:syo u", keys.contains("mora:syo u"))
        // Should NOT contain undivided romaji string to avoid false positives
        assertFalse("Should not contain plain shou", keys.contains("shou"))
        assertFalse("Should not contain plain syou", keys.contains("syou"))
        // Should contain original and hiragana/katakana
        assertTrue("Contains original", keys.contains("しょう"))
        assertTrue("Contains katakana", keys.contains("ショウ"))
    }

    @Test
    fun testShouDoesNotMatchHou() {
        val app = AppInfo(
            packageName = "com.example.game",
            name = "将棋",
            searchKeys = normalizer.normalize("将棋")
        )

        // Positive tests
        assertTrue("Matches shou", normalizer.matches(app, "shou"))
        assertTrue("Matches sho", normalizer.matches(app, "sho"))
        assertTrue("Matches sh", normalizer.matches(app, "sh"))
        assertTrue("Matches s", normalizer.matches(app, "s"))
        assertTrue("Matches syou (kunrei)", normalizer.matches(app, "syou"))
        assertTrue("Matches syo (kunrei)", normalizer.matches(app, "syo"))
        assertTrue("Matches shougi", normalizer.matches(app, "shougi"))
        assertTrue("Matches gi", normalizer.matches(app, "gi"))
        assertTrue("Matches u", normalizer.matches(app, "u"))
        assertTrue("Matches kanji", normalizer.matches(app, "将棋"))
        assertTrue("Matches hiragana", normalizer.matches(app, "しょうぎ"))
        assertTrue("Matches katakana", normalizer.matches(app, "ショウギ"))

        // False positive prevention tests - The exact requirement!
        assertFalse("Should NOT match hou", normalizer.matches(app, "hou"))
        assertFalse("Should NOT match ho", normalizer.matches(app, "ho"))
        assertFalse("Should NOT match h", normalizer.matches(app, "h"))
        assertFalse("Should NOT match ou", normalizer.matches(app, "ou"))
        assertFalse("Should NOT match o", normalizer.matches(app, "o"))
        assertFalse("Should NOT match ほう", normalizer.matches(app, "ほう"))
    }

    @Test
    fun testPrefixMatchRanking() {
        val app = AppInfo(
            packageName = "com.example.shougi",
            name = "将棋",
            searchKeys = normalizer.normalize("将棋")
        )

        // Prefix matches
        assertTrue("shou is prefix match", normalizer.isPrefixMatch(app, "shou"))
        assertTrue("sh is prefix match", normalizer.isPrefixMatch(app, "sh"))
        assertTrue("syou is prefix match", normalizer.isPrefixMatch(app, "syou"))

        // Substring matches that are NOT prefix match
        assertTrue("gi is match", normalizer.matches(app, "gi"))
        assertFalse("gi is NOT prefix match", normalizer.isPrefixMatch(app, "gi"))
        assertTrue("u is match", normalizer.matches(app, "u"))
        assertFalse("u is NOT prefix match", normalizer.isPrefixMatch(app, "u"))
    }

    @Test
    fun testSettei() {
        val app = AppInfo(
            packageName = "com.android.settings",
            name = "設定",
            searchKeys = normalizer.normalize("設定")
        )

        assertTrue("Matches settei", normalizer.matches(app, "settei"))
        assertTrue("Matches set", normalizer.matches(app, "set"))
        assertTrue("Matches sett", normalizer.matches(app, "sett"))
        assertTrue("Matches se", normalizer.matches(app, "se"))
        assertTrue("Matches 設定", normalizer.matches(app, "設定"))
        assertTrue("Matches せってい", normalizer.matches(app, "せってい"))
        assertTrue("Matches settings (via package)", normalizer.matches(app, "settings"))
    }

    @Test
    fun testKamera() {
        val app = AppInfo(
            packageName = "com.example.photos",
            name = "カメラ",
            searchKeys = normalizer.normalize("カメラ")
        )

        assertTrue("Matches kamera", normalizer.matches(app, "kamera"))
        assertTrue("Matches kame", normalizer.matches(app, "kame"))
        assertTrue("Matches ka", normalizer.matches(app, "ka"))
        assertTrue("Matches me", normalizer.matches(app, "me"))
        assertTrue("Matches ra", normalizer.matches(app, "ra"))
        assertFalse("Should NOT match amera", normalizer.matches(app, "amera"))
    }

    @Test
    fun testEnglishAppNames() {
        val app = AppInfo(
            packageName = "com.android.chrome",
            name = "Chrome",
            searchKeys = normalizer.normalize("Chrome")
        )

        assertTrue("Matches chrome", normalizer.matches(app, "chrome"))
        assertTrue("Matches ch", normalizer.matches(app, "ch"))
        assertTrue("Matches CHROME (case insensitive)", normalizer.matches(app, "CHROME"))
    }
}
