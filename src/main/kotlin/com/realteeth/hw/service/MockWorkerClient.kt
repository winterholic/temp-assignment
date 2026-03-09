package com.realteeth.hw.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

data class MockProcessRequest(val imageUrl: String)
data class MockProcessResponse(val jobId: String, val status: String)
data class MockStatusResponse(val jobId: String, val status: String, val result: String?)

class MockWorker429Exception(message: String) : RuntimeException(message)
class MockWorker500Exception(message: String) : RuntimeException(message)

@Component
class MockWorkerClient(
    private val webClient: WebClient,
    @Value("\${mock-worker.api-key}") private val apiKey: String,
) {
    fun submitProcess(imageUrl: String): MockProcessResponse {
        return webClient.post()
            .uri("/process")
            .header("X-API-KEY", apiKey)
            .bodyValue(MockProcessRequest(imageUrl = imageUrl))
            .retrieve()
            .onStatus({ it.value() == 429 }) { res ->
                res.bodyToMono<String>().defaultIfEmpty("rate limited").map {
                    MockWorker429Exception("429: $it")
                }
            }
            .onStatus(HttpStatusCode::is5xxServerError) { res ->
                res.bodyToMono<String>().defaultIfEmpty("server error").map {
                    MockWorker500Exception("5xx: $it")
                }
            }
            .bodyToMono<MockProcessResponse>()
            .block()!!
    }

    fun getStatus(mockJobId: String): MockStatusResponse {
        return webClient.get()
            .uri("/process/{jobId}", mockJobId)
            .header("X-API-KEY", apiKey)
            .retrieve()
            .onStatus({ it.value() == 429 }) { res ->
                res.bodyToMono<String>().defaultIfEmpty("rate limited").map {
                    MockWorker429Exception("429: $it")
                }
            }
            .onStatus(HttpStatusCode::is5xxServerError) { res ->
                res.bodyToMono<String>().defaultIfEmpty("server error").map {
                    MockWorker500Exception("5xx: $it")
                }
            }
            .bodyToMono<MockStatusResponse>()
            .block()!!
    }
}
