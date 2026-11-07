package com.example.guidance.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.set
import kotlin.math.pow

class ExamInstance(val filesDir: File, val examName: String) {
    // 시험 메타정보 불러오기
    val meta: ExamMeta = ExamMeta(this, this.examName)

    // 문제 번호 입력 시 그에 해당하는 과목 인덱스 출력하는 배열 생성
    public val questNoToCourseIdx: Array<Int> = Array<Int>(this.meta.questAmount) { 0 }

    // 오답 정보 불러오기
    val incorrectLibrary = ScoreLibrary(this, this.examName)

    // 과목 별 문제 생성하기
    val courses: Array<Course?> = Array<Course?>(this.meta.courseNames.size) { null }

    private var isEditMode: Boolean by mutableStateOf(false)
    private var isCompleted: Boolean by mutableStateOf(false)
    private var hasUndonePaper = false

    // 초기화
    init {
        // 문제번호 입력 => 과목번호 출력
        var courseIdx = 0
        var range = this.meta.questAmountPerCourse[courseIdx]
        for (questNo in this.questNoToCourseIdx.indices) {
            while (questNo+1 > range) {
                courseIdx++
                range += this.meta.questAmountPerCourse[courseIdx]
            }
            this.questNoToCourseIdx[questNo] = courseIdx+1
        }
    }

    // 새 문제 생성
    fun new() {
        for (i in this.courses.indices) {
            this.courses[i] = Course(
                this,
                this.meta.courseNames[i],
                i+1,
                this.meta.questAmountPerCourse[i]
            )
        }
    }

    // 기존 미완료 문제 불러오기
    fun fetchUndoneFile(): Boolean {
        val undonePaperFile = File(this.filesDir, "usr/undonepaper_${this.examName}.txt")
        if (!undonePaperFile.exists()) {
            return false
        }

        for (i in this.courses.indices) {
            this.courses[i] = Course(
                this,
                this.meta.courseNames[i],
                i+1,
                this.meta.questAmountPerCourse[i],
                undonePaperFile
            )
        }
        return true
    }

    // 완료된 문제 불러오기
    fun fetchCompletedFile(completedPaperFile: File): Boolean {
        if (!completedPaperFile.exists()) {
            return false
        }

        this.isCompleted = true
        for (i in this.courses.indices) {
            this.courses[i] = Course(
                this,
                this.meta.courseNames[i],
                i+1,
                this.meta.questAmountPerCourse[i],
                completedPaperFile
            )
        }
        return true
    }

    fun remainQuestion(): MutableList<Question> {
        val result = mutableListOf<Question>()
        this.courses.forEach { course ->
            result.addAll(course?.remainQuestion() ?: listOf())
        }
        return result
    }

    // 문제 번호 입력 시 문제 인스턴스 반환 / questNo는 0부터 시작
    fun getQuestion(questNo: Int): Question? {
        if (0 <= questNo && questNo < this.meta.questAmount) {
            val courseNo = this.questNoToCourseIdx[questNo]
            val lowerBound = this.meta.questAmountPerCourse.subList(0, courseNo-1).sum()
            return this.courses[courseNo-1]?.questions[questNo - lowerBound]
        }
        return null
    }

