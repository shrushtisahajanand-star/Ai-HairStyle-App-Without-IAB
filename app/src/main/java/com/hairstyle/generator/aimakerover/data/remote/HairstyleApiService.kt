package com.hairstyle.generator.aimakerover.data.remote

/*
 * Old ailabapi API (BuildConfig.API_KEY) — unused; kept for reference.
 * Current generate flow uses LocalHairstyleRepository + Firebase generate URL.
 *
import com.hairstyle.generator.aimakerover.data.model.AsyncTaskResultResponse
import com.hairstyle.generator.aimakerover.data.model.HairstyleSubmitResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface HairstyleApiService {

    @Multipart
    @POST("api/portrait/effects/hairstyle-editor-pro")
    suspend fun submitHairstyle(
        @Header("ailabapi-api-key") apiKey: String,
        @Part image: MultipartBody.Part,
        @Part("hair_style") hairStyle: RequestBody,
        @Part("color") color: RequestBody
    ): Response<HairstyleSubmitResponse>

    @GET("api/common/query-async-task-result")
    suspend fun queryTaskResult(
        @Header("ailabapi-api-key") apiKey: String,
        @Query("task_id") taskId: String
    ): Response<AsyncTaskResultResponse>
}
 */
