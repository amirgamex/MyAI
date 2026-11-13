package com.myai.app.data.ai

/**
 * تنظیمات اتصال به مدل هوش مصنوعی
 * کلید API هیچ‌وقت در کد هاردکد نمی‌شود و فقط توسط مالک وارد می‌شود.
 */
data class AiConfig(
    val provider: AiProvider = AiProvider.GROQ,
    val apiKey: String = "",
    val model: String = "llama-3.3-70b-versatile",
    val baseUrl: String = "https://api.groq.com/openai/v1",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024
)

enum class AiProvider(val displayName: String, val defaultBaseUrl: String, val defaultModel: String) {
    GROQ(
        displayName = "Groq (پیشنهادی - سریع و رایگان)",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "meta-llama/llama-3.3-70b-instruct:free"
    ),
    CUSTOM(
        displayName = "سفارشی (OpenAI-compatible)",
        defaultBaseUrl = "",
        defaultModel = ""
    )
}

data class AiMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class AiRequest(
    val model: String,
    val messages: List<AiMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 1024
)

data class AiChoice(
    val message: AiMessage,
    val finish_reason: String? = null
)

data class AiResponse(
    val id: String? = null,
    val choices: List<AiChoice> = emptyList(),
    val error: AiError? = null
)

data class AiError(
    val message: String? = null,
    val type: String? = null
)
