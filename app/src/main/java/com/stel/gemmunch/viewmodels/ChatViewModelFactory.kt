package com.stel.gemmunch.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stel.gemmunch.AppContainer

class ChatViewModelFactory(
    private val appContainer: AppContainer,
    private val isMultimodal: Boolean = false,
    private val initialImagePath: String? = null
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(appContainer, isMultimodal, initialImagePath) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}