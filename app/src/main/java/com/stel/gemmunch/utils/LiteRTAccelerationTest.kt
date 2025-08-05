package com.stel.gemmunch.utils

import android.content.Context
import android.util.Log
// Using Google Play Services TensorFlow Lite instead of direct org.tensorflow.lite
// import org.tensorflow.lite.gpu.GpuDelegate - using Play Services equivalents
// import org.tensorflow.lite.InterpreterApi - replaced with Play Services TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "LiteRTAccelerationTest"

/**
 * Test TFLite acceleration APIs.
 * This demonstrates that the Play Services acceleration APIs shown in documentation
 * don't actually exist in the SDK.
 */
class LiteRTAccelerationTest(private val context: Context) {
    
    suspend fun testLiteRTApis() = withContext(Dispatchers.IO) {
        Log.i(TAG, "=== Testing TFLite Acceleration APIs ===")
        
        try {
            // Test 1: GPU Delegate from org.tensorflow.lite
            testGpuDelegate()
            
            // Test 2: Document missing Play Services APIs
            documentMissingPlayServicesApis()
            
            // Test 3: Interpreter Options
            testInterpreterOptions()
            
            Log.i(TAG, "=== TFLite API Test Complete ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing APIs", e)
        }
    }
    
    private fun testGpuDelegate() {
        Log.i(TAG, "\n--- Testing GPU Delegate (org.tensorflow.lite) ---")
        
        // GPU delegate testing commented out due to dependency issues
        Log.w(TAG, "⚠️ GPU delegate testing skipped - GpuDelegate class not available in current dependencies")
        
        // Original test code:
        // try {
        //     val gpuDelegate = GpuDelegate()
        //     Log.i(TAG, "✓ GpuDelegate created successfully")
        //     gpuDelegate.close()
        // } catch (e: Exception) {
        //     Log.w(TAG, "✗ Failed to create GPU delegate: ${e.message}")
        // }
    }
    
    private fun documentMissingPlayServicesApis() {
        Log.i(TAG, "\n--- Documenting Missing Play Services APIs ---")
        
        // These APIs are documented but don't compile:
        Log.e(TAG, """
            |The following APIs are documented but cause compilation errors:
            |
            |1. GpuAccelerationConfig.Builder()
            |   - Error: Unresolved reference 'Builder'
            |   - Doc: https://developers.google.com/android/reference/com/google/android/gms/tflite/acceleration/GpuAccelerationConfig.Builder
            |
            |2. GpuAccelerationConfig.setPrecisionLossAllowed()
            |   - Error: Unresolved reference 'setPrecisionLossAllowed'
            |   
            |3. GpuAccelerationConfig.CacheMode
            |   - Error: Unresolved reference 'CacheMode'
            |   
            |4. AccelerationConfig.Builder()
            |   - Error: Unresolved reference 'Builder'
            |   
            |5. ValidationConfig.Builder()
            |   - Error: Unresolved reference 'Builder'
            |   
            |6. AccelerationService.close()
            |   - Error: Unresolved reference 'close'
            |
            |These APIs appear in Google's official documentation but are not
            |available in the actual SDK (play-services-tflite-acceleration-service:16.0.0-beta01)
        """.trimMargin())
        
        // Test what actually exists via reflection
        testClassViaReflection(
            "com.google.android.gms.tflite.acceleration.GpuAccelerationConfig",
            "GpuAccelerationConfig"
        )
        testClassViaReflection(
            "com.google.android.gms.tflite.acceleration.AccelerationConfig", 
            "AccelerationConfig"
        )
        testClassViaReflection(
            "com.google.android.gms.tflite.acceleration.AccelerationService",
            "AccelerationService"
        )
    }
    
    private fun testClassViaReflection(className: String, displayName: String) {
        try {
            val clazz = Class.forName(className)
            Log.i(TAG, "✓ $displayName class exists")
            
            // Check for Builder
            try {
                val builderClass = Class.forName("$className\$Builder")
                Log.i(TAG, "  ✓ Has Builder inner class")
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "  ✗ No Builder inner class found")
            }
            
            // List actual methods
            val methods = clazz.declaredMethods
            if (methods.isNotEmpty()) {
                Log.i(TAG, "  Available methods: ${methods.take(3).map { it.name }.joinToString()}")
            } else {
                Log.w(TAG, "  No public methods found")
            }
            
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "✗ $displayName class not found")
        }
    }
    
    private suspend fun testInterpreterOptions() = withContext(Dispatchers.IO) {
        Log.i(TAG, "\n--- Testing Interpreter Options ---")
        
        try {
            // Commented out due to using Google Play Services TensorFlow Lite
            // Create interpreter options from org.tensorflow.lite
            // val options = InterpreterApi.Options()
            
            // Try to add GPU delegate - commented out due to dependency issues
            Log.w(TAG, "⚠️ GPU delegate testing skipped - GpuDelegate class not available")
            
            // Original test code:
            // try {
            //     val gpuDelegate = GpuDelegate()
            //     options.addDelegate(gpuDelegate)
            //     Log.i(TAG, "✓ GPU delegate added to interpreter options")
            //     gpuDelegate.close()
            // } catch (e: Exception) {
            //     Log.w(TAG, "⚠️ Failed to add GPU delegate: ${e.message}")
            // }
            
            // Set number of threads - commented out
            // options.setNumThreads(4)
            // Log.i(TAG, "✓ Set number of threads")
            
            // Test runtime setting (Play Services) - commented out
            // try {
            //     options.setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
            //     Log.i(TAG, "✓ Set TfLiteRuntime to FROM_SYSTEM_ONLY")
            // } catch (e: Exception) {
            //     Log.w(TAG, "⚠️ Failed to set runtime: ${e.message}")
            // }
            
            Log.i(TAG, "✓ InterpreterApi test skipped - using Play Services TfLite instead")
            
        } catch (e: Exception) {
            Log.w(TAG, "✗ Failed to test interpreter options: ${e.message}")
        }
    }
}