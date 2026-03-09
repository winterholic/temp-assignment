package com.realteeth.hw.domain

enum class JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    fun canTransitionTo(next: JobStatus): Boolean = when (this) {
        PENDING -> next == PROCESSING
        PROCESSING -> next == COMPLETED || next == FAILED
        COMPLETED -> false
        FAILED -> false
    }
}
