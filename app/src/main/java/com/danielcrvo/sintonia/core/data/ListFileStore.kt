package com.danielcrvo.sintonia.core.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PickedFile(val path: String, val displayName: String, val sizeBytes: Long, val itemCount: Int)

@Singleton
class ListFileStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val directory get() = File(context.filesDir, "lists").also { it.mkdirs() }

        suspend fun copyFrom(uri: Uri): PickedFile? = withContext(Dispatchers.IO) {
            runCatching {
                val name = displayName(uri) ?: "list.m3u"
                val target = File(directory, "${UUID.randomUUID()}.m3u")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().buffered().use { output -> input.copyTo(output) }
                } ?: return@runCatching null
                PickedFile(target.path, name, target.length(), countItems(target))
            }.getOrNull()
        }

        suspend fun delete(path: String) = withContext(Dispatchers.IO) { File(path).delete() }

        private fun countItems(file: File): Int = file.useLines { lines -> lines.count { it.startsWith("#EXTINF") } }

        private fun displayName(uri: Uri): String? =
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }
