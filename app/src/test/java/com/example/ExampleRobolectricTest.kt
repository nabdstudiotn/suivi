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
    assertNotNull(mapAfterReset["w1"])
    assertEquals(AttendanceStatus.BLANK, mapAfterReset["w1"]!!.status)
    assertEquals("À renseigner", mapAfterReset["w1"]!!.durationFormatted)
  }
}
