package com.example.guidance

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.guidance.ui.screen.home.HomeScreen
import com.example.guidance.ui.theme.GuidenceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    private var isAssetsLoading by mutableStateOf(false)
    private var initAssetsLoad by mutableStateOf(true)
    private val homeActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            ActivityCompat.finishAffinity(this)
            finishAffinity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        getSettings()

        setContent {
            GuidenceTheme() {
                Scaffold() { innerPadding ->
                    MainLoading(Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    fun MainLoading(modifier: Modifier) {
        val scope = rememberCoroutineScope()

        Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (initAssetsLoad && !isAssetsLoading) {
                Text(text = "loading assets...")
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        loadingAssets()
                        isAssetsLoading = true
                    }
                }
            } else {
                Text(text = "loaded assets")
                enterHome()
            }
        }
    }

    fun getSettings() {
        val settingFile = File(filesDir, "setting.txt")

        if (!settingFile.exists()) {
            settingFile.writeText("초기 assets 불러오기 : true\n")
        }

        val settings = settingFile.readText().split("\n")
        initAssetsLoad = settings[0].split(":")[1].trim().contentEquals("true")
    }

    fun loadingAssets() {
        // 폴더 생성
        val internalAssets = File(filesDir, "assets/")
        if (!internalAssets.exists()) {
            internalAssets.mkdirs()
        }

        val internalUSER = File(filesDir, "usr/")
        if (!internalUSER.exists()) {
            internalUSER.mkdirs()
        }

        // asset폴더 내부저장소로 복사
        copyAssetFolder("exam", File(filesDir, "assets/exam"))
        copyAssetFolder("image", File(filesDir, "assets/image"))
        copyAssetFolder("meta", File(filesDir, "assets/meta"))
    }

    fun enterHome() {
        val intent = Intent(this@MainActivity, HomeScreen::class.java)
        homeActivityLauncher.launch(intent)
    }

    fun copyAssetFolder(fromAssetPath: String, toPath: File): Boolean {
        try {
            val assetManager = this.assets
            val files = assetManager.list(fromAssetPath) ?: return false

            if (!toPath.exists()) {
                toPath.mkdirs()
            }

            var res = true
            files.forEach { fileName ->
                val assetFilePath = if (fromAssetPath.isEmpty()) fileName else "$fromAssetPath/$fileName"
                val destFile = File(toPath, fileName)

                val subFiles = assetManager.list(assetFilePath)
                if (!subFiles.isNullOrEmpty()) {
                    res = res && copyAssetFolder(assetFilePath, destFile)
                } else {
                    res = res && copyAssetFile(assetFilePath, destFile)
                }
            }
            return res
        } catch (e1: IOException) {
            e1.printStackTrace()
            return false
        } catch (e2: SecurityException) {
            e2.printStackTrace()
            return false
        }
    }

    fun copyAssetFile(assetFilePath: String, destFile: File): Boolean {
        if (!destFile.exists()) {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                inputStream = this.assets.open(assetFilePath)
                outputStream = FileOutputStream(destFile)
                inputStream.copyTo(outputStream)
            } catch (e1: FileNotFoundException) {
                e1.printStackTrace()
                return false
            } catch (e2: IOException) {
                e2.printStackTrace()
                return false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }
        return true
    }
}
