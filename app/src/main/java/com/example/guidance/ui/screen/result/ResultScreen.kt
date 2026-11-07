package com.example.guidance.ui.screen.result

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.mutableIntListOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guidance.model.ExamInstance
import com.example.guidance.model.History
import com.example.guidance.model.Option
import com.example.guidance.model.Question
import com.example.guidance.ui.theme.GuidanceTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow

class ResultScreen : ComponentActivity() {
    private var examName: String = ""
    private var fileName: String = ""
    private var isScoring: Boolean = false

    private var examInstance: ExamInstance? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.examName = intent.getStringExtra("examName") ?: ""
        this.fileName = intent.getStringExtra("fileName") ?: ""
        this.isScoring = intent.getBooleanExtra("isScoring", false)
        if (this.examName.isEmpty() || this.fileName.isEmpty()) {
            finish()
        }

        val completedFile = File(filesDir, "usr/${this.fileName}")
        // 문제 인스턴스 생성
        this.examInstance = ExamInstance(filesDir, this.examName)
        if (!this.examInstance!!.fetchCompletedFile(completedFile)) {
            finish()
        }

        // 점수 반영
        if (this.isScoring) {
            this.isScoring = false
            recordScore()
        }

        setResult(Activity.RESULT_OK)
        enableEdgeToEdge()
        setContent {
            GuidanceTheme() {
                Scaffold() { innerPadding ->
                    this.examInstance?.ExamLoading(Modifier.padding(innerPadding), this)
                }
            }
        }
    }

    fun recordScore() {
        this.examInstance?.let { examInstance ->
            examInstance.courses.forEach { course ->
                course?.let { course ->
                    val history = this.examInstance!!.incorrectLibrary.history
                    course.incorrectQuestions.values.forEach { incorrectQuest ->
                        if (history.contains(incorrectQuest.fileName)) {
                            history[incorrectQuest.fileName]!!.recentWrong = LocalDateTime.now()
                            history[incorrectQuest.fileName]!!.accWrongCount++
                            history[incorrectQuest.fileName]!!.comboWrongCount++
                        } else {
                            history[incorrectQuest.fileName] = History(incorrectQuest.fileName, LocalDateTime.now(), 1, 1)
                        }
                    }
                    course.correctQuestions.values.forEach { correctQuest ->
                        if (history.contains(correctQuest.fileName)) {
                            history[correctQuest.fileName]!!.comboWrongCount = 0
                        }
                    }
                }
            }
            this.examInstance!!.incorrectLibrary.saveHistories()
        }
    }
}