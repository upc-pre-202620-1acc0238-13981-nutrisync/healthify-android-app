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

/** Writes a document the person chose (Storage Access Framework). Separate so the repository can be tested on the JVM. */
interface DocumentWriter {

    /**
     * Copies [content] into the document [uri] and closes it. No storage permission is needed: the `CreateDocument`
     * `Uri` already grants it. @throws IOException if it cannot be opened or written.
     */
    suspend fun write(uri: String, content: InputStream)
}

class ContentResolverDocumentWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentWriter {

    override suspend fun write(uri: String, content: InputStream) = withContext(ioDispatcher) {
        // "wt": if the document already existed, it is replaced instead of written over its content.
        val output = context.contentResolver.openOutputStream(uri.toUri(), "wt")
            ?: throw IOException("Cannot open the chosen document")
        output.use { content.copyTo(it) }
        Unit
    }
}
