package com.myai.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myai.app.data.PersonalitySettings
import com.myai.app.data.SettingsRepository
import com.myai.app.data.ai.AiConfig
import com.myai.app.data.ai.AiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    // Personality
    val name: String = "MyAI",
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,

    // AI Config
    val provider: AiProvider = AiProvider.GROQ,
    val apiKey: String = "",
    val model: String = AiProvider.GROQ.defaultModel,
    val baseUrl: String = AiProvider.GROQ.defaultBaseUrl,

    val hasChanges: Boolean = false,
    val saveSuccess: Boolean = false,
    val activeTab: Int = 0 // 0 = شخصیت, 1 = مدل
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.personalityFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        name = settings.name,
                        systemPrompt = settings.systemPrompt,
                        temperature = settings.temperature,
                        maxTokens = settings.maxTokens
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.aiConfigFlow.collect { config ->
                _uiState.update {
                    it.copy(
                        provider = config.provider,
                        apiKey = config.apiKey,
                        model = config.model,
                        baseUrl = config.baseUrl
                    )
                }
            }
        }
    }

    fun onTabChange(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, hasChanges = true, saveSuccess = false) }
    }

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(systemPrompt = value, hasChanges = true, saveSuccess = false) }
    }

    fun onTemperatureChange(value: Float) {
        _uiState.update { it.copy(temperature = value, hasChanges = true, saveSuccess = false) }
    }

    fun onMaxTokensChange(value: Int) {
        _uiState.update { it.copy(maxTokens = value, hasChanges = true, saveSuccess = false) }
    }

    fun onProviderChange(provider: AiProvider) {
        _uiState.update {
            it.copy(
                provider = provider,
                model = provider.defaultModel,
                baseUrl = provider.defaultBaseUrl,
                hasChanges = true,
                saveSuccess = false
            )
        }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value, hasChanges = true, saveSuccess = false) }
    }

    fun onModelChange(value: String) {
        _uiState.update { it.copy(model = value, hasChanges = true, saveSuccess = false) }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value, hasChanges = true, saveSuccess = false) }
    }

    fun save() {
        val current = _uiState.value

        val personality = PersonalitySettings(
            name = current.name.trim().ifEmpty { "MyAI" },
            systemPrompt = current.systemPrompt.trim(),
            temperature = current.temperature,
            maxTokens = current.maxTokens
        )

        val aiConfig = AiConfig(
            provider = current.provider,
            apiKey = current.apiKey.trim(),
            model = current.model.trim(),
            baseUrl = current.baseUrl.trim(),
            temperature = current.temperature,
            maxTokens = current.maxTokens
        )

        viewModelScope.launch {
            repository.savePersonality(personality)
            repository.saveAiConfig(aiConfig)
            _uiState.update {
                it.copy(hasChanges = false, saveSuccess = true)
            }
        }
    }
}
