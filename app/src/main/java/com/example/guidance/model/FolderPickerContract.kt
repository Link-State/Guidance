package com.example.guidance.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract


class FolderPickerContract :
    ActivityResultContract<PathParameter, ResultParameter>() {

    // Activity 재생성 및 프로세스 종료 후 유지 보장 X
    private var request: PathParameter? = null

    override fun createIntent(context: Context, input: PathParameter): Intent {
        request = input
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            input.initialUri?.let {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ResultParameter {
        val uri = if (resultCode == Activity.RESULT_OK) {intent?.data} else {null}
        return ResultParameter(
            uri = uri,
            dirNames = request?.folderNames
        )
    }
}