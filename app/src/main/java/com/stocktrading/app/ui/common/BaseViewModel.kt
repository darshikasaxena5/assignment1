package com.stocktrading.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocktrading.app.data.models.NetworkResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseViewModel : ViewModel() {

    // Loading state management
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state management
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success message state
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    protected fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    protected fun setError(message: String?) {
        _errorMessage.value = message
    }

    protected fun setSuccess(message: String?) {
        _successMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    // Helper function to handle NetworkResult
    protected fun <T> handleNetworkResult(
        result: NetworkResult<T>,
        onSuccess: (T) -> Unit,
        onError: ((String) -> Unit)? = null,
        onLoading: ((Boolean) -> Unit)? = null
    ) {
        when (result) {
            is NetworkResult.Loading -> {
                if (onLoading != null) {
                    onLoading(result.isLoading)
                } else {
                    setLoading(result.isLoading)
                }
            }
            is NetworkResult.Success -> {
                setLoading(false)
                setError(null)
                onSuccess(result.data)
            }
            is NetworkResult.Error -> {
                setLoading(false)
                if (onError != null) {
                    onError(result.message)
                } else {
                    setError(result.message)
                }
            }
        }
    }

    // Extension function for easier Flow collection
    protected fun <T> Flow<T>.collectInViewModel(action: suspend (T) -> Unit) {
        viewModelScope.launch {
            collect(action)
        }
    }

    // Enhanced error handling with retry mechanism
    protected fun <T> safeApiCall(
        operation: suspend () -> NetworkResult<T>,
        onSuccess: (T) -> Unit,
        onError: ((String) -> Unit)? = null,
        maxRetries: Int = 3
    ) {
        viewModelScope.launch {
            var attempts = 0

            while (attempts < maxRetries) {
                try {
                    when (val result = operation()) {
                        is NetworkResult.Success -> {
                            setLoading(false)
                            setError(null)
                            onSuccess(result.data)
                            return@launch
                        }
                        is NetworkResult.Error -> {
                            if (attempts == maxRetries - 1) {
                                setLoading(false)
                                if (onError != null) {
                                    onError(result.message)
                                } else {
                                    setError(result.message)
                                }
                                return@launch
                            }
                        }
                        is NetworkResult.Loading -> {
                            setLoading(true)
                        }
                    }
                } catch (e: Exception) {
                    if (attempts == maxRetries - 1) {
                        setLoading(false)
                        val errorMessage = when (e) {
                            is SocketTimeoutException -> "Connection timeout. Please try again."
                            is UnknownHostException -> "No internet connection. Please check your network."
                            else -> "Something went wrong. Please try again."
                        }
                        if (onError != null) {
                            onError(errorMessage)
                        } else {
                            setError(errorMessage)
                        }
                        return@launch
                    }
                }

                attempts++
                delay(1000L * attempts) // Exponential backoff
            }
        }
    }
}