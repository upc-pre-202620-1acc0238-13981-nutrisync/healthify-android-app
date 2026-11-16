package pe.edu.upc.healthify.features.carerelationship.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationCodeInput
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.testing.VALID_TOKEN

/** The code typed by hand: judged as it is typed, whitespace ignored, case preserved. */
class InvitationCodeInputTest {

    private fun valid(raw: String): InvitationToken =
        (InvitationCodeInput.evaluate(raw) as InvitationCodeInput.Valid).token

    @Test
    fun `nothing or only whitespace is empty`() {
        assertEquals(InvitationCodeInput.Empty, InvitationCodeInput.evaluate(""))
        assertEquals(InvitationCodeInput.Empty, InvitationCodeInput.evaluate("   \n\t "))
    }

    @Test
    fun `a full token is valid and keeps its exact upper and lower case`() {
        val token = valid(VALID_TOKEN)

        assertEquals(VALID_TOKEN, token.value)
        assertEquals("q3Zl0_8xYb-Tn4Wc2Vd9Rk7Hs1Jm6Pa5Ue0Fi3Gt4Lo", token.value)
        assertFalse(InvitationCodeInput.evaluate(VALID_TOKEN.lowercase()) == InvitationCodeInput.evaluate(VALID_TOKEN))
    }

    @Test
    fun `spaces tabs and line breaks anywhere are ignored`() {
        val grouped = VALID_TOKEN.chunked(4).joinToString(" ")
        val withBreaks = "  ${VALID_TOKEN.take(10)}\n${VALID_TOKEN.drop(10)}\t"

        assertEquals(VALID_TOKEN, valid(grouped).value)
        assertEquals(VALID_TOKEN, valid(withBreaks).value)
    }

    @Test
    fun `the grouped display of a token types back to the same token`() {
        val token = InvitationToken(VALID_TOKEN)

        assertEquals(VALID_TOKEN, valid(token.grouped()).value)
    }

    @Test
    fun `the grouped display has blocks of four`() {
        val groups = InvitationToken("kP3x9Qm2LrT7vB8nZc4WdA").grouped().split(" ")

        assertEquals(listOf("kP3x", "9Qm2", "LrT7", "vB8n", "Zc4W", "dA"), groups)
    }

    @Test
    fun `length boundaries 22 and 64 are valid and one outside is not`() {
        assertTrue(InvitationCodeInput.evaluate("a".repeat(InvitationToken.MIN_LENGTH)) is InvitationCodeInput.Valid)
        assertTrue(InvitationCodeInput.evaluate("a".repeat(InvitationToken.MAX_LENGTH)) is InvitationCodeInput.Valid)
        assertEquals(
            InvitationCodeInput.TooShort(InvitationToken.MIN_LENGTH - 1),
            InvitationCodeInput.evaluate("a".repeat(InvitationToken.MIN_LENGTH - 1)),
        )
        assertEquals(
            InvitationCodeInput.TooLong,
            InvitationCodeInput.evaluate("a".repeat(InvitationToken.MAX_LENGTH + 1)),
        )
    }

    @Test
    fun `too short reports how many characters were typed ignoring spaces`() {
        assertEquals(InvitationCodeInput.TooShort(8), InvitationCodeInput.evaluate("kP3x 9Qm2"))
    }

    @Test
    fun `a character a token never has is reported before the length`() {
        assertEquals(InvitationCodeInput.InvalidCharacters, InvitationCodeInput.evaluate("kP3x9Qm2LrT7vB8nZc4WdA!"))
        assertEquals(InvitationCodeInput.InvalidCharacters, InvitationCodeInput.evaluate("abc.def"))
        assertEquals(InvitationCodeInput.InvalidCharacters, InvitationCodeInput.evaluate("código"))
        assertEquals(InvitationCodeInput.InvalidCharacters, InvitationCodeInput.evaluate("a+b/c="))
    }

    @Test
    fun `base64 url safe symbols are accepted`() {
        assertTrue(InvitationCodeInput.evaluate("abcdefghijklmnopqrstu-_").let { it is InvitationCodeInput.Valid })
    }

    @Test
    fun `a valid input never prints the token`() {
        val text = InvitationCodeInput.evaluate(VALID_TOKEN).toString()

        assertFalse(text.contains(VALID_TOKEN))
        assertFalse(text.contains(VALID_TOKEN.take(8)))
    }

    @Test
    fun `the QR parser keeps its old behavior`() {
        // The typed path ignores inner spaces; the QR path never did, and still does not.
        assertEquals(VALID_TOKEN, InvitationToken.parse("  $VALID_TOKEN  ")?.value)
        assertEquals(null, InvitationToken.parse(VALID_TOKEN.take(10) + " " + VALID_TOKEN.drop(10)))
        assertEquals(null, InvitationToken.parse("WIFI:S:Consultorio;T:WPA;P:secreto;;"))
    }
}
