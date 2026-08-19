package dev.plumage.ui.viewer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Writes a post into Pictures/Plumage through MediaStore.
 *
 * Because minSdk is 29 this needs no runtime storage permission: the app owns the
 * entry it creates and scoped storage handles the rest. IS_PENDING is set while the
 * bytes are streaming so the gallery never indexes a half-written file.
 */
@Singleton
class MediaExporter @Inject constructor(
    @Named("image") private val client: OkHttpClient
) {

    sealed interface Result {
        data class Saved(val uri: Uri) : Result
        data class Failed(val reason: String) : Result
    }

    suspend fun export(context: Context, url: String, postId: Long, ext: String): Result =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mime = when (ext.lowercase()) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "plumage_$postId.$ext")
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Plumage"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.Failed("Could not create a file in Pictures.")

            try {
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                response.use { r ->
                    if (!r.isSuccessful) {
                        resolver.delete(uri, null, null)
                        return@withContext Result.Failed("e926 returned ${r.code}.")
                    }
                    val body = r.body ?: run {
                        resolver.delete(uri, null, null)
                        return@withContext Result.Failed("Empty response body.")
                    }
                    resolver.openOutputStream(uri)?.use { out ->
                        body.byteStream().copyTo(out)
                    } ?: run {
                        resolver.delete(uri, null, null)
                        return@withContext Result.Failed("Could not open the file for writing.")
                    }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.Saved(uri)
            } catch (e: IOException) {
                resolver.delete(uri, null, null)
                Result.Failed(e.message ?: "Download failed.")
            }
        }
}
