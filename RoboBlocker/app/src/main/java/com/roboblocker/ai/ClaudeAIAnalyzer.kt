package com.roboblocker.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Uses the Claude API to perform deep spam analysis on borderline phone numbers.
 * Only called when local heuristics are inconclusive, minimizing API costs.
 */
class ClaudeAIAnalyzer(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaType = "application/json".toMediaType()

    private val cache = LinkedHashMap<String, SpamAnalysisResult>(100, 0.75f, true)

    suspend fun analyze(phoneNumber: String, sensitivityLevel: Int = 1): SpamAnalysisResult {
        return withContext(Dispatchers.IO) {
            // Check in-memory cache first
            cache[phoneNumber]?.let { return@withContext it }

            if (apiKey.isBlank()) {
                return@withContext SpamAnalysisResult(
                    isSpam = false,
                    confidence = 0f,
                    reason = "Chave de API não configurada",
                    category = SpamCategory.UNKNOWN
                )
            }

            val sensitivityContext = when (sensitivityLevel) {
                0 -> "Apenas marque como spam se tiver certeza absoluta (>90% de confiança)."
                2 -> "Marque como spam se houver qualquer suspeita razoável (>40% de confiança)."
                else -> "Use julgamento equilibrado (>65% de confiança para marcar como spam)."
            }

            val prompt = buildString {
                appendLine("Você é um especialista em detecção de robocalls e telemarketing, com foco no Brasil.")
                appendLine()
                appendLine("Analise o número de telefone: $phoneNumber")
                appendLine()
                appendLine("Considere:")
                appendLine("- Formato e padrões do número")
                appendLine("- Prefixos conhecidos de telemarketing no Brasil (0800, 4003, 3003, etc.)")
                appendLine("- Padrões de números internacionais suspeitos")
                appendLine("- Estrutura DDI+DDD+número para números brasileiros")
                appendLine("- Padrões de spoofing ou geração automática")
                appendLine()
                appendLine("Sensibilidade: $sensitivityContext")
                appendLine()
                appendLine("Responda APENAS com JSON válido, sem nenhum texto adicional:")
                appendLine("""
                {
                  "isSpam": boolean,
                  "confidence": número de 0.0 a 1.0,
                  "reason": "explicação curta em português",
                  "category": "telemarketing|scam|robocall|legitimate|unknown"
                }
                """.trimIndent())
            }

            val requestBody = gson.toJson(
                mapOf(
                    "model" to "claude-sonnet-4-20250514",
                    "max_tokens" to 200,
                    "messages" to listOf(
                        mapOf("role" to "user", "content" to prompt)
                    )
                )
            ).toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext SpamAnalysisResult(
                        isSpam = false, confidence = 0f,
                        reason = "Erro da API: ${response.code}",
                        category = SpamCategory.UNKNOWN
                    )
                }

                val apiResponse = gson.fromJson(bodyStr, AnthropicResponse::class.java)
                val text = apiResponse.content?.firstOrNull()?.text ?: ""

                val cleanJson = text.replace("```json", "").replace("```", "").trim()
                val aiResult = gson.fromJson(cleanJson, AiSpamResult::class.java)

                val result = SpamAnalysisResult(
                    isSpam = aiResult.isSpam,
                    confidence = aiResult.confidence.coerceIn(0f, 1f),
                    reason = "[IA] ${aiResult.reason}",
                    category = mapCategory(aiResult.category)
                )

                cache[phoneNumber] = result
                result

            } catch (e: Exception) {
                SpamAnalysisResult(
                    isSpam = false, confidence = 0f,
                    reason = "Erro na análise de IA: ${e.message}",
                    category = SpamCategory.UNKNOWN
                )
            }
        }
    }

    private fun mapCategory(cat: String?): SpamCategory {
        return when (cat?.lowercase()) {
            "telemarketing" -> SpamCategory.TELEMARKETING
            "scam"          -> SpamCategory.SCAM
            "robocall"      -> SpamCategory.ROBOCALL
            "legitimate"    -> SpamCategory.LEGITIMATE
            else            -> SpamCategory.UNKNOWN
        }
    }

    // ─── API response models ──────────────────────────────────────────────────

    data class AnthropicResponse(
        val content: List<ContentBlock>?
    )

    data class ContentBlock(
        val type: String,
        val text: String
    )

    data class AiSpamResult(
        @SerializedName("isSpam")    val isSpam: Boolean = false,
        @SerializedName("confidence") val confidence: Float = 0f,
        @SerializedName("reason")    val reason: String = "",
        @SerializedName("category")  val category: String = "unknown"
    )
}
