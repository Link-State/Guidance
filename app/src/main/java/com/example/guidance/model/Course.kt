package com.example.guidance.model

import android.util.Log
import java.io.File

class Course(
    val examInstance: ExamInstance,
    val courseName: String,
    val courseOrderNum: Int,        // 1부터 시작
    val questAmountPerCourse: Int,
    val existingFile: File? = null
) {
    val questions = Array<Question?>(this.questAmountPerCourse) { null }
    private val questFileNameRegex = "[0-9]{4}-[0-9]{2}-[0-9]{2}#([0-9]*).txt".toRegex()
    val correctQuestions: MutableMap<Int, Question> = mutableMapOf()    // Option 클래스에서 실시간 수정됨
    val incorrectQuestions: MutableMap<Int, Question> = mutableMapOf()    // Option 클래스에서 실시간 수정됨

    init {
        if (this.existingFile != null) {
            // existingFile 파일을 열어서 해당 과목에 해당하는 문제(행)들 불러오기
            val content = this.existingFile.readText().split("\n")
            val startIdx = this.examInstance.meta.questAmountPerCourse.subList(0, this.courseOrderNum-1).sum()
            val mappingRegex = ("\\[(" + (List<String>(this.examInstance.meta.optionAmount) { "[0-9]*" }).joinToString(",") + ")\\]").toRegex()
            for (i in this.questions.indices) {
                val line = content[startIdx+i].split(" ")
                val match = mappingRegex.find(line[1])!!.groups
                if (match[1] != null) {
                    val fileName = line[0].trim()
                    val randomMapping = match[1]!!.value.split(",").map { i -> i.toInt() }.toTypedArray()
                    val prevMarker = line[2].trim().toInt()
                    this.questions[i] = Question(this, fileName, i, prevMarker, randomMapping)
                }
            }
        } else {
            // 기출문제파일 이름 저장할 리스트 생성
            val questForeachCourse = mutableListOf<String>()

            // 과목별 기출문제파일 분류
            File(this.examInstance.filesDir, "assets/exam/${this.examInstance.examName}/").list()?.forEach { fileName ->
                this.questFileNameRegex.find(fileName)?.groups[1]?.value?.let { strNum ->
                    var num = strNum.toInt()
                    if (num > this.examInstance.meta.questAmount) {
                        num %= this.examInstance.meta.questAmount
                        num++
                        // 오류알림 : 문제번호 초과 -> 과목 재할당
                    }

                    if (this.courseOrderNum == this.examInstance.questNoToCourseIdx[num-1]) {
                        questForeachCourse.add(fileName)
                    }
                }
            }

            // 랜덤 문제 출제
            for (i in this.questions.indices) {
                val gotcha = questForeachCourse.indices.random()
                val questFileName = questForeachCourse[gotcha]
                questForeachCourse.removeAt(gotcha)
                this.questions[i] = Question(this, questFileName, i)
            }
        }
    }

    // 현재 과목 내 남은문제
    public fun remainQuestion(): MutableList<Question> {
        val result = mutableListOf<Question>()
         this.questions.forEach { quest ->
             if (quest?.options != null && quest.options.getSelected() == 0) {
                 result.add(quest)
             }
         }
        return result
    }
}