package com.example.model

sealed interface SaveRunResult {
    data class Success(
        val resultId: String,
        val sampleCount: Int
    ) : SaveRunResult

    data class Failure(
        val stage: String,
        val exceptionType: String,
        val technicalMessage: String
    ) : SaveRunResult
}
