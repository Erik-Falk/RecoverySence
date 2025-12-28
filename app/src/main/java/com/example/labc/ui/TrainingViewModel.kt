package com.example.labc.ui

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
    private val repository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState(isLoading = true))
    val uiState: StateFlow<TrainingUiState> = _uiState

    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = TrainingUiState(isLoading = true)
                val days = repository.loadTrainingDaysFromAssets()
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
}