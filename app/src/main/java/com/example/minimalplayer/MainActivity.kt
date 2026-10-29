package com.example.minimalplayer

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MinimalPlayerTheme {
                MainAppNavigator()
            }
        }
    }
}

enum class Screen {
    PermissionRequest, Home, VideoList, Player, Settings, AppearanceSettings
}

@Composable
fun MinimalPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

fun hasVideoPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun MainAppNavigator() {
    val context = LocalContext.current
    var currentScreen by rememberSaveable { 
        mutableStateOf(if (hasVideoPermission(context)) Screen.Home else Screen.PermissionRequest) 
    }
    var currentVideoUri by rememberSaveable { mutableStateOf("") }
    var currentVideoName by rememberSaveable { mutableStateOf("") }
    var isDarkMode by rememberSaveable { mutableStateOf(false) }

    val backgroundModifier = if (isDarkMode) {
        Modifier.fillMaxSize().background(Color(0xFF121212))
    } else {
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFC0CB), Color(0xFFFFDAB9), Color(0xFFE0ECFF)),
                center = Offset(300f, 400f),
                radius = 1600f
            )
        )
    }

    Box(
        modifier = backgroundModifier.padding(if (currentScreen == Screen.Player) 0.dp else 16.dp)
    ) {
        when (currentScreen) {
            Screen.PermissionRequest -> PermissionRequestScreen(
                onGrantedOrSkipped = { currentScreen = Screen.Home }
            )
            Screen.Home -> ModernHomeScreen(
                isDarkMode = isDarkMode,
                onCardClick = { cardTitle ->
                    when (cardTitle) {
                        "本地电影" -> currentScreen = Screen.VideoList
                        "设置中心" -> currentScreen = Screen.Settings
                    }
                }
            )
            Screen.Settings -> SettingsScreen(
                isDarkMode = isDarkMode,
                onBackClick = { currentScreen = Screen.Home },
                onAppearanceClick = { currentScreen = Screen.AppearanceSettings }
            )
            Screen.AppearanceSettings -> AppearanceSettingsScreen(
                isDarkMode = isDarkMode,
                onThemeChange = { dark -> isDarkMode = dark },
                onBackClick = { currentScreen = Screen.Settings }
            )
            Screen.VideoList -> VideoListScreen(
                isDarkMode = isDarkMode,
                onBackClick = { currentScreen = Screen.Home },
                onVideoClick = { uri, name ->
                    currentVideoUri = uri
                    currentVideoName = name
                    currentScreen = Screen.Player
                }
            )
            Screen.Player -> VideoPlayerScreen(
                videoUri = currentVideoUri,
                videoName = currentVideoName,
                onBackClick = { currentScreen = Screen.VideoList }
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(onGrantedOrSkipped: () -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onGrantedOrSkipped()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "芙芙播放器",
                    fontSize = 26.sp,
                    color = Color(0xFF1D1B20),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(70.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF0F2F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📁", fontSize = 42.sp)
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "查找您的视频",
                    fontSize = 22.sp,
                    color = Color(0xFF1D1B20),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Android 限制应用访问和管理文件的方式。“文件访问权限”可帮助 芙芙播放器 查找您手机不同文件夹中的本地视频文件。",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_VIDEO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        permissionLauncher.launch(permission)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text(text = "允许访问视频文件", color = Color.White, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onGrantedOrSkipped) {
                    Text(text = "仅使用基础功能继续", color = Color.Gray, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ModernHomeScreen(isDarkMode: Boolean, onCardClick: (String) -> Unit) {
    val textColor = if (isDarkMode) Color.White else Color(0xFF1D1B20)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "芙芙播放器",
            fontSize = 26.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
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
                    isDarkMode = isDarkMode,
                    onClick = { onCardClick("本地电影") }
                ) 
            }
            item { GlassCard(title = "最近播放", subtitle = "断点续播记录", count = "历史记录", isDarkMode = isDarkMode, onClick = {}) }
            item { GlassCard(title = "我的收藏", subtitle = "精选喜爱视频", count = "快捷访问", isDarkMode = isDarkMode, onClick = {}) }
            item { 
                GlassCard(
                    title = "设置中心", 
                    subtitle = "外观与解码偏好", 
                    count = "个性化", 
                    isDarkMode = isDarkMode,
                    onClick = { onCardClick("设置中心") }
                ) 
            }
        }
    }
}

@Composable
fun GlassCard(title: String, subtitle: String, count: String, isDarkMode: Boolean, onClick: () -> Unit) {
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val subTextColor = if (isDarkMode) Color.LightGray else Color.DarkGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDarkMode) listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                    else listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.2f))
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
                Text(text = title, fontSize = 17.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 11.sp, color = subTextColor)
            }
            Text(text = count, fontSize = 11.sp, color = subTextColor)
        }
    }
}

