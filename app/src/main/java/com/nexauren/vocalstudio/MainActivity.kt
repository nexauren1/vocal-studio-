package com.nexauren.vocalstudio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF0B0C12)
private val Panel = Color(0xFF151724)
private val PanelRaised = Color(0xFF1D2030)
private val Violet = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF36D9E8)
private val MainText = Color(0xFFF5F6FC)
private val Muted = Color(0xFFA2A8BC)
private val Outline = Color(0xFF2B2E40)
private val RecordingRed = Color(0xFFFF647C)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 12, 18)
        window.navigationBarColor = android.graphics.Color.rgb(11, 12, 18)
        setContent {
            VoxoraTheme {
                VoxoraApp()
            }
        }
    }
}

@Composable
private fun VoxoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            onPrimary = Color.White,
            secondary = Cyan,
            onSecondary = Ink,
            background = Ink,
            onBackground = MainText,
            surface = Panel,
            onSurface = MainText,
            surfaceVariant = PanelRaised,
            onSurfaceVariant = Muted,
            outline = Outline
        ),
        typography = Typography(),
        content = content
    )
}

private data class AppRecording(
    val file: File,
    val lastModified: Long = file.lastModified(),
    val sizeBytes: Long = file.length()
)

private enum class AppTab {
    HOME, STUDIO, PROJECTS, SETTINGS
}

