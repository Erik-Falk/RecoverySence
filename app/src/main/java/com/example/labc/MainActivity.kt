package com.example.labc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.labc.data.TrainingRepository
import com.example.labc.ui.TrainingViewModel
import com.example.labc.ui.screens.OverviewScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = TrainingRepository(applicationContext)

        setContent {
            val vm: TrainingViewModel = viewModel(
                factory = TrainingViewModelFactory(repository, applicationContext)
            )
            OverviewScreen(viewModel = vm)
        }
    }
}