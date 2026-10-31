package com.example.guidence.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AppDropdown(selectedValue: String, onValueSelected: (String) -> Unit, items: Array<String>?) {
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().wrapContentHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick={expanded=true}) {
            Text(selectedValue)
        }
        DropdownMenu(expanded=expanded, onDismissRequest = {expanded=false}) {
            items?.forEach { item ->
                DropdownMenuItem(
                    text={ Text(item) },
                    onClick = {
                        onValueSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}