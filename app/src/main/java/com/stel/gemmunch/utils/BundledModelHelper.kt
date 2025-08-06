package com.stel.gemmunch.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Helper to copy bundled models from assets to app files directory on first launch
 */
object BundledModelHelper {
    private const val TAG = "BundledModelHelper"
    
    /**
     * Check external storage for sideloaded models
     */
    private fun checkAndCopyFromExternalStorage(context: Context): Boolean {
        try {
            val externalPaths = listOf(
                // Try /data/local/tmp first - it's world-readable
                File("/data/local/tmp"),
                // Then try standard download locations
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GemYum"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ""),
                File("/sdcard/Download/GemYum"),
                File("/sdcard/Download"),
                File("/sdcard/GemYum")
            )
            
            val models = listOf(
                "gemma-3n-E2B-it-int4.task",
                "gemma-3n-E4B-it-int4.task"
            )
            
            for (path in externalPaths) {
                if (!path.exists()) continue
                
                var foundCount = 0
                for (modelName in models) {
                    val sourceFile = File(path, modelName)
                    if (sourceFile.exists() && sourceFile.length() > 1000000) {
                        val targetFile = File(context.filesDir, modelName)
                        
                        if (targetFile.exists() && targetFile.length() == sourceFile.length()) {
                            Log.d(TAG, "Model already copied: $modelName")
                            foundCount++
                            continue
                        }
                        
                        Log.i(TAG, "Found model in ${path.absolutePath}: $modelName (${sourceFile.length() / 1024 / 1024}MB)")
                        Log.i(TAG, "Copying to app storage...")
                        
                        FileInputStream(sourceFile).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                val buffer = ByteArray(1024 * 1024) // 1MB buffer
                                var bytesRead: Int
                                var totalBytes = 0L
                                var lastLogTime = System.currentTimeMillis()
                                
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                    
                                    // Log progress every 5 seconds
                                    val now = System.currentTimeMillis()
                                    if (now - lastLogTime > 5000) {
                                        val progress = (totalBytes * 100 / sourceFile.length()).toInt()
                                        Log.d(TAG, "Copying $modelName: $progress% (${totalBytes / 1024 / 1024}MB / ${sourceFile.length() / 1024 / 1024}MB)")
                                        lastLogTime = now
                                    }
                                }
                                
                                Log.i(TAG, "✅ Successfully copied $modelName (${totalBytes / 1024 / 1024}MB)")
                                foundCount++
                            }
                        }
                    }
                }
                
                if (foundCount == models.size) {
                    Log.i(TAG, "✅ All models copied from external storage!")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking external storage", e)
        }
        return false
    }
    
    /**
     * Check for models in various locations and copy them to files directory if needed
     */
    fun copyBundledModelsIfNeeded(context: Context): Boolean {
        // First check external storage for sideloaded models
        if (checkAndCopyFromExternalStorage(context)) {
            return true
        }
        
        // Then check bundled models in assets
        try {
            val assetManager = context.assets
            val filesDir = context.filesDir
            
            // List of models to check and copy
            val models = listOf(
                "gemma-3n-E2B-it-int4.task",
                "gemma-3n-E4B-it-int4.task"
            )
            
            var allCopied = true
            
            for (modelName in models) {
                val targetFile = File(filesDir, modelName)
                
                // Skip if already exists
                if (targetFile.exists() && targetFile.length() > 1000000) {
                    Log.d(TAG, "Model already exists: $modelName (${targetFile.length()} bytes)")
                    continue
                }
                
                // Try to copy from bundled_models directory first, then models directory
                val assetPaths = listOf(
                    "bundled_models/$modelName",
                    "models/$modelName"
                )
                
                var copied = false
                for (assetPath in assetPaths) {
                    try {
                        Log.d(TAG, "Attempting to copy $assetPath to files directory...")
                        
                        assetManager.open(assetPath).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                val buffer = ByteArray(1024 * 1024) // 1MB buffer
                                var bytesRead: Int
                                var totalBytes = 0L
                                var lastLogTime = System.currentTimeMillis()
                                
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                    
                                    // Log progress every 5 seconds
                                    val now = System.currentTimeMillis()
                                    if (now - lastLogTime > 5000) {
                                        Log.d(TAG, "Copying $modelName: ${totalBytes / 1024 / 1024}MB copied...")
                                        lastLogTime = now
                                    }
                                }
                                
                                Log.i(TAG, "✅ Successfully copied $modelName (${totalBytes} bytes)")
                                copied = true
                            }
                        }
                        break // Success, don't try other paths
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not copy from $assetPath: ${e.message}")
                    }
                }
                
                if (!copied) {
                    Log.w(TAG, "❌ Failed to copy bundled model: $modelName")
                    allCopied = false
                }
            }
            
            return allCopied
        } catch (e: Exception) {
            Log.e(TAG, "Error copying bundled models", e)
            return false
        }
    }
    
    /**
     * Check if bundled models exist in assets
     */
    fun hasBundledModels(context: Context): Boolean {
        return try {
            val assetManager = context.assets
            
            // Check if bundled_models directory exists and has files
            val bundledFiles = assetManager.list("bundled_models") ?: emptyArray()
            val modelFiles = assetManager.list("models") ?: emptyArray()
            
            val hasBundled = bundledFiles.any { it.endsWith(".task") }
            val hasModels = modelFiles.any { it.endsWith(".task") }
            
            Log.d(TAG, "Bundled models check: bundled_models=${bundledFiles.size}, models=${modelFiles.size}")
            
            hasBundled || hasModels
        } catch (e: Exception) {
            Log.w(TAG, "No bundled models found: ${e.message}")
            false
        }
    }
}