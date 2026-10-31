package com.example.guidence.ui.screen.exam

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guidence.model.History
import com.example.guidence.model.Option
import com.example.guidence.model.Question
import com.example.guidence.ui.theme.GuidenceTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.set
import kotlin.math.pow

class ExamScreen : ComponentActivity() {
    private var examAmount = 80
    private val courseNames = mutableListOf<String>()
    private val courseAmounts = mutableIntListOf()
    private var optionAmount = 4
    private var createUndonePaper = false
    private var examName = ""
    private val questions = mutableListOf<Question>()
    private val scoreHistory = mutableMapOf<String, History>()
    private val optionMapping = mutableIntListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        examName = intent.getStringExtra("examName") ?: ""
        if (examName.isEmpty()) {
            finish()
        }

        // 시험 메타정보 추출
        if ("${examName}.txt" !in applicationContext.assets.list("meta/")!!) {
            finish()
        }
        courseNames.clear()
        courseAmounts.clear()
        optionMapping.clear()
        val metaFile = applicationContext.assets.open("meta/${examName}.txt")
        val meta = (metaFile.bufferedReader().use { reader ->
            reader.readText()
        }).split("\n")
        examAmount = meta[0].replace("문제 수 : ", "").trim().toInt()
        meta[1].replace("과목 : ", "").trim().split(";").forEach { eachName ->
            courseNames.add(eachName.replace(";", ""))
        }
        var amountVerify = 0
        meta[2].replace("과목 당 문제 수 : ", "").trim().split(";").forEach { eachAmount ->
            courseAmounts.add(eachAmount.replace(";", "").toInt())
            amountVerify += eachAmount.replace(";", "").toInt()
        }
        optionAmount = meta[3].replace("선지 수 : ", "").trim().toInt()
        if (amountVerify != examAmount || courseNames.size != courseAmounts.size) {
            finish()
        }

        createExam()