@Composable
fun SettingsScreen(isDarkMode: Boolean, onBackClick: () -> Unit, onAppearanceClick: () -> Unit) {
    val textColor = if (isDarkMode) Color.White else Color(0xFF1D1B20)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White.copy(alpha = 0.6f)
    val subTextColor = if (isDarkMode) Color.LightGray else Color.DarkGray

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
            Text(text = "设置中心", fontSize = 24.sp, color = textColor, fontWeight = FontWeight.Bold)
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color(0xFF333333) else Color.White.copy(alpha = 0.6f)
                )
            ) {
                Text(text = "返回", color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .clickable { onAppearanceClick() }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "外观设置", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "深色模式、浅色模式切换", fontSize = 12.sp, color = subTextColor)
                }
                Text(text = "›", fontSize = 22.sp, color = subTextColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .clickable { }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "播放偏好", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "手势、解码与播放速度默认值", fontSize = 12.sp, color = subTextColor)
                }
                Text(text = "›", fontSize = 22.sp, color = subTextColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .clickable { }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "关于 芙芙播放器", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "版本 v1.0", fontSize = 12.sp, color = subTextColor)
                }
                Text(text = "›", fontSize = 22.sp, color = subTextColor)
            }
        }
    }
}

@Composable
fun AppearanceSettingsScreen(isDarkMode: Boolean, onThemeChange: (Boolean) -> Unit, onBackClick: () -> Unit) {
    val textColor = if (isDarkMode) Color.White else Color(0xFF1D1B20)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White.copy(alpha = 0.6f)

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
            Text(text = "外观设置", fontSize = 24.sp, color = textColor, fontWeight = FontWeight.Bold)
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color(0xFF333333) else Color.White.copy(alpha = 0.6f)
                )
            ) {
                Text(text = "返回", color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "选择主题模式", fontSize = 14.sp, color = if (isDarkMode) Color.LightGray else Color.DarkGray)
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(
                    width = if (!isDarkMode) 2.dp else 0.dp,
                    color = Color(0xFF3B82F6),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onThemeChange(false) }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "浅色模式 (Light)", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "清爽简洁的纯白视觉风格", fontSize = 12.sp, color = if (isDarkMode) Color.LightGray else Color.DarkGray)
                    }
                }
                if (!isDarkMode) {
                    Text(text = "✓ 已选择", fontSize = 13.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(
                    width = if (isDarkMode) 2.dp else 0.dp,
                    color = Color(0xFF3B82F6),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onThemeChange(true) }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "深色模式 (Dark)", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "纯黑沉浸护眼风格", fontSize = 12.sp, color = if (isDarkMode) Color.LightGray else Color.DarkGray)
                    }
                }
                if (isDarkMode) {
                    Text(text = "✓ 已选择", fontSize = 13.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VideoListScreen(isDarkMode: Boolean, onBackClick: () -> Unit, onVideoClick: (String, String) -> Unit) {
    val context = LocalContext.current
    var videoList by remember { mutableStateOf(listOf<VideoItem>()) }
    val textColor = if (isDarkMode) Color.White else Color(0xFF1D1B20)

    LaunchedEffect(Unit) {
        if (!hasVideoPermission(context)) return@LaunchedEffect

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
            Text(text = "本地视频库", fontSize = 24.sp, color = textColor, fontWeight = FontWeight.Bold)
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color(0xFF333333) else Color.White.copy(alpha = 0.6f)
                )
            ) {
                Text(text = "返回", color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasVideoPermission(context)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "您未授予文件访问权限，无法加载本地视频", color = if (isDarkMode) Color.LightGray else Color.Gray, fontSize = 14.sp)
            }
        } else if (videoList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "未在手机中找到视频文件", color = if (isDarkMode) Color.LightGray else Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videoList) { video ->
                    VideoCardItem(video = video, isDarkMode = isDarkMode, onClick = { onVideoClick(video.uri, video.name) })
                }
            }
        }
    }
}

