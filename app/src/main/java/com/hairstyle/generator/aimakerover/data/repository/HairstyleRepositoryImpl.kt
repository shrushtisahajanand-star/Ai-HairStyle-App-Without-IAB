package com.hairstyle.generator.aimakerover.data.repository

/*
 * Old ailabapi repository (BuildConfig.API_KEY via HairstyleRemoteDataSource) — unused.
 * Current generate flow uses LocalHairstyleRepository.
 *
import android.content.Context
import com.hairstyle.generator.aimakerover.data.local.PreferencesManager
import com.hairstyle.generator.aimakerover.data.remote.HairstyleRemoteDataSource
import com.hairstyle.generator.aimakerover.domain.model.ResultState
import com.hairstyle.generator.aimakerover.domain.repository.HairstyleRepository
import com.hairstyle.generator.aimakerover.utils.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HairstyleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteDataSource: HairstyleRemoteDataSource,
    private val preferencesManager: PreferencesManager
) : HairstyleRepository {

    override fun generateHairstyle(
        imageFile: File,
        hairStyle: String,
        color: String
    ): Flow<ResultState<String>> = flow {
        emit(ResultState.Loading)

        try {
            if (!imageFile.exists() || imageFile.length() == 0L) {
                emit(ResultState.Error("Invalid image file. Please select another photo.", canRetry = false))
                return@flow
            }

            if (imageFile.length() > MAX_IMAGE_SIZE_BYTES) {
                emit(ResultState.Error("Image is too large. Please use a smaller photo.", canRetry = false))
                return@flow
            }

            val submitResponse = remoteDataSource.submitHairstyle(imageFile, hairStyle, color)

            // API Call 1: POST hairstyle-editor-pro → returns task_id
            val taskId = submitResponse.taskId ?: run {
                emit(ResultState.Error("Failed to start generation.", canRetry = true))
                return@flow
            }

            val imageUrl = pollForResult(taskId)
            emit(ResultState.Success(imageUrl))
        } catch (e: ApiException) {
            emit(ResultState.Error(e.message ?: "Unknown error", e.canRetry))
        } catch (e: Exception) {
            emit(ResultState.Error(e.message ?: "Something went wrong", canRetry = true))
        }
    }

    private suspend fun pollForResult(taskId: String): String {
        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            val result = remoteDataSource.pollTaskResult(taskId)

            // API Call 2: when images are returned, generation is complete
            val imageUrl = result.data?.images?.firstOrNull { it.isNotBlank() }
            if (!imageUrl.isNullOrBlank()) {
                return imageUrl
            }

            when (result.taskStatus) {
                TASK_QUEUED, TASK_PROCESSING -> {
                    delay(POLL_INTERVAL_MS)
                }
                TASK_COMPLETED -> {
                    throw ApiException("No result image returned from API")
                }
                else -> {
                    // task_status may be absent while processing — keep polling
                    if (attempt < MAX_POLL_ATTEMPTS - 1) {
                        delay(POLL_INTERVAL_MS)
                    }
                }
            }
        }

        throw ApiException("Generation timed out. Please try again.", canRetry = true)
    }

    override suspend fun downloadResultImage(url: String): Result<String> = runCatching {
        val bytes = remoteDataSource.downloadImage(url)
        val cacheDir = File(context.cacheDir, "results")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "result_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }

    override suspend fun saveRecentPhoto(uri: String) {
        preferencesManager.addRecentPhoto(uri)
    }

    override fun getRecentPhotos(): Flow<List<String>> =
        preferencesManager.getRecentPhotos()

    override suspend fun clearSession() {
        // Session is managed in ViewModel; no persistent session storage needed.
    }

    companion object {
        private const val TASK_QUEUED = 0
        private const val TASK_PROCESSING = 1
        private const val TASK_COMPLETED = 2
        private const val POLL_INTERVAL_MS = 3_000L
        private const val MAX_POLL_ATTEMPTS = 40
        private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024L
    }
}
 */