        setContent {
            GuidenceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ExamLoading(Modifier.padding(innerPadding))
                }
            }
        }
    }


    fun createExam() {
        val course = mutableListOf<String>()
        val marking = mutableIntListOf()

        if ("undonepaper_${examName}.txt" in fileList()) {
            createUndonePaper = true
            val undone_paper = openFileInput("undonepaper_${examName}.txt").bufferedReader().use {
                it.readText()
            }

            val lines = undone_paper.split("\n")
            lines[0].split(" ").forEach { x ->
                optionMapping.add(x.trim().toInt())
            }
            lines.subList(1, lines.size).forEach { line ->
                val sheet = line.split(" ")
                val examDate = sheet[0]
                val markedOpt = sheet[1].toInt()
                course.add(examDate)
                marking.add(markedOpt)
            }
        } else {
            val optMp = Array<Int>(optionAmount) { i -> i+1 }
            optMp.shuffle()
            optMp.forEach { x ->
                optionMapping.add(x)
            }

            // 랜덤 시험문제 제출
            val allExam: Array<String>? = applicationContext.assets.list("exam/$examName/")
            val allCourses = mutableListOf<MutableList<String>>()

            for (co in courseAmounts.indices) {
                allCourses.add(mutableListOf<String>())
            }

            val regex = "[0-9]{4}-[0-9]{2}-[0-9]{2}#([0-9]*).txt".toRegex()
            allExam?.forEach { item ->
                regex.find(item)?.groups[1]?.value?.let { strNum ->
                    val num = strNum.toInt()
                    var range = 1
                    for (co in courseAmounts.indices) {
                        if (range <= num && num <= courseAmounts[co]) {
                            allCourses[co].add(item)
                            break
                        }
                        range = courseAmounts[co] + 1
                    }
                }
            }

            for (co in courseAmounts.indices) {
                for (pt in (1..courseAmounts[co])) {
                    val gotcha = allCourses[co].indices.random()
                    course.add(allCourses[co][gotcha])
                    allCourses[co].removeAt(gotcha)
                    marking.add(0)
                }
            }
        }

        // 문제 파싱
        var optionRegex = ""
        for (opc in (1..optionAmount)) {
            optionRegex += "\r\n${opc}번 : (.*)"
        }
        for (partNo in (0..<examAmount)) {
            val examRegex = "시험명 : $examName\r\n기출연도 : ([0-9]{4}-[0-9]{2}-[0-9]{2})\r\n과목 : [0-9]\r\n문제번호 : ([0-9]{1,4})\r\n문제지문 : (.*)\r\n사진 : (.*)${optionRegex}\r\n정답 : ([0-9]*)\r\n해설 : (.*)".toRegex()

            var contents = ""
            try {
                val inputStream = this@ExamScreen.assets.open("exam/$examName/${course[partNo]}")
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                contents = String(buffer, Charsets.UTF_8)
            } catch (e : Exception) {
                e.printStackTrace()
            }

            examRegex.find(contents)?.groups?.let { match ->
                val examDate = match[1]?.value ?: "None"
                val examNum = match[2]?.value ?: "None"
                val statement = match[3]?.value ?: "None"
                val pictures = match[4]?.value ?: "None"
                var altPicture = ""
                val answer = match[5+optionAmount]?.value?.toIntOrNull() ?: 0
                val extractedAns = extractAnswer(answer)
                val options = Array<Option?>(optionAmount) { null }
                for (opc in (5..<5+optionAmount)) {
                    options[optionMapping[opc-5]-1] = Option(opc-4, match[opc]?.value ?: "None", extractedAns[opc-5])
                }

                val comment = (match[6+optionAmount]?.value ?: "None").replace("<#nbsp;>", "\n").trim()

                var imageBitmap: ImageBitmap? = null
                if (pictures.isNotEmpty() && pictures.contains("<!PICTURE@image[0-9]{1,3}>".toRegex())) {
                    val inputStream = applicationContext.assets.open("image/$examName/${examDate.replace("-", "")}/${pictures.replace("<!PICTURE@", "").replace(">", "").trim()}.png")
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    imageBitmap = bitmap.asImageBitmap()
                } else {
                    altPicture = pictures.replace("<#nbsp;>", "\n").trim()
                }

                questions.add(Question(course[partNo], examName, examDate, examNum, statement, pictures,imageBitmap, altPicture, options, answer, comment, partNo, marking[partNo]))
            }
        }

        fetchScoreHistory()
    }


    fun extractAnswer(answer: Int) : Array<Boolean> {
        val result = Array<Boolean>(optionAmount) { i -> false }
        for (ans in 0..<optionAmount) {
            if ((answer and (2.0).pow(ans).toInt()) == 1) {
                result[optionMapping[ans]-1] = true
            }
        }
        return result
    }


    fun fetchScoreHistory() {
        if ("${examName}_history.txt" !in fileList()) {
            return
        }

        val history = openFileInput("${examName}_history.txt").bufferedReader().use {
            it.readText()
        }

        if (history.isEmpty()) {
            return
        }

        val historyRegex = "([0-9]{4}-[0-9]{2}-[0-9]{2}#[0-9]+[.]txt)\\s([0-9]{4}-[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2})\\s([0-9]+)\\s([0-9]+)\n".toRegex()
        historyRegex.findAll(history).forEach { line ->
            val fileName = line.groups[1]?.value
            val dateStr = line.groups[2]?.value
            val accWrong = line.groups[3]?.value?.toIntOrNull()
            val comboWrong = line.groups[4]?.value?.toIntOrNull()
            if (fileName != null && dateStr != null && accWrong != null && comboWrong != null) {
                val cal: Calendar = Calendar.getInstance()
                val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
                val date: Date = df.parse(dateStr)!!
                cal.time = date

                scoreHistory[fileName] = History(fileName, cal, accWrong, comboWrong)
            }
        }
    }


    @Composable
    fun ExamLoading(modifier: Modifier) {
        val pagerState = rememberPagerState(pageCount = {examAmount + 1})
        val coroutineScope = rememberCoroutineScope()
        var pageInput by remember { mutableStateOf("") }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth(), beyondViewportPageCount = 0) { partNo ->
            Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (partNo == examAmount) {
                    var isEnabled by rememberSaveable(partNo) { mutableStateOf(true) }
                    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        val remainQuest = mutableListOf<Question>()
                        questions.forEach { quest ->
                            if (quest.marking == 0) {
                                remainQuest.add(quest)
                            }
                        }
                        Text(text = "남은 문제 수 : ${remainQuest.size}개")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                            maxItemsInEachRow = 5
                        ) {
                            if (remainQuest.size > 0) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp), thickness = 1.dp)
                            }
                            remainQuest.forEach { quest ->
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(quest.partNo)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp).padding(5.dp),
                                    contentPadding = PaddingValues(all = 0.dp),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(text = "${quest.partNo+1}")
                                }
                            }
                        }
                        Button(
                            modifier = modifier,
                            onClick = {
                                isEnabled = false
                                val result = examFinish()
                                isEnabled = true
                                if (result) {
                                    finish()
                                }
                            },
                            shape = RoundedCornerShape(7.dp),
                            enabled = isEnabled
                        ) {
                            Text(text = "완료")
                        }
                    }
                    return@HorizontalPager
                }

                Text(examName)

                // 표시
                Column(modifier = Modifier.fillMaxWidth().padding(all = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${partNo+1}.", modifier = Modifier.alignByBaseline())
                        }
                        Column(modifier = Modifier.weight(4f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = questions[partNo].examDate, modifier = Modifier.alignByBaseline(), fontSize = 10.sp, color = Color.DarkGray)
                                Text(text = "${questions[partNo].examNum}번", modifier = Modifier.alignByBaseline(), fontSize = 10.sp, color = Color.DarkGray)
                            }
                            if (scoreHistory.containsKey(questions[partNo].fileName)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "누적 ${scoreHistory[questions[partNo].fileName]!!.accWrongCount.toString()}회 오답!", modifier = Modifier.alignByBaseline(), fontSize = 10.sp, color = Color.DarkGray)
                                    val comboWrong = scoreHistory[questions[partNo].fileName]!!.comboWrongCount
                                    if (comboWrong > 1) {
                                        Text(text = "연속 ${comboWrong.toString()}회 오답!", modifier = Modifier.alignByBaseline(), fontSize = 10.sp, color = Color.hsv(360f, 0.6f, 0.62f))
                                    }
                                }
                            }
                        }
                        Row(modifier = Modifier.weight(4f), horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = {}, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp).aspectRatio(1.1618f), shape = RoundedCornerShape(5.dp)) {
                                Text(text = "저장", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                            Button(onClick = {}, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp).aspectRatio(1.1618f), shape = RoundedCornerShape(5.dp)) {
                                Text(text = "오류", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(all = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = questions[partNo].statement)
                    }

                    // 이미지
                    questions[partNo].bitmapPicture?.let { bitmap ->
                        Image(bitmap = bitmap, contentDescription = "문제사진", modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 10.dp), contentScale = ContentScale.FillWidth)
                    }

                    // 이미지설명
                    if (questions[partNo].altPicture.isNotEmpty()) {
                        TextField(
                            value = questions[partNo].altPicture,
                            modifier = Modifier.fillMaxWidth().padding(all = 12.dp),
                            readOnly = true,
                            onValueChange = {},
                            textStyle = TextStyle(fontSize = 12.sp, lineHeight = 12.sp),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    var checked by rememberSaveable(partNo) { mutableIntStateOf(questions[partNo].marking) }
                    for (opt in (1..optionAmount)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(all = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    checked = if (checked == (2.0f).pow(opt-1).toInt()) { 0 } else { (2.0f).pow(opt-1).toInt() }
                                    questions[partNo].marking = checked
                                    savePaper()
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(35.dp).align(Alignment.Top),
                                shape = RoundedCornerShape(5.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (checked == (2.0f).pow(opt-1).toInt()) {Color.DarkGray} else {Color.Unspecified})) {
                                Text(text = opt.toString(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                            Row(modifier = Modifier.weight(1f).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = questions[partNo].options[opt-1]!!.content)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp), thickness = 1.dp)

                    // 검색
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(2f))
                        Row(modifier = Modifier.weight(11f), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = pageInput,
                                onValueChange = { input ->
                                    pageInput = input.filter { it.isDigit() }
                                },
                                textStyle = TextStyle(fontSize = 12.sp, lineHeight = 12.sp),
                                placeholder = { Text(text = "페이지 입력", lineHeight = 12.sp, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.height(48.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Go
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Button(
                                onClick = {
                                    var pageNum = pageInput.toIntOrNull()
                                    if (pageNum != null) {
                                        if (pageNum < 0) {
                                            pageNum = 0
                                        }
                                        if (pageNum > examAmount+1) {
                                            pageNum = examAmount+1
                                        }
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pageNum-1)
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(48.dp).aspectRatio(1f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(text = "이동")
                            }
                        }
                        Spacer(modifier = Modifier.weight(2f))
                    }
                }
            }
        }
    }

    fun savePaper() {
        var result = ""
        optionMapping.forEach { x ->
            result += "$x "
        }
        result = result.trim() + "\n"
        for (i in (0..<examAmount)) {
            result += "${questions[i].examDate}#${questions[i].examNum}.txt ${questions[i].marking}\n"
        }
        result = result.trim()

        try {
            openFileOutput("undonepaper_${examName}.txt", MODE_PRIVATE).use { output ->
                output.write(result.toByteArray())
            }

            if (!createUndonePaper) {
                createUndonePaper = true
                val resultIntent = Intent().apply {
                    putExtra("create_undone_paper", true)
                }
                setResult(Activity.RESULT_OK, resultIntent)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    fun examFinish(): Boolean {
        // 저장
        savePaper()

        // 파일 순서번호 찾기
        var idx = 1
        val finishedTime = System.currentTimeMillis()
        while (idx < Int.MAX_VALUE) {
            if ("${finishedTime}_${examName}_${idx}.txt" !in fileList()) {
                break
            }
            idx++
        }

        // 파일명 변경
        val oldFile = File(filesDir, "undonepaper_${examName}.txt")
        val newFile = File(filesDir, "${finishedTime}_${examName}_${idx}.txt")
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)

            val resultIntent = Intent().apply {
                putExtra("exam_complete", "${finishedTime}_${examName}_${idx}.txt")
            }
            setResult(Activity.RESULT_OK, resultIntent)
            return true
        }

        return false
    }
}
