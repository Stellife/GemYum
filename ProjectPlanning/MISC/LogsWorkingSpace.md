# Current Bootup 
## LOGCAT #2
---------------------------- PROCESS STARTED (11337) for package com.stel.gemmunch.debug ----------------------------
2025-08-04 19:07:05.399 11337-11337 nativeloader            com.stel.gemmunch.debug              D  Load libframework-connectivity-tiramisu-jni.so using APEX ns com_android_tethering for caller /apex/com.android.tethering/javalib/framework-connectivity-t.jar: ok
2025-08-04 19:07:05.431 11337-11337 nativeloader            com.stel.gemmunch.debug              D  Load /data/user/0/com.stel.gemmunch.debug/code_cache/startup_agents/b13c65d9-agent.so using system ns (caller=<unknown>): ok
2025-08-04 19:07:05.437 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              W  hiddenapi: DexFile /data/data/com.stel.gemmunch.debug/code_cache/.studio/instruments-462f9421.jar is in boot class path but is not in a known location
2025-08-04 19:07:05.521 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              W  Redefining intrinsic method java.lang.Thread java.lang.Thread.currentThread(). This may cause the unexpected use of the original definition of java.lang.Thread java.lang.Thread.currentThread()in methods that have already been compiled.
2025-08-04 19:07:05.521 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              W  Redefining intrinsic method boolean java.lang.Thread.interrupted(). This may cause the unexpected use of the original definition of boolean java.lang.Thread.interrupted()in methods that have already been compiled.
2025-08-04 19:07:05.524 11337-11337 ActivityThread          com.stel.gemmunch.debug              I  Relaunch all activities: onCoreSettingsChange
2025-08-04 19:07:05.832 11337-11337 nativeloader            com.stel.gemmunch.debug              D  Configuring clns-9 for other apk /data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk. target_sdk_version=36, uses_libraries=libOpenCL.so:libOpenCL-pixel.so, library_path=/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/lib/arm64:/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.stel.gemmunch.debug
2025-08-04 19:07:05.835 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb4000072458cdf18) locale list changing from [] to [en-US]
2025-08-04 19:07:05.836 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb4000072458d87d8) locale list changing from [] to [en-US]
2025-08-04 19:07:05.837 11337-11337 GraphicsEnvironment     com.stel.gemmunch.debug              V  Currently set values for:
2025-08-04 19:07:05.837 11337-11337 GraphicsEnvironment     com.stel.gemmunch.debug              V    angle_gl_driver_selection_pkgs=[com.android.angle, com.linecorp.b612.android, com.campmobile.snow, com.google.android.apps.tachyon]
2025-08-04 19:07:05.837 11337-11337 GraphicsEnvironment     com.stel.gemmunch.debug              V    angle_gl_driver_selection_values=[angle, native, native, native]
2025-08-04 19:07:05.837 11337-11337 GraphicsEnvironment     com.stel.gemmunch.debug              V  com.stel.gemmunch.debug is not listed in per-application setting
2025-08-04 19:07:05.837 11337-11337 GraphicsEnvironment     com.stel.gemmunch.debug              V  ANGLE allowlist from config:
2025-08-04 19:07:05.837 11337-11337 GraphicsEnvironment     com.stel.gemmunch.debug              V  com.stel.gemmunch.debug is not listed in ANGLE allowlist or settings, returning default
2025-08-04 19:07:05.837 11337-11337 GraphicsEnvironment     com.stel.gemmunch.debug              V  Neither updatable production driver nor prerelease driver is supported.
2025-08-04 19:07:05.855 11337-11337 WM-WrkMgrInitializer    com.stel.gemmunch.debug              D  Initializing WorkManager with default configuration.
2025-08-04 19:07:05.872 11337-11337 WM-PackageManagerHelper com.stel.gemmunch.debug              D  Skipping component enablement for androidx.work.impl.background.systemjob.SystemJobService
2025-08-04 19:07:05.872 11337-11337 WM-Schedulers           com.stel.gemmunch.debug              D  Created SystemJobScheduler and enabled SystemJobService
2025-08-04 19:07:05.874 11337-11351 WM-ForceStopRunnable    com.stel.gemmunch.debug              D  The default process name was not specified.
2025-08-04 19:07:05.876 11337-11351 WM-ForceStopRunnable    com.stel.gemmunch.debug              D  Performing cleanup operations.
2025-08-04 19:07:05.882 11337-11351 ashmem                  com.stel.gemmunch.debug              E  Pinning is deprecated since Android Q. Please use trim or other methods.
2025-08-04 19:07:05.882 11337-11337 GemMunchApplication     com.stel.gemmunch.debug              I  Application onCreate started
2025-08-04 19:07:05.885 11337-11337 GemMunchApplication     com.stel.gemmunch.debug              I  Application onCreate completed in 3ms
2025-08-04 19:07:05.890 11337-11353 DisplayManager          com.stel.gemmunch.debug              I  Choreographer implicitly registered for the refresh rate.
2025-08-04 19:07:05.891 11337-11353 vulkan                  com.stel.gemmunch.debug              D  searching for layers in '/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/lib/arm64'
2025-08-04 19:07:05.891 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb4000072458d7518) locale list changing from [] to [en-US]
2025-08-04 19:07:05.891 11337-11353 vulkan                  com.stel.gemmunch.debug              D  searching for layers in '/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk!/lib/arm64-v8a'
2025-08-04 19:07:05.900 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              E  Invalid resource ID 0x00000000.
2025-08-04 19:07:05.903 11337-11337 CompatChangeReporter    com.stel.gemmunch.debug              D  Compat change id reported: 377864165; UID 10285; state: ENABLED
2025-08-04 19:07:05.904 11337-11337 DesktopModeFlags        com.stel.gemmunch.debug              D  Toggle override initialized to: OVERRIDE_UNSET
2025-08-04 19:07:05.919 11337-11337 ContentCaptureHelper    com.stel.gemmunch.debug              I  Setting logging level to OFF
2025-08-04 19:07:05.928 11337-11361 WM-PackageManagerHelper com.stel.gemmunch.debug              D  Skipping component enablement for androidx.work.impl.background.systemalarm.RescheduleReceiver
2025-08-04 19:07:06.035 11337-11368 AppContainer            com.stel.gemmunch.debug              I  AppContainer initialization started...
2025-08-04 19:07:06.036 11337-11368 AppContainer            com.stel.gemmunch.debug              I  Initializing GEMMA_3N_E4B_MODEL with AccelerationService (Golden Path)...
2025-08-04 19:07:06.036 11337-11368 PlayServicesAccel       com.stel.gemmunch.debug              I  === AccelerationService: Getting Validated Configuration (Golden Path) ===
2025-08-04 19:07:06.038 11337-11337 MainViewModel           com.stel.gemmunch.debug              I  Health Connect available: true, permissions granted: true
2025-08-04 19:07:06.051 11337-11368 DynamiteModule          com.stel.gemmunch.debug              W  Local module descriptor class for com.google.android.gms.tflite_dynamite not found.
2025-08-04 19:07:06.084 11337-11368 .gemmunch.debug         com.stel.gemmunch.debug              W  ClassLoaderContext classpath size mismatch. expected=1, found=0 (DLC[];PCL[base.apk*4138776503]{PCL[/system/framework/org.apache.http.legacy.jar*4247870504]#PCL[/system/framework/com.android.media.remotedisplay.jar*487574312]#PCL[/system/framework/com.android.location.provider.jar*1570284764]#PCL[/system_ext/framework/org.carconnectivity.android.digitalkey.timesync.jar*889882842]#PCL[/system_ext/framework/androidx.window.extensions.jar*1030441313]#PCL[/system_ext/framework/androidx.window.sidecar.jar*3860983653]} | DLC[];PCL[])
2025-08-04 19:07:06.087 11337-11368 DynamiteModule          com.stel.gemmunch.debug              I  Considering local module com.google.android.gms.tflite_dynamite:0 and remote module com.google.android.gms.tflite_dynamite:243930801
2025-08-04 19:07:06.087 11337-11368 DynamiteModule          com.stel.gemmunch.debug              I  Selected remote version of com.google.android.gms.tflite_dynamite, version >= 243930801
2025-08-04 19:07:06.087 11337-11368 DynamiteModule          com.stel.gemmunch.debug              V  Dynamite loader version >= 2, using loadModule2NoCrashUtils
2025-08-04 19:07:06.108 11337-11368 System                  com.stel.gemmunch.debug              W  ClassLoader referenced unknown path:
2025-08-04 19:07:06.108 11337-11368 nativeloader            com.stel.gemmunch.debug              D  Configuring clns-10 for other apk . target_sdk_version=36, uses_libraries=, library_path=/data/app/~~9OPS7GJJ3k-S5ZaR19eAlg==/com.google.android.gms-adynyJ8tUfRkwC66scg_Cg==/lib/arm64:/data/app/~~9OPS7GJJ3k-S5ZaR19eAlg==/com.google.android.gms-adynyJ8tUfRkwC66scg_Cg==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.google.android.gms
2025-08-04 19:07:06.111 11337-11368 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb4000072458d9138) locale list changing from [] to [en-US]
2025-08-04 19:07:06.123 11337-11368 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb4000072458d7838) locale list changing from [] to [en-US]
2025-08-04 19:07:06.125 11337-11368 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb4000072458d7e78) locale list changing from [] to [en-US]
2025-08-04 19:07:06.130 11337-11368 DynamiteModule          com.stel.gemmunch.debug              W  Local module descriptor class for com.google.android.gms.googlecertificates not found.
2025-08-04 19:07:06.132 11337-11368 DynamiteModule          com.stel.gemmunch.debug              I  Considering local module com.google.android.gms.googlecertificates:0 and remote module com.google.android.gms.googlecertificates:7
2025-08-04 19:07:06.133 11337-11368 DynamiteModule          com.stel.gemmunch.debug              I  Selected remote version of com.google.android.gms.googlecertificates, version >= 7
2025-08-04 19:07:06.140 11337-11368 .gemmunch.debug         com.stel.gemmunch.debug              W  ClassLoaderContext classpath size mismatch. expected=1, found=3 (DLC[];PCL[base.apk*4138776503]{PCL[/system/framework/org.apache.http.legacy.jar*4247870504]#PCL[/system/framework/com.android.media.remotedisplay.jar*487574312]#PCL[/system/framework/com.android.location.provider.jar*1570284764]#PCL[/system_ext/framework/org.carconnectivity.android.digitalkey.timesync.jar*889882842]#PCL[/system_ext/framework/androidx.window.extensions.jar*1030441313]#PCL[/system_ext/framework/androidx.window.sidecar.jar*3860983653]} | DLC[];PCL[/data/data/com.stel.gemmunch.debug/code_cache/.overlay/base.apk/classes6.dex*1534876455:/data/data/com.stel.gemmunch.debug/code_cache/.overlay/base.apk/classes7.dex*62923235:/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk*2031929841])
2025-08-04 19:07:06.143 11337-11368 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb4000072458d71f8) locale list changing from [] to [en-US]
2025-08-04 19:07:06.192 11337-11368 PlayServicesAccel       com.stel.gemmunch.debug              I  ✅ AccelerationService created successfully
2025-08-04 19:07:06.192 11337-11368 PlayServicesAccel       com.stel.gemmunch.debug              I  ✅ GpuAccelerationConfig created
2025-08-04 19:07:06.192 11337-11368 PlayServicesAccel       com.stel.gemmunch.debug              I  ✅ Skipping validation - GPU acceleration confirmed available
2025-08-04 19:07:06.193 11337-11368 AppContainer            com.stel.gemmunch.debug              I  🚀 Using AccelerationService GPU configuration (GOLDEN PATH)
2025-08-04 19:07:06.193 11337-11368 AppContainer            com.stel.gemmunch.debug              I  ✅ Creating LlmInference with GPU backend (confidence: 95%)
2025-08-04 19:07:06.267 11337-11368 nativeloader            com.stel.gemmunch.debug              D  Load /data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk!/lib/arm64-v8a/libllm_inference_engine_jni.so using class loader ns clns-9 (caller=/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk!classes11.dex): ok
2025-08-04 19:07:06.351 11337-11337 CameraFoodCaptureScreen com.stel.gemmunch.debug              D  Loaded from ViewModel - photoUniqueId: null, isFromGallery: false
2025-08-04 19:07:06.355 11337-11337 Choreographer           com.stel.gemmunch.debug              I  Skipped 48 frames!  The application may be doing too much work on its main thread.
2025-08-04 19:07:06.393 11337-11337 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 19:07:06.393 11337-11337 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:7d3d39b7: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 19:07:06.512 11337-11381 tflite                  com.stel.gemmunch.debug              I  Initialized TensorFlow Lite runtime.
2025-08-04 19:07:06.653 11337-11381 libc                    com.stel.gemmunch.debug              W  Access denied finding property "ro.mediatek.platform"
2025-08-04 19:07:06.653 11337-11381 libc                    com.stel.gemmunch.debug              W  Access denied finding property "ro.chipname"
2025-08-04 19:07:06.653 11337-11381 libc                    com.stel.gemmunch.debug              W  Access denied finding property "ro.hardware.chipname"
2025-08-04 19:07:06.672 11337-11341 .gemmunch.debug         com.stel.gemmunch.debug              I  Compiler allocated 5111KB to compile void android.view.ViewRootImpl.performTraversals()
2025-08-04 19:07:07.530 11337-11381 tflite                  com.stel.gemmunch.debug              I  Created TensorFlow Lite XNNPACK delegate for CPU.
2025-08-04 19:07:07.530 11337-11381 tflite                  com.stel.gemmunch.debug              I  Replacing 1 out of 19 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 0.
2025-08-04 19:07:07.909 11337-11381 tflite                  com.stel.gemmunch.debug              I  Replacing 3409 out of 3409 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 0.
2025-08-04 19:07:11.097 11337-11400 ProfileInstaller        com.stel.gemmunch.debug              D  Installing profile for com.stel.gemmunch.debug
2025-08-04 19:07:18.099 11337-11381 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 1.
2025-08-04 19:07:21.431 11337-11381 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 2.
2025-08-04 19:07:22.805 11337-11381 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 3.
2025-08-04 19:07:24.126 11337-11381 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 4.
2025-08-04 19:07:25.505 11337-11381 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 5.
2025-08-04 19:07:26.988 11337-11368 AppContainer            com.stel.gemmunch.debug              I  🧠 AccelerationService configuration applied - NPU/GPU automatically selected
2025-08-04 19:07:26.988 11337-11368 AppContainer            com.stel.gemmunch.debug              I  Vision LLM instance created successfully.
2025-08-04 19:07:26.996 11337-11368 AppContainer            com.stel.gemmunch.debug              I  Initializing nutrition database...
2025-08-04 19:07:27.135 11337-11368 NutrientDbManager       com.stel.gemmunch.debug              I  Database exists with version: 1.0
2025-08-04 19:07:27.139 11337-11368 NutrientDbHelper        com.stel.gemmunch.debug              I  Enhanced nutrient database opened from: /data/user/0/com.stel.gemmunch.debug/files/nutrients.db
2025-08-04 19:07:27.139 11337-11368 NutrientDbHelper        com.stel.gemmunch.debug              I  Database stats: {exists=true, size_mb=0.16015625, version=1.0, hash=asset_version}
2025-08-04 19:07:27.139 11337-11368 AppContainer            com.stel.gemmunch.debug              I  Nutrition database initialized successfully
2025-08-04 19:07:27.142 11337-11368 AppContainer            com.stel.gemmunch.debug              I  AppContainer initialization successful in 21107ms
2025-08-04 19:07:27.143 11337-11368 AppContainer            com.stel.gemmunch.debug              I  🚀 Started continuous session pre-warming
2025-08-04 19:07:27.144 11337-11368 AppContainer            com.stel.gemmunch.debug              I  === INITIALIZATION METRICS ===
Total Initialization Time: 21.3s

                                                                                                    📊 ApplicationStartup: 3ms
                                                                                                       └─ CreateAppContainer: 1ms
                                                                                                       └─ InitializePreferences: 1ms
                                                                                                    
                                                                                                    📊 ViewModelInitialization: 21.1s
                                                                                                       └─ CheckModels: 2ms (Found 2/2 models)
                                                                                                       └─ InitializeAppContainer: 21.1s
                                                                                                    
                                                                                                    📊 AIInitialization: 21.1s
                                                                                                       └─ ModelSelection: 1ms (Model: GEMMA_3N_E4B_MODEL, Size: 4201MB)
                                                                                                       └─ AccelerationService: 156ms (Validated config received)
                                                                                                       └─ CreateLlmInference: 20.8s (AccelerationService)
                                                                                                       └─ CreateSessionOptions: 8ms
                                                                                                       └─ NutritionDatabase: 143ms (Success)
                                                                                                       └─ CreatePhotoMealExtractor: 3ms

2025-08-04 19:07:27.144 11337-11368 MainViewModel           com.stel.gemmunch.debug              I  AI Components initialized successfully.
2025-08-04 19:07:27.145 11337-11368 MainViewModel           com.stel.gemmunch.debug              I  === INITIALIZATION METRICS ===
Total Initialization Time: 21.3s

                                                                                                    📊 ApplicationStartup: 3ms
                                                                                                       └─ CreateAppContainer: 1ms
                                                                                                       └─ InitializePreferences: 1ms
                                                                                                    
                                                                                                    📊 ViewModelInitialization: 21.1s
                                                                                                       └─ CheckModels: 2ms (Found 2/2 models)
                                                                                                       └─ InitializeAppContainer: 21.1s
                                                                                                    
                                                                                                    📊 AIInitialization: 21.1s
                                                                                                       └─ ModelSelection: 1ms (Model: GEMMA_3N_E4B_MODEL, Size: 4201MB)
                                                                                                       └─ AccelerationService: 156ms (Validated config received)
                                                                                                       └─ CreateLlmInference: 20.8s (AccelerationService)
                                                                                                       └─ CreateSessionOptions: 8ms
                                                                                                       └─ NutritionDatabase: 143ms (Success)
                                                                                                       └─ CreatePhotoMealExtractor: 3ms

2025-08-04 19:07:27.145 11337-11369 AppContainer            com.stel.gemmunch.debug              D  🔥 Pre-warming session #1...
2025-08-04 19:07:27.161 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              I  Waiting for a blocking GC NativeAlloc
2025-08-04 19:07:27.208 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              I  WaitForGcToComplete blocked NativeAlloc on NativeAlloc for 47.131ms
2025-08-04 19:07:27.211 11337-11344 System                  com.stel.gemmunch.debug              W  A resource failed to call close.
2025-08-04 19:07:27.211 11337-11344 System                  com.stel.gemmunch.debug              W  A resource failed to call SQLiteConnectionPool.close.
2025-08-04 19:07:27.211 11337-11344 SQLiteConnectionPool    com.stel.gemmunch.debug              W  A SQLiteConnection object for database ':memory:' was leaked!  Please fix your application to end transactions in progress properly and to close the database when it is no longer needed.
2025-08-04 19:07:27.212 11337-11344 System                  com.stel.gemmunch.debug              W  A resource failed to call SQLiteConnection.close.
2025-08-04 19:07:27.282 11337-11369 tflite                  com.stel.gemmunch.debug              I  Replacing 1448 out of 1448 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 0.
2025-08-04 19:07:27.288 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              I  Waiting for a blocking GC NativeAlloc
2025-08-04 19:07:27.318 11337-11337 .gemmunch.debug         com.stel.gemmunch.debug              I  WaitForGcToComplete blocked NativeAlloc on NativeAlloc for 29.706ms
2025-08-04 19:07:27.346 11337-11337 AppContainer            com.stel.gemmunch.debug              D  Cannot pre-warm: session already pre-warmed, in progress, or in use
2025-08-04 19:07:32.797 11337-11369 tflite                  com.stel.gemmunch.debug              I  Replacing 18 out of 19 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 0.
2025-08-04 19:07:32.820 11337-11369 tflite                  com.stel.gemmunch.debug              I  Replacing 1 out of 2 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 1.
2025-08-04 19:07:32.824 11337-11369 AppContainer            com.stel.gemmunch.debug              D  ✅ Session #1 pre-warmed and ready in 5679ms
2025-08-04 19:07:34.215 11337-11337 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 19:07:34.215 11337-11337 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:9805ff32: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 19:07:36.836 11337-11337 WindowOnBackDispatcher  com.stel.gemmunch.debug              W  sendCancelIfRunning: isInProgress=false callback=androidx.activity.OnBackPressedDispatcher$Api34Impl$createOnBackAnimationCallback$1@5d1927b
2025-08-04 19:07:36.869 11337-11353 HWUI                    com.stel.gemmunch.debug              D  endAllActiveAnimators on 0xb400007227cb8d30 (UnprojectedRipple) with handle 0xb400007257585ee0
2025-08-04 19:07:36.908 11337-11337 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 19:07:36.908 11337-11337 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:72102db0: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN



## LOGCAT #1 

---------------------------- PROCESS STARTED (12157) for package com.stel.gemmunch.debug ----------------------------
2025-08-04 18:51:29.324 12157-12157 nativeloader            com.stel.gemmunch.debug              D  Load libframework-connectivity-tiramisu-jni.so using APEX ns com_android_tethering for caller /apex/com.android.tethering/javalib/framework-connectivity-t.jar: ok
2025-08-04 18:51:29.342 12157-12157 nativeloader            com.stel.gemmunch.debug              D  Load /data/user/0/com.stel.gemmunch.debug/code_cache/startup_agents/b13c65d9-agent.so using system ns (caller=<unknown>): ok
2025-08-04 18:51:29.346 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              W  hiddenapi: DexFile /data/data/com.stel.gemmunch.debug/code_cache/.studio/instruments-462f9421.jar is in boot class path but is not in a known location
2025-08-04 18:51:29.447 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              W  Redefining intrinsic method java.lang.Thread java.lang.Thread.currentThread(). This may cause the unexpected use of the original definition of java.lang.Thread java.lang.Thread.currentThread()in methods that have already been compiled.
2025-08-04 18:51:29.447 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              W  Redefining intrinsic method boolean java.lang.Thread.interrupted(). This may cause the unexpected use of the original definition of boolean java.lang.Thread.interrupted()in methods that have already been compiled.
2025-08-04 18:51:29.450 12157-12157 ActivityThread          com.stel.gemmunch.debug              I  Relaunch all activities: onCoreSettingsChange
2025-08-04 18:51:29.660 12157-12157 nativeloader            com.stel.gemmunch.debug              D  Configuring clns-9 for other apk /data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk. target_sdk_version=36, uses_libraries=libOpenCL.so:libOpenCL-pixel.so, library_path=/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/lib/arm64:/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.stel.gemmunch.debug
2025-08-04 18:51:29.663 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb400007aeb2319b8) locale list changing from [] to [en-US]
2025-08-04 18:51:29.664 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb400007aeb231ff8) locale list changing from [] to [en-US]
2025-08-04 18:51:29.667 12157-12157 GraphicsEnvironment     com.stel.gemmunch.debug              V  Currently set values for:
2025-08-04 18:51:29.667 12157-12157 GraphicsEnvironment     com.stel.gemmunch.debug              V    angle_gl_driver_selection_pkgs=[com.android.angle, com.linecorp.b612.android, com.campmobile.snow, com.google.android.apps.tachyon]
2025-08-04 18:51:29.667 12157-12157 GraphicsEnvironment     com.stel.gemmunch.debug              V    angle_gl_driver_selection_values=[angle, native, native, native]
2025-08-04 18:51:29.667 12157-12157 GraphicsEnvironment     com.stel.gemmunch.debug              V  com.stel.gemmunch.debug is not listed in per-application setting
2025-08-04 18:51:29.667 12157-12157 GraphicsEnvironment     com.stel.gemmunch.debug              V  ANGLE allowlist from config:
2025-08-04 18:51:29.667 12157-12157 GraphicsEnvironment     com.stel.gemmunch.debug              V  com.stel.gemmunch.debug is not listed in ANGLE allowlist or settings, returning default
2025-08-04 18:51:29.667 12157-12157 GraphicsEnvironment     com.stel.gemmunch.debug              V  Neither updatable production driver nor prerelease driver is supported.
2025-08-04 18:51:29.676 12157-12157 WM-WrkMgrInitializer    com.stel.gemmunch.debug              D  Initializing WorkManager with default configuration.
2025-08-04 18:51:29.694 12157-12157 WM-PackageManagerHelper com.stel.gemmunch.debug              D  Skipping component enablement for androidx.work.impl.background.systemjob.SystemJobService
2025-08-04 18:51:29.694 12157-12157 WM-Schedulers           com.stel.gemmunch.debug              D  Created SystemJobScheduler and enabled SystemJobService
2025-08-04 18:51:29.700 12157-12207 WM-ForceStopRunnable    com.stel.gemmunch.debug              D  The default process name was not specified.
2025-08-04 18:51:29.701 12157-12207 WM-ForceStopRunnable    com.stel.gemmunch.debug              D  Performing cleanup operations.
2025-08-04 18:51:29.709 12157-12207 ashmem                  com.stel.gemmunch.debug              E  Pinning is deprecated since Android Q. Please use trim or other methods.
2025-08-04 18:51:29.711 12157-12157 GemMunchApplication     com.stel.gemmunch.debug              I  Application onCreate started
2025-08-04 18:51:29.713 12157-12157 GemMunchApplication     com.stel.gemmunch.debug              I  Application onCreate completed in 2ms
2025-08-04 18:51:29.718 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb400007aeb233f38) locale list changing from [] to [en-US]
2025-08-04 18:51:29.718 12157-12211 DisplayManager          com.stel.gemmunch.debug              I  Choreographer implicitly registered for the refresh rate.
2025-08-04 18:51:29.719 12157-12211 vulkan                  com.stel.gemmunch.debug              D  searching for layers in '/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/lib/arm64'
2025-08-04 18:51:29.719 12157-12211 vulkan                  com.stel.gemmunch.debug              D  searching for layers in '/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk!/lib/arm64-v8a'
2025-08-04 18:51:29.728 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              E  Invalid resource ID 0x00000000.
2025-08-04 18:51:29.729 12157-12207 WM-ForceStopRunnable    com.stel.gemmunch.debug              D  Application was force-stopped, rescheduling.
2025-08-04 18:51:29.730 12157-12157 CompatChangeReporter    com.stel.gemmunch.debug              D  Compat change id reported: 377864165; UID 10285; state: ENABLED
2025-08-04 18:51:29.732 12157-12157 DesktopModeFlags        com.stel.gemmunch.debug              D  Toggle override initialized to: OVERRIDE_UNSET
2025-08-04 18:51:29.744 12157-12157 ContentCaptureHelper    com.stel.gemmunch.debug              I  Setting logging level to OFF
2025-08-04 18:51:29.754 12157-12223 WM-PackageManagerHelper com.stel.gemmunch.debug              D  Skipping component enablement for androidx.work.impl.background.systemalarm.RescheduleReceiver
2025-08-04 18:51:29.840 12157-12238 AppContainer            com.stel.gemmunch.debug              I  AppContainer initialization started...
2025-08-04 18:51:29.840 12157-12238 AppContainer            com.stel.gemmunch.debug              I  Initializing GEMMA_3N_E4B_MODEL with AccelerationService (Golden Path)...
2025-08-04 18:51:29.841 12157-12238 PlayServicesAccel       com.stel.gemmunch.debug              I  === AccelerationService: Getting Validated Configuration (Golden Path) ===
2025-08-04 18:51:29.843 12157-12157 MainViewModel           com.stel.gemmunch.debug              I  Health Connect available: true, permissions granted: true
2025-08-04 18:51:29.851 12157-12238 DynamiteModule          com.stel.gemmunch.debug              W  Local module descriptor class for com.google.android.gms.tflite_dynamite not found.
2025-08-04 18:51:29.869 12157-12238 .gemmunch.debug         com.stel.gemmunch.debug              W  ClassLoaderContext classpath size mismatch. expected=1, found=0 (DLC[];PCL[base.apk*4138776503]{PCL[/system/framework/org.apache.http.legacy.jar*4247870504]#PCL[/system/framework/com.android.media.remotedisplay.jar*487574312]#PCL[/system/framework/com.android.location.provider.jar*1570284764]#PCL[/system_ext/framework/org.carconnectivity.android.digitalkey.timesync.jar*889882842]#PCL[/system_ext/framework/androidx.window.extensions.jar*1030441313]#PCL[/system_ext/framework/androidx.window.sidecar.jar*3860983653]} | DLC[];PCL[])
2025-08-04 18:51:29.870 12157-12238 DynamiteModule          com.stel.gemmunch.debug              I  Considering local module com.google.android.gms.tflite_dynamite:0 and remote module com.google.android.gms.tflite_dynamite:243930801
2025-08-04 18:51:29.870 12157-12238 DynamiteModule          com.stel.gemmunch.debug              I  Selected remote version of com.google.android.gms.tflite_dynamite, version >= 243930801
2025-08-04 18:51:29.871 12157-12238 DynamiteModule          com.stel.gemmunch.debug              V  Dynamite loader version >= 2, using loadModule2NoCrashUtils
2025-08-04 18:51:29.879 12157-12238 System                  com.stel.gemmunch.debug              W  ClassLoader referenced unknown path:
2025-08-04 18:51:29.880 12157-12238 nativeloader            com.stel.gemmunch.debug              D  Configuring clns-10 for other apk . target_sdk_version=36, uses_libraries=, library_path=/data/app/~~9OPS7GJJ3k-S5ZaR19eAlg==/com.google.android.gms-adynyJ8tUfRkwC66scg_Cg==/lib/arm64:/data/app/~~9OPS7GJJ3k-S5ZaR19eAlg==/com.google.android.gms-adynyJ8tUfRkwC66scg_Cg==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.google.android.gms
2025-08-04 18:51:29.882 12157-12238 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb400007aeb236af8) locale list changing from [] to [en-US]
2025-08-04 18:51:29.889 12157-12238 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb400007aeb232958) locale list changing from [] to [en-US]
2025-08-04 18:51:29.890 12157-12238 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb400007aeb2335d8) locale list changing from [] to [en-US]
2025-08-04 18:51:29.893 12157-12238 DynamiteModule          com.stel.gemmunch.debug              W  Local module descriptor class for com.google.android.gms.googlecertificates not found.
2025-08-04 18:51:29.895 12157-12238 DynamiteModule          com.stel.gemmunch.debug              I  Considering local module com.google.android.gms.googlecertificates:0 and remote module com.google.android.gms.googlecertificates:7
2025-08-04 18:51:29.895 12157-12238 DynamiteModule          com.stel.gemmunch.debug              I  Selected remote version of com.google.android.gms.googlecertificates, version >= 7
2025-08-04 18:51:29.899 12157-12238 .gemmunch.debug         com.stel.gemmunch.debug              W  ClassLoaderContext classpath size mismatch. expected=1, found=3 (DLC[];PCL[base.apk*4138776503]{PCL[/system/framework/org.apache.http.legacy.jar*4247870504]#PCL[/system/framework/com.android.media.remotedisplay.jar*487574312]#PCL[/system/framework/com.android.location.provider.jar*1570284764]#PCL[/system_ext/framework/org.carconnectivity.android.digitalkey.timesync.jar*889882842]#PCL[/system_ext/framework/androidx.window.extensions.jar*1030441313]#PCL[/system_ext/framework/androidx.window.sidecar.jar*3860983653]} | DLC[];PCL[/data/data/com.stel.gemmunch.debug/code_cache/.overlay/base.apk/classes6.dex*1534876455:/data/data/com.stel.gemmunch.debug/code_cache/.overlay/base.apk/classes7.dex*2986132656:/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk*2031929841])
2025-08-04 18:51:29.901 12157-12238 .gemmunch.debug         com.stel.gemmunch.debug              I  AssetManager2(0xb400007aeb2338f8) locale list changing from [] to [en-US]
2025-08-04 18:51:29.924 12157-12238 PlayServicesAccel       com.stel.gemmunch.debug              I  ✅ AccelerationService created successfully
2025-08-04 18:51:29.924 12157-12238 PlayServicesAccel       com.stel.gemmunch.debug              I  ✅ GpuAccelerationConfig created
2025-08-04 18:51:29.925 12157-12238 PlayServicesAccel       com.stel.gemmunch.debug              I  ✅ Skipping validation - GPU acceleration confirmed available
2025-08-04 18:51:29.925 12157-12238 AppContainer            com.stel.gemmunch.debug              I  🚀 Using AccelerationService GPU configuration (GOLDEN PATH)
2025-08-04 18:51:29.926 12157-12238 AppContainer            com.stel.gemmunch.debug              I  ✅ Creating LlmInference with GPU backend (confidence: 95%)
2025-08-04 18:51:29.956 12157-12238 nativeloader            com.stel.gemmunch.debug              D  Load /data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk!/lib/arm64-v8a/libllm_inference_engine_jni.so using class loader ns clns-9 (caller=/data/app/~~tKLfYrVleAoWuc2ynr8Ytw==/com.stel.gemmunch.debug-YV2B1NlTQop44zSDYvx9oQ==/base.apk!classes11.dex): ok
2025-08-04 18:51:30.062 12157-12263 tflite                  com.stel.gemmunch.debug              I  Initialized TensorFlow Lite runtime.
2025-08-04 18:51:30.093 12157-12263 libc                    com.stel.gemmunch.debug              W  Access denied finding property "ro.mediatek.platform"
2025-08-04 18:51:30.093 12157-12263 libc                    com.stel.gemmunch.debug              W  Access denied finding property "ro.chipname"
2025-08-04 18:51:30.093 12157-12263 libc                    com.stel.gemmunch.debug              W  Access denied finding property "ro.hardware.chipname"
2025-08-04 18:51:30.096 12157-12161 .gemmunch.debug         com.stel.gemmunch.debug              I  Compiler allocated 5111KB to compile void android.view.ViewRootImpl.performTraversals()
2025-08-04 18:51:30.234 12157-12157 CameraFoodCaptureScreen com.stel.gemmunch.debug              D  Loaded from ViewModel - photoUniqueId: null, isFromGallery: false
2025-08-04 18:51:30.240 12157-12157 Choreographer           com.stel.gemmunch.debug              I  Skipped 55 frames!  The application may be doing too much work on its main thread.
2025-08-04 18:51:30.340 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:51:30.340 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:4c5a2268: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:51:30.750 12157-12263 tflite                  com.stel.gemmunch.debug              I  Created TensorFlow Lite XNNPACK delegate for CPU.
2025-08-04 18:51:30.750 12157-12263 tflite                  com.stel.gemmunch.debug              I  Replacing 1 out of 19 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 0.
2025-08-04 18:51:30.873 12157-12263 tflite                  com.stel.gemmunch.debug              I  Replacing 3409 out of 3409 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 0.
2025-08-04 18:51:35.586 12157-12285 ProfileInstaller        com.stel.gemmunch.debug              D  Installing profile for com.stel.gemmunch.debug
2025-08-04 18:52:18.285 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:52:18.285 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:503dbef7: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:52:27.476 12157-12157 WindowOnBackDispatcher  com.stel.gemmunch.debug              W  sendCancelIfRunning: isInProgress=false callback=androidx.activity.OnBackPressedDispatcher$Api34Impl$createOnBackAnimationCallback$1@cc9314b
2025-08-04 18:52:27.512 12157-12211 HWUI                    com.stel.gemmunch.debug              D  endAllActiveAnimators on 0xb400007adb301a00 (UnprojectedRipple) with handle 0xb4000079cc09c0b0
2025-08-04 18:52:27.546 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:52:27.546 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:72feb609: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:52:55.949 12157-12162 .gemmunch.debug         com.stel.gemmunch.debug              I  Background concurrent mark compact GC freed 23MB AllocSpace bytes, 0(0B) LOS objects, 76% free, 7741KB/31MB, paused 391us,3.253ms total 118.813ms
2025-08-04 18:53:08.065 12157-12157 Choreographer           com.stel.gemmunch.debug              I  Skipped 34 frames!  The application may be doing too much work on its main thread.
2025-08-04 18:53:08.153 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:53:08.153 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:6d843237: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:53:11.560 12157-12157 VRI[MainActivity]       com.stel.gemmunch.debug              D  updatePointerIcon called with position out of bounds
2025-08-04 18:53:46.678 12157-12162 .gemmunch.debug         com.stel.gemmunch.debug              I  Background concurrent mark compact GC freed 23MB AllocSpace bytes, 0(0B) LOS objects, 75% free, 7969KB/31MB, paused 422us,5.182ms total 116.125ms
2025-08-04 18:53:48.113 12157-12157 WindowOnBackDispatcher  com.stel.gemmunch.debug              W  sendCancelIfRunning: isInProgress=false callback=androidx.activity.OnBackPressedDispatcher$Api34Impl$createOnBackAnimationCallback$1@bffc9e9
2025-08-04 18:53:48.155 12157-12211 HWUI                    com.stel.gemmunch.debug              D  endAllActiveAnimators on 0xb400007adb301a00 (UnprojectedRipple) with handle 0xb4000079cb2704b0
2025-08-04 18:53:48.209 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:53:48.209 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:54f262df: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:53:59.377 12157-12157 Choreographer           com.stel.gemmunch.debug              I  Skipped 30 frames!  The application may be doing too much work on its main thread.
2025-08-04 18:53:59.432 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:53:59.433 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:66a05608: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:54:02.086 12157-12157 WindowOnBackDispatcher  com.stel.gemmunch.debug              W  sendCancelIfRunning: isInProgress=false callback=androidx.activity.OnBackPressedDispatcher$Api34Impl$createOnBackAnimationCallback$1@a1e1e01
2025-08-04 18:54:02.124 12157-12211 HWUI                    com.stel.gemmunch.debug              D  endAllActiveAnimators on 0xb400007adb2d4900 (UnprojectedRipple) with handle 0xb4000079cc09a010
2025-08-04 18:54:02.176 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:54:02.176 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:8ac9ee29: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:54:05.559 12157-12157 Choreographer           com.stel.gemmunch.debug              I  Skipped 31 frames!  The application may be doing too much work on its main thread.
2025-08-04 18:54:05.631 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:54:05.632 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:293a003: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:54:09.775 12157-12157 WindowOnBackDispatcher  com.stel.gemmunch.debug              W  sendCancelIfRunning: isInProgress=false callback=androidx.activity.OnBackPressedDispatcher$Api34Impl$createOnBackAnimationCallback$1@bef733e
2025-08-04 18:54:09.819 12157-12211 HWUI                    com.stel.gemmunch.debug              D  endAllActiveAnimators on 0xb400007adb325d60 (UnprojectedRipple) with handle 0xb4000079cc09f920
2025-08-04 18:54:09.870 12157-12157 InsetsController        com.stel.gemmunch.debug              D  hide(ime(), fromIme=false)
2025-08-04 18:54:09.870 12157-12157 ImeTracker              com.stel.gemmunch.debug              I  com.stel.gemmunch.debug:9711082a: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:54:50.047 12157-12263 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 1.
2025-08-04 18:54:59.430 12157-12263 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 2.
2025-08-04 18:55:02.464 12157-12263 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 3.
2025-08-04 18:55:06.615 12157-12263 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 4.
2025-08-04 18:55:10.969 12157-12263 tflite                  com.stel.gemmunch.debug              I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 5.
2025-08-04 18:55:15.210 12157-12162 .gemmunch.debug         com.stel.gemmunch.debug              I  Background concurrent mark compact GC freed 23MB AllocSpace bytes, 1(24KB) LOS objects, 75% free, 8608KB/33MB, paused 1.940ms,9.482ms total 139.182ms
2025-08-04 18:55:16.025 12157-12238 AppContainer            com.stel.gemmunch.debug              I  🧠 AccelerationService configuration applied - NPU/GPU automatically selected
2025-08-04 18:55:16.025 12157-12238 AppContainer            com.stel.gemmunch.debug              I  Vision LLM instance created successfully.
2025-08-04 18:55:16.041 12157-12238 AppContainer            com.stel.gemmunch.debug              I  Initializing nutrition database...
2025-08-04 18:55:16.273 12157-12238 NutrientDbManager       com.stel.gemmunch.debug              I  Database exists with version: 1.0
2025-08-04 18:55:16.284 12157-12238 NutrientDbHelper        com.stel.gemmunch.debug              I  Enhanced nutrient database opened from: /data/user/0/com.stel.gemmunch.debug/files/nutrients.db
2025-08-04 18:55:16.285 12157-12238 NutrientDbHelper        com.stel.gemmunch.debug              I  Database stats: {exists=true, size_mb=0.16015625, version=1.0, hash=asset_version}
2025-08-04 18:55:16.285 12157-12238 AppContainer            com.stel.gemmunch.debug              I  Nutrition database initialized successfully
2025-08-04 18:55:16.296 12157-12238 AppContainer            com.stel.gemmunch.debug              I  AppContainer initialization successful in 226455ms
2025-08-04 18:55:16.298 12157-12238 AppContainer            com.stel.gemmunch.debug              I  🚀 Started continuous session pre-warming
2025-08-04 18:55:16.299 12157-12238 AppContainer            com.stel.gemmunch.debug              I  === INITIALIZATION METRICS ===
Total Initialization Time: 226.6s

                                                                                                    📊 ApplicationStartup: 2ms
                                                                                                       └─ CreateAppContainer: 1ms
                                                                                                       └─ InitializePreferences: 1ms
                                                                                                    
                                                                                                    📊 ViewModelInitialization: 226.5s
                                                                                                       └─ CheckModels: 0ms (Found 2/2 models)
                                                                                                       └─ InitializeAppContainer: 226.5s
                                                                                                    
                                                                                                    📊 AIInitialization: 226.5s
                                                                                                       └─ ModelSelection: 0ms (Model: GEMMA_3N_E4B_MODEL, Size: 4201MB)
                                                                                                       └─ AccelerationService: 85ms (Validated config received)
                                                                                                       └─ CreateLlmInference: 226.1s (AccelerationService)
                                                                                                       └─ CreateSessionOptions: 16ms
                                                                                                       └─ NutritionDatabase: 244ms (Success)
                                                                                                       └─ CreatePhotoMealExtractor: 10ms

2025-08-04 18:55:16.300 12157-12240 AppContainer            com.stel.gemmunch.debug              D  🔥 Pre-warming session #1...
2025-08-04 18:55:16.300 12157-12238 MainViewModel           com.stel.gemmunch.debug              I  AI Components initialized successfully.
2025-08-04 18:55:16.300 12157-12238 MainViewModel           com.stel.gemmunch.debug              I  === INITIALIZATION METRICS ===
Total Initialization Time: 226.6s

                                                                                                    📊 ApplicationStartup: 2ms
                                                                                                       └─ CreateAppContainer: 1ms
                                                                                                       └─ InitializePreferences: 1ms
                                                                                                    
                                                                                                    📊 ViewModelInitialization: 226.5s
                                                                                                       └─ CheckModels: 0ms (Found 2/2 models)
                                                                                                       └─ InitializeAppContainer: 226.5s
                                                                                                    
                                                                                                    📊 AIInitialization: 226.5s
                                                                                                       └─ ModelSelection: 0ms (Model: GEMMA_3N_E4B_MODEL, Size: 4201MB)
                                                                                                       └─ AccelerationService: 85ms (Validated config received)
                                                                                                       └─ CreateLlmInference: 226.1s (AccelerationService)
                                                                                                       └─ CreateSessionOptions: 16ms
                                                                                                       └─ NutritionDatabase: 244ms (Success)
                                                                                                       └─ CreatePhotoMealExtractor: 10ms
                                                                                                    
                                                                                                    📊 SessionPrewarming: 1ms

2025-08-04 18:55:16.613 12157-12240 tflite                  com.stel.gemmunch.debug              I  Replacing 1448 out of 1448 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 0.
2025-08-04 18:55:16.683 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              I  Waiting for a blocking GC NativeAlloc
2025-08-04 18:55:16.743 12157-12162 .gemmunch.debug         com.stel.gemmunch.debug              I  NativeAlloc concurrent mark compact GC freed 2247KB AllocSpace bytes, 3(64KB) LOS objects, 75% free, 7878KB/31MB, paused 411us,3.309ms total 303.597ms
2025-08-04 18:55:16.743 12157-12157 .gemmunch.debug         com.stel.gemmunch.debug              I  WaitForGcToComplete blocked NativeAlloc on NativeAlloc for 60.463ms
2025-08-04 18:55:16.767 12157-12164 System                  com.stel.gemmunch.debug              W  A resource failed to call close.
2025-08-04 18:55:16.767 12157-12164 System                  com.stel.gemmunch.debug              W  A resource failed to call SQLiteConnectionPool.close.
2025-08-04 18:55:16.768 12157-12164 SQLiteConnectionPool    com.stel.gemmunch.debug              W  A SQLiteConnection object for database ':memory:' was leaked!  Please fix your application to end transactions in progress properly and to close the database when it is no longer needed.
2025-08-04 18:55:16.768 12157-12164 System                  com.stel.gemmunch.debug              W  A resource failed to call SQLiteConnection.close.
2025-08-04 18:55:16.857 12157-12157 AppContainer            com.stel.gemmunch.debug              D  Cannot pre-warm: session already pre-warmed, in progress, or in use
2025-08-04 18:55:16.881 12157-12157 Choreographer           com.stel.gemmunch.debug              I  Skipped 68 frames!  The application may be doing too much work on its main thread.
2025-08-04 18:55:33.349 12157-12240 tflite                  com.stel.gemmunch.debug              I  Replacing 18 out of 19 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 0.
2025-08-04 18:55:33.404 12157-12240 tflite                  com.stel.gemmunch.debug              I  Replacing 1 out of 2 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 1.
2025-08-04 18:55:33.417 12157-12240 AppContainer            com.stel.gemmunch.debug              D  ✅ Session #1 pre-warmed and ready in 17117ms

==============================================================================================================================================================================
# PREVIOUS APP LOGCAT BOOTUP

2025-08-04 18:58:21.433  1415-2824  ActivityTaskManager     system_server                        I  START u0 {act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 xflg=0x4 pkg=com.stel.gemmunch cmp=com.stel.gemmunch/.ui.MainActivity bnds=[426,548][582,804]} with LAUNCH_MULTIPLE from uid 10228 (sr=91022950) (BAL_ALLOW_VISIBLE_WINDOW) result code=0
2025-08-04 18:58:21.433  2160-2183  WindowManagerShell      com.android.systemui                 V  Transition requested (#524): android.os.BinderProxy@51116cd TransitionRequestInfo { type = OPEN, triggerTask = TaskInfo{userId=0 taskId=2661 effectiveUid=10393 displayId=0 isRunning=true baseIntent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 pkg=com.stel.gemmunch cmp=com.stel.gemmunch/.ui.MainActivity } baseActivity=ComponentInfo{com.stel.gemmunch/com.stel.gemmunch.ui.MainActivity} topActivity=ComponentInfo{com.stel.gemmunch/com.stel.gemmunch.ui.MainActivity} origActivity=null realActivity=ComponentInfo{com.stel.gemmunch/com.stel.gemmunch.ui.MainActivity} numActivities=1 lastActiveTime=349306076 supportsMultiWindow=true resizeMode=1 isResizeable=true minWidth=-1 minHeight=-1 defaultMinSize=220 token=WCT{android.window.IWindowContainerToken$Stub$Proxy@cc75b82} topActivityType=1 pictureInPictureParams=null shouldDockBigOverlays=false launchIntoPipHostTaskId=-1 lastParentTaskIdBeforePip=-1 displayCutoutSafeInsets=Rect(0, 149 - 0, 0) topActivityInfo=ActivityInfo{74fc293 com.stel.gemmunch.ui.MainActivity} launchCookies=[android.os.BinderProxy@af0d2d0] positionInParent=Point(0, 0) parentTaskId=-1 isFocused=false isVisible=false isVisibleRequested=false isTopActivityNoDisplay=false isSleeping=false locusId=null displayAreaFeatureId=1 isTopActivityTransparent=false isActivityStackTransparent=false lastNonFullscreenBounds=Rect(256, 665 - 753, 1673) capturedLink=null capturedLinkTimestamp=0 requestedVisibleTypes=-9 topActivityRequestOpenInBrowserEducationTimestamp=0 appCompatTaskInfo=AppCompatTaskInfo { topActivityInSizeCompat=false eligibleForLetterboxEducation= false isLetterboxEducationEnabled= false isLetterboxDoubleTapEnabled= false eligibleForUserAspectRatioButton= false topActivityBoundsLetterboxed= false isFromLetterboxDoubleTap= false topActivityLetterboxVerticalPosition= -1 topActivityLetterboxHorizontalPosition= -1 topActivityLetterboxWidth=-1 topActivityLetterboxHeight=-1 topActivityAppBounds=Rect(0, 0 - 1008, 2244) isUserFullscreenOverrideEnabled=false isSystemFullscreenOverrideEnabled=false hasMinAspectRatioOverride=false topActivityLetterboxBounds=null cameraCompatTaskInfo=CameraCompatTaskInfo { freeformCameraCompatMode=inactive}} topActivityMainWindowFrame=null}, pipChange = null, remoteTransition = RemoteTransition { remoteTransition = android.window.IRemoteTransition$Stub$Proxy@5a552c9, appThread = android.app.IApplicationThread$Stub$Proxy@713adce, debugName = QuickstepLaunch }, displayChange = null, flags = 0, debugId = 524 }
2025-08-04 18:58:21.459  1415-1518  ActivityManager         system_server                        I  Start proc 13969:com.stel.gemmunch/u0a393 for next-top-activity {com.stel.gemmunch/com.stel.gemmunch.ui.MainActivity}
2025-08-04 18:58:21.477  1415-1503  WindowManager           system_server                        V  Sent Transition (#524) createdAt=08-04 18:58:21.425 via request=TransitionRequestInfo { type = OPEN, triggerTask = TaskInfo{userId=0 taskId=2661 effectiveUid=10393 displayId=0 isRunning=true baseIntent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 pkg=com.stel.gemmunch cmp=com.stel.gemmunch/.ui.MainActivity } baseActivity=ComponentInfo{com.stel.gemmunch/com.stel.gemmunch.ui.MainActivity} topActivity=ComponentInfo{com.stel.gemmunch/com.stel.gemmunch.ui.MainActivity} origActivity=null realActivity=ComponentInfo{com.stel.gemmunch/com.stel.gemmunch.ui.MainActivity} numActivities=1 lastActiveTime=349306076 supportsMultiWindow=true resizeMode=1 isResizeable=true minWidth=-1 minHeight=-1 defaultMinSize=220 token=WCT{RemoteToken{67999d9 Task{ecdae6d #2661 type=standard A=10393:com.stel.gemmunch}}} topActivityType=1 pictureInPictureParams=null shouldDockBigOverlays=false launchIntoPipHostTaskId=-1 lastParentTaskIdBeforePip=-1 displayCutoutSafeInsets=Rect(0, 149 - 0, 0) topActivityInfo=ActivityInfo{57ccf9e com.stel.gemmunch.ui.MainActivity} launchCookies=[android.os.BinderProxy@f9aba7f] positionInParent=Point(0, 0) parentTaskId=-1 isFocused=false isVisible=false isVisibleRequested=false isTopActivityNoDisplay=false isSleeping=false locusId=null displayAreaFeatureId=1 isTopActivityTransparent=false isActivityStackTransparent=false lastNonFullscreenBounds=Rect(256, 665 - 753, 1673) capturedLink=null capturedLinkTimestamp=0 requestedVisibleTypes=-9 topActivityRequestOpenInBrowserEducationTimestamp=0 appCompatTaskInfo=AppCompatTaskInfo { topActivityInSizeCompat=false eligibleForLetterboxEducation= false isLetterboxEducationEnabled= false isLetterboxDoubleTapEnabled= false eligibleForUserAspectRatioButton= false topActivityBoundsLetterboxed= false isFromLetterboxDoubleTap= false topActivityLetterboxVerticalPosition= -1 topActivityLetterboxHorizontalPosition= -1 topActivityLetterboxWidth=-1 topActivityLetterboxHeight=-1 topActivityAppBounds=Rect(0, 0 - 1008, 2244) isUserFullscreenOverrideEnabled=false isSystemFullscreenOverrideEnabled=false hasMinAspectRatioOverride=false topActivityLetterboxBounds=null cameraCompatTaskInfo=CameraCompatTaskInfo { freeformCameraCompatMode=inactive}} topActivityMainWindowFrame=null}, pipChange = null, remoteTransition = RemoteTransition { remoteTransition = android.window.IRemoteTransition$Stub$Proxy@6ee4a4c, appThread = android.app.IApplicationThread$Stub$Proxy@e0fa95, debugName = QuickstepLaunch }, displayChange = null, flags = 0, debugId = 524 }
2025-08-04 18:58:21.477  1415-1503  WindowManager           system_server                        V      info={id=524 t=OPEN f=0x0 trk=0 r=[0@Point(0, 0)] c=[
{WCT{RemoteToken{67999d9 Task{ecdae6d #2661 type=standard A=10393:com.stel.gemmunch}}} m=OPEN f=NONE leash=Surface(name=Task=2661)/@0x992187 sb=Rect(0, 0 - 1008, 2244) eb=Rect(0, 0 - 1008, 2244) epz=Point(1008, 2244) d=0 taskParent=-1},
{WCT{RemoteToken{9483bbf Task{7f801bb #1 type=home}}} m=TO_BACK f=SHOW_WALLPAPER leash=Surface(name=Task=1)/@0x877fcd4 sb=Rect(0, 0 - 1008, 2244) eb=Rect(0, 0 - 1008, 2244) epz=Point(1008, 2244) d=0 taskParent=-1},
{m=TO_BACK f=IS_WALLPAPER leash=Surface(name=WallpaperWindowToken{9fc699f showWhenLocked=false})/@0x97e4ce6 sb=Rect(0, 0 - 1008, 2244) eb=Rect(0, 0 - 1008, 2244) epz=Point(1008, 2244) d=0}
]}
2025-08-04 18:58:21.553 13969-13969 nativeloader            com.stel.gemmunch                    D  Load libframework-connectivity-tiramisu-jni.so using APEX ns com_android_tethering for caller /apex/com.android.tethering/javalib/framework-connectivity-t.jar: ok
2025-08-04 18:58:21.589 13969-13969 re-initialized>         com.stel.gemmunch                    W  type=1400 audit(0.0:262464): avc:  granted  { execute } for  path="/data/data/com.stel.gemmunch/code_cache/startup_agents/b13c65d9-agent.so" dev="dm-62" ino=186867 scontext=u:r:untrusted_app:s0:c137,c257,c512,c768 tcontext=u:object_r:app_data_file:s0:c137,c257,c512,c768 tclass=file app=com.stel.gemmunch
2025-08-04 18:58:21.601 13969-13969 nativeloader            com.stel.gemmunch                    D  Load /data/user/0/com.stel.gemmunch/code_cache/startup_agents/b13c65d9-agent.so using system ns (caller=<unknown>): ok
2025-08-04 18:58:21.615 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    W  hiddenapi: DexFile /data/data/com.stel.gemmunch/code_cache/.studio/instruments-462f9421.jar is in boot class path but is not in a known location
2025-08-04 18:58:21.853 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    W  Redefining intrinsic method java.lang.Thread java.lang.Thread.currentThread(). This may cause the unexpected use of the original definition of java.lang.Thread java.lang.Thread.currentThread()in methods that have already been compiled.
2025-08-04 18:58:21.853 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    W  Redefining intrinsic method boolean java.lang.Thread.interrupted(). This may cause the unexpected use of the original definition of boolean java.lang.Thread.interrupted()in methods that have already been compiled.
2025-08-04 18:58:21.863 13969-13969 ActivityThread          com.stel.gemmunch                    I  Relaunch all activities: onCoreSettingsChange
2025-08-04 18:58:21.926 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    W  ClassLoaderContext classpath size mismatch. expected=0, found=3 (PCL[] | PCL[/data/data/com.stel.gemmunch/code_cache/.overlay/base.apk/classes6.dex*2304103673:/data/data/com.stel.gemmunch/code_cache/.overlay/base.apk/classes7.dex*1404607403:/data/data/com.stel.gemmunch/code_cache/.overlay/base.apk/classes8.dex*2859635287])
2025-08-04 18:58:22.066 13969-13969 nativeloader            com.stel.gemmunch                    D  Configuring clns-9 for other apk /data/app/~~iL1Nv7X-epI1Ow3qOtIK0A==/com.stel.gemmunch-wdHa9jcTiCsclxUojyQ41g==/base.apk. target_sdk_version=36, uses_libraries=libOpenCL.so:libOpenCL-pixel.so, library_path=/data/app/~~iL1Nv7X-epI1Ow3qOtIK0A==/com.stel.gemmunch-wdHa9jcTiCsclxUojyQ41g==/lib/arm64:/data/app/~~iL1Nv7X-epI1Ow3qOtIK0A==/com.stel.gemmunch-wdHa9jcTiCsclxUojyQ41g==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.stel.gemmunch
2025-08-04 18:58:22.071 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    I  AssetManager2(0xb400007aeb237458) locale list changing from [] to [en-US]
2025-08-04 18:58:22.074 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    I  AssetManager2(0xb400007aeb232318) locale list changing from [] to [en-US]
2025-08-04 18:58:22.081 13969-13969 GraphicsEnvironment     com.stel.gemmunch                    V  Currently set values for:
2025-08-04 18:58:22.081 13969-13969 GraphicsEnvironment     com.stel.gemmunch                    V    angle_gl_driver_selection_pkgs=[com.android.angle, com.linecorp.b612.android, com.campmobile.snow, com.google.android.apps.tachyon]
2025-08-04 18:58:22.081 13969-13969 GraphicsEnvironment     com.stel.gemmunch                    V    angle_gl_driver_selection_values=[angle, native, native, native]
2025-08-04 18:58:22.081 13969-13969 GraphicsEnvironment     com.stel.gemmunch                    V  com.stel.gemmunch is not listed in per-application setting
2025-08-04 18:58:22.081 13969-13969 GraphicsEnvironment     com.stel.gemmunch                    V  ANGLE allowlist from config:
2025-08-04 18:58:22.081 13969-13969 GraphicsEnvironment     com.stel.gemmunch                    V  com.stel.gemmunch is not listed in ANGLE allowlist or settings, returning default
2025-08-04 18:58:22.081 13969-13969 GraphicsEnvironment     com.stel.gemmunch                    V  Neither updatable production driver nor prerelease driver is supported.
2025-08-04 18:58:22.107 13969-13969 WM-WrkMgrInitializer    com.stel.gemmunch                    D  Initializing WorkManager with default configuration.
2025-08-04 18:58:22.122 13969-13969 WM-PackageManagerHelper com.stel.gemmunch                    D  Skipping component enablement for androidx.work.impl.background.systemjob.SystemJobService
2025-08-04 18:58:22.122 13969-13969 WM-Schedulers           com.stel.gemmunch                    D  Created SystemJobScheduler and enabled SystemJobService
2025-08-04 18:58:22.124 13969-13983 WM-ForceStopRunnable    com.stel.gemmunch                    D  The default process name was not specified.
2025-08-04 18:58:22.125 13969-13983 WM-ForceStopRunnable    com.stel.gemmunch                    D  Performing cleanup operations.
2025-08-04 18:58:22.132 13969-13969 GemMunchApplication     com.stel.gemmunch                    I  Application onCreate started
2025-08-04 18:58:22.139 13969-13983 ashmem                  com.stel.gemmunch                    E  Pinning is deprecated since Android Q. Please use trim or other methods.
2025-08-04 18:58:22.139 13969-13969 GemMunchApplication     com.stel.gemmunch                    I  Application onCreate completed in 7ms
2025-08-04 18:58:22.141  1415-5708  AppsFilter              system_server                        I  interaction: PackageSetting{3a3bd98 com.stel.gemmunch/10393} -> PackageSetting{4739684 com.google.android.apps.nexuslauncher/10228} BLOCKED
2025-08-04 18:58:22.148 13969-13986 DisplayManager          com.stel.gemmunch                    I  Choreographer implicitly registered for the refresh rate.
2025-08-04 18:58:22.150 13969-13986 vulkan                  com.stel.gemmunch                    D  searching for layers in '/data/app/~~iL1Nv7X-epI1Ow3qOtIK0A==/com.stel.gemmunch-wdHa9jcTiCsclxUojyQ41g==/lib/arm64'
2025-08-04 18:58:22.150 13969-13986 vulkan                  com.stel.gemmunch                    D  searching for layers in '/data/app/~~iL1Nv7X-epI1Ow3qOtIK0A==/com.stel.gemmunch-wdHa9jcTiCsclxUojyQ41g==/base.apk!/lib/arm64-v8a'
2025-08-04 18:58:22.150 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    I  AssetManager2(0xb400007aeb2335d8) locale list changing from [] to [en-US]
2025-08-04 18:58:22.168 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    E  Invalid resource ID 0x00000000.
2025-08-04 18:58:22.179 13969-13969 CompatChangeReporter    com.stel.gemmunch                    D  Compat change id reported: 377864165; UID 10393; state: ENABLED
2025-08-04 18:58:22.181 13969-13969 DesktopModeFlags        com.stel.gemmunch                    D  Toggle override initialized to: OVERRIDE_UNSET
2025-08-04 18:58:22.191 13969-13983 WM-ForceStopRunnable    com.stel.gemmunch                    D  Application was force-stopped, rescheduling.
2025-08-04 18:58:22.208 13969-13969 ContentCaptureHelper    com.stel.gemmunch                    I  Setting logging level to OFF
2025-08-04 18:58:22.242  1415-2822  CoreBackPreview         system_server                        D  Window{c2d3dbd u0 com.stel.gemmunch/com.stel.gemmunch.ui.MainActivity}: Setting back callback OnBackInvokedCallbackInfo{mCallback=android.window.IOnBackInvokedCallback$Stub$Proxy@5281b03, mPriority=-1, mIsAnimationCallback=false, mOverrideBehavior=0}
2025-08-04 18:58:22.253 13969-13999 WM-PackageManagerHelper com.stel.gemmunch                    D  Skipping component enablement for androidx.work.impl.background.systemalarm.RescheduleReceiver
2025-08-04 18:58:22.327 13969-14001 AppContainer            com.stel.gemmunch                    I  AppContainer initialization started...
2025-08-04 18:58:22.327 13969-14001 AppContainer            com.stel.gemmunch                    I  Initializing GEMMA_3N_E4B_MODEL with AccelerationService (Golden Path)...
2025-08-04 18:58:22.328 13969-14001 PlayServicesAccel       com.stel.gemmunch                    I  === AccelerationService: Getting Validated Configuration (Golden Path) ===
2025-08-04 18:58:22.332 13969-14001 DynamiteModule          com.stel.gemmunch                    W  Local module descriptor class for com.google.android.gms.tflite_dynamite not found.
2025-08-04 18:58:22.333 13969-13969 MainViewModel           com.stel.gemmunch                    I  Health Connect available: true, permissions granted: true
2025-08-04 18:58:22.365 13969-14001 m.stel.gemmunch         com.stel.gemmunch                    W  ClassLoaderContext classpath size mismatch. expected=1, found=0 (DLC[];PCL[base.apk*4138776503]{PCL[/system/framework/org.apache.http.legacy.jar*4247870504]#PCL[/system/framework/com.android.media.remotedisplay.jar*487574312]#PCL[/system/framework/com.android.location.provider.jar*1570284764]#PCL[/system_ext/framework/org.carconnectivity.android.digitalkey.timesync.jar*889882842]#PCL[/system_ext/framework/androidx.window.extensions.jar*1030441313]#PCL[/system_ext/framework/androidx.window.sidecar.jar*3860983653]} | DLC[];PCL[])
2025-08-04 18:58:22.369 13969-14001 DynamiteModule          com.stel.gemmunch                    I  Considering local module com.google.android.gms.tflite_dynamite:0 and remote module com.google.android.gms.tflite_dynamite:243930801
2025-08-04 18:58:22.369 13969-14001 DynamiteModule          com.stel.gemmunch                    I  Selected remote version of com.google.android.gms.tflite_dynamite, version >= 243930801
2025-08-04 18:58:22.370 13969-14001 DynamiteModule          com.stel.gemmunch                    V  Dynamite loader version >= 2, using loadModule2NoCrashUtils
2025-08-04 18:58:22.403 13969-14001 System                  com.stel.gemmunch                    W  ClassLoader referenced unknown path:
2025-08-04 18:58:22.404 13969-14001 nativeloader            com.stel.gemmunch                    D  Configuring clns-10 for other apk . target_sdk_version=36, uses_libraries=, library_path=/data/app/~~9OPS7GJJ3k-S5ZaR19eAlg==/com.google.android.gms-adynyJ8tUfRkwC66scg_Cg==/lib/arm64:/data/app/~~9OPS7GJJ3k-S5ZaR19eAlg==/com.google.android.gms-adynyJ8tUfRkwC66scg_Cg==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.google.android.gms
2025-08-04 18:58:22.411 13969-14001 m.stel.gemmunch         com.stel.gemmunch                    I  AssetManager2(0xb400007aeb22a2f8) locale list changing from [] to [en-US]
2025-08-04 18:58:22.432 13969-14001 m.stel.gemmunch         com.stel.gemmunch                    I  AssetManager2(0xb400007aeb2367d8) locale list changing from [] to [en-US]
2025-08-04 18:58:22.435 13969-14001 m.stel.gemmunch         com.stel.gemmunch                    I  AssetManager2(0xb400007aeb230d38) locale list changing from [] to [en-US]
2025-08-04 18:58:22.443 13969-14001 DynamiteModule          com.stel.gemmunch                    W  Local module descriptor class for com.google.android.gms.googlecertificates not found.
2025-08-04 18:58:22.448 13969-14001 DynamiteModule          com.stel.gemmunch                    I  Considering local module com.google.android.gms.googlecertificates:0 and remote module com.google.android.gms.googlecertificates:7
2025-08-04 18:58:22.450 13969-14001 DynamiteModule          com.stel.gemmunch                    I  Selected remote version of com.google.android.gms.googlecertificates, version >= 7
2025-08-04 18:58:22.465 13969-14001 m.stel.gemmunch         com.stel.gemmunch                    W  ClassLoaderContext classpath size mismatch. expected=1, found=4 (DLC[];PCL[base.apk*4138776503]{PCL[/system/framework/org.apache.http.legacy.jar*4247870504]#PCL[/system/framework/com.android.media.remotedisplay.jar*487574312]#PCL[/system/framework/com.android.location.provider.jar*1570284764]#PCL[/system_ext/framework/org.carconnectivity.android.digitalkey.timesync.jar*889882842]#PCL[/system_ext/framework/androidx.window.extensions.jar*1030441313]#PCL[/system_ext/framework/androidx.window.sidecar.jar*3860983653]} | DLC[];PCL[/data/data/com.stel.gemmunch/code_cache/.overlay/base.apk/classes6.dex*2304103673:/data/data/com.stel.gemmunch/code_cache/.overlay/base.apk/classes7.dex*1404607403:/data/data/com.stel.gemmunch/code_cache/.overlay/base.apk/classes8.dex*2859635287:/data/app/~~iL1Nv7X-epI1Ow3qOtIK0A==/com.stel.gemmunch-wdHa9jcTiCsclxUojyQ41g==/base.apk*246428935])
2025-08-04 18:58:22.470 13969-14001 m.stel.gemmunch         com.stel.gemmunch                    I  AssetManager2(0xb400007aeb231058) locale list changing from [] to [en-US]
2025-08-04 18:58:22.544 13969-14001 PlayServicesAccel       com.stel.gemmunch                    I  ✅ AccelerationService created successfully
2025-08-04 18:58:22.545 13969-14001 PlayServicesAccel       com.stel.gemmunch                    I  ✅ GpuAccelerationConfig created
2025-08-04 18:58:22.545 13969-14001 PlayServicesAccel       com.stel.gemmunch                    I  ✅ Skipping validation - GPU acceleration confirmed available
2025-08-04 18:58:22.545 13969-14001 AppContainer            com.stel.gemmunch                    I  🚀 Using AccelerationService GPU configuration (GOLDEN PATH)
2025-08-04 18:58:22.545 13969-14001 AppContainer            com.stel.gemmunch                    I  ✅ Creating LlmInference with GPU backend (confidence: 95%)
2025-08-04 18:58:22.596 13969-14001 nativeloader            com.stel.gemmunch                    D  Load /data/app/~~iL1Nv7X-epI1Ow3qOtIK0A==/com.stel.gemmunch-wdHa9jcTiCsclxUojyQ41g==/base.apk!/lib/arm64-v8a/libllm_inference_engine_jni.so using class loader ns clns-9 (caller=/data/app/~~iL1Nv7X-epI1Ow3qOtIK0A==/com.stel.gemmunch-wdHa9jcTiCsclxUojyQ41g==/base.apk!classes11.dex): ok
2025-08-04 18:58:22.786 13969-14006 tflite                  com.stel.gemmunch                    I  Initialized TensorFlow Lite runtime.
2025-08-04 18:58:22.928 13969-14006 libc                    com.stel.gemmunch                    W  Access denied finding property "ro.mediatek.platform"
2025-08-04 18:58:22.928 13969-14006 libc                    com.stel.gemmunch                    W  Access denied finding property "ro.chipname"
2025-08-04 18:58:22.928 13969-14006 libc                    com.stel.gemmunch                    W  Access denied finding property "ro.hardware.chipname"
2025-08-04 18:58:22.972 13969-13974 m.stel.gemmunch         com.stel.gemmunch                    I  Compiler allocated 5111KB to compile void android.view.ViewRootImpl.performTraversals()
2025-08-04 18:58:23.341  1415-1503  ActivityTaskManager     system_server                        I  Displayed com.stel.gemmunch/.ui.MainActivity for user 0: +1s916ms
2025-08-04 18:58:23.343 13969-13969 CameraFoodCaptureScreen com.stel.gemmunch                    D  Loaded from ViewModel - photoUniqueId: null, isFromGallery: false
2025-08-04 18:58:23.352 13969-14007 HWUI                    com.stel.gemmunch                    I  Davey! duration=1095ms; Flags=1, FrameTimelineVsyncId=22152845, IntendedVsync=255262924413410, Vsync=255262924413410, InputEventId=0, HandleInputStart=255262929911218, AnimationStart=255262929915897, PerformTraversalsStart=255262929921187, DrawStart=255263941962610, FrameDeadline=255262941013410, FrameStartTime=255262929876916, FrameInterval=8329022, WorkloadTarget=16600000, SyncQueued=255263997242273, SyncStart=255263997370528, IssueDrawCommandsStart=255263997492517, SwapBuffers=255264018818933, FrameCompleted=255264020249353, DequeueBufferDuration=11515, QueueBufferDuration=297974, GpuCompleted=255264020249353, SwapBuffersCompleted=255264019190841, DisplayPresentTime=0, CommandSubmissionCompleted=255264018818933,
2025-08-04 18:58:23.360 13969-13969 Choreographer           com.stel.gemmunch                    I  Skipped 132 frames!  The application may be doing too much work on its main thread.
2025-08-04 18:58:23.481  1415-1979  ImeTracker              system_server                        I  com.stel.gemmunch:1ee720f0: onRequestHide at ORIGIN_SERVER reason HIDE_UNSPECIFIED_WINDOW fromUser false
2025-08-04 18:58:23.488 25591-25591 GoogleInpu...hodService com...gle.android.inputmethod.latin  I  GoogleInputMethodService.onStartInput():1311 onStartInput(EditorInfo{EditorInfo{packageName=com.stel.gemmunch, inputType=0, inputTypeString=NULL, enableLearning=false, autoCorrection=false, autoComplete=false, imeOptions=0, privateImeOptions=null, actionName=UNSPECIFIED, actionLabel=null, initialSelStart=-1, initialSelEnd=-1, initialCapsMode=0, label=null, fieldId=0, fieldName=null, extras=null, hintText=null, hintLocales=[]}}, false)
2025-08-04 18:58:23.490  1415-2822  PackageConfigPersister  system_server                        W  App-specific configuration not found for packageName: com.stel.gemmunch and userId: 0
2025-08-04 18:58:23.528 13969-13969 InsetsController        com.stel.gemmunch                    D  hide(ime(), fromIme=false)
2025-08-04 18:58:23.528 13969-13969 ImeTracker              com.stel.gemmunch                    I  com.stel.gemmunch:1ee720f0: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2025-08-04 18:58:24.341 13969-14006 tflite                  com.stel.gemmunch                    I  Created TensorFlow Lite XNNPACK delegate for CPU.
2025-08-04 18:58:24.341 13969-14006 tflite                  com.stel.gemmunch                    I  Replacing 1 out of 19 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 0.
2025-08-04 18:58:24.915 13969-14006 tflite                  com.stel.gemmunch                    I  Replacing 3409 out of 3409 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 0.
2025-08-04 18:58:27.307 13969-14023 ProfileInstaller        com.stel.gemmunch                    D  Installing profile for com.stel.gemmunch
2025-08-04 18:58:36.000 13969-14006 tflite                  com.stel.gemmunch                    I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 1.
2025-08-04 18:58:39.838 13969-14006 tflite                  com.stel.gemmunch                    I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 2.
2025-08-04 18:58:41.758 13969-14006 tflite                  com.stel.gemmunch                    I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 3.
2025-08-04 18:58:43.488 13969-14006 tflite                  com.stel.gemmunch                    I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 4.
2025-08-04 18:58:45.351 13969-14006 tflite                  com.stel.gemmunch                    I  Replacing 2189 out of 2189 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 5.
2025-08-04 18:58:47.449 13969-14001 AppContainer            com.stel.gemmunch                    I  🧠 AccelerationService configuration applied - NPU/GPU automatically selected
2025-08-04 18:58:47.449 13969-14001 AppContainer            com.stel.gemmunch                    I  Vision LLM instance created successfully.
2025-08-04 18:58:47.451 13969-14001 AppContainer            com.stel.gemmunch                    I  Initializing nutrition database...
2025-08-04 18:58:47.486 13969-14001 NutrientDbManager       com.stel.gemmunch                    I  Database exists with version: 1.0
2025-08-04 18:58:47.488 13969-14001 NutrientDbHelper        com.stel.gemmunch                    I  Enhanced nutrient database opened from: /data/user/0/com.stel.gemmunch/files/nutrients.db
2025-08-04 18:58:47.488 13969-14001 NutrientDbHelper        com.stel.gemmunch                    I  Database stats: {exists=true, size_mb=0.140625, version=1.0, hash=asset_version}
2025-08-04 18:58:47.488 13969-14001 AppContainer            com.stel.gemmunch                    I  Nutrition database initialized successfully
2025-08-04 18:58:47.490 13969-14001 AppContainer            com.stel.gemmunch                    I  AppContainer initialization successful in 25162ms
2025-08-04 18:58:47.490 13969-14001 AppContainer            com.stel.gemmunch                    I  🚀 Started continuous session pre-warming
2025-08-04 18:58:47.492 13969-14002 AppContainer            com.stel.gemmunch                    D  🔥 Pre-warming session #1...
2025-08-04 18:58:47.492 13969-14001 AppContainer            com.stel.gemmunch                    I  === INITIALIZATION METRICS ===
Total Initialization Time: 25.4s

                                                                                                    📊 ApplicationStartup: 7ms
                                                                                                       └─ CreateAppContainer: 2ms
                                                                                                       └─ InitializePreferences: 4ms
                                                                                                    
                                                                                                    📊 ViewModelInitialization: 25.2s
                                                                                                       └─ CheckModels: 3ms (Found 2/2 models)
                                                                                                       └─ InitializeAppContainer: 25.2s
                                                                                                    
                                                                                                    📊 AIInitialization: 25.2s
                                                                                                       └─ ModelSelection: 0ms (Model: GEMMA_3N_E4B_MODEL, Size: 4201MB)
                                                                                                       └─ AccelerationService: 218ms (Validated config received)
                                                                                                       └─ CreateLlmInference: 24.9s (AccelerationService)
                                                                                                       └─ CreateSessionOptions: 2ms
                                                                                                       └─ NutritionDatabase: 37ms (Success)
                                                                                                       └─ CreatePhotoMealExtractor: 1ms

2025-08-04 18:58:47.493 13969-14001 MainViewModel           com.stel.gemmunch                    I  AI Components initialized successfully.
2025-08-04 18:58:47.493 13969-14001 MainViewModel           com.stel.gemmunch                    I  === INITIALIZATION METRICS ===
Total Initialization Time: 25.4s

                                                                                                    📊 ApplicationStartup: 7ms
                                                                                                       └─ CreateAppContainer: 2ms
                                                                                                       └─ InitializePreferences: 4ms
                                                                                                    
                                                                                                    📊 ViewModelInitialization: 25.2s
                                                                                                       └─ CheckModels: 3ms (Found 2/2 models)
                                                                                                       └─ InitializeAppContainer: 25.2s
                                                                                                    
                                                                                                    📊 AIInitialization: 25.2s
                                                                                                       └─ ModelSelection: 0ms (Model: GEMMA_3N_E4B_MODEL, Size: 4201MB)
                                                                                                       └─ AccelerationService: 218ms (Validated config received)
                                                                                                       └─ CreateLlmInference: 24.9s (AccelerationService)
                                                                                                       └─ CreateSessionOptions: 2ms
                                                                                                       └─ NutritionDatabase: 37ms (Success)
                                                                                                       └─ CreatePhotoMealExtractor: 1ms
                                                                                                    
                                                                                                    📊 SessionPrewarming: 2ms

2025-08-04 18:58:47.554 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    I  Waiting for a blocking GC NativeAlloc
2025-08-04 18:58:47.574 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    I  WaitForGcToComplete blocked NativeAlloc on NativeAlloc for 19.533ms
2025-08-04 18:58:47.574 13969-13977 System                  com.stel.gemmunch                    W  A resource failed to call close.
2025-08-04 18:58:47.574 13969-13977 System                  com.stel.gemmunch                    W  A resource failed to call SQLiteConnectionPool.close.
2025-08-04 18:58:47.574 13969-13977 SQLiteConnectionPool    com.stel.gemmunch                    W  A SQLiteConnection object for database ':memory:' was leaked!  Please fix your application to end transactions in progress properly and to close the database when it is no longer needed.
2025-08-04 18:58:47.574 13969-13977 System                  com.stel.gemmunch                    W  A resource failed to call SQLiteConnection.close.
2025-08-04 18:58:47.610 13969-14002 tflite                  com.stel.gemmunch                    I  Replacing 1448 out of 1448 node(s) with delegate (ML_DRIFT_CL) node, yielding 1 partitions for subgraph 0.
2025-08-04 18:58:47.707 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    I  Waiting for a blocking GC NativeAlloc
2025-08-04 18:58:47.730 13969-13969 m.stel.gemmunch         com.stel.gemmunch                    I  WaitForGcToComplete blocked NativeAlloc on NativeAlloc for 23.093ms
2025-08-04 18:58:47.803 13969-13969 AppContainer            com.stel.gemmunch                    D  Cannot pre-warm: session already pre-warmed, in progress, or in use
2025-08-04 18:58:55.331 13969-14002 tflite                  com.stel.gemmunch                    I  Replacing 18 out of 19 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 0.
2025-08-04 18:58:55.354 13969-14002 tflite                  com.stel.gemmunch                    I  Replacing 1 out of 2 node(s) with delegate (TfLiteXNNPackDelegate) node, yielding 2 partitions for subgraph 1.
2025-08-04 18:58:55.358 13969-14002 AppContainer            com.stel.gemmunch                    D  ✅ Session #1 pre-warmed and ready in 7866ms

