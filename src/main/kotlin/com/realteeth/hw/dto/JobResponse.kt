package com.realteeth.hw.dto

import com.realteeth.hw.domain.Job
import com.realteeth.hw.domain.JobStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class JobResponse(
    @Schema(description = "작업 ID")
    val jobId: UUID,
    @Schema(description = "작업 상태", example = "PENDING")
    val status: JobStatus,
    @Schema(description = "처리 결과 (COMPLETED 시 값 존재)")
    val result: String?,
    @Schema(description = "실패 원인 (FAILED 시 값 존재)")
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(job: Job) = JobResponse(
            jobId = job.id,
            status = job.status,
            result = job.result,
            errorMessage = job.errorMessage,
            createdAt = job.createdAt,
            updatedAt = job.updatedAt,
        )
    }
}
