package com.danielcrvo.sintonia.core.data.importing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ImportFiles {
    fun tempFile(name: String): File
    fun cleanup()
}

@Singleton
class CacheImportFiles
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ImportFiles {
        private val directory = File(context.cacheDir, "import")

        override fun tempFile(name: String): File = File(directory, name).also { directory.mkdirs() }

        override fun cleanup() {
            directory.deleteRecursively()
        }
    }
