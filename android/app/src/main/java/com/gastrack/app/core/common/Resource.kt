package com.gastrack.app.core.common

sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val status: Int? = null, val retryable: Boolean = false) : Resource<Nothing>
}
