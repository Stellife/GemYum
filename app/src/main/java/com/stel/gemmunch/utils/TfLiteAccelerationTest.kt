package com.stel.gemmunch.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.java.TfLite
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
// Using Google Play Services TensorFlow Lite instead of direct org.tensorflow.lite
// import org.tensorflow.lite.InterpreterApi - replaced with Play Services TfLite
// import org.tensorflow.lite.gpu.GpuDelegate - using Play Services equivalents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "TfLiteAccelerationTest"

/**
 * Test class to investigate org.tensorflow.lite acceleration APIs.
 */
class TfLiteAccelerationTest(private val context: Context) {
    
    suspend fun testAccelerationApis() = withContext(Dispatchers.IO) {
        Log.i(TAG, "=== Testing org.tensorflow.lite APIs ===")
        
        try {
            // Initialize TfLite first using Play Services
            initializeTfLiteViaPlayServices()
            
            // Test InterpreterApi Options - commented out due to using Play Services TfLite
            // testInterpreterApiOptions()
            
            // Test acceleration classes via reflection
            testAccelerationClassesViaReflection()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing TfLite APIs", e)
        }
    }
    
    private suspend fun initializeTfLiteViaPlayServices() = withContext(Dispatchers.IO) {
        Log.i(TAG, "\n--- Initializing TfLite via Play Services ---")
        
        try {
            val initOptions = TfLiteInitializationOptions.builder()
                .setEnableGpuDelegateSupport(true)
                .build()
            
            val initTask = TfLite.initialize(context, initOptions)
            Tasks.await(initTask, 10, TimeUnit.SECONDS)
            
            Log.i(TAG, "✓ TfLite initialized via Play Services")
            
        } catch (e: Exception) {
            Log.w(TAG, "✗ Failed to initialize TfLite: ${e.message}")
        }
    }
    
    // Commented out due to using Google Play Services TensorFlow Lite instead
    /*
    private fun testInterpreterApiOptions() {
        Log.i(TAG, "\n--- Testing InterpreterApi.Options ---")
        
        try {
            // Test InterpreterApi Options
            val options = InterpreterApi.Options()
            Log.i(TAG, "✓ InterpreterApi.Options created")
            
            // Set runtime to Play Services
            try {
                options.setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
                Log.i(TAG, "✓ Set runtime to FROM_SYSTEM_ONLY")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to set runtime: ${e.message}")
            }
            
            // Try GPU delegate - commented out due to dependency issues
            Log.w(TAG, "⚠️ GPU delegate testing skipped - GpuDelegate class not available")
            
            // Original test code:
            // try {
            //     val gpuDelegate = GpuDelegate()
            //     options.addDelegate(gpuDelegate)
            //     Log.i(TAG, "✓ GPU delegate added")
            //     gpuDelegate.close()
            // } catch (e: Exception) {
            //     Log.w(TAG, "⚠️ Failed to add GPU delegate: ${e.message}")
            // }
            
            // Set thread count
            options.setNumThreads(4)
            Log.i(TAG, "✓ Set thread count to 4")
            
        } catch (e: Exception) {
            Log.w(TAG, "✗ Failed to test InterpreterApi.Options: ${e.message}")
        }
    }
    */
    
    private fun testAccelerationClassesViaReflection() {
        Log.i(TAG, "\n--- Testing org.tensorflow.lite.acceleration classes ---")
        
        // Test ValidatedAccelerationConfig
        testClassExistence(
            "org.tensorflow.lite.acceleration.ValidatedAccelerationConfig",
            "ValidatedAccelerationConfig"
        )
        
        // Test AccelerationConfig (might not exist in org.tensorflow.lite)
        testClassExistence(
            "org.tensorflow.lite.acceleration.AccelerationConfig",
            "AccelerationConfig (org.tensorflow.lite)"
        )
        
        // Test if there's an AccelerationService in org.tensorflow.lite
        testClassExistence(
            "org.tensorflow.lite.acceleration.AccelerationService",
            "AccelerationService (org.tensorflow.lite)"
        )
        
        // Test GPU delegate factory
        testClassExistence(
            "org.tensorflow.lite.gpu.GpuDelegateFactory",
            "GpuDelegateFactory"
        )
        
        // Test NNAPI delegate
        testClassExistence(
            "org.tensorflow.lite.nnapi.NnApiDelegate",
            "NnApiDelegate"
        )
    }
    
    private fun testClassExistence(className: String, displayName: String) {
        try {
            val clazz = Class.forName(className)
            Log.i(TAG, "✓ $displayName class exists")
            
            // Log some basic info about the class
            val methods = clazz.declaredMethods.size
            val constructors = clazz.declaredConstructors.size
            Log.i(TAG, "  Methods: $methods, Constructors: $constructors")
            
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "✗ $displayName class not found")
        }
    }
}