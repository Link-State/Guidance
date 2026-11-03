package com.example.guidance.ui.screen.result

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
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
import com.example.guidance.model.History
import com.example.guidance.model.Option
import com.example.guidance.model.Question
import com.example.guidance.ui.theme.GuidenceTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow

class ResultScreen : ComponentActivity() {
    private var examAmount = 80
    private val courseNames = mutableListOf<String>()
    private val courseAmounts = mutableIntListOf()
    private val courseCorrectCount = mutableIntListOf()
    private var optionAmount = 4
    private val questions = mutableListOf<Question>()
    private lateinit var examName: String
    private val correctAnswer = Color.hsv(110f, 0.41f, 0.49f)
    private val realAnswer = Color.hsv(193f, 0.42f, 0.59f)
    private val wrongAnswer = Color.hsv(360f, 0.64f, 0.66f)
    private val dontCareOption = Color.Unspecified
    private var isScoring = false
    private val scoreHistory = mutableMapOf<String, History>()
    private val optionMapping = mutableIntListOf()
    private var isEditMode by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        examName = intent.getStringExtra("examName") ?: ""
        val fileName = intent.getStringExtra("fileName") ?: ""
        if (fileName.isEmpty() || !File(filesDir, "/usr/$fileName").exists()) {
            finish()
        }
        isScoring = intent.getBooleanExtra("isScoring", false)

        // 시험 메타정보 추출 및 검증
        val metaFile = File(filesDir, "assets/meta/${examName}.txt")
        if (!metaFile.exists()) {
            finish()
        }
        courseNames.clear()
        courseAmounts.clear()
        courseCorrectCount.clear()
        optionMapping.clear()
        val meta = File(filesDir, "assets/meta/${examName}.txt").readText().split("\n")
        examAmount = meta[0].replace("문제 수 : ", "").trim().toInt()
        meta[1].replace("과목 : ", "").trim().split(";").forEach { eachName ->
            courseNames.add(eachName.replace(";", ""))
            courseCorrectCount.add(0)
        }
        var amountVerify = 0
        meta[2].replace("과목 당 문제 수 : ", "").trim().split(";").forEach { eachAmount ->
            courseAmounts.add(eachAmount.replace(";", "").toInt())
            amountVerify += eachAmount.replace(";", "").toInt()
        }
        optionAmount = meta[3].replace("선지 수 : ", "").trim().toInt()
        if (amountVerify != examAmount || courseNames.size != courseAmounts.size || courseNames.size != courseCorrectCount.size || courseAmounts.size != courseCorrectCount.size) {
            finish()
        }

        recoveryExam(fileName)
        setResult(Activity.RESULT_OK)

