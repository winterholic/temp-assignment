package com.realteeth.hw.repository

import com.realteeth.hw.domain.Job
import com.realteeth.hw.domain.JobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobRepository : JpaRepository<Job, UUID> {
    fun findFirstByImageUrlAndStatusIn(imageUrl: String, statuses: List<JobStatus>): Job?
    fun findAllByStatusIn(statuses: List<JobStatus>): List<Job>
}
