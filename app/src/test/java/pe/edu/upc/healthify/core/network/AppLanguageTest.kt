package pe.edu.upc.healthify.core.network

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageTest {

    @Test
    fun `only english and spanish are sent, anything else falls back to spanish`() {
        assertEquals("en", AndroidAppLanguageProvider.toSupportedLanguage(Locale.US))
        assertEquals("en", AndroidAppLanguageProvider.toSupportedLanguage(Locale.forLanguageTag("en-GB")))
        assertEquals("es", AndroidAppLanguageProvider.toSupportedLanguage(Locale.forLanguageTag("es-PE")))
        assertEquals("es", AndroidAppLanguageProvider.toSupportedLanguage(Locale.FRENCH))
        assertEquals("es", AndroidAppLanguageProvider.toSupportedLanguage(null))
    }
}