@Composable
private fun VoxoraApp() {
    val context = LocalContext.current
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var recordings by remember { mutableStateOf(loadRecordings(context)) }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var statusMessage by rememberSaveable { mutableStateOf("") }
    var pendingDeleteName by rememberSaveable { mutableStateOf<String?>(null) }
    var playingFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var activeFile by remember { mutableStateOf<File?>(null) }

    val recorderState = remember { mutableStateOf<MediaRecorder?>(null) }
    val playerState = remember { mutableStateOf<MediaPlayer?>(null) }

    fun startRecording() {
        if (isRecording) return
        try {
            val directory = File(context.filesDir, "recordings")
            if (!directory.exists() && !directory.mkdirs()) {
                statusMessage = context.getString(R.string.recording_failed)
                return
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val output = File(directory, "Voxora_$stamp.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setOutputFile(output.absolutePath)
            recorder.prepare()
            recorder.start()

            recorderState.value = recorder
            activeFile = output
            elapsedSeconds = 0
            isRecording = true
            isPaused = false
            statusMessage = ""
        } catch (_: Exception) {
            recorderState.value?.let { runCatching { it.release() } }
            recorderState.value = null
            activeFile?.delete()
            activeFile = null
            isRecording = false
            isPaused = false
            statusMessage = context.getString(R.string.recording_failed)
        }
    }

    fun stopRecording() {
        val recorder = recorderState.value
        val output = activeFile
        if (recorder == null) {
            isRecording = false
            isPaused = false
            return
        }
        val stoppedSuccessfully = runCatching { recorder.stop() }.isSuccess
        runCatching { recorder.reset() }
        runCatching { recorder.release() }
        recorderState.value = null
        activeFile = null
        isRecording = false
        isPaused = false

        if (stoppedSuccessfully && output != null && output.exists() && output.length() > 1024L) {
            recordings = loadRecordings(context)
            statusMessage = context.getString(R.string.recording_saved)
        } else {
            output?.delete()
            statusMessage = context.getString(R.string.recording_too_short)
        }
    }

    fun stopPlayback() {
        playerState.value?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        playerState.value = null
        playingFileName = null
    }

    fun playRecording(file: File) {
        stopPlayback()
        try {
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener { completed ->
                runCatching { completed.release() }
                if (playerState.value === completed) {
                    playerState.value = null
                    playingFileName = null
                }
            }
            player.prepare()
            player.start()
            playerState.value = player
            playingFileName = file.name
            statusMessage = ""
        } catch (_: Exception) {
            stopPlayback()
            statusMessage = context.getString(R.string.playback_failed)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else statusMessage = context.getString(R.string.permission_required)
    }

    fun requestRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorderState.value?.let { recorder ->
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
            }
            playerState.value?.let { player ->
                runCatching { if (player.isPlaying) player.stop() }
                runCatching { player.release() }
            }
        }
    }

    val isPortuguese = currentLanguageIsPortuguese()
    val activeTab = AppTab.entries[selectedTabIndex.coerceIn(0, AppTab.entries.lastIndex)]

    Scaffold(
        containerColor = Ink,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            NavigationBar(
                containerColor = Panel,
                contentColor = Muted,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == AppTab.HOME,
                    onClick = { selectedTabIndex = AppTab.HOME.ordinal },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home)) },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.STUDIO,
                    onClick = { selectedTabIndex = AppTab.STUDIO.ordinal },
                    icon = { Icon(Icons.Outlined.Mic, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_studio)) },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.PROJECTS,
                    onClick = {
                        recordings = loadRecordings(context)
                        selectedTabIndex = AppTab.PROJECTS.ordinal
                    },
                    icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_projects)) },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.SETTINGS,
                    onClick = { selectedTabIndex = AppTab.SETTINGS.ordinal },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = navColors()
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Ink)
        ) {
            when (activeTab) {
                AppTab.HOME -> HomeScreen(
                    recordings = recordings,
                    statusMessage = statusMessage,
                    onOpenStudio = { selectedTabIndex = AppTab.STUDIO.ordinal },
                    onOpenProjects = {
                        recordings = loadRecordings(context)
                        selectedTabIndex = AppTab.PROJECTS.ordinal
                    },
                    playingFileName = playingFileName,
                    onPlay = ::playRecording,
                    onStopPlayback = ::stopPlayback
                )
                AppTab.STUDIO -> StudioScreen(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    elapsedSeconds = elapsedSeconds,
                    statusMessage = statusMessage,
                    onStart = ::requestRecording,
                    onPause = {
                        runCatching { recorderState.value?.pause() }
                            .onSuccess { isPaused = true }
                            .onFailure { statusMessage = context.getString(R.string.recording_failed) }
                    },
                    onResume = {
                        runCatching { recorderState.value?.resume() }
                            .onSuccess { isPaused = false }
                            .onFailure { statusMessage = context.getString(R.string.recording_failed) }
                    },
                    onStop = ::stopRecording
                )
                AppTab.PROJECTS -> ProjectsScreen(
                    recordings = recordings,
                    playingFileName = playingFileName,
                    onPlay = ::playRecording,
                    onStopPlayback = ::stopPlayback,
                    onDelete = { pendingDeleteName = it.file.name }
                )
                AppTab.SETTINGS -> SettingsScreen(
                    isPortuguese = isPortuguese,
                    onChooseLanguage = { languageTag ->
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(languageTag)
                        )
                    }
                )
            }
        }
    }

    val pendingFile = pendingDeleteName?.let { name ->
        recordings.firstOrNull { it.file.name == name }
    }
    if (pendingFile != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteName = null },
            containerColor = PanelRaised,
            title = { Text(stringResource(R.string.delete_recording_title), color = MainText) },
            text = { Text(stringResource(R.string.delete_recording_body), color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    if (playingFileName == pendingFile.file.name) stopPlayback()
                    pendingFile.file.delete()
                    recordings = loadRecordings(context)
                    pendingDeleteName = null
                }) {
                    Text(stringResource(R.string.delete_action), color = RecordingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteName = null }) {
                    Text(stringResource(R.string.cancel), color = Muted)
                }
            }
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Cyan,
    selectedTextColor = MainText,
    indicatorColor = Violet.copy(alpha = 0.22f),
    unselectedIconColor = Muted,
    unselectedTextColor = Muted
)

private fun currentLanguageIsPortuguese(): Boolean {
    val appLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return if (appLocales.isNotBlank()) {
        appLocales.startsWith("pt", ignoreCase = true)
    } else {
        Locale.getDefault().language.startsWith("pt", ignoreCase = true)
    }
}

private fun loadRecordings(context: Context): List<AppRecording> {
    val directory = File(context.filesDir, "recordings")
    return directory.listFiles()
        ?.filter { it.isFile && it.extension.equals("m4a", ignoreCase = true) }
        ?.sortedByDescending { it.lastModified() }
        ?.map { AppRecording(it) }
        .orEmpty()
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(Locale.US, minutes, seconds)
}

private fun formatFileSize(context: Context, bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb >= 1024.0) {
        context.getString(R.string.file_size_mb, kb / 1024.0)
    } else {
        context.getString(R.string.file_size_kb, kb)
    }
}

private fun formatModified(timestamp: Long): String =
    SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(timestamp))

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Violet.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Waves, contentDescription = null, tint = Cyan, modifier = Modifier.size(25.dp))
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandMark()
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "VOXORA",
                color = MainText,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.2.sp,
                fontSize = 18.sp
            )
            Text(
                text = stringResource(R.string.tagline),
                color = Muted,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Cyan)
        )
    }
}

