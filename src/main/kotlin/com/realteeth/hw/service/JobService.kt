package com.realteeth.hw.service

import com.realteeth.hw.domain.Job
import com.realteeth.hw.domain.JobStatus
import com.realteeth.hw.dto.JobRequest
import com.realteeth.hw.repository.JobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class CreateJobResult(val job: Job, val isNew: Boolean)

@Service
class JobService(
    private val jobRepository: JobRepository,
) {
    @Transactional
    fun createOrGetJob(request: JobRequest): CreateJobResult {
        val existing = jobRepository.findFirstByImageUrlAndStatusIn(
            request.imageUrl,
            listOf(JobStatus.PENDING, JobStatus.PROCESSING),
        )

        if (existing != null) {
            return CreateJobResult(job = existing, isNew = false)
        }

        val job = jobRepository.save(Job(imageUrl = request.imageUrl))
        return CreateJobResult(job = job, isNew = true)
    }

    @Transactional(readOnly = true)
    fun getJob(id: UUID): Job =
        jobRepository.findById(id).orElseThrow { NoSuchElementException("Job not found: $id") }

    @Transactional(readOnly = true)
    fun listJobs(): List<Job> = jobRepository.findAll()
}