@Composable
fun VideoCardItem(video: VideoItem, isDarkMode: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White.copy(alpha = 0.5f)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1D1B20)

    LaunchedEffect(video.uri) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(video.uri)
                val bm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(320, 180), null)
                } else {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val b = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()
                    b
                }
                thumbnailBitmap = bm
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDarkMode) listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                    else listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = "加载中...", fontSize = 11.sp, color = Color.Gray)
                }

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

            Text(
                text = video.name,
                fontSize = 13.sp,
                color = textColor,
                maxLines = 2,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "本地高清视频",
                fontSize = 11.sp,
                color = if (isDarkMode) Color.LightGray else Color.DarkGray
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
fun VideoPlayerScreen(videoUri: String, videoName: String, onBackClick: () -> Unit) {
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
    var isScreenLocked by remember { mutableStateOf(false) } // 锁屏状态
    var showControls by remember { mutableStateOf(true) }     // 控制条显示/隐藏

    // 播放状态控制
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var videoDuration by remember { mutableStateOf(0L) }

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
                override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                    isPlaying = isPlayingChanged
                }
            })
        }
    }

    // 定时刷新播放进度
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            videoDuration = exoPlayer.duration.coerceAtLeast(1L)
            delay(250)
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
            // 锁屏时禁用水平滑动快进手势
            .pointerInput(isScreenLocked) {
                if (!isScreenLocked) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            showSlideTip = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isSpeedingUp && abs(dragAmount) > 2f) {
                                val currentPos = exoPlayer.currentPosition
                                val duration = exoPlayer.duration.coerceAtLeast(1L)
                                val seekOffset = (dragAmount * 15).toLong() 
                                val targetPosition = (currentPos + seekOffset).coerceIn(0L, duration)
                                
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
            }
            // 点击空白处切换控制条显示状态（未锁屏时）
            .clickable(enabled = !isScreenLocked) {
                showControls = !showControls
            }
    ) {
        // 自定义视频画面视图，不使用自带控制器
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT 
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 仅在未锁屏且 showControls 为 true 时显示所有控制界面
        if (!isScreenLocked) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. 顶部标题栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                exoPlayer.release()
                                onBackClick()
                            }
                        ) {
                            Text(text = "〈", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = videoName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 2. 底部控制条（播放/暂停 + 进度条 + 时间）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = if (isPlaying) "⏸" else "▶",
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            }

                            Text(
                                text = formatDuration(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp
                            )

                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = { newValue ->
                                    currentPosition = newValue.toLong()
                                },
                                onValueChangeFinished = {
                                    exoPlayer.seekTo(currentPosition)
                                },
                                valueRange = 0f..videoDuration.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF3B82F6),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                            )

                            Text(
                                text = formatDuration(videoDuration),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 3. 右侧半屏：长按倍速手势区域
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
                                                    val currentPos = exoPlayer.currentPosition
                                                    val duration = exoPlayer.duration.coerceAtLeast(1L)
                                                    val seekOffset = (deltaX * 15).toLong()
                                                    val targetPosition = (currentPos + seekOffset).coerceIn(0L, duration)
                                                    
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

                    // 4. 右侧悬浮功能按钮（截图 📷 和 锁屏 🔒）
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "已成功截取视频画面", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Text(text = "📷", fontSize = 18.sp)
                        }

                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                Toast.makeText(context, "屏幕已锁定，长按与滑动倍速已禁用", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Text(text = "🔓", fontSize = 18.sp)
                        }
                    }
                }
            }
        } else {
            // 锁屏状态下：隐藏所有控制条和手势，仅在屏幕左侧显示一个极简的解锁图标
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        isScreenLocked = false
                        Toast.makeText(context, "屏幕已解除锁定", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 32.dp)
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔒", fontSize = 22.sp)
                }
            }
        }

        // 提示信息浮窗（倍速、快进提示）
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
