package com.example.guidance.ui.screen.setting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guidance.ui.theme.GuidenceTheme
import java.io.File

class EditTextScreen : ComponentActivity() {
    private var path = ""
    private var defaultContent = ""
    private var isChangeableTitle = false
    private var title by mutableStateOf("")
    private var content by mutableStateOf("")
    private var originalContent = ""
    private var isSaved by mutableStateOf(true)
    private var unsaveAlert by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        path = intent.getStringExtra("path") ?: ""
        defaultContent = intent.getStringExtra("defaultContent") ?: ""
        isChangeableTitle = intent.getBooleanExtra("changeableTitle", false)

        val editFile = File(filesDir, path)
        title = editFile.name
        content = if (editFile.exists()) {
            editFile.readText()
        } else {
            defaultContent
        }
        originalContent = content

        setContent {
            GuidenceTheme() {
                Scaffold() { innerPadding ->
                    EditorLoading(Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    fun EditorLoading(modifier: Modifier) {
        val unsaved = Color.hsv(0f, 0.84f, 0.73f)
        val saved = Color.hsv(140f, 0.86f, 0.58f)

        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(all = 12.dp).weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = if (isSaved) {title} else {"$title *"},
                    modifier = Modifier.fillMaxWidth().weight(7f),
                    readOnly = !isChangeableTitle,
                    onValueChange = { input ->
                        title = input
                    },
                    textStyle = TextStyle(fontSize = 14.sp, lineHeight = 14.sp),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    modifier = Modifier.weight(1.5f),
                    onClick = {
                        save()
                    },
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(text = "저장")
                }

                Button(
                    modifier = Modifier.weight(1.5f),
                    onClick = {
                        if (isSaved) {
                            done()
                        } else {
                            unsaveAlert = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSaved) {saved} else {unsaved}),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(text = "나가기")
                }
            }
            TextField(
                value = content,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).weight(9f),
                onValueChange = { input ->
                    content = input
                    isSaved = originalContent.contentEquals(content)
                },
                textStyle = TextStyle(fontSize = 12.sp, lineHeight = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // 팝업
        if (unsaveAlert) {
            AlertDialog(onDismissRequest = {
                unsaveAlert = false
            }, title = {
                Text(text = "")
            }, text = {
                Text(text = "변경사항을 저장하시겠습니까?")
            }, confirmButton = {
                TextButton(onClick = {
                    save()
                    unsaveAlert = false
                    done()
                }) {
                    Text(text = "저장 및 종료", color = saved)
                }
            }, dismissButton = {
                TextButton(onClick = {
                    unsaveAlert = false
                    done()
                }) {
                    Text(text = "나가기", color = unsaved)
                }
            })
        }
    }

    fun save() {
        val editFile = File(filesDir, path)
        editFile.writeText(content)
        originalContent = content
        isSaved = true
    }

    fun done() {
        finish()
    }
}