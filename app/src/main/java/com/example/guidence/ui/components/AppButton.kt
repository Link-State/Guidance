package com.example.guidence.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AppButton(text: String, fx:() -> Unit = {}) {
    Button(onClick=fx) {
        Text(text)
    }
}