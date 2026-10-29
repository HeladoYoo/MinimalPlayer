package com.example.minimalplayer

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Home) }
    var currentVideoUri by rememberSaveable { mutableStateOf("") }

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
            .padding(if (currentScreen == Screen.Player) 0.dp else 16.dp)
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
            Text(text = "本地视频库", fontSize = 22.sp, color = Color(0xFF1D1B20))
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
            // ✨ 替换为双列网格布局样式
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videoList) { video ->
                    VideoCardItem(video = video, onClick = { onVideoClick(video.uri) })
                }
            }
        }
    }
}

// ✨ 双列网格中的单个视频卡片组件
@Composable
fun VideoCardItem(video: VideoItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 模拟视频封面图区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "▶ 播放", fontSize = 13.sp, color = Color.DarkGray)
                
                // 右下角显示视频时长
                val min = video.duration / 1000 / 60
                val sec = (video.duration / 1000) % 60
                val durationText = String.format("%02d:%02d", min, sec)
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = durationText, fontSize = 10.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 视频标题（最多显示 2 行）
            Text(
                text = video.name,
                fontSize = 13.sp,
                color = Color(0xFF1D1B20),
                maxLines = 2,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "本地高清视频",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun VideoPlayerScreen(videoUri: String, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        val window = activity?.window
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var isSpeedingUp by remember { mutableStateOf(false) }
    var showSpeedTip by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "初始化异常: ${e.localizedMessage}"
            }

            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    errorMessage = "播放失败: ${error.errorCodeName} - ${error.message}"
                }
            })
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    var slideTipText by remember { mutableStateOf("") }
    var targetTimeText by remember { mutableStateOf("") }
    var showSlideTip by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        showSlideTip = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!isSpeedingUp && abs(dragAmount) > 2f) {
                            val currentPosition = exoPlayer.currentPosition
                            val duration = exoPlayer.duration.coerceAtLeast(1L)
                            val seekOffset = (dragAmount * 15).toLong() 
                            var targetPosition = (currentPosition + seekOffset).coerceIn(0L, duration)
                            
                            exoPlayer.seekTo(targetPosition)
                            
                            val offsetSec = seekOffset / 1000
                            val sign = if (offsetSec >= 0) "+" else ""
                            slideTipText = if (dragAmount > 0) "快进 ⏩ (${sign}${offsetSec}s)" else "快退 ⏪ (${offsetSec}s)"
                            targetTimeText = "${formatDuration(targetPosition)} / ${formatDuration(duration)}"
                            showSlideTip = true
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT 
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    exoPlayer.release()
                    onBackClick()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Text(text = "返回", color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            val startTime = System.currentTimeMillis()
                            val startPosition = down.position
                            var isDragging = false
                            var isLongPress = false

                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { !it.pressed }) {
                                    break
                                }
                                
                                val currentPos = event.changes.first().position
                                if (abs(currentPos.x - startPosition.x) > 10f) {
                                    isDragging = true
                                    break
                                }

                                if (System.currentTimeMillis() - startTime > 400) {
                                    isLongPress = true
                                    break
                                }
                            }

                            if (isDragging) {
                                var lastX = startPosition.x
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { !it.pressed }) {
                                        showSlideTip = false
                                        break
                                    }
                                    val currentX = event.changes.first().position.x
                                    val deltaX = currentX - lastX
                                    lastX = currentX

                                    if (abs(deltaX) > 0.5f) {
                                        val currentPosition = exoPlayer.currentPosition
                                        val duration = exoPlayer.duration.coerceAtLeast(1L)
                                        val seekOffset = (deltaX * 15).toLong()
                                        var targetPosition = (currentPosition + seekOffset).coerceIn(0L, duration)
                                        
                                        exoPlayer.seekTo(targetPosition)
                                        
                                        val offsetSec = seekOffset / 1000
                                        val sign = if (offsetSec >= 0) "+" else ""
                                        slideTipText = if (deltaX > 0) "快进 ⏩ (${sign}${offsetSec}s)" else "快退 ⏪ (${offsetSec}s)"
                                        targetTimeText = "${formatDuration(targetPosition)} / ${formatDuration(duration)}"
                                        showSlideTip = true
                                        showSpeedTip = false
                                    }
                                }
                            } else if (isLongPress) {
                                exoPlayer.setPlaybackSpeed(2.0f)
                                isSpeedingUp = true
                                showSpeedTip = true
                                showSlideTip = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { !it.pressed }) {
                                        break
                                    }
                                }

                                exoPlayer.setPlaybackSpeed(1.0f)
                                isSpeedingUp = false
                                showSpeedTip = false
                            }
                        }
                    }
                }
        )

        if (showSpeedTip || showSlideTip || errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = errorMessage ?: if (showSpeedTip) "⚡ 正在以 2.0x 倍速播放" else slideTipText,
                        color = if (errorMessage != null) Color.Red else Color.White,
                        fontSize = 15.sp
                    )
                    if (showSlideTip && !showSpeedTip && errorMessage == null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = targetTimeText,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

data class VideoItem(val name: String, val uri: String, val duration: Long)
