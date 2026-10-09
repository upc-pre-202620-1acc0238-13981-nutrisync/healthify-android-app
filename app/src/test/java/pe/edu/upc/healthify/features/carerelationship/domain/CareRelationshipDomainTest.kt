package pe.edu.upc.healthify.features.carerelationship.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import pe.edu.upc.healthify.testing.VALID_TOKEN

class CareRelationshipDomainTest {

    @Test
    fun `invitation token accepts base64 url-safe between 22 and 64 chars`() {
        assertEquals(VALID_TOKEN, InvitationToken.parse(VALID_TOKEN)?.value)
        assertEquals("A".repeat(22), InvitationToken.parse("A".repeat(22))?.value)
        assertEquals("a-_9".repeat(16), InvitationToken.parse("a-_9".repeat(16))?.value)
    }

    @Test
    fun `invitation token trims what the camera reads`() {
        assertEquals(VALID_TOKEN, InvitationToken.parse("  $VALID_TOKEN\n")?.value)
    }

    @Test
    fun `anything else is not an invitation`() {
        assertNull(InvitationToken.parse(""))
        assertNull(InvitationToken.parse("A".repeat(21)))
        assertNull(InvitationToken.parse("A".repeat(65)))
        assertNull(InvitationToken.parse("https://example.com/menu?table=4"))
        assertNull(InvitationToken.parse("abc+def/ghi=jklmnopqrstu"))
        assertThrows(IllegalArgumentException::class.java) { InvitationToken("short") }
    }

    @Test
    fun `invitation token never prints its value`() {
        assertFalse(InvitationToken(VALID_TOKEN).toString().contains(VALID_TOKEN))
    }

    @Test
    fun `consent scope is versioned and never blank`() {
        assertTrue(ConsentScope.CURRENT.value.startsWith("consent-v"))
        assertThrows(IllegalArgumentException::class.java) { ConsentScope(" ") }
        assertThrows(IllegalArgumentException::class.java) { ConsentScope("diary,self-weigh-ins") }
    }

    @Test
    fun `care link status follows consent and closure`() {
        assertEquals(PatientLinkStatus.ACTIVE, link(isActive = true, hasConsent = true).patientStatus)
        assertEquals(PatientLinkStatus.PENDING_CONSENT, link().patientStatus)
        assertEquals(PatientLinkStatus.NO_LINK, link(isRevoked = true).patientStatus)
        assertEquals(PatientLinkStatus.NO_LINK, link(isDischarged = true).patientStatus)
    }

    @Test
    fun `an active care link needs consent and cannot be closed`() {
        assertThrows(IllegalArgumentException::class.java) { link(isActive = true, hasConsent = false) }
        assertThrows(IllegalArgumentException::class.java) { link(isActive = true, hasConsent = true, isRevoked = true) }
    }

    private fun link(
        isActive: Boolean = false,
        hasConsent: Boolean = false,
        isRevoked: Boolean = false,
        isDischarged: Boolean = false,
    ) = CareLink(
        id = CareLinkId(5),
        patientId = PatientId(12),
        isActive = isActive,
        hasConsent = hasConsent,
        aiProcessingGranted = false,
        isRevoked = isRevoked,
        isDischarged = isDischarged,
    )
}
