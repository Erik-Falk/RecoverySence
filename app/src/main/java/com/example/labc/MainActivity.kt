package com.example.labc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.labc.data.TrainingRepository
import com.example.labc.ui.screens.RecoverySenseApp
import com.example.labc.ui.TrainingViewModel
import com.example.labc.TrainingViewModelFactory
import com.example.labc.ui.theme.LabCTheme   // byt till ditt riktiga tema om det heter något annat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = TrainingRepository(applicationContext)

        setContent {
            LabCTheme {
                val vm: TrainingViewModel = viewModel(
                    factory = TrainingViewModelFactory(repository, applicationContext)
                )

                // ⬇️ Ny “root” som innehåller Home/Graph/Recommendation
                RecoverySenseApp(viewModel = vm)
            }
        }
    }
}