@Composable
private fun HomeScreen(
    recordings: List<AppRecording>,
    statusMessage: String,
    onOpenStudio: () -> Unit,
    onOpenProjects: () -> Unit,
    playingFileName: String?,
    onPlay: (File) -> Unit,
    onStopPlayback: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BrandHeader() }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.hello_again),
                    color = Cyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    stringResource(R.string.home_title),
                    color = MainText,
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp
                )
                Text(
                    stringResource(R.string.home_subtitle),
                    color = Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, Outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.studio_title),
                                color = MainText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.recording_count, recordings.size),
                                color = Muted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Violet.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Mic, contentDescription = null, tint = Cyan, modifier = Modifier.size(29.dp))
                        }
                    }
                    WaveformPreview()
                    Button(
                        onClick = onOpenStudio,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White)
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(stringResource(R.string.open_studio), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            item { StatusBanner(statusMessage) }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.recent_takes),
                    color = MainText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenProjects) {
                    Text(stringResource(R.string.view_all), color = Cyan)
                }
            }
        }

        if (recordings.isEmpty()) {
            item {
                EmptyState(
                    icon = { Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Cyan, modifier = Modifier.size(28.dp)) },
                    title = stringResource(R.string.empty_recordings_title),
                    body = stringResource(R.string.empty_recordings_body)
                )
            }
        } else {
            items(recordings.take(3), key = { it.file.name }) { recording ->
                RecordingRow(
                    recording = recording,
                    playing = playingFileName == recording.file.name,
                    onPlay = {
                        if (playingFileName == recording.file.name) onStopPlayback()
                        else onPlay(recording.file)
                    },
                    onDelete = null
                )
            }
        }

        item {
            Text(
                stringResource(R.string.effects_title).uppercase(Locale.getDefault()),
                color = Muted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureTile(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = Cyan) },
                    title = stringResource(R.string.effects_title),
                    subtitle = stringResource(R.string.effects_body)
                )
                FeatureTile(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Violet) },
                    title = stringResource(R.string.editor_title),
                    subtitle = stringResource(R.string.editor_body)
                )
            }
        }
        item { Spacer(Modifier.height(2.dp)) }
    }
}

