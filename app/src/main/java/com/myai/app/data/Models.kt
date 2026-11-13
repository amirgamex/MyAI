package com.myai.app.data

import java.util.UUID

/**
 * مدل پیام در گفتگو
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * تنظیمات شخصیت هوش مصنوعی
 * این تنظیمات توسط مالک (تو) کنترل می‌شود
 */
data class PersonalitySettings(
    val name: String = "MyAI",
    val systemPrompt: String = """
        تو MyAI هستی، دستیار هوش مصنوعی شخصی و وفادار مالک.
        همیشه مودب، مفید و واضح پاسخ بده.
        پاسخ‌هایت را به زبان فارسی و طبیعی بنویس.
        هویت مالک را محترم بشمار و از او پیروی کن.
    """.trimIndent(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024
)

/**
 * وضعیت کلی برنامه
 */
data class AppState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val personality: PersonalitySettings = PersonalitySettings(),
    val currentScreen: Screen = Screen.CHAT
)

enum class Screen {
    CHAT,
    SETTINGS
}
