package com.stel.gemmunch.agent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "ModelDownloader"

/**
 * A singleton object to handle the downloading of AI model assets.
 */
object ModelDownloader {

    private val client = OkHttpClient()

    /**
     * Downloads a list of models sequentially, emitting the overall progress.
     * It checks for existing files and skips them if they are already present.
     *
     * @param context The application context.
     * @param modelsToDownload A list of [ModelAsset] objects to be downloaded.
     * @return A Flow that emits the [MultiDownloadState] of the download process.
     */
    fun downloadAllModels(
        context: Context,
        modelsToDownload: List<ModelAsset>
    ): Flow<MultiDownloadState> = flow {
        val destinationDir = context.filesDir
        val downloadedFiles = mutableMapOf<String, File>()

        for (asset in modelsToDownload) {
            val destinationFile = File(destinationDir, asset.fileName)
            downloadedFiles[asset.logicalName] = destinationFile

            if (destinationFile.exists() && destinationFile.length() > 0) {
                Log.i(TAG, "File '${asset.fileName}' already exists. Skipping download.")
                continue
            }

            // If we reach here, a file needs to be downloaded.
            emit(MultiDownloadState.InProgress(asset.fileName, 0))

            val token = com.stel.gemmunch.BuildConfig.HF_TOKEN
            val request = Request.Builder()
                .url(asset.url)
                .header("Authorization", "Bearer $token")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code} for ${asset.fileName}")

                    val body = response.body ?: throw IOException("Empty body for ${asset.fileName}")
                    val totalBytes = body.contentLength()
                    val totalMB = totalBytes / (1024 * 1024)

                    body.byteStream().use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            val buffer = ByteArray(8_192)
                            var bytesRead: Int
                            var downloadedBytes: Long = 0
                            var lastProgress = -1

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                if (totalBytes > 0) {
                                    val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                    if (progress != lastProgress) {
                                        val downloadedMB = downloadedBytes / (1024 * 1024)
                                        emit(MultiDownloadState.InProgress(
                                            asset.fileName, 
                                            progress,
                                            downloadedMB,
                                            totalMB
                                        ))
                                        lastProgress = progress
                                    }
                                }
                            }
                        }
                    }
                }
                Log.i(TAG, "Model '${asset.fileName}' downloaded successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Model download failed for '${asset.fileName}'", e)
                if (destinationFile.exists()) destinationFile.delete()
                emit(MultiDownloadState.Failed(e.localizedMessage ?: "Unknown error"))
                return@flow // Stop the flow on failure
            }
        }

        // If the loop completes without returning early, all downloads are finished.
        Log.i(TAG, "All models are present on device.")
        emit(MultiDownloadState.AllComplete(downloadedFiles))
    }.flowOn(Dispatchers.IO)
}