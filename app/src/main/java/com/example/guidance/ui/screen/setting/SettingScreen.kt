package com.example.guidance.ui.screen.setting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.example.guidance.ui.theme.GuidenceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri
import com.example.guidance.model.FolderPickerContract
import com.example.guidance.model.PathParameter

class SettingScreen : ComponentActivity() {
    private var initAssetsLoad by mutableStateOf(true)
    private val examList = mutableStateListOf<String>()
    private val usrFileList = mutableStateListOf<String>()
    private val metaFileList = mutableStateMapOf<String, Boolean>()
    private var savingAssets by mutableStateOf(false)

    private var selectedAssetExam by mutableStateOf("")
    private var selectedUsrExam by mutableStateOf("")

    private var hasAlert by mutableStateOf(false)
    private var alertMsg by mutableStateOf("")
    private var dismissBtnMsg by mutableStateOf("")
    private var confirmBtnMsg by mutableStateOf("")
    private var confirmTxtCor by mutableStateOf(Color.Unspecified)
    private var confirmEvent by mutableStateOf({})


    private val editorActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            //
        }
    }
    private val exportAssetPicker = registerForActivityResult(FolderPickerContract()) { dest ->
        if (dest.uri != null) {
            val sourceDirs = dest.dirNames

            contentResolver.takePersistableUriPermission(
                dest.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    sourceDirs?.forEach { dir ->
                        exportAsset(dest.uri, dir)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingScreen, "저장 완료", Toast.LENGTH_SHORT).show()
                    savingAssets = false
                }
            }
        } else {
            savingAssets = false
        }
    }
    private val importAssetPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    importAsset(uri)
                }
                withContext(Dispatchers.Main) {
                    updateBtnState()
                    Toast.makeText(this@SettingScreen, "추가 완료", Toast.LENGTH_SHORT).show()
                    savingAssets = false
                }
            }
        } else {
            savingAssets = false
        }
    }
    private val exportAllDataPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingScreen, "이 작업은 오래걸립니다.", Toast.LENGTH_LONG).show()
                }
                withContext(Dispatchers.IO) {
                    exportAllData(uri)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingScreen, "저장 완료", Toast.LENGTH_SHORT).show()
                    savingAssets = false
                }
            }
        } else {
            savingAssets = false
        }
    }

    private val importAllDataPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            lifecycleScope.launch {
                var resultCode: Int
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingScreen, "이 작업은 오래걸립니다.", Toast.LENGTH_LONG).show()
                }
                withContext(Dispatchers.IO) {
                    resultCode = importAllData(uri)
                }
                withContext(Dispatchers.Main) {
                    when (resultCode) {
                        0 -> {
                            updateBtnState()
                            Toast.makeText(this@SettingScreen, "덮어쓰기 완료", Toast.LENGTH_LONG).show()
                        }
                        1 -> {
                            Toast.makeText(this@SettingScreen, "실패 : 롤리팝 미만 버전 사용", Toast.LENGTH_LONG).show()
                        }
                        2 -> {
                            Toast.makeText(this@SettingScreen, "실패 : 불러올 폴더에 /assets 폴더 없음", Toast.LENGTH_LONG).show()
                        }
                        3 -> {
                            Toast.makeText(this@SettingScreen, "실패 : 불러올 폴더에 /assets/exam 폴더 없음", Toast.LENGTH_LONG)
                                .show()
                        }
                        4 -> {
                            Toast.makeText(this@SettingScreen, "실패 : 불러올 폴더에 /assets/image 폴더 없음", Toast.LENGTH_LONG)
                                .show()
                        }
                        5 -> {
                            Toast.makeText(this@SettingScreen, "실패 : 불러올 폴더에 /assets/meta 폴더 없음", Toast.LENGTH_LONG)
                                .show()
                        }
                        6 -> {
                            Toast.makeText(this@SettingScreen, "실패 : 불러올 폴더에 /usr 폴더 없음", Toast.LENGTH_LONG).show()
                        }
                        7 -> {
                            Toast.makeText(this@SettingScreen, "실패 : 불러올 폴더에 /setting.txt 파일 없음", Toast.LENGTH_LONG)
                                .show()
                        }
                        8 -> {
                            updateBtnState()
                            Toast.makeText(this@SettingScreen, "assets, usr 덮어쓰기 완료, setting.txt 불러오기 실패", Toast.LENGTH_LONG).show()
                        }
                    }
                    savingAssets = false
                }
            }
        } else {
            savingAssets = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateBtnState()
        setResult(Activity.RESULT_OK)

        setContent {
            GuidenceTheme() {
                Scaffold() { innerPadding ->
                    SettingLoading(Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    fun SettingLoading(modifier: Modifier) {
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // assets 설정
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, start = 32.dp, end = 32.dp), thickness = 2.dp)
            Text(text = "assets", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 32.dp, end = 32.dp), thickness = 2.dp)
            FlowColumn(
                modifier = Modifier.fillMaxWidth().padding(5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                maxItemsInEachColumn = 10
            ) {
                // 전체 assets
                Text(text = "일반 assets 관리", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(5.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "초기 assets 로드")
                        Switch(
                            checked = initAssetsLoad,
                            onCheckedChange = {
                                initAssetsLoad = it
                                saveSetting()
                            }
                        )
                    }

                    // assets 저장
                    Button(
                        modifier = Modifier.height(40.dp),
                        enabled = !savingAssets,
                        onClick = {
                            savingAssets = true
                            exportAssetPicker.launch(
                                PathParameter(
                                    "content://com.android.externalstorage.documents/tree/primary%3ADownload".toUri(),
                                    listOf("assets/exam", "assets/image")
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(111f, 0.57f, 0.36f)),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(text = "assets 내보내기")
                    }

                    // assets 추가
                    Button(
                        modifier = Modifier.height(40.dp),
                        enabled = !savingAssets,
                        onClick = {
                            if (Environment.isExternalStorageManager()) {
                                savingAssets = true
                                importAssetPicker.launch(null)
                            } else {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:$packageName".toUri())
                                startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(129f, 0.57f, 0.36f)),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(text = "assets 불러오기")
                    }

                    // assets 초기화
                    Button(
                        modifier = Modifier.height(40.dp),
                        enabled = !savingAssets,
                        onClick = {
                            savingAssets = true
                            alertMsg = "모든 asset을 삭제하고 초기 상태로 복구할까요? 이 작업은 앱의 재시작이 필요합니다."
                            dismissBtnMsg = "취소"
                            confirmBtnMsg = "초기화"
                            confirmEvent = { resetAssets() }
                            confirmTxtCor = Color.hsv(0f, 0.84f, 0.73f)
                            hasAlert = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(0f, 0.84f, 0.73f)),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(text = "assets 초기화")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 96.dp, vertical = 16.dp), thickness = 1.dp)

                // 개별 assets
                Text(text = "개별 assets 관리", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(5.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        examList.forEach { examName ->
                            Button(
                                modifier = Modifier.height(40.dp),
                                onClick = {
                                    selectedAssetExam = examName
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (examName.contentEquals(selectedAssetExam)) {Color.Unspecified} else {Color.Gray}),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(text = examName)
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    ) {
                        if (selectedAssetExam.isNotEmpty()) {
                            Button(
                                modifier = Modifier.height(30.dp),
                                enabled = !savingAssets,
                                onClick = {
                                    savingAssets = true
                                    exportAssetPicker.launch(PathParameter("content://com.android.externalstorage.documents/tree/primary%3ADownload".toUri(), listOf("assets/exam/${selectedAssetExam}", "assets/image/${selectedAssetExam}")))
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(145f, 0.57f, 0.36f)),
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(text = "$selectedAssetExam 저장")
                            }

                            Button(
                                modifier = Modifier.height(30.dp),
                                enabled = !savingAssets,
                                onClick = {
                                    alertMsg = "$selectedAssetExam 관련 asset을 삭제할까요?"
                                    dismissBtnMsg = "취소"
                                    confirmBtnMsg = "삭제"
                                    confirmEvent = {
                                        deleteAsset(selectedAssetExam)
                                    }
                                    confirmTxtCor = Color.hsv(0f, 0.84f, 0.73f)
                                    hasAlert = true
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(0f, 0.84f, 0.73f)),
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(text = "$selectedAssetExam 삭제")
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 96.dp, vertical = 16.dp), thickness = 1.dp)

                // meta 수정
                Text(text = "meta 설정", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    val existFile = Color.hsv(160f, 0.57f, 0.36f)
                    val notExistFile = Color.hsv(25f, 0.78f, 0.69f)
                    metaFileList.keys.forEach { key ->
                        Button(
                            modifier = Modifier.height(40.dp),
                            onClick = {
                                openEditor("assets/meta/${key}.txt", if (!metaFileList[key]!!) {"문제 수 : 15\n과목 : 일반A;일반B;일반C\n과목 당 문제 수 : 3;5;7\n선지 수 : 4"} else {""})
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (metaFileList[key]!!) {existFile} else {notExistFile}),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(text = key)
                        }
                    }
                }
            }

            // usr 설정
            HorizontalDivider(modifier = Modifier.padding(top = 64.dp, bottom = 12.dp, start = 32.dp, end = 32.dp), thickness = 2.dp)
            Text(text = "usr", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 32.dp, end = 32.dp), thickness = 2.dp)
            FlowColumn(
                modifier = Modifier.fillMaxWidth().padding(5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                maxItemsInEachColumn = 10
            ) {
                // usr
                Text(text = "  시험별 usr 설정", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(5.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        examList.forEach { examName ->
                            Button(
                                modifier = Modifier.height(40.dp),
                                onClick = {
                                    selectedUsrExam = examName
                                    getUsrFileList(examName)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (examName.contentEquals(selectedUsrExam)) {Color.Unspecified} else {Color.Gray}),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(text = examName)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    ) {
                        usrFileList.forEach { fileName ->
                            Button(
                                modifier = Modifier.height(30.dp),
                                onClick = {
                                    openEditor("usr/${fileName}")
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(173f, 0.57f, 0.36f)),
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(text = fileName)
                            }
                        }
                    }
                }
            }

            // 내부저장소
            HorizontalDivider(modifier = Modifier.padding(top = 64.dp, bottom = 12.dp, start = 32.dp, end = 32.dp), thickness = 2.dp)
            Text(text = "Internal Storage", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 32.dp, end = 32.dp), thickness = 2.dp)
            FlowColumn(
                modifier = Modifier.fillMaxWidth().padding(5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                maxItemsInEachColumn = 10
            ) {
                // 앱 데이터 관리
                Text(text = "앱 데이터", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(5.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Button(
                            modifier = Modifier.height(40.dp),
                            enabled = !savingAssets,
                            onClick = {
                                savingAssets = true
                                alertMsg = "모든 파일을 삭제 후, 지정한 데이터로 모두 덮어씌웁니다."
                                dismissBtnMsg = "취소"
                                confirmBtnMsg = "덮어쓰기"
                                confirmEvent = { importAllDataPicker.launch(null) }
                                confirmTxtCor = Color.hsv(0f, 0.84f, 0.73f)
                                hasAlert = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(155f, 0.57f, 0.36f)),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text(text = "전체 앱데이터 불러오기")
                        }

                        Button(
                            modifier = Modifier.height(40.dp),
                            enabled = !savingAssets,
                            onClick = {
                                savingAssets = true
                                exportAllDataPicker.launch(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.hsv(155f, 0.57f, 0.36f)),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text(text = "전체 앱데이터 내보내기")
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 64.dp, bottom = 12.dp, start = 32.dp, end = 32.dp), thickness = 2.dp)
        }

        if (hasAlert) {
            AlertDialog(onDismissRequest = {
                hasAlert = false
                alertMsg = ""
                confirmBtnMsg = ""
                dismissBtnMsg = ""
                confirmEvent = {}
                confirmTxtCor = Color.Unspecified
                savingAssets = false
            }, title = {
                Text(text = "")
            }, text = {
                Text(text = alertMsg)
            }, confirmButton = {
                TextButton(
                    onClick = {
                        confirmEvent()

                        hasAlert = false
                        alertMsg = ""
                        confirmBtnMsg = ""
                        dismissBtnMsg = ""
                        confirmEvent = {}
                        confirmTxtCor = Color.Unspecified
                    }
                ) {
                    Text(text = confirmBtnMsg, color = confirmTxtCor)
                }
            }, dismissButton = {
                TextButton(onClick = {
                    hasAlert = false
                    alertMsg = ""
                    confirmBtnMsg = ""
                    dismissBtnMsg = ""
                    confirmEvent = {}
                    confirmTxtCor = Color.Unspecified
                    savingAssets = false
                }) {
                    Text(text = dismissBtnMsg)
                }
            })
        }
    }

    private fun importAllData(sourceUri: Uri): Int {
        val sourceDir = DocumentFile.fromTreeUri(this@SettingScreen, sourceUri) ?: return 1
        val destAssetsDir = File(filesDir, "assets")
        val destUsrDir = File(filesDir, "usr")
        val destSettingFile = File(filesDir, "setting.txt")

        // assets 폴더 존재 확인
        var sourceAssetsFolder: DocumentFile? = null
        for (file in sourceDir.listFiles()) {
            if (file.isDirectory && !file.name.isNullOrEmpty() && file.name.contentEquals("assets")) {
                sourceAssetsFolder = file
                break
            }
        }
        if (sourceAssetsFolder == null) {
            return 2
        }

        // assets 폴더 내 exam 폴더 존재 확인
        var sourceExamFolder: DocumentFile? = null
        for (file in sourceAssetsFolder.listFiles()) {
            if (file.isDirectory && !file.name.isNullOrEmpty() && file.name.contentEquals("exam")) {
                sourceExamFolder = file
                break
            }
        }
        if (sourceExamFolder == null) {
            return 3
        }

        // assets 폴더 내 image 폴더 존재 확인
        var sourceImageFolder: DocumentFile? = null
        for (file in sourceAssetsFolder.listFiles()) {
            if (file.isDirectory && !file.name.isNullOrEmpty() && file.name.contentEquals("image")) {
                sourceImageFolder = file
                break
            }
        }
        if (sourceImageFolder == null) {
            return 4
        }

        // assets 폴더 내 meta 폴더 존재 확인
        var sourceMetaFolder: DocumentFile? = null
        for (file in sourceAssetsFolder.listFiles()) {
            if (file.isDirectory && !file.name.isNullOrEmpty() && file.name.contentEquals("meta")) {
                sourceMetaFolder = file
                break
            }
        }
        if (sourceMetaFolder == null) {
            return 5
        }

        // usr 폴더 존재 확인
        var sourceUsrFolder: DocumentFile? = null
        for (file in sourceDir.listFiles()) {
            if (file.isDirectory && !file.name.isNullOrEmpty() && file.name.contentEquals("usr")) {
                sourceUsrFolder = file
                break
            }
        }
        if (sourceUsrFolder == null) {
            return 6
        }

        // setting.txt 파일 존재 확인
        var sourceSettingFile: DocumentFile? = null
        for (file in sourceDir.listFiles()) {
            if (file.isFile && !file.name.isNullOrEmpty() && file.name.contentEquals("setting.txt")) {
                sourceSettingFile = file
                break
            }
        }
        if (sourceSettingFile == null) {
            return 7
        }

        // 모두 삭제
        deleteDirectory(File(filesDir, "assets"))
        deleteDirectory(File(filesDir, "usr"))
        File(filesDir, "setting.txt").delete()

        // 폴더 재생성
        destAssetsDir.mkdirs()
        destUsrDir.mkdirs()

        // assets, usr, setting.txt 불러오기
        copyDirectory(sourceAssetsFolder, destAssetsDir)
        copyDirectory(sourceUsrFolder, destUsrDir)
        contentResolver.openInputStream(sourceSettingFile.uri)?.use { input ->
            destSettingFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return 0
    }

    private fun exportAllData(destUri: Uri) {
        val assetsDir = File(filesDir, "assets")
        if (!assetsDir.exists() || !assetsDir.isDirectory) {
            return
        }

        val usrDir = File(filesDir, "usr")
        if (!usrDir.exists() || !usrDir.isDirectory) {
            return
        }

        val settingFile = File(filesDir, "setting.txt")
        if (!settingFile.exists() || !settingFile.isFile) {
            return
        }

        var newFolder = DocumentFile.fromTreeUri(this, destUri) ?: return
        newFolder = newFolder.findFile("Guidance")?.takeIf { it.isDirectory } ?: newFolder.createDirectory("Guidance") ?: return
        val newAssets = newFolder.findFile("assets")?.takeIf { it.isDirectory } ?: newFolder.createDirectory("assets") ?: return
        val newUsr = newFolder.findFile("usr")?.takeIf { it.isDirectory } ?: newFolder.createDirectory("usr") ?: return

        copyDirectory(assetsDir, newAssets)
        copyDirectory(usrDir, newUsr)
        val newFile = newFolder.createFile("application/octet-stream", settingFile.name) ?: return
        contentResolver.openOutputStream(newFile.uri)?.use { output ->
            settingFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    private fun deleteAsset(assetName: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                deleteDirectory(File(filesDir, "assets/exam/${assetName}"))
                deleteDirectory(File(filesDir, "assets/image/${assetName}"))
            }
            withContext(Dispatchers.Main) {
                updateBtnState()
                Toast.makeText(this@SettingScreen, "삭제 완료", Toast.LENGTH_LONG).show()
                savingAssets = false
            }
        }
    }

    private fun resetAssets() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                deleteDirectory(File(filesDir, "assets/exam"))
                deleteDirectory(File(filesDir, "assets/image"))
            }
            withContext(Dispatchers.Main) {
                savingAssets = false
                initAssetsLoad = true
                saveSetting()
                Toast.makeText(this@SettingScreen, "초기화 완료, 앱을 재시작 해주세요.", Toast.LENGTH_LONG).show()

                ActivityCompat.finishAffinity(this@SettingScreen)
                finishAffinity()
            }
        }
    }

    private fun importAsset(sourceUri: Uri): Boolean {
        val sourceDir = DocumentFile.fromTreeUri(this@SettingScreen, sourceUri) ?: return false
        val destExamDir = File(filesDir, "assets/exam")
        val destImageDir = File(filesDir, "assets/image")

        if (destExamDir.exists()) {
            destExamDir.mkdirs()
        }

        if (destImageDir.exists()) {
            destImageDir.mkdirs()
        }

        // exam 폴더 존재 확인
        var sourceExamFolder: DocumentFile? = null
        for (file in sourceDir.listFiles()) {
            if (file.isDirectory && !file.name.isNullOrEmpty() && file.name.contentEquals("exam")) {
                sourceExamFolder = file
                break
            }
        }
        if (sourceExamFolder == null) {
            return false
        }

        // image 폴더 존재 확인
        var sourceImageFolder: DocumentFile? = null
        for (file in sourceDir.listFiles()) {
            if (file.isDirectory && !file.name.isNullOrEmpty() && file.name.contentEquals("image")) {
                sourceImageFolder = file
                break
            }
        }

        copyDirectory(sourceExamFolder, destExamDir)
        if (sourceImageFolder != null) {
            copyDirectory(sourceImageFolder, destImageDir)
        }
        return true
    }

    private fun exportAsset(destUri: Uri, sourceDirName: String) {
        val sourceDir = File(filesDir, sourceDirName)
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            return
        }

        var newFolder = DocumentFile.fromTreeUri(this, destUri) ?: return
        ("Guidance/$sourceDirName").split("/").forEach { path ->
            newFolder = newFolder.findFile(path)?.takeIf { it.isDirectory } ?: newFolder.createDirectory(path) ?: return
        }

        copyDirectory(sourceDir, newFolder)
    }

    private fun deleteDirectory(target: File) {
        target.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        }
        target.delete()
    }

    private fun copyDirectory(source: DocumentFile, dest: File) {
        source.listFiles().forEach { file ->
            if (file.isDirectory) {
                val newDest = File(dest, file.name!!)
                newDest.mkdirs()
                copyDirectory(file, newDest)
            } else {
                val fileName = file.name ?: return@forEach
                val destFile = File(dest, fileName)
                contentResolver.openInputStream(file.uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun copyDirectory(source: File, dest: DocumentFile) {
        source.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val newDir = dest.createDirectory(file.name) ?: return@forEach
                copyDirectory(file, newDir)
            } else {
                val newFile = dest.createFile("application/octet-stream", file.name) ?: return@forEach
                contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    fun openEditor(path: String, defaultContent: String = "") {
        val intent = Intent(this@SettingScreen, EditTextScreen::class.java)
        intent.putExtra("path", path)
        intent.putExtra("defaultContent", defaultContent)
        editorActivityLauncher.launch(intent)
    }

    fun saveSetting() {
        val settingFile = File(filesDir, "setting.txt")

        if (!settingFile.exists()) {
            settingFile.writeText("초기 assets 불러오기 : true\n")
        }

        settingFile.writeText("초기 assets 불러오기 : $initAssetsLoad\n")
    }

    fun updateBtnState() {
        getExamList()
        if (examList.isNotEmpty()) {
            selectedAssetExam = if (examList.contains(selectedAssetExam)) {selectedAssetExam}  else {examList[0]}
            selectedUsrExam = if (examList.contains(selectedUsrExam)) {selectedUsrExam} else {examList[0]}
            getUsrFileList(selectedUsrExam)
        } else {
            selectedAssetExam = ""
            selectedUsrExam = ""
            usrFileList.clear()
        }
        getMetaFileList()
        getSettings()
    }

    fun getSettings() {
        val settingFile = File(filesDir, "setting.txt")

        if (!settingFile.exists()) {
            settingFile.writeText("초기 assets 불러오기 : true\n")
        }

        val settings = settingFile.readText().split("\n")
        initAssetsLoad = settings[0].split(":")[1].trim().contentEquals("true")
    }

    fun getExamList() {
        val examFolder = File(filesDir, "assets/exam/")

        if (!examFolder.exists()) {
            examFolder.mkdirs()
        }

        examList.clear()
        examFolder.list()!!.forEach { folder ->
            examList.add(folder)
        }
    }

    fun getUsrFileList(examName: String) {
        val usrFolder = File(filesDir, "usr/")

        if (!usrFolder.exists()) {
            usrFolder.mkdirs()
        }

        usrFileList.clear()
        usrFolder.list()!!.forEach { file ->
            if (file.contains("undonepaper_${examName}.txt")) {
                usrFileList.add(file)
            }
            if (file.contains("[0-9]{13}_${examName}_[0-9]{1,13}[.]txt".toRegex())) {
                usrFileList.add(file)
            }
            if (file.contains("${examName}_history.txt")) {
                usrFileList.add(file)
            }
        }
    }

    fun getMetaFileList() {
        val metaFolder = File(filesDir, "assets/meta/")

        if (metaFolder.exists()) {
            metaFolder.mkdirs()
        }

        metaFileList.clear()
        examList.forEach { folder ->
            val examMetaFile = File(metaFolder, "${folder}.txt")
            metaFileList[folder] = examMetaFile.exists()
        }
    }
}