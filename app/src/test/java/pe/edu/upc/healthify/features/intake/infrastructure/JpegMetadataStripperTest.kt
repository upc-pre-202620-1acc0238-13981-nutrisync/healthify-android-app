package pe.edu.upc.healthify.features.intake.infrastructure

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.intake.infrastructure.photo.JpegMetadataStripper
import java.io.ByteArrayOutputStream

class JpegMetadataStripperTest {

    private val jfif = segment(0xE0, "JFIF\u0000".toByteArray() + byteArrayOf(1, 1, 0, 0, 1, 0, 1, 0, 0))
    private val exif = segment(0xE1, "Exif\u0000\u0000GPS -12.0464 -77.0428 Pixel 9".toByteArray())
    private val xmp = segment(0xE1, "http://ns.adobe.com/xap/1.0/\u0000<x:xmpmeta>María</x:xmpmeta>".toByteArray())
    private val icc = segment(0xE2, "ICC_PROFILE\u0000profile".toByteArray())
    private val iptc = segment(0xED, "Photoshop 3.0\u0000IPTC".toByteArray())
    private val app15 = segment(0xEF, "vendor".toByteArray())
    private val comment = segment(0xFE, "Tomada en casa de María".toByteArray())
    private val adobe = segment(0xEE, "Adobe\u0000d".toByteArray())
    private val quantization = segment(0xDB, ByteArray(65) { it.toByte() })
    private val frame = segment(0xC0, byteArrayOf(8, 0, 16, 0, 16, 1, 1, 0x11, 0))
    private val huffman = segment(0xC4, ByteArray(20) { 1 })
    private val scanHeader = segment(0xDA, byteArrayOf(1, 1, 0, 0, 0x3F, 0))

    /** Datos de imagen con un marcador de reinicio y un 0xFF escapado (FF 00): se copian tal cual. */
    private val entropy = byteArrayOf(0x12, 0x34, 0xFF.toByte(), 0x00, 0x56, 0xFF.toByte(), 0xD0.toByte(), 0x78)
    private val eoi = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    private val soi = byteArrayOf(0xFF.toByte(), 0xD8.toByte())

    @Test
    fun `removes EXIF, XMP, ICC, IPTC, APP15 and comments`() {
        val jpeg = jpeg(jfif, exif, xmp, icc, iptc, app15, comment, adobe, quantization, frame, huffman)

        val stripped = JpegMetadataStripper.strip(jpeg)

        assertArrayEquals(jpeg(jfif, adobe, quantization, frame, huffman), stripped)
        assertFalse(stripped.containsText("Exif"))
        assertFalse(stripped.containsText("GPS"))
        assertFalse(stripped.containsText("María"))
        assertFalse(stripped.containsText("ICC_PROFILE"))
        assertFalse(stripped.containsText("Photoshop"))
    }

    @Test
    fun `keeps the image data untouched after the start of scan`() {
        val stripped = JpegMetadataStripper.strip(jpeg(exif, quantization, frame))

        val tail = scanHeader + entropy + eoi
        assertArrayEquals(tail, stripped.copyOfRange(stripped.size - tail.size, stripped.size))
        assertTrue(JpegMetadataStripper.isJpeg(stripped))
    }

    @Test
    fun `a clean JPEG comes back identical`() {
        val clean = jpeg(jfif, quantization, frame, huffman)
        assertArrayEquals(clean, JpegMetadataStripper.strip(clean))
    }

    @Test
    fun `fill bytes between segments are tolerated`() {
        val padded = soi + byteArrayOf(0xFF.toByte()) + exif + jfif + scanHeader + entropy + eoi

        val stripped = JpegMetadataStripper.strip(padded)

        assertFalse(stripped.containsText("Exif"))
        assertTrue(stripped.containsText("JFIF"))
    }

    @Test
    fun `rejects anything that is not a well formed JPEG`() {
        assertThrows(IllegalArgumentException::class.java) { JpegMetadataStripper.strip("RIFF....WEBP".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { JpegMetadataStripper.strip(soi + byteArrayOf(0xFF.toByte(), 0xE1.toByte(), 0x7F, 0x00)) }
        assertThrows(IllegalArgumentException::class.java) { JpegMetadataStripper.strip(soi + byteArrayOf(0x00, 0x01)) }
        assertFalse(JpegMetadataStripper.isJpeg(byteArrayOf(0x89.toByte(), 0x50, 0x4E)))
    }

    @Test
    fun `segment lengths include their own two bytes`() {
        assertEquals(2 + 2 + 6, segment(0xFE, "abcdef".toByteArray()).size)
    }

    private fun jpeg(vararg segments: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(soi)
        segments.forEach(out::write)
        out.write(scanHeader)
        out.write(entropy)
        out.write(eoi)
        return out.toByteArray()
    }

    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        val length = payload.size + 2
        return byteArrayOf(0xFF.toByte(), marker.toByte(), (length shr 8).toByte(), (length and 0xFF).toByte()) + payload
    }

    private fun ByteArray.containsText(text: String): Boolean = String(this, Charsets.ISO_8859_1).contains(
        String(text.toByteArray(), Charsets.ISO_8859_1),
    )
}
