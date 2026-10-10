package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.AtelierDate
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreAttendanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticatedAccess_failsFast() {
        runBlocking {
            auth.signOut()
            val repo = FirestoreAttendanceRepository(firestore, auth)
            try {
                repo.addWorker(
                    firstName = "Amira",
                    lastName = "Ben Ali",
                    role = "Couturière",
                    hourlyRateMillimes = 3500L,
                    hireDateIso = "2026-01-01"
                )
                fail("Expected exception when unauthenticated")
            } catch (e: IllegalStateException) {
                assertTrue(e.message?.contains("authenticated") == true)
            }
        }
    }

    @Test
    fun authenticatedUser_canAddWorkerAndLogAttendance() {
        runBlocking {
            val uid = signInTestUser("manager_workshop@example.com")
            assertNotNull(uid)

            val repo = FirestoreAttendanceRepository(firestore, auth)
            val worker = repo.addWorker(
                firstName = "Fatma",
                lastName = "Trabelsi",
                role = "Piqueuse",
                hourlyRateMillimes = 3600L,
                hireDateIso = "2026-02-01"
            )

            assertNotNull(worker.id)
            assertEquals("Fatma", worker.firstName)

            // Save attendance entry
            val entry = AttendanceEntry(
                workerId = worker.id,
                dateIso = AtelierDate.todayTunis().isoString,
                status = AttendanceStatus.WORKED,
                hours = 8,
                minutes = 0,
                note = "Journée complète"
            )
            repo.saveAttendanceEntry(entry)

            // Verify retrieval
            val attendanceMap = repo.getAttendanceForDate(AtelierDate.todayTunis().isoString).first()
            assertTrue(attendanceMap.containsKey(worker.id))
            assertEquals(480, attendanceMap[worker.id]?.workedMinutes)
        }
    }

    @Test
    fun crossUserAccess_isPreventedBySecurityRules() {
        runBlocking {
            // User 1 creates a worker
            val user1Uid = signInTestUser("user1@example.com")
            val repo1 = FirestoreAttendanceRepository(firestore, auth)
            val worker = repo1.addWorker(
                firstName = "Samira",
                lastName = "Kefi",
                role = "Finisseuse",
                hourlyRateMillimes = 3400L,
                hireDateIso = "2026-03-01"
            )

            // User 2 signs in
            signInTestUser("user2@example.com")
            // User 2 tries to access user 1's worker directly on Firestore
            try {
                firestore.collection("users").document(user1Uid).collection("workers").document(worker.id).get().await()
                fail("Expected PERMISSION_DENIED exception for cross-user document access")
            } catch (e: Exception) {
                assertTrue(e.message?.contains("PERMISSION_DENIED") == true || e.message?.contains("permission") == true)
            }
        }
    }
}
