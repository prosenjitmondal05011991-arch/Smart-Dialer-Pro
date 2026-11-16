package com.example.smartdialer.data.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class PermissionRequired(
        val permission: String,
        val permanentlyDenied: Boolean = false
    ) : UiState<Nothing>

    data class Empty(
        val filteredEmpty: Boolean = false
    ) : UiState<Nothing>

    data class Success<T>(
        val data: T
    ) : UiState<T>

    data class Error(
        val message: String
    ) : UiState<Nothing>
}
