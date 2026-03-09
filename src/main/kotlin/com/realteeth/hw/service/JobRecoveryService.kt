package com.realteeth.hw.service

import com.realteeth.hw.domain.JobStatus
import com.realteeth.hw.repository.JobRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class JobRecoveryService(
    private val jobRepository: JobRepository,
    private val jobProcessor: JobProcessor,
) {
    private val logger = LoggerFactory.getLogger(JobRecoveryService::class.java)

    @PostConstruct
    fun recoverIncompleteJobs() {
        val incompleteJobs = jobRepository.findAllByStatusIn(
            listOf(JobStatus.PENDING, JobStatus.PROCESSING)
        )

        if (incompleteJobs.isEmpty()) return

        logger.info("미완료 작업 {}개 복구 시작", incompleteJobs.size)

        incompleteJobs.forEach { job ->
            if (job.status == JobStatus.PROCESSING && job.mockJobId == null) {
                job.errorMessage = "복구 불가"
                job.status = JobStatus.FAILED
                job.updatedAt = Instant.now()
                jobRepository.save(job)
            } else {
                jobProcessor.process(job.id)
            }
        }
    }
}
