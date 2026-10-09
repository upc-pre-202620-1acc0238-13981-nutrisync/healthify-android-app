package pe.edu.upc.healthify.features.iam.domain.valueobject

import org.junit.Assert.assertEquals
import org.junit.Test

class IamValueObjectsTest {

    @Test(expected = IllegalArgumentException::class)
    fun `user id must be positive`() {
        UserId(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `session id must be positive`() {
        SessionId(-1)
    }

    @Test
    fun `email is trimmed and lower cased`() {
        assertEquals("ana@mail.com", EmailAddress("  Ana@Mail.COM ").value)
    }

    @Test
    fun `invalid emails are rejected`() {
        listOf("", "ana", "@mail.com", "ana@", "a@b@c", "an a@mail.com").forEach { raw ->
            assertThrows(raw) { EmailAddress(raw) }
        }
    }

    @Test
    fun `person name exposes greeting, full name and initial`() {
        val name = PersonName.of(" maría josé ", " López ")

        assertEquals("maría", name.firstGivenName)
        assertEquals("maría josé López", name.fullName)
        assertEquals('M', name.initial)
    }

    @Test
    fun `person name requires given names, trimmed values and max length`() {
        assertThrows("blank") { PersonName("  ", "López") }
        assertThrows("untrimmed") { PersonName(" Ana", "López") }
        assertThrows("too long") { PersonName("a".repeat(PersonName.MAX_LENGTH + 1), "") }
    }

    @Test
    fun `preferred language defaults to spanish`() {
        assertEquals(PreferredLanguage.ENGLISH, PreferredLanguage.fromCode("EN"))
        assertEquals(PreferredLanguage.SPANISH, PreferredLanguage.fromCode("es"))
        assertEquals(PreferredLanguage.SPANISH, PreferredLanguage.fromCode("fr"))
        assertEquals(PreferredLanguage.SPANISH, PreferredLanguage.fromCode(null))
    }

    private fun assertThrows(case: String, block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException for: $case")
    }
}
