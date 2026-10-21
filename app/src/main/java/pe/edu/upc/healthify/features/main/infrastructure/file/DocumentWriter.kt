package pe.edu.upc.healthify.features.main.infrastructure.file

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pe.edu.upc.healthify.core.di.IoDispatcher
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/** Escribe un documento que eligió la persona (Storage Access Framework). Separado del repositorio para probar este en la JVM. */
interface DocumentWriter {

    /**
     * Copia [content] en el documento [uri] y lo cierra. No hace falta permiso de almacenamiento: el `Uri` de
     * `CreateDocument` ya lo concede. @throws IOException si no se puede abrir o escribir.
     */
    suspend fun write(uri: String, content: InputStream)
}

class ContentResolverDocumentWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentWriter {

    override suspend fun write(uri: String, content: InputStream) = withContext(ioDispatcher) {
        // "wt": si el documento ya existía, se reemplaza en vez de escribir encima de su contenido.
        val output = context.contentResolver.openOutputStream(uri.toUri(), "wt")
            ?: throw IOException("Cannot open the chosen document")
        output.use { content.copyTo(it) }
        Unit
    }
}
