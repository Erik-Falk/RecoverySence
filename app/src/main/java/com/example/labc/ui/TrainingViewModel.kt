package com.example.labc.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labc.data.TrainingRepository
import com.example.labc.data.model.TrainingDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TrainingUiState(
    val isLoading: Boolean = false,
    val trainingDays: List<TrainingDay> = emptyList(),
    val errorMessage: String? = null
)

class TrainingViewModel(
    private val repository: TrainingRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState(isLoading = true))
    val uiState: StateFlow<TrainingUiState> = _uiState

    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = TrainingUiState(isLoading = true)
                val days = repository.getAllTrainingDays()
                _uiState.value = TrainingUiState(
                    isLoading = false,
                    trainingDays = days
                )
            } catch (e: Exception) {
                _uiState.value = TrainingUiState(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.importFromUri(uri, appContext)
                val days = repository.getAllTrainingDays()
                _uiState.value = _uiState.value.copy(
                    trainingDays = days,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Kunde inte importera filen: ${e.message}"
                )
            }
        }
    }
}
