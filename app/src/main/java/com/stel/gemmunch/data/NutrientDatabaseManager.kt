package com.stel.gemmunch.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.security.MessageDigest

/**
 * Manages the nutrition database with multiple deployment strategies:
 * 1. Check for embedded asset (if using compressed)
 * 2. Download from CDN if needed
 * 3. Verify integrity with SHA256
 * 4. Handle updates when new version available
 */
class NutrientDatabaseManager(private val context: Context) {
    
    companion object {
        private const val TAG = "NutrientDbManager"
        private const val DB_NAME = "nutrients.db"
        private const val DB_VERSION_KEY = "nutrients_db_version"
        private const val DB_HASH_KEY = "nutrients_db_hash"
        
        // CDN URLs - Replace with your actual CDN/hosting service
        private const val CDN_BASE_URL = "https://your-cdn.com/gemmunch/"
        private const val MANIFEST_URL = "${CDN_BASE_URL}nutrients_manifest.json"
        private const val FULL_DB_URL = "${CDN_BASE_URL}nutrients.db.gz"
        private const val LITE_DB_URL = "${CDN_BASE_URL}nutrients_lite.db.gz"
    }
    
    data class DatabaseManifest(
        @SerializedName("version") val version: String,
        @SerializedName("created_at") val createdAt: String,
        @SerializedName("databases") val databases: Map<String, DatabaseInfo>,
        @SerializedName("hash") val hash: Map<String, String>
    )
    
    data class DatabaseInfo(
        @SerializedName("filename") val filename: String,
        @SerializedName("size_mb") val sizeMb: Double,
        @SerializedName("compressed_filename") val compressedFilename: String,
        @SerializedName("compressed_size_mb") val compressedSizeMb: Double,
        @SerializedName("entry_count") val entryCount: Int
    )
    
    private val client = OkHttpClient()
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("nutrient_db_prefs", Context.MODE_PRIVATE)
    
    /**
     * Ensures the nutrition database is available and up-to-date.
     * @param useLiteVersion Whether to use the smaller lite version
     * @param forceUpdate Whether to force re-download even if current version exists
     */
    suspend fun ensureDatabase(
        useLiteVersion: Boolean = false,
        forceUpdate: Boolean = false
    ): File = withContext(Dispatchers.IO) {
        val dbFile = File(context.filesDir, DB_NAME)
        
        // Step 1: Check if we need to initialize or update
        if (!forceUpdate && dbFile.exists() && dbFile.length() > 0) {
            val currentVersion = prefs.getString(DB_VERSION_KEY, null)
            if (currentVersion != null) {
                Log.i(TAG, "Database exists with version: $currentVersion")
                
                // TODO: Enable update checks when CDN is configured
                // Optional: Check for updates
                // if (shouldCheckForUpdates()) {
                //     checkForUpdates(currentVersion, useLiteVersion)
                // }
                
                return@withContext dbFile
            }
        }
        
        // Step 2: Try to extract from assets first (if you ship compressed version)
        if (tryExtractFromAssets(dbFile)) {
            Log.i(TAG, "Successfully extracted database from assets")
            return@withContext dbFile
        }
        
        // Step 3: Download from CDN
        Log.i(TAG, "Downloading database from CDN...")
        downloadDatabase(dbFile, useLiteVersion)
        
        return@withContext dbFile
    }
    