@Composable
private fun StudioScreen(
    isRecording: Boolean,
    isPaused: Boolean,
    elapsedSeconds: Int,
    statusMessage: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BrandHeader() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.studio_title),
                    color = MainText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
                Text(stringResource(R.string.studio_subtitle), color = Muted, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, Outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                when {
                                    isRecording && !isPaused -> RecordingRed.copy(alpha = 0.13f)
                                    isPaused -> Violet.copy(alpha = 0.16f)
                                    else -> Cyan.copy(alpha = 0.10f)
                                }
                            )
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isRecording && !isPaused) RecordingRed else Cyan)
                        )
                        Text(
                            text = when {
                                isRecording && isPaused -> stringResource(R.string.recording_paused)
                                isRecording -> stringResource(R.string.recording_in_progress)
                                else -> stringResource(R.string.ready_to_record)
                            },
                            color = if (isRecording && !isPaused) RecordingRed else Cyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    WaveformPreview(active = isRecording && !isPaused, tall = true)

                    Text(
                        formatDuration(elapsedSeconds),
                        color = MainText,
                        fontSize = 46.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        stringResource(R.string.record_your_voice),
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (!isRecording) {
                        FilledIconButton(
                            onClick = onStart,
                            modifier = Modifier.size(94.dp),
                            shape = CircleShape,
                            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                containerColor = Violet,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Outlined.Mic, contentDescription = stringResource(R.string.record_your_voice), modifier = Modifier.size(38.dp))
                        }
                        Text(stringResource(R.string.open_studio), color = Muted, style = MaterialTheme.typography.labelMedium)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = if (isPaused) onResume else onPause,
                                modifier = Modifier.size(60.dp),
                                shape = CircleShape,
                                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                    containerColor = PanelRaised,
                                    contentColor = Cyan
                                )
                            ) {
                                Icon(
                                    if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                    contentDescription = stringResource(if (isPaused) R.string.resume else R.string.pause),
                                    modifier = Modifier.size(27.dp)
                                )
                            }
                            FilledIconButton(
                                onClick = onStop,
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                    containerColor = RecordingRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Outlined.Stop, contentDescription = stringResource(R.string.stop), modifier = Modifier.size(30.dp))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                            Text(stringResource(if (isPaused) R.string.resume else R.string.pause), color = Muted, style = MaterialTheme.typography.labelSmall)
                            Text(stringResource(R.string.stop), color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        if (statusMessage.isNotBlank()) {
            item { StatusBanner(statusMessage) }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PanelRaised)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Cyan)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.privacy_title), color = MainText, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.privacy_body), color = Muted, style = MaterialTheme.typography.bodySmall, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsScreen(
    recordings: List<AppRecording>,
    playingFileName: String?,
    onPlay: (File) -> Unit,
    onStopPlayback: () -> Unit,
    onDelete: (AppRecording) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BrandHeader() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.projects_title),
                    color = MainText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
                Text(stringResource(R.string.projects_subtitle), color = Muted, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Text(
                stringResource(R.string.recording_count, recordings.size),
                color = Cyan,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (recordings.isEmpty()) {
            item {
                EmptyState(
                    icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Cyan, modifier = Modifier.size(28.dp)) },
                    title = stringResource(R.string.no_projects_title),
                    body = stringResource(R.string.no_projects_body)
                )
            }
        } else {
            items(recordings, key = { it.file.name }) { recording ->
                RecordingRow(
                    recording = recording,
                    playing = playingFileName == recording.file.name,
                    onPlay = {
                        if (playingFileName == recording.file.name) onStopPlayback()
                        else onPlay(recording.file)
                    },
                    onDelete = { onDelete(recording) }
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    isPortuguese: Boolean,
    onChooseLanguage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BrandHeader() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.settings_title),
                    color = MainText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
                Text(stringResource(R.string.settings_subtitle), color = Muted, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, Outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Violet.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Language, contentDescription = null, tint = Cyan)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.language_title), color = MainText, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.language_subtitle), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onChooseLanguage("pt-PT") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isPortuguese) Cyan else Outline)
                        ) {
                            Text(stringResource(R.string.portuguese), color = if (isPortuguese) Cyan else MainText)
                        }
                        OutlinedButton(
                            onClick = { onChooseLanguage("en") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (!isPortuguese) Cyan else Outline)
                        ) {
                            Text(stringResource(R.string.english), color = if (!isPortuguese) Cyan else MainText)
                        }
                    }
                    TextButton(
                        onClick = { onChooseLanguage("") },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(stringResource(R.string.system_language), color = Muted)
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, Outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.privacy_title), color = MainText, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.privacy_body), color = Muted, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(stringResource(R.string.app_info), color = Muted, style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.by_nexauren), color = Cyan, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun WaveformPreview(active: Boolean = false, tall: Boolean = false) {
    val heights = listOf(
        12, 20, 30, 18, 25, 38, 22, 14, 28, 42, 32, 19, 25, 35, 44, 27,
        18, 32, 40, 24, 15, 30, 39, 22, 13, 26, 34, 18, 28, 42, 31, 20,
        12, 24, 37, 27, 16, 32, 43, 22, 14, 28
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (tall) 72.dp else 48.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, value ->
            val barHeight = if (tall) (value * 1.45f).dp else (value * 0.92f).dp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight.coerceAtMost(if (tall) 66.dp else 43.dp))
                    .clip(CircleShape)
                    .background(
                        when {
                            active && index % 3 == 0 -> RecordingRed
                            index % 4 == 0 -> Cyan.copy(alpha = if (active) 0.95f else 0.75f)
                            else -> Violet.copy(alpha = if (active) 0.95f else 0.62f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun StatusBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Cyan.copy(alpha = 0.09f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, tint = Cyan, modifier = Modifier.size(19.dp))
        Text(message, color = MainText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Cyan.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(title, color = MainText, fontWeight = FontWeight.SemiBold)
            Text(body, color = Muted, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun FeatureTile(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(PanelRaised),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(title, color = MainText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp, minLines = 2)
            Text(
                stringResource(R.string.coming_soon),
                color = Cyan,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun RecordingRow(
    recording: AppRecording,
    playing: Boolean,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (playing) Cyan.copy(alpha = 0.13f) else Violet.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = if (playing) Cyan else Violet)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    recording.file.nameWithoutExtension.replace('_', ' '),
                    color = MainText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    formatModified(recording.lastModified) + " · " + formatFileSize(LocalContext.current, recording.sizeBytes),
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(if (playing) R.string.stop else R.string.play),
                    tint = if (playing) Cyan else MainText
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = Muted)
                }
            }
        }
    }
}
