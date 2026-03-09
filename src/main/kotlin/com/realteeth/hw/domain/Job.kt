package com.realteeth.hw.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "jobs",
    indexes = [Index(name = "idx_image_url_status", columnList = "imageUrl,status")]
)
class Job(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val imageUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobStatus = JobStatus.PENDING,

    var mockJobId: String? = null,

    @Column(columnDefinition = "TEXT")
    var result: String? = null,

    var retryCount: Int = 0,

    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,

    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    fun transitionTo(newStatus: JobStatus) {
        check(status.canTransitionTo(newStatus)) {
            "Invalid state transition: $status -> $newStatus"
        }
        status = newStatus
        updatedAt = Instant.now()
    }
}
