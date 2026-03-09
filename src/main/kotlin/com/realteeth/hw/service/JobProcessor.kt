package com.realteeth.hw.service

import com.realteeth.hw.domain.Job
import com.realteeth.hw.domain.JobStatus
import com.realteeth.hw.repository.JobRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class JobProcessor(
    private val jobRepository: JobRepository,
    private val mockWorkerClient: MockWorkerClient,
    @Value("\${job.processor.poll-interval-ms:3000}")   val pollIntervalMs: Long = 3_000L,
    @Value("\${job.processor.initial-backoff-ms:1000}") val initialBackoffMs: Long = 1_000L,
    @Value("\${job.processor.poll-timeout-ms:600000}")  val pollTimeoutMs: Long = 10 * 60 * 1_000L,
    @Value("\${job.processor.max-500-retry:3}")         val max500Retry: Int = 3,
) {
    private val logger = LoggerFactory.getLogger(JobProcessor::class.java)
    private val maxBackoffMs = 32_000L

    @Async
    fun process(jobId: UUID) {
        try {
            val job = loadJob(jobId)

            if (job.status != JobStatus.PENDING) {
                return
            }

            val mockResponse = submitWithRetry(job)

            job.mockJobId = mockResponse.jobId
            job.transitionTo(JobStatus.PROCESSING)
            saveJob(job)

            when (mockResponse.status) {
                "COMPLETED" -> { finalizeCompleted(job, null); return }
                "FAILED"    -> { finalizeFailed(job, "Mock Worker FAILED"); return }
            }

            pollUntilComplete(job)

        } catch (e: Exception) {
            logger.error("처리 중 오류 발생: jobId={}", jobId, e)
            safeMarkFailed(jobId, e.message ?: "알 수 없는 오류")
        }
    }

    private fun submitWithRetry(job: Job): MockProcessResponse {
        var backoffMs = initialBackoffMs
        var retryCount = job.retryCount

        while (true) {
            try {
                return mockWorkerClient.submitProcess(job.imageUrl)
            } catch (e: MockWorker429Exception) {
                Thread.sleep(backoffMs)
                backoffMs = minOf(backoffMs * 2, maxBackoffMs)
            } catch (e: MockWorker500Exception) {
                retryCount++
                job.retryCount = retryCount
                saveJob(job)
                if (retryCount >= max500Retry) {
                    throw RuntimeException("Mock Worker 오류: ${e.message}", e)
                }
                Thread.sleep(initialBackoffMs)
            }
        }
    }

    private fun pollUntilComplete(job: Job) {
        val deadline = System.currentTimeMillis() + pollTimeoutMs
        var backoffMs = initialBackoffMs

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollIntervalMs)

            try {
                val statusRes = mockWorkerClient.getStatus(job.mockJobId!!)
                backoffMs = initialBackoffMs

                when (statusRes.status) {
                    "COMPLETED" -> { finalizeCompleted(job, statusRes.result); return }
                    "FAILED"    -> { finalizeFailed(job, "Mock Worker returned FAILED"); return }
                }
            } catch (e: MockWorker429Exception) {
                Thread.sleep(backoffMs)
                backoffMs = minOf(backoffMs * 2, maxBackoffMs)
            } catch (e: MockWorker500Exception) {
                // 다음 사이클에서 재시도
            }
        }

        finalizeFailed(job, "타임아웃: ${pollTimeoutMs / 60_000}분 내에 응답 없음")
    }

    private fun finalizeCompleted(job: Job, result: String?) {
        job.result = result
        job.transitionTo(JobStatus.COMPLETED)
        saveJob(job)
    }

    private fun finalizeFailed(job: Job, reason: String) {
        job.errorMessage = reason
        job.transitionTo(JobStatus.FAILED)
        saveJob(job)
    }

    private fun safeMarkFailed(jobId: UUID, reason: String) {
        try {
            val job = loadJob(jobId)
            if (job.status == JobStatus.PENDING || job.status == JobStatus.PROCESSING) {
                job.errorMessage = reason
                job.status = JobStatus.FAILED
                job.updatedAt = Instant.now()
                saveJob(job)
            }
        } catch (e: Exception) {
            logger.error("FAILED 처리 중 오류: jobId={}", jobId, e)
        }
    }

    private fun loadJob(jobId: UUID): Job =
        jobRepository.findById(jobId).orElseThrow { NoSuchElementException("Job not found: $jobId") }

    private fun saveJob(job: Job): Job = jobRepository.save(job)
}
