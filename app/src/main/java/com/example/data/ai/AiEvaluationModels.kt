package com.example.data.ai

import com.example.domain.model.Worker
import java.time.LocalDate

/**
 * AI Monthly Evaluation: Strict product boundaries.
 * 
 * Rules:
 * - Scored from 100:
 *   - 40 points: worked-hour pattern around 7-hour reference.
 *   - 30 points: attendance and recorded absences (AB).
 *   - 30 points: dated manager observations.
 * - Each conclusion must expose dated evidence links using 'Voir la preuve'.
 * - If evidence is insufficient, show 'Données insuffisantes'.
 * - Gemini MUST NOT modify attendance, rates, payroll, payments, archives,
 *   permissions, worker status, or audit records.
 * - Minimal data only: Never send passwords, secrets, or unnecessary personal info.
 */
data class AiEvidence(
  val date: LocalDate,
  val description: String
)

data class AiMonthlyEvaluation(
  val workerId: String,
  val monthYear: String,
  val totalScore: Int,
  val hourPatternScore: Int,
  val attendanceScore: Int,
  val observationScore: Int,
  val summaryFr: String,
  val evidences: List<AiEvidence>,
  val isInsufficientData: Boolean = false
) {
  companion object {
    fun insufficientData(workerId: String, monthYear: String): AiMonthlyEvaluation {
      return AiMonthlyEvaluation(
        workerId = workerId,
        monthYear = monthYear,
        totalScore = 0,
        hourPatternScore = 0,
        attendanceScore = 0,
        observationScore = 0,
        summaryFr = "Données insuffisantes",
        evidences = emptyList(),
        isInsufficientData = true
      )
    }
  }
}
