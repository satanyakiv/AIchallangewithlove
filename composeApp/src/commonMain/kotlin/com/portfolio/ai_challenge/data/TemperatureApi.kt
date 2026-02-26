package com.portfolio.ai_challenge.data

import com.portfolio.ai_challenge.SERVER_PORT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TemperatureRequest(val prompt: String, val temperature: Double)

@Serializable
data class TemperatureResponse(val temperature: Double, val content: String, val error: String? = null)

@Serializable
data class AnalyzeRequest(val results: List<TemperatureResult>)

@Serializable
data class TemperatureResult(val temperature: Double, val content: String)

@Serializable
data class AnalyzeResponse(val comparison: String, val recommendations: List<Recommendation>)

@Serializable
data class Recommendation(val temperature: Double, val bestFor: String)

@Serializable
private data class StreamChunk(val content: String? = null, val error: String? = null)

private val streamJson = Json { ignoreUnknownKeys = true }

class TemperatureApi(private val client: HttpClient) {
    private val baseUrl = "http://${getServerHost()}:$SERVER_PORT"

    suspend fun streamTemperatureResult(
        prompt: String,
        temperature: Double,
        onToken: (String) -> Unit,
    ): String {
        val fullContent = StringBuilder()

        client.preparePost("$baseUrl/api/temperature/stream") {
            contentType(ContentType.Application.Json)
            setBody(TemperatureRequest(prompt, temperature))
            timeout { requestTimeoutMillis = 120_000 }
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                val chunk = try {
                    streamJson.decodeFromString<StreamChunk>(data)
                } catch (_: Exception) { continue }

                if (chunk.error != null) {
                    throw RuntimeException(chunk.error)
                }
                if (chunk.content != null) {
                    fullContent.append(chunk.content)
                    onToken(chunk.content)
                }
            }
        }

        return fullContent.toString()
    }

    suspend fun analyzeResults(results: List<TemperatureResponse>): AnalyzeResponse {
        return client.post("$baseUrl/api/temperature/analyze") {
            contentType(ContentType.Application.Json)
            setBody(AnalyzeRequest(results.map { TemperatureResult(it.temperature, it.content) }))
        }.body()
    }
}