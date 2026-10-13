package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.RoomAttendanceRepository
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.HourlyRateHistoryEntity
import com.example.data.local.SuiviDatabase
import com.example.data.local.WorkerEntity
import com.example.domain.calculation.AtelierCalculations
import com.example.domain.model.AdjustmentType
import com.example.domain.model.AdvanceRecord
import com.example.domain.model.AtelierDate
import com.example.domain.model.AtelierSection
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AttendanceViewMode
import com.example.domain.model.HourlyRateRecord
import com.example.data.local.AnnouncementReceiptEntity
import com.example.data.local.AuditEventEntity
import com.example.data.local.ChafaPermissionsEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CompanyMemberEntity
import com.example.data.local.CompanyProfileEntity
import com.example.data.local.DeviceSessionEntity
import com.example.data.local.WorkshopAnnouncementEntity
import com.example.domain.model.AuditActionCategory
import com.example.domain.model.AuditActionType
import com.example.domain.model.AuditEvent
import com.example.domain.model.ChafaPermissions
import com.example.domain.model.CompanyProfile
import com.example.domain.model.DeviceSession
import com.example.domain.model.AnnouncementAudience
import com.example.domain.model.AnnouncementPriority
import com.example.domain.model.AnnouncementStatus
import com.example.domain.model.AnnouncementType
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConversationType
import com.example.domain.model.MemberRole
import com.example.domain.model.MembershipStatus
import com.example.domain.model.MessageSyncStatus
import com.example.domain.model.buildDirectConversationId
import com.example.domain.model.buildGroupConversationId
import com.example.domain.model.PaymentRecord
import com.example.domain.model.PayrollAdjustment
import com.example.domain.model.Worker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var database: SuiviDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, SuiviDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read product name from context asserts Suivi`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Suivi", appName)
  }

  @Test
  fun `test initial database starts empty with no fake workers`() = runBlocking {
    val workerDao = database.workerDao()
    val count = workerDao.countWorkers()
    assertEquals(0, count)
  }

  @Test
  fun `test adding real worker creates profile and initial rate history`() = runBlocking {
    val repository = RoomAttendanceRepository(database)
    val worker = repository.addWorker(
      firstName = "Sarra",
      lastName = "Mansouri",
      role = "Coupeuse",
      hourlyRateMillimes = 7000L,
      hireDateIso = "2026-10-01"
    )

    assertEquals("Sarra Mansouri", worker.fullName)
    assertEquals("SM", worker.initials)
    assertEquals(7000L, worker.currentRateMillimes)
    assertTrue(worker.isActive)

    // Check rate history has 1 initial record
    val rates = repository.getRatesForWorker(worker.id).first()
    assertEquals(1, rates.size)
    assertEquals(7000L, rates[0].rateMillimes)
    assertEquals("2026-10-01", rates[0].effectiveDateIso)
  }

  @Test
  fun `test updating effective hourly rate preserves rate history`() = runBlocking {
    val repository = RoomAttendanceRepository(database)
    val worker = repository.addWorker(
      firstName = "Amira",
      lastName = "Ben Salem",
      role = "Piqueuse",
      hourlyRateMillimes = 6500L,
      hireDateIso = "2026-01-01"
    )

    // Apply new rate starting 2026-03-01
    repository.updateHourlyRate(
      workerId = worker.id,
      newRateMillimes = 7200L,
      effectiveDateIso = "2026-03-01",
      motif = "Augmentation annuelle"
    )

    val updatedWorker = repository.getWorkerById(worker.id).first()
    assertNotNull(updatedWorker)
    assertEquals(7200L, updatedWorker!!.currentRateMillimes)

    val rates = repository.getRatesForWorker(worker.id).first()
    assertEquals(2, rates.size)
    assertEquals(7200L, rates[0].rateMillimes) // ordered desc
    assertEquals(6500L, rates[1].rateMillimes)
  }

  @Test
  fun `test archiving and reactivating worker preserves single profile`() = runBlocking {
    val repository = RoomAttendanceRepository(database)
    val worker = repository.addWorker(
      firstName = "Fatma",
      lastName = "Dridi",
      role = "Surjeteuse",
      hourlyRateMillimes = 6500L,
      hireDateIso = "2026-01-10"
    )

    // Worker leaves / is archived
    repository.setWorkerActiveStatus(worker.id, false, "2026-06-30")
    val archived = repository.getWorkerById(worker.id).first()
    assertNotNull(archived)
    assertFalse(archived!!.isActive)

    // Worker returns later
    repository.setWorkerActiveStatus(worker.id, true, "2026-09-01")
    val returned = repository.getWorkerById(worker.id).first()
    assertNotNull(returned)
    assertTrue(returned!!.isActive)
    assertEquals(worker.id, returned.id) // Same continuous profile
    assertEquals("2026-09-01", returned.returnDateIso)
  }

  @Test
  fun `test Equipe search matches french accents case insensitively`() {
    val query = "hel"
    val normalizedQuery = com.example.domain.model.normalizeForFrenchSearch(query)
    
    val worker1 = Worker("w1", "Héléna", "Kacem", "HK", 6500L, "Piqueuse")
    val worker2 = Worker("w2", "Fatma", "Chérif", "FC", 6500L, "Modéliste")

    val matches1 = com.example.domain.model.normalizeForFrenchSearch(worker1.firstName).contains(normalizedQuery)
    val matches2 = com.example.domain.model.normalizeForFrenchSearch(worker2.role).contains(normalizedQuery)

    assertTrue(matches1)
    assertFalse(matches2)

    val roleQuery = com.example.domain.model.normalizeForFrenchSearch("modeliste")
    val roleMatch = com.example.domain.model.normalizeForFrenchSearch(worker2.role).contains(roleQuery)
    assertTrue(roleMatch)
  }

  @Test
  fun `test Equipe worker draft preserves uncommitted changes`() {
    val emptyDraft = com.example.domain.model.WorkerDraft()
    assertFalse(emptyDraft.isNotEmpty)

    val activeDraft = com.example.domain.model.WorkerDraft(
      firstName = "Najet",
      lastName = "Bouali",
      role = "Piqueuse",
      rateInput = "7.000"
    )
    assertTrue(activeDraft.isNotEmpty)
  }

  @Test
  fun `test Equipe duplicate homonyms have independent unique IDs`() = runBlocking {
    val repository = RoomAttendanceRepository(database)
    val worker1 = repository.addWorker("Sonia", "Trabelsi", "Piqueuse", 6500L, "2026-01-01")
    val worker2 = repository.addWorker("Sonia", "Trabelsi", "Finisseuse", 7000L, "2026-02-01")

    assertEquals(worker1.fullName, worker2.fullName)
    assertTrue(worker1.id != worker2.id)

    val allWorkers = repository.getAllWorkers().first()
    assertEquals(2, allWorkers.size)
  }

  @Test
  fun `test date boundaries and tomorrow locked logic`() {
    val today = AtelierDate(2026, 10, 8)
    val yesterday = today.previousDay()
    val tomorrow = today.nextDay()

    assertEquals("2026-10-08", today.isoString)
    assertEquals("2026-10-07", yesterday.isoString)
    assertEquals("2026-10-09", tomorrow.isoString)

    assertTrue(yesterday < today)
    assertTrue(today < tomorrow)
    val dayAfterTomorrow = tomorrow.nextDay()
    assertTrue(dayAfterTomorrow > tomorrow)
  }

  @Test
  fun `test blank versus AB versus rest day semantics`() {
    val blank = AttendanceEntry("w1", "2026-10-08", AttendanceStatus.BLANK)
    val absent = AttendanceEntry("w1", "2026-10-08", AttendanceStatus.ABSENT)
    val rest = AttendanceEntry("w1", "2026-10-08", AttendanceStatus.REST_DAY)
    val worked = AttendanceEntry("w1", "2026-10-08", AttendanceStatus.WORKED, hours = 7, minutes = 30)

    assertEquals("À renseigner", blank.durationFormatted)
    assertEquals("AB", absent.durationFormatted)
    assertEquals("Jour de repos", rest.durationFormatted)
    assertEquals("7 h 30", worked.durationFormatted)

    assertEquals(0, blank.totalMinutes)
    assertEquals(0, absent.totalMinutes)
    assertEquals(0, rest.totalMinutes)
    assertEquals(450, worked.totalMinutes)

    val sunday = AtelierDate(2026, 10, 11) // 2026-10-11 is a Sunday
    assertTrue(sunday.isSunday)
    assertEquals("Dimanche", sunday.dayOfWeekNameFrench)
  }

  @Test
  fun `test rate effective dates and exact millimes calculation`() {
    val rateHistory = listOf(
      HourlyRateRecord(id = 1, workerId = "w1", effectiveDateIso = "2026-01-01", rateMillimes = 6000L, motif = "Embauche"),
      HourlyRateRecord(id = 2, workerId = "w1", effectiveDateIso = "2026-02-15", rateMillimes = 7000L, motif = "Augmentation")
    )

    val rateJan = AtelierCalculations.findEffectiveRateForDate("2026-01-20", rateHistory, 6000L)
    assertEquals(6000L, rateJan)

    val rateFeb = AtelierCalculations.findEffectiveRateForDate("2026-02-20", rateHistory, 6000L)
    assertEquals(7000L, rateFeb)

    val entries = listOf(
      AttendanceEntry("w1", "2026-01-20", AttendanceStatus.WORKED, hours = 7, minutes = 30),
      AttendanceEntry("w1", "2026-02-20", AttendanceStatus.WORKED, hours = 7, minutes = 30)
    )

    val grossMillimes = AtelierCalculations.calculateGrossPayMillimes(entries, rateHistory, 6000L)
    assertEquals(97500L, grossMillimes) // 45000 + 52500 = 97500 millimes = 97.500 TND
  }

  @Test
  fun `test month filtering in archives excludes months without worked hours`() {
    val rateHistory = listOf(
      HourlyRateRecord(id = 1, workerId = "w1", effectiveDateIso = "2026-01-01", rateMillimes = 6500L, motif = "Initial")
    )

    val entries = listOf(
      AttendanceEntry("w1", "2026-01-10", AttendanceStatus.WORKED, hours = 7, minutes = 30),
      AttendanceEntry("w1", "2026-02-10", AttendanceStatus.WORKED, hours = 8, minutes = 0),
      AttendanceEntry("w1", "2026-03-01", AttendanceStatus.ABSENT),
      AttendanceEntry("w1", "2026-03-02", AttendanceStatus.BLANK)
    )

    val summaries = AtelierCalculations.calculateWorkedMonthsSummaries(entries, rateHistory, 6500L)

    assertEquals(2, summaries.size)
    assertEquals("2026-02", summaries[0].yearMonth)
    assertEquals("2026-01", summaries[1].yearMonth)
  }

  @Test
  fun `test approved section order conforms to Section 2`() {
    val expectedOrder = listOf(
      "Pointage",
      "Équipe",
      "Paie",
      "Communication",
      "Archives",
      "Paramètres"
    )
    val actualOrder = AtelierSection.values().map { it.titleFrench }
    assertEquals(expectedOrder, actualOrder)
  }

  @Test
  fun `test advance deduction and deterministic payroll settlement`() {
    // Worker with 7000 millimes (7.000 TND) rate
    val entries = listOf(
      AttendanceEntry("w1", "2026-10-01", AttendanceStatus.WORKED, hours = 10, minutes = 0) // 10h * 7.000 = 70.000 TND (70,000 millimes)
    )
    val rateHistory = listOf(
      HourlyRateRecord(id = 1, workerId = "w1", effectiveDateIso = "2026-10-01", rateMillimes = 7000L)
    )
    val advances = listOf(
      AdvanceRecord(id = "adv1", workerId = "w1", dateIso = "2026-10-05", amountMillimes = 20000L, motif = "Avance") // 20.000 TND
    )
    val adjustments = listOf(
      PayrollAdjustment(id = "adj1", type = AdjustmentType.PRIME, workerId = "w1", yearMonth = "2026-10", dateIso = "2026-10-10", amountMillimes = 10000L, motif = "Prime assiduité") // +10.000 TND
    )
    val payments = listOf(
      PaymentRecord(id = "pay1", workerId = "w1", yearMonth = "2026-10", dateIso = "2026-10-31", amountMillimes = 30000L) // -30.000 TND paid
    )

    val payroll = AtelierCalculations.calculateWorkerMonthPayroll(
      workerId = "w1",
      yearMonth = "2026-10",
      monthEntries = entries,
      rateHistory = rateHistory,
      fallbackRateMillimes = 7000L,
      advances = advances,
      adjustments = adjustments,
      payments = payments,
      oldUnpaidBalanceMillimes = 0L
    )

    assertEquals(70000L, payroll.basePayMillimes) // 70.000 TND
    assertEquals(20000L, payroll.advancesMillimes) // 20.000 TND
    assertEquals(10000L, payroll.primesMillimes) // 10.000 TND
    // totalDue = base (70000) + primes (10000) - advances (20000) = 60000 millimes = 60.000 TND
    assertEquals(60000L, payroll.totalDueMillimes)
    assertEquals(30000L, payroll.paidAmountMillimes)
    // remainingDue = totalDue (60000) - paid (30000) = 30000 millimes = 30.000 TND
    assertEquals(30000L, payroll.remainingDueMillimes)
  }

  @Test
  fun `test Paie Stage 3 eligibility is blocked during open month unless month ended or worker real stop`() {
    val entries = listOf(
      AttendanceEntry("w1", "2026-10-01", AttendanceStatus.WORKED, hours = 8, minutes = 0)
    )
    val rateHistory = listOf(HourlyRateRecord(id = 1, workerId = "w1", effectiveDateIso = "2026-10-01", rateMillimes = 7000L))

    val activeWorker = Worker("w1", "Nadia", "Kacem", "NK", 7000L, "Piqueuse", isActive = true)

    // Reference date mid-month (2026-10-15) -> Month is OPEN, not eligible for final calculation
    val midMonth = AtelierDate(2026, 10, 15)
    val payrollOpen = AtelierCalculations.calculateWorkerMonthPayroll(
      workerId = "w1",
      yearMonth = "2026-10",
      monthEntries = entries,
      rateHistory = rateHistory,
      fallbackRateMillimes = 7000L,
      advances = emptyList(),
      adjustments = emptyList(),
      payments = emptyList(),
      worker = activeWorker,
      referenceDate = midMonth
    )

    assertFalse(payrollOpen.isEligibleForFinal)
    assertEquals(com.example.domain.calculation.PayrollEligibilityStatus.EN_COURS_MOIS_OUVERT, payrollOpen.eligibilityStatus)

    // Reference date at month end (2026-10-31) -> Month is FINISHED, calculation is eligible
    val endMonth = AtelierDate(2026, 10, 31)
    val payrollClosed = AtelierCalculations.calculateWorkerMonthPayroll(
      workerId = "w1",
      yearMonth = "2026-10",
      monthEntries = entries,
      rateHistory = rateHistory,
      fallbackRateMillimes = 7000L,
      advances = emptyList(),
      adjustments = emptyList(),
      payments = emptyList(),
      worker = activeWorker,
      referenceDate = endMonth
    )

    assertTrue(payrollClosed.isEligibleForFinal)
    assertEquals(com.example.domain.calculation.PayrollEligibilityStatus.ELIGIBLE_FINAL, payrollClosed.eligibilityStatus)

    // Mid-month with real worker stop (archived before month end e.g. 2026-10-10) -> Eligible
    val stoppedWorker = Worker(
      id = "w1",
      firstName = "Nadia",
      lastName = "Kacem",
      initials = "NK",
      currentRateMillimes = 7000L,
      role = "Piqueuse",
      isActive = false,
      archivedDateIso = "2026-10-10"
    )
    val payrollStopped = AtelierCalculations.calculateWorkerMonthPayroll(
      workerId = "w1",
      yearMonth = "2026-10",
      monthEntries = entries,
      rateHistory = rateHistory,
      fallbackRateMillimes = 7000L,
      advances = emptyList(),
      adjustments = emptyList(),
      payments = emptyList(),
      worker = stoppedWorker,
      referenceDate = midMonth
    )

    assertTrue(payrollStopped.isEligibleForFinal)
    assertEquals(com.example.domain.calculation.PayrollEligibilityStatus.ELIGIBLE_ARRET_ACTIVITE, payrollStopped.eligibilityStatus)

    // AB alone without archived stop is NOT a worker stop -> Still ineligible during open month
    val abEntries = listOf(
      AttendanceEntry("w1", "2026-10-01", AttendanceStatus.WORKED, hours = 8, minutes = 0),
      AttendanceEntry("w1", "2026-10-02", AttendanceStatus.ABSENT),
      AttendanceEntry("w1", "2026-10-03", AttendanceStatus.ABSENT)
    )
    val payrollAbOnly = AtelierCalculations.calculateWorkerMonthPayroll(
      workerId = "w1",
      yearMonth = "2026-10",
      monthEntries = abEntries,
      rateHistory = rateHistory,
      fallbackRateMillimes = 7000L,
      advances = emptyList(),
      adjustments = emptyList(),
      payments = emptyList(),
      worker = activeWorker, // Active worker with absences only
      referenceDate = midMonth
    )
    assertFalse(payrollAbOnly.isEligibleForFinal)
  }

  @Test
  fun `test Paie Stage 3 Grade 3 blocks approval and payment when incomplete`() {
    val rateHistory = listOf(HourlyRateRecord(id = 1, workerId = "w1", effectiveDateIso = "2026-10-01", rateMillimes = 7000L))

    // Month with unrecorded day (BLANK)
    val incompleteEntries = listOf(
      AttendanceEntry("w1", "2026-10-01", AttendanceStatus.WORKED, hours = 8, minutes = 0),
      AttendanceEntry("w1", "2026-10-02", AttendanceStatus.BLANK)
    )

    val payrollIncomplete = AtelierCalculations.calculateWorkerMonthPayroll(
      workerId = "w1",
      yearMonth = "2026-10",
      monthEntries = incompleteEntries,
      rateHistory = rateHistory,
      fallbackRateMillimes = 7000L,
      advances = emptyList(),
      adjustments = emptyList(),
      payments = emptyList(),
      referenceDate = AtelierDate(2026, 10, 31)
    )

    assertEquals(com.example.domain.calculation.PayrollAuditGrade.GRADE_3_BLOQUE, payrollIncomplete.auditGrade)
    assertEquals(com.example.domain.calculation.PayrollEligibilityStatus.INCOMPLET, payrollIncomplete.eligibilityStatus)
    assertTrue(payrollIncomplete.isGrade3Blocked)
  }

  @Test
  fun `test announcements room persistence and lifecycle`() = runBlocking {
    val repository = RoomAttendanceRepository(database)
    val initialList = repository.getAnnouncements().first()
    assertTrue(initialList.isEmpty())

    repository.createAnnouncement(
      title = "Horaire d'été",
      content = "Début du travail à 07h00",
      audience = "Tout l'atelier",
      expirationDateIso = "2026-10-31",
      author = "Gérante"
    )

    val listAfterCreate = repository.getAnnouncements().first()
    assertEquals(1, listAfterCreate.size)
    assertEquals("Horaire d'été", listAfterCreate[0].title)
    assertEquals("Tout l'atelier", listAfterCreate[0].targetAudience)

    repository.deleteAnnouncement(listAfterCreate[0].id)
    val listAfterDelete = repository.getAnnouncements().first()
    assertTrue(listAfterDelete.isEmpty())
  }

  @Test
  fun `test worker isEmployedOn accurately filters by employment window`() {
    val workerActive = Worker(
      id = "w1",
      firstName = "Nadia",
      lastName = "Trabelsi",
      initials = "NT",
      currentRateMillimes = 6500L,
      role = "Piqueuse",
      isActive = true,
      hireDateIso = "2026-10-01"
    )

    // Cannot appear before hire date
    assertFalse(workerActive.isEmployedOn("2026-09-30"))
    // Appears on hire date and subsequent active dates
    assertTrue(workerActive.isEmployedOn("2026-10-01"))
    assertTrue(workerActive.isEmployedOn("2026-10-08"))

    // Stopped worker cannot appear after archive date
    val workerStopped = Worker(
      id = "w2",
      firstName = "Hela",
      lastName = "Gharbi",
      initials = "HG",
      currentRateMillimes = 6500L,
      role = "Finisseuse",
      isActive = false,
      hireDateIso = "2026-01-01",
      archivedDateIso = "2026-06-30"
    )
    assertTrue(workerStopped.isEmployedOn("2026-05-15"))
    assertFalse(workerStopped.isEmployedOn("2026-07-01"))
  }

  @Test
  fun `test Sunday automatic rest day rule in Africa-Tunis`() {
    val sunday = AtelierDate(2026, 10, 11)
    assertTrue(sunday.isSunday)
    assertEquals("Dimanche", sunday.dayOfWeekNameFrench)

    val restEntry = AttendanceEntry(
      workerId = "w1",
      dateIso = sunday.isoString,
      status = AttendanceStatus.REST_DAY
    )
    assertEquals(0, restEntry.workedMinutes)
    assertEquals("Jour de repos", restEntry.durationFormatted)
    assertFalse(restEntry.status == AttendanceStatus.ABSENT)
    assertFalse(restEntry.status == AttendanceStatus.BLANK)
  }

  @Test
  fun `test Pointage view modes Jour Semaine Mois and day generation`() {
    assertEquals("Jour", AttendanceViewMode.JOUR.titleFrench)
    assertEquals("Semaine", AttendanceViewMode.SEMAINE.titleFrench)
    assertEquals("Mois", AttendanceViewMode.MOIS.titleFrench)

    val date = AtelierDate(2026, 10, 8) // Thursday
    val weekDays = date.getWeekDays()
    assertEquals(7, weekDays.size)
    assertEquals("2026-10-05", weekDays[0].isoString) // Monday
    assertEquals("Lundi", weekDays[0].dayOfWeekNameFrench)
    assertEquals("2026-10-11", weekDays[6].isoString) // Sunday
    assertEquals("Dimanche", weekDays[6].dayOfWeekNameFrench)

    val monthDays = date.getMonthDays()
    assertEquals(31, monthDays.size)
    assertEquals("2026-10-01", monthDays[0].isoString)
    assertEquals("2026-10-31", monthDays[30].isoString)
  }

  @Test
  fun `test worked duration exact integer minutes storage without floats`() {
    val entry = AttendanceEntry(
      workerId = "w1",
      dateIso = "2026-10-08",
      status = AttendanceStatus.WORKED,
      hours = 7,
      minutes = 30
    )
    assertEquals(450, entry.workedMinutes)
    assertEquals("7 h 30", entry.durationFormatted)

    val zeroEntry = AttendanceEntry(
      workerId = "w1",
      dateIso = "2026-10-08",
      status = AttendanceStatus.BLANK
    )
    assertEquals(0, zeroEntry.workedMinutes)
    assertEquals("À renseigner", zeroEntry.durationFormatted)
  }

  @Test
  fun `test Room repository saving attendance entry and resetting to unrecorded`() = runBlocking {
    val repository = RoomAttendanceRepository(database)
    val entry = AttendanceEntry(
      workerId = "w1",
      dateIso = "2026-10-08",
      status = AttendanceStatus.WORKED,
      hours = 7,
      minutes = 30
    )
    repository.saveAttendanceEntry(entry)

    val mapAfterSave = repository.getAttendanceForDate("2026-10-08").first()
    assertNotNull(mapAfterSave["w1"])
    assertEquals(AttendanceStatus.WORKED, mapAfterSave["w1"]!!.status)
    assertEquals(450, mapAfterSave["w1"]!!.workedMinutes)

    // Reset to unrecorded
    repository.resetAttendanceEntry("w1", "2026-10-08")
    val mapAfterReset = repository.getAttendanceForDate("2026-10-08").first()
    val entryAfterReset = mapAfterReset["w1"] ?: AttendanceEntry("w1", "2026-10-08", AttendanceStatus.BLANK)
    assertEquals(AttendanceStatus.BLANK, entryAfterReset.status)
    assertEquals("À renseigner", entryAfterReset.durationFormatted)
  }

  @Test
  fun `test Pointage v2 deterministic minutes 00 or 30 and integer duration`() {
    val entry7h30 = AttendanceEntry("w1", "2026-10-08", AttendanceStatus.WORKED, hours = 7, minutes = 30)
    val entry8h00 = AttendanceEntry("w1", "2026-10-08", AttendanceStatus.WORKED, hours = 8, minutes = 0)
    val entry12h30 = AttendanceEntry("w1", "2026-10-08", AttendanceStatus.WORKED, hours = 12, minutes = 30)

    // 7h 30 = 450 minutes
    assertEquals(450, entry7h30.workedMinutes)
    assertEquals(450, entry7h30.totalMinutes)
    assertEquals("7 h 30", entry7h30.durationFormatted)

    // 8h 00 = 480 minutes
    assertEquals(480, entry8h00.workedMinutes)
    assertEquals("8 h", entry8h00.durationFormatted)

    // 12h 30 = 750 minutes
    assertEquals(750, entry12h30.workedMinutes)
    assertEquals("12 h 30", entry12h30.durationFormatted)
  }

  @Test
  fun `test Pointage v2 ordering groups workers by consecutive AB threshold`() {
    val wA = Worker("wA", "Amira", "Bouazizi", "AB", 6500L, "Piqueuse", isActive = true)
    val wB = Worker("wB", "Bochra", "Trabelsi", "BT", 6500L, "Piqueuse", isActive = true)
    val wC = Worker("wC", "Cyrine", "Gharbi", "CG", 6500L, "Piqueuse", isActive = true)

    val date = AtelierDate(2026, 10, 8) // Thursday
    // wA has 5 consecutive AB days (2026-10-04 is Sunday, skipped; days: 02, 03, 05, 06, 07, 08)
    val entries = mutableMapOf<String, AttendanceEntry>()
    val abDays = listOf("2026-10-02", "2026-10-03", "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08")
    for (d in abDays) {
      entries["wA_$d"] = AttendanceEntry("wA", d, AttendanceStatus.ABSENT)
    }

    // wB has 2 AB days
    entries["wB_2026-10-07"] = AttendanceEntry("wB", "2026-10-07", AttendanceStatus.ABSENT)
    entries["wB_2026-10-08"] = AttendanceEntry("wB", "2026-10-08", AttendanceStatus.ABSENT)

    // wC worked on 2026-10-08
    entries["wC_2026-10-08"] = AttendanceEntry("wC", "2026-10-08", AttendanceStatus.WORKED, hours = 7, minutes = 30)

    val state = com.example.ui.attendance.AttendanceUiState(
      currentDate = date,
      allWorkers = listOf(wA, wB, wC),
      monthAttendanceEntries = entries
    )

    // Check consecutive AB calculation
    assertTrue(state.consecutiveAbDaysFor("wA") >= 5)
    assertEquals(2, state.consecutiveAbDaysFor("wB"))
    assertEquals(0, state.consecutiveAbDaysFor("wC"))

    // Pointage ordered list puts < 5 consecutive AB first (Bochra, Cyrine), then >= 5 at bottom (Amira)
    val ordered = state.pointageOrderedWorkers
    assertEquals(3, ordered.size)
    assertEquals("Bochra Trabelsi", ordered[0].fullName)
    assertEquals("Cyrine Gharbi", ordered[1].fullName)
    assertEquals("Amira Bouazizi", ordered[2].fullName)
  }

  @Test
  fun `test Pointage v2 single context deletion preserves worker profile and rate history`() = runBlocking {
    val repository = RoomAttendanceRepository(database)
    val worker = repository.addWorker(
      firstName = "Mouna",
      lastName = "Kefi",
      role = "Coupeuse",
      hourlyRateMillimes = 7500L,
      hireDateIso = "2026-01-01"
    )

    // Save attendance for 2 dates
    repository.saveAttendanceEntry(
      AttendanceEntry(workerId = worker.id, dateIso = "2026-10-05", status = AttendanceStatus.WORKED, hours = 8, minutes = 0)
    )
    repository.saveAttendanceEntry(
      AttendanceEntry(workerId = worker.id, dateIso = "2026-10-06", status = AttendanceStatus.WORKED, hours = 7, minutes = 30)
    )

    // Delete attendance only for 2026-10-06
    repository.resetAttendanceEntry(worker.id, "2026-10-06")

    // Verify 2026-10-06 is reset to BLANK / unrecorded
    val map06 = repository.getAttendanceForDate("2026-10-06").first()
    val entry06 = map06[worker.id] ?: AttendanceEntry(worker.id, "2026-10-06", AttendanceStatus.BLANK)
    assertEquals(AttendanceStatus.BLANK, entry06.status)

    // Verify 2026-10-05 is intact
    val map05 = repository.getAttendanceForDate("2026-10-05").first()
    assertEquals(AttendanceStatus.WORKED, map05[worker.id]?.status)
    assertEquals(480, map05[worker.id]?.workedMinutes)

    // Verify worker profile and rate history are intact
    val profile = repository.getWorkerById(worker.id).first()
    assertNotNull(profile)
    assertEquals("Mouna Kefi", profile!!.fullName)
    val rates = repository.getRatesForWorker(worker.id).first()
    assertEquals(1, rates.size)
    assertEquals(7500L, rates[0].rateMillimes)
  }

  @Test
  fun `test Communication Room cache inserts and queries by conversation`() = runBlocking {
    val chatDao = database.chatMessageDao()
    val convId = buildGroupConversationId("atelier_nabd_01")

    val msg1 = ChatMessageEntity(
      id = "msg_001",
      conversationId = convId,
      companyId = "atelier_nabd_01",
      type = "GROUP",
      senderId = "uid_directrice",
      senderName = "Mme Directrice",
      senderRole = "Directrice",
      text = "Bonjour à toute l'équipe de l'atelier.",
      clientTimestamp = 1700000000000L,
      syncStatus = "SYNCED"
    )
    val msg2 = ChatMessageEntity(
      id = "msg_002",
      conversationId = convId,
      companyId = "atelier_nabd_01",
      type = "GROUP",
      senderId = "uid_chafe",
      senderName = "Châfa Atelier",
      senderRole = "Chafe",
      text = "Bonjour Madame, les machines sont prêtes.",
      clientTimestamp = 1700000010000L,
      syncStatus = "SYNCED"
    )

    chatDao.insertMessage(msg1)
    chatDao.insertMessage(msg2)

    val list = chatDao.getMessagesForConversation(convId).first()
    assertEquals(2, list.size)
    assertEquals("msg_001", list[0].id)
    assertEquals("msg_002", list[1].id)
    assertEquals("GROUP", list[1].type)
  }

  @Test
  fun `test Communication outbox queue and retry status transitions`() = runBlocking {
    val chatDao = database.chatMessageDao()
    val convId = buildGroupConversationId("atelier_nabd_01")

    val pendingMsg = ChatMessageEntity(
      id = "msg_offline_1",
      conversationId = convId,
      companyId = "atelier_nabd_01",
      type = "GROUP",
      senderId = "uid_directrice",
      senderName = "Mme Directrice",
      senderRole = "Directrice",
      text = "Message envoyé hors-ligne",
      clientTimestamp = System.currentTimeMillis(),
      syncStatus = "PENDING"
    )

    chatDao.insertMessage(pendingMsg)

    // Check it appears in outbox
    val outbox = chatDao.getPendingMessages()
    assertEquals(1, outbox.size)
    assertEquals("msg_offline_1", outbox[0].id)

    // Mark as failed
    chatDao.updateSyncStatus("msg_offline_1", "FAILED", serverTimestamp = null, errorMessage = "Network timeout")
    val updated = chatDao.getMessageById("msg_offline_1")
    assertNotNull(updated)
    assertEquals("FAILED", updated!!.syncStatus)
    assertEquals("Network timeout", updated.errorMessage)

    // Re-queue on retry
    chatDao.updateSyncStatus("msg_offline_1", "PENDING", serverTimestamp = null, errorMessage = null)
    val requeued = chatDao.getMessageById("msg_offline_1")
    assertEquals("PENDING", requeued!!.syncStatus)

    // Mark synced when confirmed by Firestore
    chatDao.updateSyncStatus("msg_offline_1", "SYNCED", serverTimestamp = System.currentTimeMillis(), errorMessage = null)
    val synced = chatDao.getMessageById("msg_offline_1")
    assertEquals("SYNCED", synced!!.syncStatus)
    val emptyOutbox = chatDao.getPendingMessages()
    assertTrue(emptyOutbox.isEmpty())
  }

  @Test
  fun `test Communication idempotency and deduplication on repeated insert`() = runBlocking {
    val chatDao = database.chatMessageDao()
    val convId = buildGroupConversationId("atelier_nabd_01")

    val msg = ChatMessageEntity(
      id = "msg_dedup_01",
      conversationId = convId,
      companyId = "atelier_nabd_01",
      type = "GROUP",
      senderId = "uid_directrice",
      senderName = "Mme Directrice",
      senderRole = "Directrice",
      text = "Test déduplication",
      clientTimestamp = 1700000000000L,
      syncStatus = "PENDING"
    )

    chatDao.insertMessage(msg)
    // Inserting the same message ID again with updated sync status (REPLACE conflict strategy)
    val msgUpdated = msg.copy(syncStatus = "SYNCED", serverTimestamp = 1700000001000L)
    chatDao.insertMessage(msgUpdated)

    val count = chatDao.countMessagesForConversation(convId)
    assertEquals(1, count)
    val retrieved = chatDao.getMessageById("msg_dedup_01")
    assertEquals("SYNCED", retrieved?.syncStatus)
    assertEquals(1700000001000L, retrieved?.serverTimestamp)
  }

  @Test
  fun `test Company isolation ensures no message leak across companies`() = runBlocking {
    val chatDao = database.chatMessageDao()
    val convCompanyA = buildGroupConversationId("company_A")
    val convCompanyB = buildGroupConversationId("company_B")

    val msgA = ChatMessageEntity(
      id = "msg_comp_a",
      conversationId = convCompanyA,
      companyId = "company_A",
      type = "GROUP",
      senderId = "u_a",
      senderName = "Directrice A",
      senderRole = "Directrice",
      text = "Secret Company A",
      clientTimestamp = 1000L,
      syncStatus = "SYNCED"
    )
    val msgB = ChatMessageEntity(
      id = "msg_comp_b",
      conversationId = convCompanyB,
      companyId = "company_B",
      type = "GROUP",
      senderId = "u_b",
      senderName = "Directrice B",
      senderRole = "Directrice",
      text = "Secret Company B",
      clientTimestamp = 2000L,
      syncStatus = "SYNCED"
    )

    chatDao.insertMessage(msgA)
    chatDao.insertMessage(msgB)

    val messagesA = chatDao.getMessagesForConversation(convCompanyA).first()
    assertEquals(1, messagesA.size)
    assertEquals("Secret Company A", messagesA[0].text)

    val messagesB = chatDao.getMessagesForConversation(convCompanyB).first()
    assertEquals(1, messagesB.size)
    assertEquals("Secret Company B", messagesB[0].text)
  }

  @Test
  fun `test Announcement Room cache insertion and query by company`() = runBlocking {
    val annDao = database.workshopAnnouncementDao()
    val now = System.currentTimeMillis()

    val ann1 = WorkshopAnnouncementEntity(
      id = "ann_01",
      companyId = "company_1",
      authorId = "u_dir",
      authorName = "Mme Directrice",
      authorRole = "DIRECTRICE",
      type = "INFORMATION",
      title = "Réunion d'atelier",
      content = "Réunion demain à 08h30.",
      priority = "NORMALE",
      targetAudience = "TOUS",
      startEpochMs = now,
      endEpochMs = now + 7 * 86400000L,
      durationDays = 7,
      status = "ACTIVE",
      version = 1,
      createdAtEpochMs = now,
      updatedAtEpochMs = now,
      syncStatus = "SYNCED"
    )

    annDao.insertOrUpdate(ann1)
    val list = annDao.getAnnouncementsForCompany("company_1").first()
    assertEquals(1, list.size)
    assertEquals("ann_01", list[0].id)
    assertEquals("Réunion d'atelier", list[0].title)
  }

  @Test
  fun `test Announcement cancellation updates status without deletion`() = runBlocking {
    val annDao = database.workshopAnnouncementDao()
    val now = System.currentTimeMillis()

    val ann = WorkshopAnnouncementEntity(
      id = "ann_cancel",
      companyId = "company_1",
      authorId = "u_dir",
      authorName = "Mme Directrice",
      authorRole = "DIRECTRICE",
      type = "NOTIFICATION",
      title = "Annonce à annuler",
      content = "Contenu",
      priority = "IMPORTANTE",
      targetAudience = "TOUS",
      startEpochMs = now,
      endEpochMs = now + 86400000L,
      durationDays = 1,
      status = "ACTIVE",
      version = 1,
      createdAtEpochMs = now,
      updatedAtEpochMs = now,
      syncStatus = "SYNCED"
    )

    annDao.insertOrUpdate(ann)
    annDao.updateStatus("ann_cancel", "CANCELLED", now + 1000L)

    val updated = annDao.getAnnouncementById("ann_cancel")
    assertNotNull(updated)
    assertEquals("CANCELLED", updated!!.status)
  }

  @Test
  fun `test Announcement read receipt idempotency and timestamp update`() = runBlocking {
    val receiptDao = database.announcementReceiptDao()
    val firstRead = 1000L
    val secondRead = 5000L

    val receiptInitial = AnnouncementReceiptEntity(
      id = "ann_1_u_worker_v1",
      announcementId = "ann_1",
      companyId = "company_1",
      recipientUserId = "u_worker",
      recipientName = "Fatma Dridi",
      recipientRole = "OUVRIERE",
      firstReadAtEpochMs = firstRead,
      lastReadAtEpochMs = firstRead,
      announcementVersion = 1,
      syncStatus = "SYNCED"
    )

    receiptDao.insertOrUpdate(receiptInitial)

    // Re-opening the announcement updates lastReadAt while preserving firstReadAt
    val receiptUpdated = receiptInitial.copy(lastReadAtEpochMs = secondRead)
    receiptDao.insertOrUpdate(receiptUpdated)

    val list = receiptDao.getReceiptsForAnnouncement("ann_1").first()
    assertEquals(1, list.size) // Exact single row maintained!
    assertEquals(firstRead, list[0].firstReadAtEpochMs)
    assertEquals(secondRead, list[0].lastReadAtEpochMs)
    assertEquals("Fatma Dridi", list[0].recipientName)
  }

  @Test
  fun `test Announcements company isolation prevents data leak`() = runBlocking {
    val annDao = database.workshopAnnouncementDao()
    val now = System.currentTimeMillis()

    val annA = WorkshopAnnouncementEntity(
      id = "ann_a", companyId = "company_A", authorId = "u_a", authorName = "Dir A", authorRole = "DIRECTRICE",
      type = "INFORMATION", title = "Annonce A", content = "Secret A", priority = "NORMALE", targetAudience = "TOUS",
      startEpochMs = now, endEpochMs = now + 86400000L, durationDays = 1, status = "ACTIVE", version = 1,
      createdAtEpochMs = now, updatedAtEpochMs = now, syncStatus = "SYNCED"
    )
    val annB = WorkshopAnnouncementEntity(
      id = "ann_b", companyId = "company_B", authorId = "u_b", authorName = "Dir B", authorRole = "DIRECTRICE",
      type = "INFORMATION", title = "Annonce B", content = "Secret B", priority = "NORMALE", targetAudience = "TOUS",
      startEpochMs = now, endEpochMs = now + 86400000L, durationDays = 1, status = "ACTIVE", version = 1,
      createdAtEpochMs = now, updatedAtEpochMs = now, syncStatus = "SYNCED"
    )

    annDao.insertOrUpdate(annA)
    annDao.insertOrUpdate(annB)

    val listA = annDao.getAnnouncementsForCompany("company_A").first()
    assertEquals(1, listA.size)
    assertEquals("Annonce A", listA[0].title)

    val listB = annDao.getAnnouncementsForCompany("company_B").first()
    assertEquals(1, listB.size)
    assertEquals("Annonce B", listB[0].title)
  }

  @Test
  fun `test Audit log inserts events and queries chronologically by company`() = runBlocking {
    val auditDao = database.auditEventDao()
    val companyId = "comp_audit_01"
    val t1 = 1000000000L
    val t2 = 2000000000L
    val t3 = 3000000000L

    val event1 = AuditEventEntity(
      id = "ev_1",
      companyId = companyId,
      actionType = AuditActionType.POINTAGE_SAVED.name,
      actorId = "u_chafa",
      actorName = "Salma Ben Amor",
      actorRole = "CHAFE",
      detailsFr = "Pointage enregistré pour 12 opératrices",
      timestampEpochMs = t1,
      syncStatus = "SYNCED"
    )
    val event2 = AuditEventEntity(
      id = "ev_2",
      companyId = companyId,
      actionType = AuditActionType.ADVANCE_RECORDED.name,
      actorId = "u_directrice",
      actorName = "Fatma Directrice",
      actorRole = "DIRECTRICE",
      detailsFr = "Acompte de 50.000 TND accordé à Amina",
      timestampEpochMs = t2,
      syncStatus = "SYNCED"
    )
    val event3 = AuditEventEntity(
      id = "ev_3",
      companyId = companyId,
      actionType = AuditActionType.PERMISSION_CHANGED.name,
      actorId = "u_directrice",
      actorName = "Fatma Directrice",
      actorRole = "DIRECTRICE",
      detailsFr = "Permissions Châfa mises à jour",
      timestampEpochMs = t3,
      syncStatus = "SYNCED"
    )

    auditDao.insertAudit(event1)
    auditDao.insertAudit(event2)
    auditDao.insertAudit(event3)

    val list = auditDao.getAuditEvents(companyId).first()
    assertEquals(3, list.size)
    // Verify chronological descending order (newest first)
    assertEquals("ev_3", list[0].id)
    assertEquals(t3, list[0].timestampEpochMs)
    assertEquals(AuditActionType.PERMISSION_CHANGED.name, list[0].actionType)

    assertEquals("ev_2", list[1].id)
    assertEquals(t2, list[1].timestampEpochMs)

    assertEquals("ev_1", list[2].id)
    assertEquals(t1, list[2].timestampEpochMs)
    assertEquals("CHAFE", list[2].actorRole)

    // Test domain conversion and Tunis time formatting
    val domainEvent = list[0].toDomain()
    assertTrue(domainEvent.formattedDateTimeTunis.contains("Heure de Tunis"))
  }

  @Test
  fun `test Audit log strictly isolates events between workshops`() = runBlocking {
    val auditDao = database.auditEventDao()

    val evA = AuditEventEntity(
      id = "ev_a",
      companyId = "company_Alpha",
      actionType = AuditActionType.COMPANY_UPDATED.name,
      actorId = "dir_a",
      actorName = "Directrice Alpha",
      actorRole = "DIRECTRICE",
      detailsFr = "Mise à jour atelier Alpha",
      timestampEpochMs = 1500L,
      syncStatus = "SYNCED"
    )
    val evB = AuditEventEntity(
      id = "ev_b",
      companyId = "company_Beta",
      actionType = AuditActionType.COMPANY_UPDATED.name,
      actorId = "dir_b",
      actorName = "Directrice Beta",
      actorRole = "DIRECTRICE",
      detailsFr = "Mise à jour atelier Beta",
      timestampEpochMs = 1600L,
      syncStatus = "SYNCED"
    )

    auditDao.insertAudit(evA)
    auditDao.insertAudit(evB)

    val listA = auditDao.getAuditEvents("company_Alpha").first()
    assertEquals(1, listA.size)
    assertEquals("ev_a", listA[0].id)
    assertEquals("company_Alpha", listA[0].companyId)

    val listB = auditDao.getAuditEvents("company_Beta").first()
    assertEquals(1, listB.size)
    assertEquals("ev_b", listB[0].id)
    assertEquals("company_Beta", listB[0].companyId)
  }

  @Test
  fun `test Company profile updates and persists in Room`() = runBlocking {
    val profileDao = database.companyProfileDao()
    val companyId = "atelier_test_01"

    val profile = CompanyProfileEntity(
      companyId = companyId,
      name = "Atelier Couture Nabd",
      ownerId = "owner_123",
      ownerName = "Mme Fatma",
      matriculeFiscal = "1458921/A/M/000",
      address = "Charguia 1, Tunis",
      phone = "+216 71 840 220",
      standardDailyHours = 8,
      referenceRateMillimes = 3500L,
      currency = "TND"
    )

    profileDao.insertOrUpdate(profile)

    val retrieved = profileDao.getCompanyProfile(companyId).first()
    assertNotNull(retrieved)
    assertEquals("Atelier Couture Nabd", retrieved?.name)
    assertEquals("1458921/A/M/000", retrieved?.matriculeFiscal)
    assertEquals(8, retrieved?.standardDailyHours)

    // Update with new phone and 9h standard
    val updated = profile.copy(phone = "+216 71 999 888", standardDailyHours = 9)
    profileDao.insertOrUpdate(updated)

    val retrievedUpdated = profileDao.getCompanyProfile(companyId).first()
    assertEquals("+216 71 999 888", retrievedUpdated?.phone)
    assertEquals(9, retrievedUpdated?.standardDailyHours)
  }

  @Test
  fun `test Chafa permissions matrix persists and updates accurately`() = runBlocking {
    val permDao = database.chafaPermissionsDao()
    val companyId = "atelier_perms_01"

    val perms = ChafaPermissionsEntity(
      companyId = companyId,
      canEditPastAttendance = false,
      canRecordAdvances = true,
      canPublishAnnouncements = false,
      canViewPayrollFinancials = false,
      canManageWorkers = false,
      canViewAuditLogs = true,
      lastUpdatedEpochMs = 1000L
    )

    permDao.insertOrUpdate(perms)

    val initial = permDao.getPermissions(companyId).first()
    assertNotNull(initial)
    assertEquals(false, initial?.canEditPastAttendance)
    assertEquals(true, initial?.canRecordAdvances)

    // Directrice authorizes Chafa to edit past attendance and manage workers
    val updated = perms.copy(canEditPastAttendance = true, canManageWorkers = true, lastUpdatedEpochMs = 2000L)
    permDao.insertOrUpdate(updated)

    val modified = permDao.getPermissions(companyId).first()
    assertEquals(true, modified?.canEditPastAttendance)
    assertEquals(true, modified?.canManageWorkers)
    assertEquals(true, modified?.canRecordAdvances)
    assertEquals(false, modified?.canPublishAnnouncements)
  }

  @Test
  fun `test Device session tracking and revocation in Room`() = runBlocking {
    val sessionDao = database.deviceSessionDao()
    val companyId = "comp_sess_01"

    val tablet = DeviceSessionEntity(
      id = "sess_tab",
      companyId = companyId,
      userId = "u_dir",
      userName = "Directrice",
      userRole = "DIRECTRICE",
      deviceName = "Samsung Galaxy Tab A9",
      deviceType = "TABLET",
      ipOrLocation = "Tunis, Tunisie",
      isCurrentDevice = true,
      isRevoked = false,
      lastActiveEpochMs = 1000L
    )
    val phone = DeviceSessionEntity(
      id = "sess_phone",
      companyId = companyId,
      userId = "u_chafa",
      userName = "Salma Châfa",
      userRole = "CHAFE",
      deviceName = "Redmi Note 12",
      deviceType = "PHONE",
      ipOrLocation = "Tunis, Tunisie",
      isCurrentDevice = false,
      isRevoked = false,
      lastActiveEpochMs = 2000L
    )

    sessionDao.insertAll(listOf(tablet, phone))

    val list = sessionDao.getSessions(companyId).first()
    assertEquals(2, list.size)
    assertEquals(false, list.find { it.id == "sess_phone" }?.isRevoked)

    // Revoke access for the phone
    sessionDao.revokeSession("sess_phone")

    val listAfterRevoke = sessionDao.getSessions(companyId).first()
    val revokedPhone = listAfterRevoke.find { it.id == "sess_phone" }
    val activeTablet = listAfterRevoke.find { it.id == "sess_tab" }

    assertEquals(true, revokedPhone?.isRevoked)
    assertEquals(false, activeTablet?.isRevoked)
  }
}



