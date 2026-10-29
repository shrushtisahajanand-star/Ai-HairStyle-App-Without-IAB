package com.hairstyle.changer.aitryon.di

import com.hairstyle.changer.aitryon.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Shared OkHttp client for generate / feedback / videos / image downloads.
 * Retrofit + the old ailabapi service stay commented below.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
}

/*
 * Old ailabapi Retrofit setup (BuildConfig.BASE_URL / API_KEY) — unused.
 * Current generate flow uses LocalHairstyleRepository with the Firebase URL.
 *
import com.hairstyle.changer.aitryon.data.remote.HairstyleApiService
import com.hairstyle.changer.aitryon.data.repository.HairstyleRepositoryImpl
import com.hairstyle.changer.aitryon.domain.repository.HairstyleRepository
import dagger.Binds
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideHairstyleApiService(retrofit: Retrofit): HairstyleApiService =
        retrofit.create(HairstyleApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHairstyleRepository(
        impl: HairstyleRepositoryImpl
    ): HairstyleRepository
}
 */
