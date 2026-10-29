package com.hairstyle.changer.aitryon.model

import com.google.gson.annotations.SerializedName

//data class HairstyleSubmitResponse(
//    @SerializedName("request_id") val requestId: String?,
//    @SerializedName("log_id") val logId: String?,
//    @SerializedName("error_code") val errorCode: Int?,
//    @SerializedName("error_msg") val errorMsg: String?,
//    @SerializedName("error_detail") val errorDetail: ErrorDetail?,
//    @SerializedName("task_type") val taskType: String?,
//    @SerializedName("task_id") val taskId: String?
//)
//
//data class AsyncTaskResultResponse(
//    @SerializedName("request_id") val requestId: String?,
//    @SerializedName("log_id") val logId: String?,
//    @SerializedName("error_code") val errorCode: Int?,
//    @SerializedName("error_msg") val errorMsg: String?,
//    @SerializedName("error_detail") val errorDetail: ErrorDetail?,
//    @SerializedName("task_status") val taskStatus: Int?,
//    @SerializedName("data") val data: TaskResultData?
//)

data class TaskResultData(
    @SerializedName("images") val images: List<String>?
)

data class ErrorDetail(
    @SerializedName("status_code") val statusCode: Int?,
    @SerializedName("code") val code: String?,
    @SerializedName("code_message") val codeMessage: String?,
    @SerializedName("message") val message: String?
)
