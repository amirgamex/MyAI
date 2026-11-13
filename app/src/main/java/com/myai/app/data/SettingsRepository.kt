package com.myai.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.myai.app.data.ai.AiConfig
import com.myai.app.data.ai.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "myai_settings")

class SettingsRepository(private val context: Context) {

    // Personality
    private val nameKey = stringPreferencesKey("ai_name")
    private val promptKey = stringPreferencesKey("system_prompt")
    private val temperatureKey = floatPreferencesKey("temperature")
    private val maxTokensKey = intPreferencesKey("max_tokens")

    // AI Config
    private val providerKey = stringPreferencesKey("ai_provider")
    private val apiKeyKey = stringPreferencesKey("api_key")
    private val modelKey = stringPreferencesKey("model")
    private val baseUrlKey = stringPreferencesKey("base_url")

    val personalityFlow: Flow<PersonalitySettings> = context.dataStore.data.map { prefs ->
        PersonalitySettings(
            name = prefs[nameKey] ?: "MyAI",
            systemPrompt = prefs[promptKey] ?: PersonalitySettings().systemPrompt,
            temperature = prefs[temperatureKey] ?: 0.7f,
            maxTokens = prefs[maxTokensKey] ?: 1024
        )
    }

    val aiConfigFlow: Flow<AiConfig> = context.dataStore.data.map { prefs ->
        val providerName = prefs[providerKey] ?: AiProvider.GROQ.name
        val provider = try {
            AiProvider.valueOf(providerName)
        } catch (e: Exception) {
            AiProvider.GROQ
        }

        AiConfig(
            provider = provider,
            apiKey = prefs[apiKeyKey] ?: "",
            model = prefs[modelKey] ?: provider.defaultModel,
            baseUrl = prefs[baseUrlKey] ?: provider.defaultBaseUrl,
            temperature = prefs[temperatureKey] ?: 0.7f,
            maxTokens = prefs[maxTokensKey] ?: 1024
        )
    }

    suspend fun savePersonality(settings: PersonalitySettings) {
        context.dataStore.edit { prefs ->
            prefs[nameKey] = settings.name
            prefs[promptKey] = settings.systemPrompt
            prefs[temperatureKey] = settings.temperature
            prefs[maxTokensKey] = settings.maxTokens
        }
    }

    suspend fun saveAiConfig(config: AiConfig) {
        context.dataStore.edit { prefs ->
            prefs[providerKey] = config.provider.name
            prefs[apiKeyKey] = config.apiKey
            prefs[modelKey] = config.model
            prefs[baseUrlKey] = config.baseUrl
            prefs[temperatureKey] = config.temperature
            prefs[maxTokensKey] = config.maxTokens
        }
    }
}
