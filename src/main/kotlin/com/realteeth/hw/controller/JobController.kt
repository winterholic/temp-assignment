package com.realteeth.hw.controller

import com.realteeth.hw.dto.JobRequest
import com.realteeth.hw.dto.JobResponse
import com.realteeth.hw.service.JobProcessor
import com.realteeth.hw.service.JobService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Jobs")
@RestController
@RequestMapping("/jobs")
class JobController(
    private val jobService: JobService,
    private val jobProcessor: JobProcessor,
) {
    @Operation(summary = "이미지 처리 요청")
    @PostMapping
    fun submitJob(@Valid @RequestBody request: JobRequest): ResponseEntity<JobResponse> {
        val (job, isNew) = jobService.createOrGetJob(request)

        if (isNew) {
            jobProcessor.process(job.id)
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(JobResponse.from(job))
    }

    @Operation(summary = "작업 상태 조회")
    @GetMapping("/{jobId}")
    fun getJob(@PathVariable jobId: UUID): ResponseEntity<JobResponse> =
        try {
            ResponseEntity.ok(JobResponse.from(jobService.getJob(jobId)))
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }

    @Operation(summary = "전체 작업 목록")
    @GetMapping
    fun listJobs(): ResponseEntity<List<JobResponse>> =
        ResponseEntity.ok(jobService.listJobs().map { JobResponse.from(it) })
}
