package com.myai.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import com.myai.app.file.FileHelper

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myai.app.data.ChatMessage
import com.myai.app.ui.theme.AiBubble
import com.myai.app.ui.theme.UserBubble
import com.myai.app.voice.SpeechHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Speech Helper
    val speechHelper = remember { SpeechHelper(context) }
    val isListening by speechHelper.isListening.collectAsState()
    val isSpeaking by speechHelper.isSpeaking.collectAsState()
    val partialResult by speechHelper.partialResult.collectAsState()

    // File Helper
    val fileHelper = remember { FileHelper(context) }

    // Initialize TTS
    LaunchedEffect(Unit) {
        speechHelper.initializeTts()
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceInput(speechHelper, viewModel)
        } else {
            viewModel.showError("برای استفاده از قابلیت صوتی، مجوز میکروفون لازم است")
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val result = fileHelper.readTextFile(uri)
            result.fold(
                onSuccess = { fileInfo ->
                    val prompt = fileHelper.buildSummarizePrompt(fileInfo)
                    viewModel.sendFileSummary(fileInfo.name, prompt)
                },
                onFailure = { error ->
                    viewModel.showError(error.message ?: "خطا در خواندن فایل")
                }
            )
        }
    }


    // Auto scroll
    LaunchedEffect(uiState.messages.size, isListening) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    // Speak AI responses automatically (optional - can be toggled later)
    LaunchedEffect(uiState.messages.size) {
        val lastMessage = uiState.messages.lastOrNull()
        if (lastMessage != null && !lastMessage.isFromUser && !uiState.isLoading) {
            // فقط اگر کاربر خواسته باشد صحبت کند (فعلاً دستی)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.personality.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "پاک کردن گفتگو"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // نمایش متن موقت هنگام گوش دادن
                if (isListening && partialResult.isNotBlank()) {
                    Text(
                        text = partialResult,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MessageInputBar(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputChange,
                    onSend = viewModel::sendMessage,
                    isLoading = uiState.isLoading,
                    isListening = isListening,
                    onMicClick = {
                        if (isListening) {
                            speechHelper.stopListening()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                startVoiceInput(speechHelper, viewModel)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    onAttachClick = {
                        // فقط فایل‌های متنی برای نسخه فعلی
                        filePickerLauncher.launch("text/*")
                    },
                    onSpeakLast = {
                        val lastAi = uiState.messages.lastOrNull { !it.isFromUser }
                        lastAi?.let { speechHelper.speak(it.content) }
                    },
                    isSpeaking = isSpeaking
                )

            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.messages.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onSpeak = { speechHelper.speak(message.content) }
                        )
                    }

                    if (uiState.isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }
}

private fun startVoiceInput(speechHelper: SpeechHelper, viewModel: ChatViewModel) {
    speechHelper.startListening(
        onResult = { text ->
            viewModel.onInputChange(text)
            viewModel.sendMessage()
        },
        onError = { error ->
            viewModel.showError(error)
        }
    )
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onSpeak: () -> Unit
) {
    val isUser = message.isFromUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            IconButton(
                onClick = onSpeak,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "پخش صوتی",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) UserBubble else AiBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    isListening: Boolean,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit,
    onSpeakLast: () -> Unit,
    isSpeaking: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // دکمه پیوست فایل
            IconButton(onClick = onAttachClick, enabled = !isLoading && !isListening) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "انتخاب فایل",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // دکمه میکروفون
            FilledIconButton(
                onClick = onMicClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isListening)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "توقف" else "صحبت کن",
                    tint = if (isListening)
                        MaterialTheme.colorScheme.onError
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(if (isListening) "در حال گوش دادن..." else "پیامت را بنویس...")
                },
                maxLines = 4,
                enabled = !isListening,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading && !isListening,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "ارسال",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(AiBubble)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "در حال نوشتن...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MyAI",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "دستیار شخصی تو",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "پیام بنویس یا دکمه میکروفون را بزن",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
