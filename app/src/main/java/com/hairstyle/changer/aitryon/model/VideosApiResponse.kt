package com.hairstyle.changer.aitryon.model

import com.google.gson.annotations.SerializedName

data class VideosApiResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: VideosApiData? = null
)

data class VideosApiData(
    @SerializedName("HomeImages") val homeImages: List<VideoItemDto>? = null,
    @SerializedName("HomeVideo") val homeVideo: List<VideoItemDto>? = null,
    @SerializedName("IntroImages") val introImages: List<VideoItemDto>? = null,
    @SerializedName("IntroVideo") val introVideo: List<VideoItemDto>? = null,
    @SerializedName("FemaleImages") val femaleImages: List<VideoItemDto>? = null,
    @SerializedName("MaleImages") val maleImages: List<VideoItemDto>? = null
)

data class VideoItemDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("url") val url: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("header") val header: String? = null,
    @SerializedName("description") val description: String? = null
)
