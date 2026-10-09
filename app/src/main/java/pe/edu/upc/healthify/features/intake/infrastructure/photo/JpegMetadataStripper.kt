package pe.edu.upc.healthify.features.intake.infrastructure.photo

import java.io.ByteArrayOutputStream

/**
 * Quita de un JPEG todos los segmentos de metadatos antes de subirlo (IN-7: «la IA recibe la imagen sin ningún dato
 * del paciente»): APP1 (EXIF con GPS, fecha, modelo del teléfono, y XMP), APP2 (ICC), APP3…APP13 (IPTC/Photoshop),
 * APP15 y COM (comentarios). Conserva APP0 (JFIF) y APP14 (Adobe, necesario para decodificar ciertos colores), igual
 * que el `IPhotoMetadataStripper` del backend. Los píxeles no se tocan: todo lo que sigue a SOS se copia tal cual.
 *
 * Kotlin puro para probarlo en la JVM. Que el backend vuelva a limpiar es una segunda barrera, no la primera.
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

    /** Si [bytes] empieza con la firma de un JPEG (FF D8 FF). */
    fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 && bytes[0].u() == MARKER_PREFIX && bytes[1].u() == SOI && bytes[2].u() == MARKER_PREFIX

    /** @throws IllegalArgumentException si [jpeg] no es un JPEG bien formado. */
    fun strip(jpeg: ByteArray): ByteArray {
        require(isJpeg(jpeg)) { "Not a JPEG" }
        val out = ByteArrayOutputStream(jpeg.size)
        out.write(jpeg, 0, 2)
        var i = 2
        while (i < jpeg.size) {
            require(jpeg[i].u() == MARKER_PREFIX) { "Corrupt JPEG: expected a marker at $i" }
            // Bytes de relleno 0xFF entre segmentos.
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
                        // Desde aquí son datos de imagen (y posibles escaneos más): se copian enteros.
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

    /** APP1…APP13, APP15 y COM. */
    private fun isMetadata(marker: Int): Boolean =
        marker == COM || marker == APP15 || (marker in (APP0 + 1)..APP15 && marker != APP14)

    private fun Byte.u(): Int = toInt() and 0xFF
}
