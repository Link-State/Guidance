package com.example.guidance.model

import android.net.Uri

data class PathParameter(
    val initialUri: Uri?,
    val folderNames: List<String>?
)
