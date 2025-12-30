package com.example.labc.ui.screens

sealed class Screen {
    object Home : Screen()
    object Graph : Screen()
    object Recommendation : Screen()
}