package com.myai.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myai.app.data.ai.AiProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToMemory: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = uiState.hasChanges
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "ذخیره"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = uiState.activeTab) {
                Tab(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.onTabChange(0) },
                    text = { Text("شخصیت") }
                )
                Tab(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.onTabChange(1) },
                    text = { Text("مدل هوش مصنوعی") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (uiState.activeTab) {
                    0 -> PersonalityTab(uiState, viewModel)
                    1 -> ModelTab(uiState, viewModel)
                }

                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.hasChanges
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ذخیره تغییرات")
                }

                if (uiState.saveSuccess) {
                    Text(
                        text = "✓ تنظیمات با موفقیت ذخیره شد",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider()

                // دکمه مدیریت حافظه
                OutlinedButton(
                    onClick = onNavigateToMemory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مدیریت حافظه بلندمدت")
                }

                Text(
                    text = "در این بخش می‌توانی اطلاعات مهم را ذخیره، ویرایش یا پاک کنی تا دستیار به خاطر بسپارد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun PersonalityTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    OutlinedTextField(
        value = uiState.name,
        onValueChange = viewModel::onNameChange,
        label = { Text("نام دستیار") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        supportingText = { Text("این نام در بالای صفحه چت نمایش داده می‌شود") }
    )

    OutlinedTextField(
        value = uiState.systemPrompt,
        onValueChange = viewModel::onPromptChange,
        label = { Text("شخصیت و دستورات") },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        minLines = 6,
        supportingText = {
            Text("این متن شخصیت، لحن و قوانین دستیار را مشخص می‌کند")
        }
    )

    Column {
        Text(
            text = "میزان خلاقیت: ${"%.1f".format(uiState.temperature)}",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = uiState.temperature,
            onValueChange = viewModel::onTemperatureChange,
            valueRange = 0.1f..1.5f,
            steps = 13
        )
        Text(
            text = "کمتر = دقیق‌تر | بیشتر = خلاق‌تر",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Column {
        Text(
            text = "حداکثر طول پاسخ: ${uiState.maxTokens}",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = uiState.maxTokens.toFloat(),
            onValueChange = { viewModel.onMaxTokensChange(it.toInt()) },
            valueRange = 256f..4096f,
            steps = 14
        )
    }
}

@Composable
private fun ModelTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    Text(
        text = "ارائه‌دهنده مدل",
        style = MaterialTheme.typography.labelLarge
    )

    AiProvider.entries.forEach { provider ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = uiState.provider == provider,
                onClick = { viewModel.onProviderChange(provider) }
            )
            Text(
                text = provider.displayName,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    var showApiKey by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = uiState.apiKey,
        onValueChange = viewModel::onApiKeyChange,
        label = { Text("کلید API") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { showApiKey = !showApiKey }) {
                Text(if (showApiKey) "مخفی" else "نمایش")
            }
        },
        supportingText = {
            Text("کلید را از سایت ارائه‌دهنده بگیر و اینجا وارد کن. فقط روی گوشی خودت ذخیره می‌شود.")
        }
    )

    OutlinedTextField(
        value = uiState.model,
        onValueChange = viewModel::onModelChange,
        label = { Text("نام مدل") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    if (uiState.provider == AiProvider.CUSTOM) {
        OutlinedTextField(
            value = uiState.baseUrl,
            onValueChange = viewModel::onBaseUrlChange,
            label = { Text("آدرس پایه (Base URL)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("مثال: https://api.example.com/v1") }
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Text(
        text = "راهنمای دریافت کلید رایگان:",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = "• Groq: به console.groq.com برو، ثبت‌نام کن و کلید بساز.\n" +
              "• OpenRouter: از openrouter.ai کلید رایگان بگیر.\n\n" +
              "کلید فقط روی گوشی خودت ذخیره می‌شود.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
