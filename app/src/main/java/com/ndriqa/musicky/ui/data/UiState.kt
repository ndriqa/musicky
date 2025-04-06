package com.ndriqa.musicky.ui.data

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    object Loading : UiState<Nothing>()
    data class Error(val exception: Exception, val onRetry: () -> Unit = { }) : UiState<Nothing>()
}
