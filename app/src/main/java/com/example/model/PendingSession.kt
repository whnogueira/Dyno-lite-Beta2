package com.example.model

data class PendingSession(
    val sessionId: String,
    val vehicleId: String,
    val vehicleName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val sampleCount: Int,
    val status: String, // "PENDING", "FINALIZED", "FAILED"
    val errorMessage: String? = null,
    val errorStage: String? = null,
    val errorExceptionType: String? = null,
    val invalidField: String? = null,
    val samples: List<RunSample> = emptyList(),
    val lastAttemptTimestamp: Long = System.currentTimeMillis()
)
