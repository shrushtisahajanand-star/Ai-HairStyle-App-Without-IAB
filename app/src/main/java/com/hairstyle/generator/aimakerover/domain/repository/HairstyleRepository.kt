//package com.hairstyle.generator.aimakerover.domain.repository
//
///*
// * Old ailabapi repository contract (BuildConfig.API_KEY path) — unused.
// * Current generate flow uses LocalHairstyleRepository.
// *
//import com.hairstyle.generator.aimakerover.domain.model.ResultState
//import kotlinx.coroutines.flow.Flow
//import java.io.File
//
//interface HairstyleRepository {
//
//    fun generateHairstyle(
//        imageFile: File,
//        hairStyle: String,
//        color: String
//    ): Flow<ResultState<String>>
//
//    suspend fun downloadResultImage(url: String): Result<String>
//
//    suspend fun saveRecentPhoto(uri: String)
//
//    fun getRecentPhotos(): Flow<List<String>>
//
//    suspend fun clearSession()
//}
// */
