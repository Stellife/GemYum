package com.stel.gemmunch.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.java.TfLite
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "AccelerationTest"

/**
 * Comprehensive test to find all available TFLite acceleration APIs.
 */
class ComprehensiveAccelerationTest(private val context: Context) {
    
    suspend fun runAllTests() = withContext(Dispatchers.IO) {
        Log.i(TAG, "=== Starting Comprehensive TFLite Acceleration Tests ===")
        
        // Test 1: Check module installation
        checkModuleInstallation()
        
        // Test 2: Initialize TfLite with options
        initializeTfLite()
        
        // Test 3: Test all known APIs
        testGooglePlayServicesApis()
        testOrgTensorflowLiteApis()
        testLiteRTApis()
        
        // Test 4: Document missing APIs
        val missingApisTest = DocumentedButMissingApisTest(context)
        missingApisTest.documentMissingApis()
        
        Log.i(TAG, "=== Tests Complete ===")
    }
    
    private suspend fun checkModuleInstallation() = withContext(Dispatchers.IO) {
        Log.i(TAG, "\n--- Module Installation Check ---")
        
        try {
            val moduleInstallClient = ModuleInstall.getClient(context)
            val optionalModuleApi = TfLite.getClient(context)
            
            val availabilityTask = moduleInstallClient.areModulesAvailable(optionalModuleApi)
            val result = Tasks.await(availabilityTask, 5, TimeUnit.SECONDS)
            
            Log.i(TAG, "TfLite modules available: ${result.areModulesAvailable()}")
            
            if (!result.areModulesAvailable()) {
                Log.i(TAG, "Installing TfLite modules...")
                val moduleInstallRequest = ModuleInstallRequest.newBuilder()
                    .addApi(optionalModuleApi)
                    .build()
                
                val installTask = moduleInstallClient.installModules(moduleInstallRequest)
                Tasks.await(installTask, 30, TimeUnit.SECONDS)
                Log.i(TAG, "Module installation requested")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Module installation check failed", e)
        }
    }
    
    private suspend fun initializeTfLite() = withContext(Dispatchers.IO) {
        Log.i(TAG, "\n--- TfLite Initialization ---")
        
        try {
            val initOptions = TfLiteInitializationOptions.builder()
                .setEnableGpuDelegateSupport(true)
                .build()
            
            val initTask = TfLite.initialize(context, initOptions)
            Tasks.await(initTask, 10, TimeUnit.SECONDS)
            
            Log.i(TAG, "TfLite initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "TfLite initialization failed", e)
        }
    }
    
    private fun testGooglePlayServicesApis() {
        Log.i(TAG, "\n--- Google Play Services APIs ---")
        
        // Test what's actually available
        try {
            // GPU availability - we know this works
            val gpuTask = TfLiteGpu.isGpuDelegateAvailable(context)
            Log.i(TAG, "✓ TfLiteGpu.isGpuDelegateAvailable exists")
            
            // Try to access acceleration APIs through reflection
            try {
                val accelerationClass = Class.forName("com.google.android.gms.tflite.acceleration.AccelerationService")
                Log.i(TAG, "✓ AccelerationService class found via reflection")
                Log.i(TAG, "  Methods: ${accelerationClass.declaredMethods.map { it.name }}")
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "✗ AccelerationService class not found")
            }
            
            try {
                val configClass = Class.forName("com.google.android.gms.tflite.acceleration.AccelerationConfig")
                Log.i(TAG, "✓ AccelerationConfig class found via reflection")
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "✗ AccelerationConfig class not found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Play Services APIs", e)
        }
    }
    
    private fun testOrgTensorflowLiteApis() {
        Log.i(TAG, "\n--- org.tensorflow.lite APIs ---")
        
        try {
            // Test InterpreterApi
            try {
                val interpreterClass = Class.forName("org.tensorflow.lite.InterpreterApi")
                Log.i(TAG, "✓ InterpreterApi class found")
                
                // Check for Options
                val optionsClass = Class.forName("org.tensorflow.lite.InterpreterApi\$Options")
                Log.i(TAG, "✓ InterpreterApi.Options class found")
                
                // Check for TfLiteRuntime
                val runtimeClass = Class.forName("org.tensorflow.lite.InterpreterApi\$Options\$TfLiteRuntime")
                Log.i(TAG, "✓ TfLiteRuntime enum found")
                
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "✗ InterpreterApi classes not found: ${e.message}")
            }
            
            // Test acceleration package
            try {
                val validatedConfigClass = Class.forName("org.tensorflow.lite.acceleration.ValidatedAccelerationConfig")
                Log.i(TAG, "✓ ValidatedAccelerationConfig found")
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "✗ ValidatedAccelerationConfig not found")
            }
            
            // Test GPU package
            try {
                val gpuDelegateClass = Class.forName("org.tensorflow.lite.gpu.GpuDelegate")
                Log.i(TAG, "✓ GpuDelegate class found")
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "✗ GpuDelegate class not found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing org.tensorflow.lite APIs", e)
        }
    }
    
    private suspend fun testLiteRTApis() = withContext(Dispatchers.IO) {
        Log.i(TAG, "\n--- LiteRT APIs (com.google.ai.edge.litert) ---")
        
        try {
            // Run the LiteRT test
            val liteRTTest = LiteRTAccelerationTest(context)
            liteRTTest.testLiteRTApis()
            
            // Also check with reflection
            try {
                val gpuDelegateClass = Class.forName("com.google.ai.edge.litert.LiteRTGpuDelegateOptions")
                Log.i(TAG, "✓ LiteRTGpuDelegateOptions class found")
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "✗ LiteRTGpuDelegateOptions not found")
            }
            
            try {
                val accelConfigClass = Class.forName("com.google.ai.edge.litert.acceleration.AccelerationConfig")
                Log.i(TAG, "✓ LiteRT AccelerationConfig class found")
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "✗ LiteRT AccelerationConfig not found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error testing LiteRT APIs", e)
        }
    }
}