    @Composable
    public fun ExamLoading(modifier: Modifier, activity: Activity) {
        val pagerState = rememberPagerState(pageCount = {this.meta.questAmount + 1})
        val coroutineScope = rememberCoroutineScope()
        var pageInput by remember { mutableStateOf("") }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth(), beyondViewportPageCount = 0) { questNo ->
            Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (questNo == this@ExamInstance.meta.questAmount) {
                    if (this@ExamInstance.isCompleted) {
                        this@ExamInstance.LastPage(activity, modifier, questNo)
                    } else {
                        this@ExamInstance.RemainPage(activity, modifier, pagerState, coroutineScope, questNo)
                    }
                    return@HorizontalPager
                } else {
                    var quest: Question? by remember { mutableStateOf(getQuestion(questNo)) }
                    Text(this@ExamInstance.examName)

                    Column(modifier = Modifier.fillMaxWidth().padding(all = 6.dp)) {

                        // 헤더
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "${questNo+1}.", modifier = Modifier.alignByBaseline())
                            }
                            Column(modifier = Modifier.weight(4f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = quest?.examDate ?: "None", modifier = Modifier.alignByBaseline(), fontSize = 10.sp, color = Color.DarkGray)
                                    Text(text = "${quest?.examNum ?: "None"}번", modifier = Modifier.alignByBaseline(), fontSize = 10.sp, color = Color.DarkGray)
                                }
                                quest?.let { quest ->
                                    if (this@ExamInstance.incorrectLibrary.hasQuestion(quest)) {
                                        val history = this@ExamInstance.incorrectLibrary.getHistory(quest)!!
                                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "누적 ${history.accWrongCount}회 오답!", modifier = Modifier.alignByBaseline(), fontSize = 10.sp, color = Color.DarkGray)
                                            val comboWrong = history.comboWrongCount
                                            if (comboWrong > 1) {
                                                Text(text = "연속 ${comboWrong}회 오답!", modifier = Modifier.alignByBaseline(), fontSize = 10.sp, color = Color.hsv(360f, 0.6f, 0.62f))
                                            }
                                        }
                                    }
                                }
                            }
                            Row(modifier = Modifier.weight(4f), horizontalArrangement = Arrangement.spacedBy(15.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = {}, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp).aspectRatio(1.1618f), shape = RoundedCornerShape(5.dp)) {
                                    Text(text = "저장", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                }
                                Button(
                                    onClick = {
                                        this@ExamInstance.isEditMode = !this@ExamInstance.isEditMode
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.width(40.dp).aspectRatio(1.1618f),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(text = if (this@ExamInstance.isEditMode) {"완료"} else {"수정"}, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                }
                            }
                        }

                        // 지문
                        var statementContent by rememberSaveable(questNo) { mutableStateOf(quest?.statement ?: "None") }
                        Row(modifier = Modifier.fillMaxWidth().padding(all = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (this@ExamInstance.isEditMode) {
                                val originStatement = quest?.statement ?: "None"
                                TextField(
                                    value = statementContent,
                                    onValueChange = { input ->
                                        statementContent = input
                                        quest?.let { quest ->
                                            quest.changeState["STATEMENT"] = !originStatement.contentEquals(statementContent)
                                            quest.statement = statementContent
                                        }
                                    }
                                )
                            } else {
                                Text(text = statementContent)
                            }
                        }

                        // 이미지
                        quest?.bitmapPicture?.let { bitmap ->
                            Image(bitmap = bitmap, contentDescription = "문제사진", modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 10.dp), contentScale = ContentScale.FillWidth)
                        }

                        // 이미지설명
                        quest?.let { quest ->
                            if (quest.altPicture.trim().isNotEmpty()) {
                                var altPictureContent by rememberSaveable(questNo) { mutableStateOf(quest.altPicture) }
                                val originAltPicture = if (this@ExamInstance.isEditMode) {quest.altPicture} else {""}
                                TextField(
                                    value = altPictureContent,
                                    modifier = Modifier.fillMaxWidth().padding(all = 12.dp),
                                    readOnly = !this@ExamInstance.isEditMode,
                                    onValueChange = { input ->
                                        altPictureContent = input
                                        quest.changeState["ALT_PICTURE"] = !originAltPicture.contentEquals(altPictureContent)
                                        quest.altPicture = altPictureContent
                                    },
                                    textStyle = TextStyle(fontSize = 12.sp, lineHeight = 12.sp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // 옵션
                        quest?.options?.let {
                            quest!!.options!!.optionMapping.keys.sorted().forEach { optNum ->
                                OptionParts(activity, questNo, quest!!, optNum)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp), thickness = 1.dp)

                        // 해설
                        if (quest != null && this@ExamInstance.isCompleted) {
                            this@ExamInstance.CommentParts(questNo, quest!!)
                        }

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
                                            if (pageNum > this@ExamInstance.meta.questAmount+1) {
                                                pageNum = this@ExamInstance.meta.questAmount+1
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

            if (!this@ExamInstance.isEditMode) {
                saveChanged()
            }
        }
    }

    // 옵션
    @Composable
    private fun OptionParts(activity: Activity, questNo: Int, quest: Question, optNum: Int) {
        var checked by rememberSaveable(questNo) { mutableIntStateOf(quest.options!!.getSelected()) }

        var btnClickFx = {
            if (quest.options!!.isSelected(optNum)) {
                quest.options.deselect(optNum)
            } else {
                quest.options.select(optNum)
            }
            checked = quest.options.getSelected()
            savePaper(activity)
        }
        var numBtn = ButtonDefaults.buttonColors(containerColor = Color.Unspecified)
        var txtDecor = TextDecoration.None
        var fontWg = FontWeight.Normal

        // 해설지 모드
        if (this.isCompleted) {
            btnClickFx = {}

            // 정답인 옵션
            if (quest.options!!.answerMapping.contains(optNum)) {
                // 선택한 옵션
                if (quest.options.isSelected(optNum)) {
                    numBtn = ButtonDefaults.buttonColors(containerColor = Color.hsv(110f, 0.41f, 0.49f))
                }
                // 선택 안한 옵션
                else {
                    numBtn = ButtonDefaults.buttonColors(containerColor = Color.hsv(193f, 0.42f, 0.59f))
                }
                fontWg = FontWeight.Bold
            }
            // 정답이 아닌 옵션
            else {
                // 선택한 옵션
                if (quest.options.isSelected(optNum)) {
                    numBtn = ButtonDefaults.buttonColors(containerColor = Color.hsv(360f, 0.64f, 0.66f))
                    txtDecor = TextDecoration.LineThrough
                }
            }
        }
        // 시험 모드
        else {
            if (checked.and((2.0f).pow(optNum-1).toInt()) > 0) {
                numBtn = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(all = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { btnClickFx() },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(35.dp).align(Alignment.Top),
                shape = RoundedCornerShape(5.dp),
                colors = numBtn) {
                Text(text = optNum.toString(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            var optionContent by rememberSaveable(questNo) { mutableStateOf(quest.options!!.optionMapping[optNum]!!.content ) }
            Row(modifier = Modifier.weight(1f).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (this@ExamInstance.isEditMode) {
                    val originOption = quest.options!!.optionMapping[optNum]!!.content
                    TextField(value = optionContent,
                        onValueChange = { input ->
                            optionContent = input
                            quest.changeState["OPTION_${optNum}"] = !originOption.contentEquals(optionContent)
                            quest.options.optionMapping[optNum]!!.content = optionContent
                        }
                    )
                } else {
                    Text(
                        text = optionContent,
                        textDecoration = txtDecor,
                        fontWeight = fontWg)
                }
            }
        }
    }

    // 마지막 페이지 (남은 문제 수)
    @Composable
    private fun RemainPage(activity: Activity, modifier: Modifier, pagerState: PagerState, coroutineScope: CoroutineScope, questNo: Int) {
        var isEnabled by rememberSaveable(questNo) { mutableStateOf(true) }
        Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val remainQuest = this@ExamInstance.remainQuestion()
            Text(text = "남은 문제 수 : ${remainQuest.size}개")
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                maxItemsInEachRow = 5
            ) {
                if (remainQuest.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp), thickness = 1.dp)
                }
                remainQuest.forEach { quest ->
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(quest.questNo)
                            }
                        },
                        modifier = Modifier.size(40.dp).padding(5.dp),
                        contentPadding = PaddingValues(all = 0.dp),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(text = "${quest.questNo+1}")
                    }
                }
            }
            Button(
                modifier = modifier,
                onClick = {
                    isEnabled = false
                    val result = examFinish(activity)
                    isEnabled = true
                    if (result) {
                        activity.finish()
                    }
                },
                shape = RoundedCornerShape(7.dp),
                enabled = isEnabled
            ) {
                Text(text = "완료")
            }
        }
    }

    // 마지막 페이지 (해설 마지막)
    @Composable
    private fun LastPage(activity: Activity, modifier: Modifier, questNo: Int) {
        Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
            Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                var totalCorrect = 0
                this@ExamInstance.courses.forEach { course ->
                    totalCorrect += course!!.correctQuestions.size
                    Text(modifier = modifier, text = "${course.courseName} : ${((course.correctQuestions.size / course.questAmountPerCourse)*100f).toInt()}점 (${course.correctQuestions.size}개)")
                }
                Text(modifier = modifier, text = "총 합 : ${((totalCorrect.toFloat() / this@ExamInstance.meta.questAmount.toFloat())*100f).toInt()}점")
            }
            Button(
                modifier = modifier,
                onClick = {
                    activity.finish()
                },
                shape = RoundedCornerShape(5.dp)
            ) {
                Text(text = "나가기")
            }
        }
    }

    // 해설
    @Composable
    private fun CommentParts(questNo: Int, quest: Question) {
        // 해설
        var commentContent by rememberSaveable(questNo) { mutableStateOf(quest.comment) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Spacer(modifier = Modifier.weight(1f))
            val originComment = if (this@ExamInstance.isEditMode) {quest.comment} else {""}
            TextField(
                value = commentContent,
                readOnly = !this@ExamInstance.isEditMode,
                onValueChange = { input ->
                    commentContent = input
                    quest.changeState["COMMENT"] = !originComment.contentEquals(commentContent)
                    quest.comment = commentContent
                },
                placeholder = { Text(text = "해설없음") },
                modifier = Modifier.weight(13f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.padding(all = 12.dp))
    }


    fun saveChanged() {
        this.courses.forEach { crs ->
            crs.let { course ->
                course!!.questions.forEach { quest ->
                    quest?.options?.let {
                        if (quest.changeState.containsValue(true)) {
                            val sortedOption = quest.options.optionMapping.values.sortedBy { it.originNo }
                            var optionContents = ""
                            sortedOption.forEach { optionData ->
                                optionContents += "${optionData.originNo}번 : ${optionData.content.trim()}\n"
                            }
                            optionContents = optionContents.trim()

                            var picture = quest.pictureUniqueTag
                            if (!picture.contains("<!PICTURE@image[0-9]{1,3}>".toRegex())) {
                                picture = quest.altPicture.trim().replace("\n", "<#nbsp;>")
                            }
                            val newContent = "시험명 : $examName\n기출연도 : ${quest.examDate}\n과목 : ${course.courseOrderNum}\n문제번호 : ${quest.examNum}\n문제지문 : ${quest.statement.trim()}\n사진 : ${picture}\n${optionContents}\n정답 : ${quest.options.answerOptionIdx}\n해설 : ${quest.comment.trim().replace("\n", "<#nbsp;>")}"
                            val newAssets = File(filesDir, "assets/exam/${examName}/${quest.fileName}")
                            newAssets.writeText(newContent)
                        }

                        quest.changeState.keys.forEach { key ->
                            quest.changeState[key] = false
                        }
                    }
                }
            }
        }
    }

    fun savePaper(activity: Activity) {
        var contents = ""
        this.courses.forEach { crs ->
            crs.let { course ->
                course!!.questions.forEach { quest ->
                    quest?.options?.let {
                        contents += "${quest.fileName} [${quest.options.getMappingArray().joinToString(",")}] ${quest.options.getSelected()}\n"
                    }
                }
            }
        }
        contents = contents.trim()

        val undonePaperFile = File(filesDir, "usr/undonepaper_${examName}.txt")
        undonePaperFile.writeText(contents)
        if (!this.hasUndonePaper) {
            this.hasUndonePaper = true
            val resultIntent = Intent().apply {
                putExtra("create_undone_paper", true)
            }
            activity.setResult(Activity.RESULT_OK, resultIntent)
        }
    }

    fun examFinish(activity: Activity): Boolean {
        // 저장
        savePaper(activity)

        // 파일 순서번호 찾기
        var idx = 1
        val finishedTime = System.currentTimeMillis()
        while (idx < Int.MAX_VALUE) {
            if (!File(filesDir, "usr/${finishedTime}_${examName}_${idx}.txt").exists()) {
                break
            }
            idx++
        }

        // 파일명 변경
        val oldFile = File(filesDir, "usr/undonepaper_${examName}.txt")
        val newFile = File(filesDir, "usr/${finishedTime}_${examName}_${idx}.txt")
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)

            val resultIntent = Intent().apply {
                putExtra("exam_complete", "${finishedTime}_${examName}_${idx}.txt")
            }
            activity.setResult(Activity.RESULT_OK, resultIntent)
            return true
        }

        return false
    }
}