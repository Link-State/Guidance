package com.example.guidance.model

import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScoreLibrary(
    val examInstance: ExamInstance,
    val examName: String
) {
    public val history = mutableMapOf<String, History>()
    private val historyRegex = "([0-9]{4}-[0-9]{2}-[0-9]{2}#[0-9]+[.]txt)\\s([0-9]{4}-[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2})\\s([0-9]+)\\s([0-9]+)\r?\n".toRegex()

    //
    init {
        val historyFile = File(this.examInstance.filesDir, "usr/${this.examName}_history.txt")
        if (!historyFile.exists()) {
            historyFile.writeText("")
        }

        val historyContent = historyFile.readText()
        if (historyContent.isNotEmpty()) {
            historyRegex.findAll(historyContent).forEach { line ->
                val fileName = line.groups[1]?.value
                val formattedDate = line.groups[2]?.value
                val accWrong = line.groups[3]?.value?.toIntOrNull()
                val comboWrong = line.groups[4]?.value?.toIntOrNull()
                if (fileName != null && formattedDate != null && accWrong != null && comboWrong != null) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val localDateTime = LocalDateTime.parse(formattedDate, formatter)
                    this.history[fileName] = History(fileName, localDateTime, accWrong, comboWrong)
                }
            }
        }
    }

    // 기존 파일 덮어쓰기 후 저장
    fun saveHistories() {
        val histFile = File(this.examInstance.filesDir, "usr/${this.examName}_history.txt")
        histFile.writeText("")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        this.history.keys.forEach { key ->
            histFile.appendText("${this.history[key]!!.fileName} ${formatter.format(this.history[key]!!.recentWrong)} ${this.history[key]!!.accWrongCount} ${this.history[key]!!.comboWrongCount}\n")
        }
    }

    fun hasQuestion(question: Question): Boolean {
        return this.history.contains(question.fileName)
    }

    fun hasQuestion(fileName: String): Boolean {
        return this.history.contains(fileName)
    }

    fun getHistory(question: Question): History? {
        return this.history[question.fileName]
    }

    fun getHistory(fileName: String): History? {
        return this.history[fileName]
    }

}