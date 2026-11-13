package com.myai.app.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * سرویس ارتباط با مدل‌های هوش مصنوعی
 * از API سازگار با OpenAI استفاده می‌کند (Groq, OpenRouter, و غیره)
 */
class AiService {

    suspend fun chat(
        config: AiConfig,
        systemPrompt: String,
        history: List<AiMessage>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(
                    Exception("کلید API وارد نشده است.\nاز بخش تنظیمات کلید را وارد کن.")
                )
            }

            val messages = mutableListOf<AiMessage>()
            messages.add(AiMessage(role = "system", content = systemPrompt))
            messages.addAll(history)
            messages.add(AiMessage(role = "user", content = userMessage))

            val requestBody = JSONObject().apply {
                put("model", config.model)
                put("temperature", config.temperature.toDouble())
                put("max_tokens", config.maxTokens)
                put("messages", JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
            }

            val url = URL("${config.baseUrl.trimEnd('/')}/chat/completions")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                // برای OpenRouter
                setRequestProperty("HTTP-Referer", "https://myai.app")
                setRequestProperty("X-Title", "MyAI")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            connection.disconnect()

            if (responseCode !in 200..299) {
                val errorJson = try {
                    JSONObject(responseText).optJSONObject("error")?.optString("message")
                        ?: responseText
                } catch (e: Exception) {
                    responseText
                }
                return@withContext Result.failure(
                    Exception("خطای سرور ($responseCode):\n$errorJson")
                )
            }

            val json = JSONObject(responseText)
            val content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            Result.success(content)

        } catch (e: Exception) {
            Result.failure(
                Exception("خطا در ارتباط با هوش مصنوعی:\n${e.message ?: "خطای ناشناخته"}")
            )
        }
    }
}
