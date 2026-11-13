package com.myai.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myai.app.data.ChatMessage
import com.myai.app.data.PersonalitySettings
import com.myai.app.data.SettingsRepository
import com.myai.app.data.ai.AiConfig
import com.myai.app.data.ai.AiMessage
import com.myai.app.data.ai.AiService
import com.myai.app.data.db.AppDatabase
import com.myai.app.data.db.MemoryEntity
import com.myai.app.data.db.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val personality: PersonalitySettings = PersonalitySettings(),
    val aiConfig: AiConfig = AiConfig(),
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val chatDao = AppDatabase.getInstance(application).chatDao()
    private val aiService = AiService()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var longTermMemories: List<MemoryEntity> = emptyList()

    init {
        viewModelScope.launch {
            settingsRepository.personalityFlow.collect { personality ->
                _uiState.update { it.copy(personality = personality) }
            }
        }
        viewModelScope.launch {
            settingsRepository.aiConfigFlow.collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
        viewModelScope.launch {
            chatDao.getAllMemories().collect { memories ->
                longTermMemories = memories
            }
        }
        viewModelScope.launch {
            chatDao.getMessages().collect { entities ->
                val messages = entities.map {
                    ChatMessage(
                        id = it.id,
                        content = it.content,
                        isFromUser = it.isFromUser,
                        timestamp = it.timestamp
                    )
                }
                _uiState.update { it.copy(messages = messages) }
                if (messages.isEmpty()) {
                    addWelcomeMessage()
                }
            }
        }
    }

    private fun buildSystemPromptWithMemory(): String {
        val base = _uiState.value.personality.systemPrompt
        if (longTermMemories.isEmpty()) return base
        val memoryText = longTermMemories.joinToString("\n") {
            "- ${it.key}: ${it.value}"
        }
        return "$base\n\n### اطلاعات ذخیره‌شده توسط مالک (حتماً رعایت کن):\n$memoryText"
    }

    private suspend fun addWelcomeMessage() {
        val name = _uiState.value.personality.name
        val welcome = ChatMessage(
            content = "سلام! من $name هستم.\nچطور می‌تونم کمکت کنم؟",
            isFromUser = false
        )
        saveMessage(welcome)
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isLoading) return

        viewModelScope.launch {
            val userMessage = ChatMessage(content = text, isFromUser = true)
            saveMessage(userMessage)

            _uiState.update {
                it.copy(inputText = "", isLoading = true, errorMessage = null)
            }

            val config = _uiState.value.aiConfig

            // اگر کلید API نبود → حالت آفلاین ساده
            if (config.apiKey.isBlank()) {
                val offlineReply = generateOfflineReply(text)
                val aiMessage = ChatMessage(content = offlineReply, isFromUser = false)
                saveMessage(aiMessage)
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val history = _uiState.value.messages
                .filter { it.content.isNotBlank() }
                .takeLast(20)
                .map {
                    AiMessage(
                        role = if (it.isFromUser) "user" else "assistant",
                        content = it.content
                    )
                }

            val systemPrompt = buildSystemPromptWithMemory()

            val result = aiService.chat(
                config = config,
                systemPrompt = systemPrompt,
                history = history.dropLast(1),
                userMessage = text
            )

            result.fold(
                onSuccess = { reply ->
                    val aiMessage = ChatMessage(content = reply, isFromUser = false)
                    saveMessage(aiMessage)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "خطای ناشناخته"
                    // اگر خطای شبکه بود، پاسخ آفلاین بده
                    val finalReply = if (errorMsg.contains("شبکه", ignoreCase = true) ||
                        errorMsg.contains("connect", ignoreCase = true) ||
                        errorMsg.contains("Unable to resolve", ignoreCase = true)
                    ) {
                        generateOfflineReply(text) + "\n\n(حالت آفلاین فعال شد چون ارتباط با سرور برقرار نشد)"
                    } else {
                        "⚠️ $errorMsg"
                    }
                    val aiMessage = ChatMessage(content = finalReply, isFromUser = false)
                    saveMessage(aiMessage)
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                }
            )
        }
    }

    /**
     * پاسخ‌های ساده محلی وقتی کلید API نیست یا اینترنت قطع است
     */
    private fun generateOfflineReply(userText: String): String {
        val name = _uiState.value.personality.name
        val lower = userText.lowercase()

        // جستجو در حافظه بلندمدت
        val relatedMemory = longTermMemories.find {
            lower.contains(it.key.lowercase()) || it.key.lowercase().contains(lower.take(10))
        }
        if (relatedMemory != null) {
            return "از حافظه‌ام: ${relatedMemory.key} = ${relatedMemory.value}"
        }

        return when {
            lower.contains("سلام") || lower.contains("درود") ->
                "سلام! من $name هستم. الان در حالت آفلاین هستم. برای پاسخ‌های هوشمند کلید API را در تنظیمات وارد کن."
            lower.contains("اسم") ->
                "اسم من $name است."
            lower.contains("چطوری") || lower.contains("حالت") ->
                "خوبم، ممنون! (حالت آفلاین)"
            lower.contains("ممنون") || lower.contains("مرسی") ->
                "خواهش می‌کنم!"
            else ->
                "الان به مدل آنلاین وصل نیستم.\n\n" +
                "برای فعال‌سازی هوش مصنوعی واقعی:\n" +
                "۱. برو به تنظیمات\n" +
                "۲. تب «مدل هوش مصنوعی»\n" +
                "۳. یک کلید رایگان از Groq وارد کن و ذخیره کن."
        }
    }

    private suspend fun saveMessage(message: ChatMessage) {
        chatDao.insertMessage(
            MessageEntity(
                id = message.id,
                content = message.content,
                isFromUser = message.isFromUser,
                timestamp = message.timestamp
            )
        )
    }

    fun clearChat() {
        viewModelScope.launch {
            chatDao.clearConversation()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
        viewModelScope.launch {
            val errorMessage = ChatMessage(
                content = "⚠️ $message",
                isFromUser = false
            )
            saveMessage(errorMessage)
        }
    }

    fun sendFileSummary(fileName: String, prompt: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val userMessage = ChatMessage(
                content = "📄 فایل «$fileName» را خلاصه کن",
                isFromUser = true
            )
            saveMessage(userMessage)

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val config = _uiState.value.aiConfig
            if (config.apiKey.isBlank()) {
                val msg = ChatMessage(
                    content = "برای خلاصه‌سازی فایل باید کلید API را در تنظیمات وارد کنی.",
                    isFromUser = false
                )
                saveMessage(msg)
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val systemPrompt = buildSystemPromptWithMemory() +
                    "\n\nکاربر یک فایل فرستاده. آن را خلاصه کن و نکات مهم را به فارسی بگو."

            val result = aiService.chat(
                config = config,
                systemPrompt = systemPrompt,
                history = emptyList(),
                userMessage = prompt
            )

            result.fold(
                onSuccess = { reply ->
                    saveMessage(ChatMessage(content = reply, isFromUser = false))
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    saveMessage(
                        ChatMessage(
                            content = "⚠️ ${error.message ?: "خطا در خلاصه‌سازی"}",
                            isFromUser = false
                        )
                    )
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }
}
