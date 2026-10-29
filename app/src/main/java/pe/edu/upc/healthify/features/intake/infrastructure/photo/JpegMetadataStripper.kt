package pe.edu.upc.healthify.features.intake.infrastructure.photo

import java.io.ByteArrayOutputStream

/**
 * Removes every metadata segment from a JPEG before upload ("the AI receives the image without any patient
 * data"): APP1 (EXIF with GPS, date, phone model, and XMP), APP2 (ICC), APP3…APP13 (IPTC/Photoshop),
 * APP15 and COM (comments). Keeps APP0 (JFIF) and APP14 (Adobe, needed to decode certain colors), like
 * the backend's `IPhotoMetadataStripper`. Pixels are untouched: everything after SOS is copied as is.
 *
 * Pure Kotlin so it can be tested on the JVM. The backend cleaning again is a second barrier, not the first.
 */
object JpegMetadataStripper {

    private const val MARKER_PREFIX = 0xFF
    private const val SOI = 0xD8
    private const val EOI = 0xD9
    private const val SOS = 0xDA
    private const val TEM = 0x01
    private const val RST0 = 0xD0
    private const val RST7 = 0xD7
    private const val APP0 = 0xE0
    private const val APP14 = 0xEE
    private const val APP15 = 0xEF
    private const val COM = 0xFE

    /** Whether [bytes] starts with a JPEG signature (FF D8 FF). */
    fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 && bytes[0].u() == MARKER_PREFIX && bytes[1].u() == SOI && bytes[2].u() == MARKER_PREFIX

    /** @throws IllegalArgumentException if [jpeg] is not a well-formed JPEG. */
    fun strip(jpeg: ByteArray): ByteArray {
        require(isJpeg(jpeg)) { "Not a JPEG" }
        val out = ByteArrayOutputStream(jpeg.size)
        out.write(jpeg, 0, 2)
        var i = 2
        while (i < jpeg.size) {
            require(jpeg[i].u() == MARKER_PREFIX) { "Corrupt JPEG: expected a marker at $i" }
            // 0xFF fill bytes between segments.
            while (i + 1 < jpeg.size && jpeg[i + 1].u() == MARKER_PREFIX) i++
            require(i + 1 < jpeg.size) { "Corrupt JPEG: truncated marker" }
            val marker = jpeg[i + 1].u()
            when {
                marker == EOI -> {
                    out.write(jpeg, i, 2)
                    return out.toByteArray()
                }
                marker == TEM || marker in RST0..RST7 -> {
                    out.write(jpeg, i, 2)
                    i += 2
                }
                else -> {
                    require(i + 3 < jpeg.size) { "Corrupt JPEG: truncated segment length" }
                    val length = (jpeg[i + 2].u() shl 8) or jpeg[i + 3].u()
                    val end = i + 2 + length
                    require(length >= 2 && end <= jpeg.size) { "Corrupt JPEG: bad segment length" }
                    if (marker == SOS) {
                        // From here on it is image data (and possibly more scans): copied whole.
                        out.write(jpeg, i, jpeg.size - i)
                        return out.toByteArray()
                    }
                    if (!isMetadata(marker)) out.write(jpeg, i, end - i)
                    i = end
                }
            }
        }
        return out.toByteArray()
    }

    /** APP1…APP13, APP15 and COM. */
    private fun isMetadata(marker: Int): Boolean =
        marker == COM || marker == APP15 || (marker in (APP0 + 1)..APP15 && marker != APP14)

    private fun Byte.u(): Int = toInt() and 0xFF
}
