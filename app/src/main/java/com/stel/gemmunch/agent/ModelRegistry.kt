package com.stel.gemmunch.agent

/** Defines the type of an AI model to categorize its purpose. */
enum class ModelType {
    GENERATIVE_VISION,
    LANGUAGE_MODEL
}

/**
 * Represents a single model file that the app needs.
 * @param logicalName A unique key to identify the model (e.g., "GEMMA_3N_E4B_MODEL").
 * @param displayName The user-facing name for selection in the UI.
 * @param url The public Hugging Face URL to download the model from.
 * @param fileName The local filename to save the model as.
 * @param type The category of the model.
 */
data class ModelAsset(
    val logicalName: String,
    val displayName: String,
    val url: String,
    val fileName: String,
    val type: ModelType
)

/**
 * A singleton object that provides a complete, definitive list of all AI models
 * required by the GemMunch application.
 */
object ModelRegistry {
    // The single list of all models required by the app.
    private val allModels = listOf(
        // Vision models (.task format) - compatible with MediaPipe LlmInference
        ModelAsset(
            logicalName = "GEMMA_3N_E2B_MODEL",
            displayName = "Gemma 3n E2B (Fast)",
            url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task",
            fileName = "gemma-3n-E2B-it-int4.task",
            type = ModelType.GENERATIVE_VISION
        ),
        ModelAsset(
            logicalName = "GEMMA_3N_E4B_MODEL",
            displayName = "Gemma 3n E4B (Accurate)",
            url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task",
            fileName = "gemma-3n-E4B-it-int4.task",
            type = ModelType.GENERATIVE_VISION
        ),
        // Language models (.litertlm format) - require LiteRT-LM runtime (not MediaPipe)
        // Currently text-only, no vision support
//        ModelAsset(
//            logicalName = "GEMMA_3N_E2B_LM",
//            displayName = "Gemma 3n E2B LM (Text)",
//            url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm-preview/resolve/main/gemma-3n-E2B-it-int4.litertlm",
//            fileName = "gemma-3n-E2B-it-int4.litertlm",
//            type = ModelType.LANGUAGE_MODEL
//        ),
//        ModelAsset(
//            logicalName = "GEMMA_3N_E4B_LM",
//            displayName = "Gemma 3n E4B LM (Text)",
//            url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm-preview/resolve/main/gemma-3n-E4B-it-int4.litertlm",
//            fileName = "gemma-3n-E4B-it-int4.litertlm",
//            type = ModelType.LANGUAGE_MODEL
//        )
    )

    /** Returns the complete list of all models for the downloader. */
    fun getAllModels(): List<ModelAsset> = allModels

    /**
     * Defines the minimum set of models required for the app to function.
     * For GemMunch, only vision models are essential for food capture.
     */
    fun getEssentialModelsForSetup(): List<ModelAsset> {
        return allModels.filter { it.type == ModelType.GENERATIVE_VISION }
    }
    
    /**
     * Returns optional language models that can be downloaded for additional features.
     */
    fun getOptionalLanguageModels(): List<ModelAsset> {
        return allModels.filter { it.type == ModelType.LANGUAGE_MODEL }
    }
}