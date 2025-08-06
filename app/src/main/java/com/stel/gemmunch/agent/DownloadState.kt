package com.stel.gemmunch.agent

import java.io.File

/**
 * Represents the different states of a multi-file download process.
 */
sealed interface MultiDownloadState {
    /** The app is verifying if the models exist. This is the initial state. */
    data object Checking : MultiDownloadState

    /** The download process has not started because models are missing. */
    data object Idle : MultiDownloadState

    /**
     * The download process is in progress.
     * @param currentFileName The name of the file currently being downloaded.
     * @param progress The progress (0-100) of the current file download.
     * @param downloadedMB The amount downloaded in MB.
     * @param totalMB The total size of the file in MB.
     */
    data class InProgress(
        val currentFileName: String,
        val progress: Int,
        val downloadedMB: Long = 0,
        val totalMB: Long = 0
    ) : MultiDownloadState

    /**
     * The download for all files has completed successfully.
     * @param files A map where the key is a logical model name and the value is the downloaded File object.
     */
    data class AllComplete(val files: Map<String, File>) : MultiDownloadState

    /**
     * The download process failed for one of the files.
     * @param reason A string explaining the reason for the failure.
     */
    data class Failed(val reason: String) : MultiDownloadState
}