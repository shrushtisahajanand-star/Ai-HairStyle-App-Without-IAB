package com.hairstyle.generator.aimakerover.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun createCameraImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = File(context.cacheDir, "camera")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File(storageDir, "IMG_$timeStamp.jpg")
    }

    fun getUriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

    fun getCacheDir(context: Context, subDir: String): File {
        val dir = File(context.cacheDir, subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
