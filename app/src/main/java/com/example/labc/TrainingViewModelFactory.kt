package com.example.labc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.labc.data.TrainingRepository
import com.example.labc.ui.TrainingViewModel

class TrainingViewModelFactory(
    private val repository: TrainingRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainingViewModel::class.java)) {
            return TrainingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}