        enableEdgeToEdge()
        setContent {
            GuidenceTheme() {
                Scaffold() { innerPadding ->
                    CommentLoading(Modifier.padding(innerPadding))
                }
            }
        }
    }

    fun recoveryExam(fileName: String) {
        val course = mutableListOf<String>()
        val marking = mutableIntListOf()
        val resultPaper = File(filesDir, "usr/$fileName").bufferedReader().use {
            it.readText()
        }

        val lines = resultPaper.split("\n")
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

        // 문제 파싱
        var optionRegex = ""
        for (opc in (1..optionAmount)) {
            optionRegex += "\r?\n${opc}번 : (.*)"
        }

        var partNo = 0
        for (co in courseAmounts.indices) {
            for (pt in (1..courseAmounts[co])) {
                val examRegex = "시험명 : $examName\r?\n기출연도 : ([0-9]{4}-[0-9]{2}-[0-9]{2})\r?\n과목 : [0-9]\r?\n문제번호 : ([0-9]{1,4})\r?\n문제지문 : (.*)\r?\n사진 : (.*)${optionRegex}\r?\n정답 : ([0-9]*)\r?\n해설 : (.*)".toRegex()

                val contents = File(filesDir, "assets/exam/$examName/${course[partNo]}").readText()

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
                    val comment = (match[match.size-1]?.value ?: "None").replace("<#nbsp;>", "\n").trim()

                    var imageBitmap: ImageBitmap? = null
                    if (pictures.contains("<!PICTURE@image[0-9]{1,3}>".toRegex())) {
                        val inputStream = File(filesDir, "assets/image/$examName/${examDate.replace("-", "")}/${pictures.replace("<!PICTURE@", "").replace(">", "").trim()}.png").inputStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        imageBitmap = bitmap.asImageBitmap()
                    } else {
                        altPicture = pictures.replace("<#nbsp;>", "\n").trim()
                    }

                    questions.add(Question(course[partNo], examName, examDate, examNum, co+1, courseNames[co], statement, pictures,imageBitmap, altPicture, options, answer, comment, partNo, marking[partNo]))
                    questions.last().changeState["STATEMENT"] = false
                    questions.last().changeState["ALT_PICTURE"] = false
                    for (i in (1..optionAmount)) {
                        questions.last().changeState["OPTION_${i}"] = false
                    }
                    questions.last().changeState["COMMENT"] = false

                    if (isAnswer(marking[partNo], answer)) {
                        courseCorrectCount[co]++
                    }
                }

                partNo++
            }
        }

        // 이전 틀린기록 불러오기
        fetchScoreHistory()

        if (isScoring) {
            recordScore()
        }
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

    // 마킹한 숫자가 2의 승수값, 정답값도 2의 승수값일때 정답 판별
    fun isAnswer(markingExp: Int, answerExp: Int) : Boolean {
        return markingExp == (2.0f).pow(optionMapping[log2(answerExp.toFloat()).toInt()]-1).toInt()
    }

    fun fetchScoreHistory() {
        val history = File(filesDir, "usr/${examName}_history.txt")
        if (!history.exists()) {
            return
        }

        val historyContent = history.readText()
        if (historyContent.isEmpty()) {
            return
        }

        val historyRegex = "([0-9]{4}-[0-9]{2}-[0-9]{2}#[0-9]+[.]txt)\\s([0-9]{4}-[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2})\\s([0-9]+)\\s([0-9]+)\r?\n".toRegex()
        historyRegex.findAll(historyContent).forEach { line ->
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

    fun recordScore() {
        questions.forEach { quest ->
            if (!isAnswer(quest.marking, quest.answer)) {
                if (scoreHistory.containsKey(quest.fileName)) {
                    scoreHistory[quest.fileName]!!.recentWrong = Calendar.getInstance()
                    scoreHistory[quest.fileName]!!.accWrongCount++
                    scoreHistory[quest.fileName]!!.comboWrongCount++
                }
                else {
                    scoreHistory[quest.fileName] = History(quest.fileName, Calendar.getInstance(), 1, 1)
                }
            }
            else {
                if (scoreHistory.containsKey(quest.fileName)) {
                    scoreHistory[quest.fileName]!!.comboWrongCount = 0
                }
            }
        }

        // 기존 파일 덮어쓰기 후 저장
        val histFile = File(filesDir, "usr/${examName}_history.txt")
        histFile.writeText("")
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        scoreHistory.keys.forEach { key ->
            histFile.appendText("${scoreHistory[key]!!.fileName} ${df.format(scoreHistory[key]!!.recentWrong.time)} ${scoreHistory[key]!!.accWrongCount} ${scoreHistory[key]!!.comboWrongCount}\n")
        }
    }

    @Composable
    fun CommentLoading(modifier: Modifier) {
        val pagerState = rememberPagerState(pageCount = {examAmount + 1})
        val coroutineScope = rememberCoroutineScope()
        var pageInput by remember { mutableStateOf("") }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { partNo ->
            Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (partNo == examAmount) {
                    // 전체 통계
                    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                        Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                            var totalCorrect = 0
                            for (co in courseCorrectCount.indices) {
                                totalCorrect += courseCorrectCount[co]
                                Text(modifier = modifier, text = "${courseNames[co]} : ${((courseCorrectCount[co].toFloat() / courseAmounts[co].toFloat())*100f).toInt()}점 (${courseCorrectCount[co]}개)")
                            }
                            Text(modifier = modifier, text = "총 합 : ${((totalCorrect.toFloat() / examAmount.toFloat())*100f).toInt()}점")
                        }
                        Button(
                            modifier = modifier,
                            onClick = {
                                finish()
                            },
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(text = "나가기")
                        }
                    }
                    return@HorizontalPager
                }

                Text(examName)

                Column(modifier = Modifier.fillMaxWidth().padding(all = 6.dp)) {
                    // 헤더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(2f),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${partNo + 1}.", modifier = Modifier.alignByBaseline())
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
                        Row(
                            modifier = Modifier.weight(4f),
                            horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {},
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.width(40.dp).aspectRatio(1.1618f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = "저장",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(
                                onClick = {
                                    isEditMode = !isEditMode
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.width(40.dp).aspectRatio(1.1618f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = if (isEditMode) {"완료"} else {"수정"},
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // 지문
                    var statementContent by rememberSaveable(partNo) { mutableStateOf(questions[partNo].statement) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(all = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditMode) {
                            val originStatement = questions[partNo].statement
                            TextField(
                                value = statementContent,
                                onValueChange = { input ->
                                    statementContent = input
                                    questions[partNo].changeState["STATEMENT"] = !originStatement.contentEquals(statementContent)
                                    questions[partNo].statement = statementContent
                                }
                            )
                        } else {
                            Text(text = statementContent)
                        }
                    }

                    // 이미지
                    questions[partNo].bitmapPicture?.let { bitmap ->
                        Image(bitmap = bitmap, contentDescription = "문제사진", modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 10.dp), contentScale = ContentScale.FillWidth)
                    }

                    // 이미지설명
                    if (questions[partNo].altPicture.trim().isNotEmpty()) {
                        var altPictureContent by rememberSaveable(partNo) { mutableStateOf(questions[partNo].altPicture) }
                        val originAltPicture = if (isEditMode) {questions[partNo].altPicture} else {""}
                        TextField(
                            value = altPictureContent,
                            modifier = Modifier.fillMaxWidth().padding(all = 12.dp),
                            readOnly = !isEditMode,
                            onValueChange = { input ->
                                altPictureContent = input
                                questions[partNo].changeState["ALT_PICTURE"] = !originAltPicture.contentEquals(altPictureContent)
                                questions[partNo].altPicture = altPictureContent
                            },
                            textStyle = TextStyle(fontSize = 12.sp, lineHeight = 12.sp),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // 옵션
                    for (opt in (1..optionAmount)) {
                        val opt2Exp = (2.0).pow(opt-1).toInt()
                        Row(modifier = Modifier.fillMaxWidth().padding(all = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {},
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(35.dp).align(Alignment.Top),
                                shape = RoundedCornerShape(5.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isAnswer(opt2Exp, questions[partNo].answer)) { if (isAnswer(questions[partNo].marking, questions[partNo].answer)) {correctAnswer} else {realAnswer} } else { if (questions[partNo].marking == opt2Exp) {wrongAnswer} else {dontCareOption}}),
                            ) {
                                Text(text = opt.toString(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                            var optionContent by rememberSaveable(partNo) { mutableStateOf(questions[partNo].options[opt-1]!!.content) }
                            Row(modifier = Modifier.weight(1f).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isEditMode) {
                                    val originOption = questions[partNo].options[opt-1]!!.content
                                    TextField(
                                        value = optionContent,
                                        onValueChange = { input ->
                                            optionContent = input
                                            questions[partNo].changeState["OPTION_${opt}"] = !originOption.contentEquals(optionContent)
                                            questions[partNo].options[opt-1]!!.content = optionContent
                                        }
                                    )
                                } else {
                                    Text(
                                        text = optionContent,
                                        textDecoration = if (!isAnswer(opt2Exp, questions[partNo].answer) && opt2Exp == questions[partNo].marking) {TextDecoration.LineThrough} else {TextDecoration.None},
                                        fontWeight = if (isAnswer(opt2Exp, questions[partNo].answer)) {FontWeight.Bold} else {FontWeight.Normal}
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp), thickness = 1.dp)

                    // 해설
                    var commentContent by rememberSaveable(partNo) { mutableStateOf(questions[partNo].comment) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Top
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        val originComment = if (isEditMode) {questions[partNo].comment} else {""}
                        TextField(
                            value = commentContent,
                            readOnly = !isEditMode,
                            onValueChange = { input ->
                                commentContent = input
                                questions[partNo].changeState["COMMENT"] = !originComment.contentEquals(commentContent)
                                questions[partNo].comment = commentContent
                            },
                            placeholder = { Text(text = "해설없음") },
                            modifier = Modifier.weight(13f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.padding(all = 12.dp))

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
                                onValueChange = { pageInput = if (it.isEmpty() || it.toIntOrNull() != null) { it } else { return@TextField } },
                                textStyle = TextStyle(fontSize = 12.sp, lineHeight = 12.sp),
                                placeholder = { Text(text = "페이지 입력", lineHeight = 12.sp, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.height(48.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
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
                                            pagerState.scrollToPage(pageNum-1)
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

            if (!isEditMode) {
                saveChanged(filesDir)
            }
        }
    }

    fun saveChanged(filesDir: File) {
        questions.forEach { quest ->
            if (quest.changeState.containsValue(true)) {
                val inverseOption = quest.options.sortedBy { it?.originIdx }
                var optionRegex = ""
                for (opc in inverseOption.indices) {
                    optionRegex += "${opc+1}번 : ${inverseOption[opc]?.content?.trim()}\n"
                }
                optionRegex = optionRegex.trim()

                var picture = quest.pictures
                if (!picture.contains("<!PICTURE@image[0-9]{1,3}>".toRegex())) {
                    picture = quest.altPicture.trim().replace("\n", "<#nbsp;>")
                }
                val newContent = "시험명 : $examName\n기출연도 : ${quest.examDate}\n과목 : ${quest.courseNum}\n문제번호 : ${quest.examNum}\n문제지문 : ${quest.statement.trim()}\n사진 : ${picture}\n${optionRegex}\n정답 : ${quest.answer}\n해설 : ${quest.comment.trim().replace("\n", "<#nbsp;>")}"
                val newAssets = File(filesDir, "assets/exam/${examName}/${quest.fileName}")
                newAssets.writeText(newContent)
            }

            quest.changeState.keys.forEach { key ->
                quest.changeState[key] = false
            }
        }
    }
}