    /**
     * Attempts to extract database from app assets (if shipped with app)
     */
    private fun tryExtractFromAssets(targetFile: File): Boolean {
        return try {
            // First try uncompressed database
            try {
                context.assets.open("nutrients.db").use { assetStream ->
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { outputStream ->
                        assetStream.copyTo(outputStream)
                    }
                }
                Log.i(TAG, "Successfully copied nutrients.db from assets")
                
                // Store version info
                prefs.edit()
                    .putString(DB_VERSION_KEY, "1.0")
                    .putString(DB_HASH_KEY, "asset_version")
                    .apply()
                
                return true
            } catch (e: Exception) {
                Log.d(TAG, "nutrients.db not found in assets, trying compressed version")
            }
            
            // Try compressed version
            val assetFiles = context.assets.list("") ?: emptyArray()
            val compressedDbAsset = assetFiles.find { 
                it == "nutrients.db.gz" || it == "nutrients_lite.db.gz" 
            }
            
            if (compressedDbAsset != null) {
                Log.i(TAG, "Found compressed database in assets: $compressedDbAsset")
                
                context.assets.open(compressedDbAsset).use { assetStream ->
                    GZIPInputStream(assetStream).use { gzipStream ->
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { outputStream ->
                            gzipStream.copyTo(outputStream)
                        }
                    }
                }
                
                // Calculate and store hash
                val hash = calculateFileHash(targetFile)
                prefs.edit()
                    .putString(DB_VERSION_KEY, "1.0")
                    .putString(DB_HASH_KEY, hash)
                    .apply()
                
                return true
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract database from assets", e)
            false
        }
    }
    
    /**
     * Downloads database from CDN
     */
    private suspend fun downloadDatabase(targetFile: File, useLiteVersion: Boolean) {
        // First, fetch manifest to get latest version info
        val manifest = fetchManifest()
        val dbType = if (useLiteVersion) "lite" else "full"
        val dbInfo = manifest.databases[dbType] 
            ?: throw IOException("Database type '$dbType' not found in manifest")
        
        val downloadUrl = if (useLiteVersion) LITE_DB_URL else FULL_DB_URL
        val expectedHash = manifest.hash[dbType]
            ?: throw IOException("Hash for '$dbType' not found in manifest")
        
        Log.i(TAG, "Downloading ${dbInfo.compressedFilename} (${dbInfo.compressedSizeMb} MB)...")
        
        val request = Request.Builder()
            .url(downloadUrl)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download database: ${response.code}")
            }
            
            val body = response.body ?: throw IOException("Empty response body")
            
            // Download to temp file first
            val tempFile = File(targetFile.parent, "${targetFile.name}.tmp")
            
            body.byteStream().use { input ->
                GZIPInputStream(input).use { gzipStream ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        
                        while (gzipStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                            
                            // Could emit progress here if needed
                            if (totalBytes % (1024 * 1024) == 0L) {
                                Log.d(TAG, "Downloaded ${totalBytes / 1024 / 1024} MB...")
                            }
                        }
                    }
                }
            }
            
            // Verify hash
            val actualHash = calculateFileHash(tempFile)
            if (actualHash != expectedHash) {
                tempFile.delete()
                throw IOException("Database hash mismatch! Expected: $expectedHash, Got: $actualHash")
            }
            
            // Move temp file to final location
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)
            
            // Save version info
            prefs.edit()
                .putString(DB_VERSION_KEY, manifest.version)
                .putString(DB_HASH_KEY, actualHash)
                .apply()
            
            Log.i(TAG, "Successfully downloaded and verified database v${manifest.version}")
        }
    }
    
    /**
     * Fetches the database manifest from CDN
     */
    private suspend fun fetchManifest(): DatabaseManifest = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(MANIFEST_URL)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch manifest: ${response.code}")
            }
            
            val json = response.body?.string() 
                ?: throw IOException("Empty manifest response")
            
            gson.fromJson(json, DatabaseManifest::class.java)
        }
    }
    
    /**
     * Checks if we should check for database updates
     */
    private fun shouldCheckForUpdates(): Boolean {
        // Check once per week
        val lastCheck = prefs.getLong("last_update_check", 0)
        val now = System.currentTimeMillis()
        val weekInMillis = 7 * 24 * 60 * 60 * 1000L
        
        return (now - lastCheck) > weekInMillis
    }
    
    /**
     * Checks for database updates in the background
     */
    private suspend fun checkForUpdates(currentVersion: String, useLiteVersion: Boolean) {
        try {
            val manifest = fetchManifest()
            
            if (manifest.version != currentVersion) {
                Log.i(TAG, "New database version available: ${manifest.version} (current: $currentVersion)")
                // Could show notification or auto-update based on user preference
            }
            
            prefs.edit()
                .putLong("last_update_check", System.currentTimeMillis())
                .apply()
                
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check for updates", e)
        }
    }
    
    /**
     * Calculates SHA256 hash of a file
     */
    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Gets database statistics
     */
    fun getDatabaseStats(): Map<String, Any> {
        val dbFile = File(context.filesDir, DB_NAME)
        
        return mapOf(
            "exists" to dbFile.exists(),
            "size_mb" to if (dbFile.exists()) dbFile.length() / 1024.0 / 1024.0 else 0.0,
            "version" to (prefs.getString(DB_VERSION_KEY, "unknown") ?: "unknown"),
            "hash" to (prefs.getString(DB_HASH_KEY, "unknown") ?: "unknown")
        )
    }
}