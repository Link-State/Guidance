package com.example.guidance.ui.screen.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guidance.ui.screen.exam.ExamScreen
import com.example.guidance.ui.screen.result.ResultScreen
import com.example.guidance.ui.screen.setting.SettingScreen
import com.example.guidance.ui.theme.GuidenceTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class HomeScreen : ComponentActivity() {
    private val initOption = "시험 선택"
    private var examExpanded by mutableStateOf(false)
    private var onClickNewExam by mutableStateOf(false)
    private var onClickDeleteHist by mutableStateOf("")
    private var selectedExamName by mutableStateOf(initOption)
    private var hasUndonePaper by mutableStateOf(false)
    private val examList = mutableStateListOf<String>()

    private val examActLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val examComplete = result.data?.getStringExtra("exam_complete") ?: ""
            if (examComplete.isNotEmpty()) {
                hasUndonePaper = false
                viewExamResult(examComplete, true)
            } else {
                hasUndonePaper = result.data?.getBooleanExtra("create_undone_paper", false) ?: false
            }
        }
    }

    private val settingActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateExamList()
        }
    }

    private val resultActLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateExamList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(Activity.RESULT_OK)

        updateExamList()

        setContent {
            GuidenceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Loading(Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    fun Loading(modifier: Modifier) {
        Column(modifier.fillMaxSize()) {
            // 헤더
            Row(modifier = Modifier.fillMaxWidth().padding(5.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    modifier = Modifier.size(30.dp),
                    onClick = {
                        val intent = Intent(this@HomeScreen, SettingScreen::class.java)
                        settingActivityLauncher.launch(intent)
                    },
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(0f, 0f, 0.5f))
                ) {
                    Text(text = "─\n─\n─", lineHeight = 3.sp, autoSize = TextAutoSize.StepBased(1.sp, 100.sp, 2.sp))
                }
            }

            // 드롭다운 메뉴
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
                    Button(onClick={examExpanded=true}, shape = RoundedCornerShape(5.dp)) {
                        Text(selectedExamName)
                    }
                    DropdownMenu(expanded=examExpanded, onDismissRequest = {examExpanded=false}) {
                        examList.forEach { item ->
                            DropdownMenuItem(
                                text={ Text(item) },
                                onClick = {
                                    selectExam(item)
                                    examExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 시험 시작 버튼
            Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (hasUndonePaper) {
                    Button(
                        modifier = modifier,
                        onClick = {
                            clickStartExamBtn(this@HomeScreen, selectedExamName)
                        },
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(text = "계속")
                    }
                }

                Button(
                    modifier = modifier,
                    onClick = {
                        onClickNewExam = hasUndonePaper
                        if (!hasUndonePaper && !selectedExamName.contentEquals(initOption)) {
                            clickStartExamBtn(this@HomeScreen, selectedExamName)
                        }
                    },
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(text = "새 시험 생성")
                }

                Button(
                    modifier = modifier,
                    onClick = {

                    },
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text("북마크")
                }
            }

            // 채점 기록
            val scrollState = rememberScrollState()
            Column(modifier = modifier.fillMaxWidth().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (selectedExamName.contentEquals(initOption)) {
                    return@Column
                }
                val resultRegex = "([0-9]*)_${selectedExamName}_([0-9]*).txt".toRegex()
                File(filesDir, "usr/").list()!!.sortedDescending().forEach { file ->
                    val matches = resultRegex.find(file)?.groups
                    if (matches?.get(1)?.value?.toLong() != null) {
                        val examDate = matches[1]!!.value.toLong()
                        val date = Date(examDate)
                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", LocalLocale.current.platformLocale)
                        val formattedDate = formatter.format(date)
                        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                modifier = Modifier.height(40.dp).align(Alignment.CenterVertically),
                                onClick = { viewExamResult(file) },
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(text = formattedDate)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                modifier = Modifier.size(40.dp).align(Alignment.CenterVertically),
                                onClick = { onClickDeleteHist = file },
                                shape = RoundedCornerShape(5.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "D")
                            }
                        }
                    }
                }
            }

            if (onClickNewExam) {
                AlertDialog(onDismissRequest = {
                    onClickNewExam = false
                }, title = {
                    Text(text = "")
                }, text = {
                    Text(text = "진행 중인 시험을 취소할까요?")
                }, confirmButton = {
                    TextButton(onClick = {
                        File(filesDir, "usr/undonepaper_${selectedExamName}.txt").delete()
                        hasUndonePaper = false
                        onClickNewExam = false
                    }) {
                        Text("확인")
                    }
                }, dismissButton = {
                    TextButton(onClick = { onClickNewExam = false }) {
                        Text("취소")
                    }
                })
            }

            if (onClickDeleteHist.isNotEmpty()) {
                AlertDialog(onDismissRequest = {
                    onClickDeleteHist = ""
                }, title = {
                    Text(text = "")
                }, text = {
                    Text(text = "$onClickDeleteHist 기록을 삭제할까요?")
                }, confirmButton = {
                    TextButton(onClick = {
                        File(filesDir, "usr/$onClickDeleteHist").delete()
                        onClickDeleteHist = ""
                    }) {
                        Text("삭제", color = Color.hsv(0f, 0.63f, 0.76f))
                    }
                }, dismissButton = {
                    TextButton(onClick = { onClickDeleteHist = "" }) {
                        Text("취소")
                    }
                })
            }
        }
    }

    private fun selectExam(examName: String = initOption) {
        selectedExamName = examName
        hasUndonePaper = !examName.contentEquals(initOption) && File(filesDir, "usr/undonepaper_${selectedExamName}.txt").exists()
    }

    private fun updateExamList() {
        examList.clear()
        var updateInitSelected = false
        File(filesDir, "assets/exam/").list()?.forEach { examName ->
            examList.add(examName)
            if (!updateInitSelected && examName.contentEquals(selectedExamName)) {
                updateInitSelected = true
            }
        }
        if (updateInitSelected) {
            selectExam(selectedExamName)
        } else {
            selectExam(initOption)
        }
    }

    private fun clickStartExamBtn(context: Context, examName: String) {
        val intent = Intent(context, ExamScreen::class.java)
        intent.putExtra("examName", examName)
        examActLauncher.launch(intent)
    }

    private fun viewExamResult(fileName: String, isScoring: Boolean=false) {
        if (!File(filesDir, "usr/$fileName").exists()) {
            return
        }
        val intent = Intent(this@HomeScreen, ResultScreen::class.java)
        intent.putExtra("examName", selectedExamName)
        intent.putExtra("fileName", fileName)
        intent.putExtra("isScoring", isScoring)
        resultActLauncher.launch(intent)
    }
}