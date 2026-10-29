package com.example.minimalplayer

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions)

        setContent {
            MinimalPlayerTheme {
                MainAppNavigator()
            }
        }
    }
}

enum class Screen {
    Home, VideoList, Player
}

@Composable
fun MinimalPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
fun MainAppNavigator() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var currentVideoUri by remember { mutableStateOf("") }

    val meshBackgroundBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFC0CB),
            Color(0xFFFFDAB9),
            Color(0xFFE0ECFF)
        ),
        center = Offset(300f, 400f),
        radius = 1600f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(meshBackgroundBrush)
            .padding(16.dp)
    ) {
        when (currentScreen) {
            Screen.Home -> ModernHomeScreen(
                onCardClick = { currentScreen = Screen.VideoList }
            )
            Screen.VideoList -> VideoListScreen(
                onBackClick = { currentScreen = Screen.Home },
                onVideoClick = { uri ->
                    currentVideoUri = uri
                    currentScreen = Screen.Player
                }
            )
            Screen.Player -> VideoPlayerScreen(
                videoUri = currentVideoUri,
                onBackClick = { currentScreen = Screen.VideoList }
            )
        }
    }
}

@Composable
fun ModernHomeScreen(onCardClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "MinimalPlayer",
            fontSize = 26.sp,
            color = Color(0xFF1D1B20),
            modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { 
                GlassCard(
                    title = "本地电影", 
                    subtitle = "4K / 高清专区", 
                    count = "点击进入列表",
                    onClick = onCardClick
                ) 
            }
            item { GlassCard(title = "最近播放", subtitle = "断点续播记录", count = "历史记录", onClick = {}) }
            item { GlassCard(title = "我的收藏", subtitle = "精选喜爱视频", count = "快捷访问", onClick = {}) }
            item { GlassCard(title = "设置中心", subtitle = "外观与解码偏好", count = "个性化", onClick = {}) }
        }
    }
}

@Composable
fun GlassCard(title: String, subtitle: String, count: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heig
