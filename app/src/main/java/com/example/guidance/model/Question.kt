package com.example.guidance.model

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.collections.set
import kotlin.math.pow

class Question(
    val course: Course,
    val fileName: String,
    val questNo: Int,
    private val prevMarked: Int = -1,
    private val randomMapping: Array<Int> = arrayOf()
) {
    public val examDate: String
    public val examNum: String
    public var statement: String
    public val pictureUniqueTag: String
    public val bitmapPicture: ImageBitmap?
    public var altPicture: String = ""
    public val options: Option?
    public var comment: String = ""
    public val changeState: MutableMap<String, Boolean> = mutableMapOf()

    // 정규표현식 생성
    private val optionRegex = (List<String>(this.course.examInstance.meta.optionAmount) { opc ->
        "\r?\n${opc+1}번 : (.*)"
    }).joinToString(separator = "")
    val examRegex = "시험명 : ${this.course.examInstance.examName}\r?\n기출연도 : ([0-9]{4}-[0-9]{2}-[0-9]{2})\r?\n과목 : [0-9]\r?\n문제번호 : ([0-9]{1,4})\r?\n문제지문 : (.*)\r?\n사진 : (.*)${this.optionRegex}\r?\n정답 : ([0-9]*)\r?\n해설 : (.*)".toRegex()

    init {
        // 문제 파싱
        val contents = File(this.course.examInstance.filesDir, "assets/exam/${this.course.examInstance.examName}/${this.fileName}").readText()
        val match = this.examRegex.find(contents)?.groups
        if (match != null) {
            this.examDate = match[1]?.value ?: "None"
            this.examNum = match[2]?.value ?: "None"
            this.statement = match[3]?.value ?: "None"
            this.pictureUniqueTag = match[4]?.value ?: "None"
            this.altPicture = ""

            // 정답은 1부터 시작해서 2의 제곱수들의 합으로 저장되어있음
            val answer = match[5+this.course.examInstance.meta.optionAmount]?.value?.toIntOrNull() ?: 0

            // 옵션 랜덤위치화
            val optionMapping: MutableMap<Int, OptionData> = mutableMapOf()
            if (this.randomMapping.isNotEmpty() && this.randomMapping.size == this.course.examInstance.meta.optionAmount) {
                for (opc in this.randomMapping.indices) {
                    optionMapping[this.randomMapping[opc]] = OptionData(opc+1, match[5+opc]?.value ?: "None")
                }
            } else {
                val shuffledArray = (Array<Int>(this.course.examInstance.meta.optionAmount){ i -> i+1 })
                shuffledArray.shuffle()
                for (opc in shuffledArray.indices) {
                    optionMapping[shuffledArray[opc]] = OptionData(opc+1, match[5+opc]?.value ?: "None")
                }
            }

            this.options = Option(this, optionMapping, answer, if (this.prevMarked >= 0) {this.prevMarked} else {0})
            this.comment = (match[6+optionMapping.size]?.value ?: "None").replace("<#nbsp;>", "\n").trim()

            if (this.pictureUniqueTag.contains("<!PICTURE@image[0-9]{1,3}>".toRegex())) {
                val inputStream = File(this.course.examInstance.filesDir, "assets/image/${this.course.examInstance.examName}/${this.examDate.replace("-", "")}/${this.pictureUniqueTag.replace("<!PICTURE@", "").replace(">", "").trim()}.png").inputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                this.bitmapPicture = bitmap.asImageBitmap()
            } else {
                this.bitmapPicture = null
                this.altPicture = this.pictureUniqueTag.replace("<#nbsp;>", "\n").trim()
            }

            // 수정사항
            this.changeState["STATEMENT"] = false
            this.changeState["ALT_PICTURE"] = false
            for (i in (1..optionMapping.size)) {
                this.changeState["OPTION_${i}"] = false
            }
            this.changeState["COMMENT"] = false
        } else {
            this.examDate = "Null"
            this.examNum = "Null"
            this.statement = this.fileName
            this.pictureUniqueTag = "Null"
            this.bitmapPicture = null
            this.altPicture = "Null"
            this.options = null
//            Toast.makeText(this, "${course[partNo]} 파일을 불러오는데 문제가 발생했습니다.", Toast.LENGTH_LONG).show()
        }
    }
}