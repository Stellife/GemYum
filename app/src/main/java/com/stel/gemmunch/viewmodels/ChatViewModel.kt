package com.stel.gemmunch.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stel.gemmunch.AppContainer
import com.stel.gemmunch.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.time.Instant

class ChatViewModel(
    private val appContainer: AppContainer,
    private val isMultimodal: Boolean = false,
    private val initialImagePath: String? = null
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private var conversationHistory = mutableListOf<String>()
    
    init {
        viewModelScope.launch {
            
            // If there's an initial image, add a system message
            initialImagePath?.let {
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "I've received your photo. What would you like to know about this meal?",
                        isFromUser = false
                    )
                )
            }
        }
    }
    
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = text,
                isFromUser = true
            )
            addMessage(userMessage)
            
            // Process with AI
            _isLoading.value = true
            try {
                val response = processWithAI(text)
                
                // Add AI response
                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = response,
                    isFromUser = false
                )
                addMessage(aiMessage)
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "I'm having trouble processing that. Could you try rephrasing?",
                    isFromUser = false
                )
                addMessage(errorMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun processWithAI(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Build conversation context
                val prompt = buildPrompt(userMessage)
                
                // Get appropriate session based on mode
                // Note: For now, we use vision session for both modes
                // TODO: Implement text-only session for better performance
                val session = appContainer.getReadyVisionSession()
                
                // Generate response
                session.addQueryChunk(prompt)
                val response = session.generateResponse()
                
                // Update conversation history
                conversationHistory.add("User: $userMessage")
                conversationHistory.add("Assistant: $response")
                
                response
            } catch (e: Exception) {
                "I apologize, but I'm having trouble processing your request. Could you try again?"
            }
        }
    }
    
    private fun buildPrompt(userMessage: String): String {
        val basePrompt = if (isMultimodal) {
            """
            You are a helpful nutrition assistant analyzing a meal from a photo.
            Help the user understand the nutritional content of their meal.
            Be conversational and friendly.
            
            Current conversation:
            ${conversationHistory.takeLast(6).joinToString("\n")}
            
            User: $userMessage
            Assistant:
            """.trimIndent()
        } else {
            """
            You are a helpful nutrition assistant. The user is describing their meal to you.
            Help them track the nutritional content. Be conversational and friendly.
            When you identify specific foods, I can look up their nutrition information.
            
            Current conversation:
            ${conversationHistory.takeLast(6).joinToString("\n")}
            
            User: $userMessage
            Assistant:
            """.trimIndent()
        }
        
        return basePrompt
    }
    
    
    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }
    
    fun addImageToConversation(imagePath: String) {
        viewModelScope.launch {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "I've added a photo to analyze. What would you like to know about it?",
                isFromUser = false,
                imagePath = imagePath
            )
            addMessage(message)
        }
    }
}