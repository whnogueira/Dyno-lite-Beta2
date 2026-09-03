package com.example.model

/**
 * Histórico de revisões de uma passagem corrigida pelo usuário (Requisito 8).
 */
data class TestResultRevision(
  val revisionId: String,
  val testResultId: String,
  val revisionNumber: Int,
  val createdAt: String,
  val reason: String = "Dados da passagem corrigidos pelo usuário",
  val note: String? = null,
  val previousConfigurationJson: String = "{}",
  val correctedConfigurationJson: String = "{}",
  val previousCalculatedResultJson: String = "{}",
  val correctedCalculatedResultJson: String = "{}"
)
