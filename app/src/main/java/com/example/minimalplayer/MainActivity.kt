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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

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
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 17.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 11.sp, color = Color.DarkGray)
            }
            Text(text = count, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun VideoListScreen(onBackClick: () -> Unit, onVideoClick: (String) -> Unit) {
    val context = LocalContext.current
    var videoList by remember { mutableStateOf(listOf<VideoItem>()) }

    LaunchedEffect(Unit) {
        val list = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "未知视频"
                val duration = it.getLong(durationColumn)
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
                
                list.add(VideoItem(name, contentUri.toString(), duration))
            }
        }
        videoList = list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "本地视频库", fontSize = 22.sp, color = Color.Black)
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.6f))
            ) {
                Text(text = "返回", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (videoList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未找到视频，请检查是否已授予存储/媒体权限",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(videoList) { video ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.45f))
                            .clickable {
                                onVideoClick(video.uri)
                            }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(text = video.name, fontSize = 15.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "时长: ${video.duration / 1000 / 60} 分钟 | 点击内置播放", 
                                fontSize = 11.sp, 
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(videoUri: String, onBackClick: () -> Unit) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = {
                exoPlayer.release()
                onBackClick()
            },
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Text(text = "返回", color = Color.White)
        }
    }
}

data class VideoItem(val name: String, val uri: String, val duration: Long)
