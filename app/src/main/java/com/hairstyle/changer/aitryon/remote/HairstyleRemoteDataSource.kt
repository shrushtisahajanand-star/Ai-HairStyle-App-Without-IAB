//package com.hairstyle.changer.aitryon.data.remote
//
///*
// * Old ailabapi remote data source (BuildConfig.API_KEY) — unused; kept for reference.
// * Current generate flow uses LocalHairstyleRepository + Firebase generate URL.
// *
//import com.hairstyle.changer.aitryon.BuildConfig
//import com.hairstyle.changer.aitryon.data.model.AsyncTaskResultResponse
//import com.hairstyle.changer.aitryon.data.model.HairstyleSubmitResponse
//import com.hairstyle.changer.aitryon.utils.ApiException
//import com.hairstyle.changer.aitryon.utils.NetworkUtils
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import java.io.File
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class HairstyleRemoteDataSource @Inject constructor(
//    private val apiService: HairstyleApiService,
//    private val okHttpClient: OkHttpClient,
//    private val networkUtils: NetworkUtils
//) {
//
//    suspend fun submitHairstyle(
//        imageFile: File,
//        hairStyle: String,
//        color: String
//    ): HairstyleSubmitResponse {
//        ensureNetwork()
//
//        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
//        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
//        val styleBody = hairStyle.toRequestBody("text/plain".toMediaTypeOrNull())
//        val colorBody = color.toRequestBody("text/plain".toMediaTypeOrNull())
//
//        val response = apiService.submitHairstyle(
//            apiKey = BuildConfig.API_KEY,
//            image = imagePart,
//            hairStyle = styleBody,
//            color = colorBody
//        )
//
//        return handleResponse(response) { body ->
//            body.errorCode?.takeIf { it != 0 }?.let { code ->
//                throw ApiException(
//                    message = body.errorDetail?.message ?: body.errorMsg ?: "API error",
//                    code = code
//                )
//            }
//            if (body.taskId.isNullOrBlank()) {
//                throw ApiException(
//                    message = body.errorDetail?.message
//                        ?: body.errorMsg
//                        ?: "Invalid API response: missing task ID",
//                    code = body.errorDetail?.statusCode ?: response.code()
//                )
//            }
//            body
//        }
//    }
//
//    suspend fun pollTaskResult(taskId: String): AsyncTaskResultResponse {
//        ensureNetwork()
//
//        val response = apiService.queryTaskResult(
//            apiKey = BuildConfig.API_KEY,
//            taskId = taskId
//        )
//
//        return handleResponse(response) { body ->
//            body.errorCode?.takeIf { it != 0 }?.let { code ->
//                throw ApiException(
//                    message = body.errorDetail?.message ?: body.errorMsg ?: "API error",
//                    code = code
//                )
//            }
//            body
//        }
//    }
//
//    suspend fun downloadImage(url: String): ByteArray {
//        ensureNetwork()
//
//        val request = Request.Builder().url(url).build()
//        okHttpClient.newCall(request).execute().use { response ->
//            if (!response.isSuccessful) {
//                throw ApiException(
//                    message = "Failed to download result image",
//                    code = response.code
//                )
//            }
//            return response.body?.bytes()
//                ?: throw ApiException("Empty image response", response.code)
//        }
//    }
//
//    private fun ensureNetwork() {
//        if (!networkUtils.isNetworkAvailable()) {
//            throw ApiException("No internet connection. Please check your network.", canRetry = true)
//        }
//    }
//
//    private fun <T, R> handleResponse(
//        response: retrofit2.Response<T>,
//        block: (T) -> R
//    ): R where T : Any {
//        if (!response.isSuccessful) {
//            throw mapHttpError(response.code(), response.message())
//        }
//
//        val body = response.body()
//            ?: throw ApiException("Empty response from server", response.code())
//
//        return block(body)
//    }
//
//    private fun mapHttpError(code: Int, message: String): ApiException {
//        val userMessage = when (code) {
//            401 -> "Authentication failed. Invalid API key."
//            403 -> "Access denied. Please try again later."
//            404 -> "Service not found. Please try again."
//            408, 504 -> "Request timed out. Please retry."
//            in 500..599 -> "Server error. Please try again later."
//            else -> message.ifBlank { "API error ($code)" }
//        }
//        return ApiException(userMessage, code, canRetry = code != 401 && code != 403)
//    }
//}
// */
