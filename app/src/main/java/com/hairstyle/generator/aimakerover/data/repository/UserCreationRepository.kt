package com.hairstyle.generator.aimakerover.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hairstyle.generator.aimakerover.domain.model.UserCreation
import com.hairstyle.generator.aimakerover.utils.ResultImageHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCreationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val resultImageHelper: ResultImageHelper
) {

    private val creationsDir: File
        get() = File(context.filesDir, CREATIONS_DIR).apply { mkdirs() }

    private val indexFile: File
        get() = File(creationsDir, INDEX_FILE)

    suspend fun saveGeneratedImage(
        imageUrl: String?,
        tempLocalPath: String? = null
    ): UserCreation = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val destFile = File(creationsDir, "$id.jpg")

        when {
            !imageUrl.isNullOrBlank() && imageUrl.startsWith("http", ignoreCase = true) -> {
                val downloaded = resultImageHelper.downloadToCache(imageUrl)
                downloaded.copyTo(destFile, overwrite = true)
            }
            !tempLocalPath.isNullOrBlank() -> {
                val source = File(tempLocalPath)
                require(source.exists()) { "Local image file not found" }
                source.copyTo(destFile, overwrite = true)
            }
            !imageUrl.isNullOrBlank() -> {
                val source = File(imageUrl)
                require(source.exists()) { "Local image file not found" }
                source.copyTo(destFile, overwrite = true)
            }
            else -> error("No image available to save")
        }

        require(destFile.exists() && destFile.length() > 0L) { "Failed to store creation image" }

        val creation = UserCreation(
            id = id,
            localPath = destFile.absolutePath,
            imageUrl = imageUrl?.takeIf { it.startsWith("http", ignoreCase = true) },
            createdAt = System.currentTimeMillis()
        )
        val updated = loadIndex().toMutableList().apply { add(0, creation) }
        saveIndex(updated)
        creation
    }

    fun getAllCreations(): List<UserCreation> {
        return loadIndex()
            .filter { File(it.localPath).exists() }
            .sortedByDescending { it.createdAt }
    }

    fun getCreation(id: String): UserCreation? {
        return getAllCreations().firstOrNull { it.id == id }
    }

    fun deleteCreation(id: String): Boolean {
        val current = loadIndex()
        val target = current.firstOrNull { it.id == id } ?: return false
        runCatching { File(target.localPath).delete() }
        saveIndex(current.filterNot { it.id == id })
        return true
    }

    private fun loadIndex(): List<UserCreation> {
        if (!indexFile.exists()) return emptyList()
        val json = indexFile.readText()
        if (json.isBlank()) return emptyList()
        val type = TypeToken.getParameterized(List::class.java, UserCreation::class.java).type
        return runCatching { gson.fromJson<List<UserCreation>>(json, type) }
            .getOrDefault(emptyList())
    }

    private fun saveIndex(items: List<UserCreation>) {
        indexFile.writeText(gson.toJson(items))
    }

    companion object {
        private const val CREATIONS_DIR = "user_creations"
        private const val INDEX_FILE = "index.json"
    }
}
