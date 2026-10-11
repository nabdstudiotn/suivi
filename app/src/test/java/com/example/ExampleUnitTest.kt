package com.example

import com.example.data.ai.AiMonthlyEvaluation
import com.example.data.sync.SyncState
import com.example.domain.model.Worker
import com.example.domain.model.WorkerProfileStatus
import com.example.domain.model.normalizeForFrenchSearch
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNormalizeForFrenchSearch() {
    val input = "Hélène  Chérif"
    val normalized = normalizeForFrenchSearch(input)
    assertEquals("helene cherif", normalized)
  }

  @Test
  fun testWorkerProfileStatuses() {
    assertEquals("Active", WorkerProfileStatus.ACTIVE.labelFr)
    assertEquals("Archivée", WorkerProfileStatus.ARCHIVED.labelFr)
    assertEquals("Brouillon", WorkerProfileStatus.PENDING_SAVE.labelFr)
    assertEquals("Conflit à vérifier", WorkerProfileStatus.CONFLICT.labelFr)
    assertEquals("Lecture seule", WorkerProfileStatus.READ_ONLY.labelFr)
  }

  @Test
  fun testWorkerFullName() {
    val worker = Worker(
      id = "test-123",
      firstName = "Amira",
      lastName = "Trabelsi",
      activeFrom = LocalDate.of(2026, 1, 15)
    )
    assertEquals("Amira Trabelsi", worker.fullName)
  }

  @Test
  fun testAiEvaluationBoundaries() {
    val insufficient = AiMonthlyEvaluation.insufficientData("worker-1", "2026-10")
    assertTrue(insufficient.isInsufficientData)
    assertEquals("Données insuffisantes", insufficient.summaryFr)
    assertEquals(0, insufficient.totalScore)
  }

  @Test
  fun testSyncStateHonesty() {
    val localState = SyncState.LocalOnly
    assertTrue(localState is SyncState.LocalOnly)
    val unavailableState = SyncState.SyncUnavailable("Hors-ligne")
    assertEquals("Hors-ligne", (unavailableState as SyncState.SyncUnavailable).reason)
  }
}
