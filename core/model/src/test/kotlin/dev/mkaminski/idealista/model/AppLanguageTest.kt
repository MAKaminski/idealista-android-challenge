package dev.mkaminski.idealista.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `every requested language ships`() {
        assertEquals(
            listOf("en", "es", "fr", "pt", "it"),
            AppLanguage.entries.map { it.tag },
        )
    }

    @Test
    fun `an exact tag resolves`() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromTag("fr"))
    }

    /** A device set to Mexican Spanish must land on Spanish, not fall back to English. */
    @Test
    fun `a regional variant resolves to its primary language`() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es-419"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es-ES"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromTag("pt-BR"))
    }

    /** Some platforms hand back underscores rather than hyphens. */
    @Test
    fun `an underscore separated tag resolves`() {
        assertEquals(AppLanguage.ITALIAN, AppLanguage.fromTag("it_IT"))
    }

    @Test
    fun `case does not matter`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("EN-GB"))
    }

    /** Empty means "no explicit choice" — the system decides, which is not the same as English. */
    @Test
    fun `an empty or absent tag means follow the system`() {
        assertNull(AppLanguage.fromTag(""))
        assertNull(AppLanguage.fromTag("   "))
        assertNull(AppLanguage.fromTag(null))
    }

    @Test
    fun `a language the app does not ship does not resolve`() {
        assertNull(AppLanguage.fromTag("de"))
    }

    /** The picker is only usable if each option is readable to the person who needs it. */
    @Test
    fun `each language is named in its own language`() {
        assertEquals("Español", AppLanguage.SPANISH.endonym)
        assertEquals("Français", AppLanguage.FRENCH.endonym)
        assertEquals("Português", AppLanguage.PORTUGUESE.endonym)
        assertEquals("Italiano", AppLanguage.ITALIAN.endonym)
        assertTrue(AppLanguage.entries.map { it.endonym }.toSet().size == AppLanguage.entries.size)
    }
}
