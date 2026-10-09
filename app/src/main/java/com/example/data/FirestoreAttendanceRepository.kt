package com.example.data

import com.example.domain.model.AdjustmentType
import com.example.domain.model.AdvanceRecord
import com.example.domain.model.Announcement
import com.example.domain.model.AtelierDate
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.HourlyRateRecord
import com.example.domain.model.PaymentRecord
import com.example.domain.model.PayrollAdjustment
import com.example.domain.model.Worker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirestoreAttendanceRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AttendanceRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: error("Operation requires authenticated user")
    }

    private val userDoc
        get() = firestore.collection("users").document(requireUserId())

    private val workersCollection
        get() = userDoc.collection("workers")

    private val attendanceCollection
        get() = userDoc.collection("attendance")

    private val ratesCollection
        get() = userDoc.collection("rates")

    private val advancesCollection
        get() = userDoc.collection("advances")

    private val adjustmentsCollection
        get() = userDoc.collection("adjustments")

    private val paymentsCollection
        get() = userDoc.collection("payments")

    private val announcementsCollection
        get() = userDoc.collection("announcements")

    override fun getActiveWorkers(): Flow<List<Worker>> {
        return workersCollection
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToWorker(it) }
                    .filter { it.isActive }
                    .sortedBy { it.fullName }
            }
    }

    override fun getAllWorkers(): Flow<List<Worker>> {
        return workersCollection
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToWorker(it) }
                    .sortedBy { it.fullName }
            }
    }

    override fun getWorkerById(workerId: String): Flow<Worker?> {
        return workersCollection.document(workerId)
            .snapshots()
            .map { doc -> if (doc.exists()) docToWorker(doc) else null }
    }

    override fun getAttendanceForDate(dateIso: String): Flow<Map<String, AttendanceEntry>> {
        return attendanceCollection
            .whereEqualTo("dateIso", dateIso)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToAttendanceEntry(it) }
                    .associateBy { it.workerId }
            }
    }

    override fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceEntry>> {
        return attendanceCollection
            .whereEqualTo("workerId", workerId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToAttendanceEntry(it) }
                    .sortedByDescending { it.dateIso }
            }
    }

    override fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntry>> {
        return attendanceCollection
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToAttendanceEntry(it) }
                    .filter { it.dateIso.startsWith(monthPrefix) }
            }
    }

    override fun getRatesForWorker(workerId: String): Flow<List<HourlyRateRecord>> {
        return ratesCollection
            .whereEqualTo("workerId", workerId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToRate(it) }
                    .sortedByDescending { it.effectiveDateIso }
            }
    }

    override suspend fun saveAttendanceEntry(entry: AttendanceEntry): Unit = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val docId = "${entry.workerId}_${entry.dateIso}"
        val data = hashMapOf<String, Any>(
            "id" to docId,
            "userId" to uid,
            "workerId" to entry.workerId,
            "dateIso" to entry.dateIso,
            "status" to entry.status.name,
            "minutesWorked" to entry.workedMinutes,
            "note" to (entry.note ?: ""),
            "recordedBy" to "Utilisateur",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        attendanceCollection.document(docId).set(data).await()
        Unit
    }

    override suspend fun resetAttendanceEntry(workerId: String, dateIso: String): Unit = withContext(Dispatchers.IO) {
        val docId = "${workerId}_$dateIso"
        attendanceCollection.document(docId).delete().await()
        Unit
    }

    override suspend fun markDayAsRestDay(dateIso: String): Unit = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val workersSnapshot = workersCollection.get().await()
        for (doc in workersSnapshot.documents) {
            val workerId = doc.getString("id") ?: continue
            val docId = "${workerId}_$dateIso"
            val data = hashMapOf<String, Any>(
                "id" to docId,
                "userId" to uid,
                "workerId" to workerId,
                "dateIso" to dateIso,
                "status" to AttendanceStatus.REST_DAY.name,
                "minutesWorked" to 0,
                "note" to "Jour de repos",
                "recordedBy" to "Système",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            attendanceCollection.document(docId).set(data).await()
        }
        Unit
    }

    override suspend fun addWorker(
        firstName: String,
        lastName: String,
        role: String,
        hourlyRateMillimes: Long,
        hireDateIso: String
    ): Worker = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val workerData = hashMapOf<String, Any>(
            "id" to id,
            "userId" to uid,
            "firstName" to firstName,
            "lastName" to lastName,
            "role" to role,
            "startDateIso" to hireDateIso,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        workersCollection.document(id).set(workerData).await()

        val rateId = UUID.randomUUID().toString()
        val rateData = hashMapOf<String, Any>(
            "id" to rateId,
            "userId" to uid,
            "workerId" to id,
            "effectiveDateIso" to hireDateIso,
            "hourlyRateMillimes" to hourlyRateMillimes.toInt(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        ratesCollection.document(rateId).set(rateData).await()

        val initials = "${firstName.take(1)}${lastName.take(1)}".uppercase()
        Worker(
            id = id,
            firstName = firstName,
            lastName = lastName,
            initials = initials,
            currentRateMillimes = hourlyRateMillimes,
            role = role,
            isActive = true,
            hireDateIso = hireDateIso
        )
    }

    override suspend fun updateWorker(worker: Worker): Unit = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val data = hashMapOf<String, Any>(
            "id" to worker.id,
            "userId" to uid,
            "firstName" to worker.firstName,
            "lastName" to worker.lastName,
            "role" to worker.role,
            "startDateIso" to worker.hireDateIso,
            "isActive" to worker.isActive,
            "updatedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        workersCollection.document(worker.id).set(data).await()
        Unit
    }

    override suspend fun setWorkerActiveStatus(
        workerId: String,
        isActive: Boolean,
        dateIso: String
    ): Unit = withContext(Dispatchers.IO) {
        workersCollection.document(workerId).update(
            mapOf(
                "isActive" to isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    override suspend fun updateHourlyRate(
        workerId: String,
        newRateMillimes: Long,
        effectiveDateIso: String,
        motif: String
    ): Unit = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val rateId = UUID.randomUUID().toString()
        val rateData = hashMapOf<String, Any>(
            "id" to rateId,
            "userId" to uid,
            "workerId" to workerId,
            "effectiveDateIso" to effectiveDateIso,
            "hourlyRateMillimes" to newRateMillimes.toInt(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        ratesCollection.document(rateId).set(rateData).await()
        Unit
    }

    override fun getAdvancesForMonth(monthPrefix: String): Flow<List<AdvanceRecord>> {
        return advancesCollection
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToAdvance(it) }
                    .filter { it.dateIso.startsWith(monthPrefix) }
                    .sortedByDescending { it.dateIso }
            }
    }

    override fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceRecord>> {
        return advancesCollection
            .whereEqualTo("workerId", workerId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToAdvance(it) }
                    .sortedByDescending { it.dateIso }
            }
    }

    override suspend fun recordAdvance(
        workerId: String,
        dateIso: String,
        amountMillimes: Long,
        motif: String,
        author: String
    ): Unit = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val data = hashMapOf<String, Any>(
            "id" to id,
            "userId" to uid,
            "workerId" to workerId,
            "dateIso" to dateIso,
            "amountMillimes" to amountMillimes.toInt(),
            "reason" to motif,
            "recordedBy" to author,
            "createdAt" to FieldValue.serverTimestamp()
        )
        advancesCollection.document(id).set(data).await()
        Unit
    }

    override fun getAdjustmentsForMonth(yearMonth: String): Flow<List<PayrollAdjustment>> {
        return adjustmentsCollection
            .whereEqualTo("monthIso", yearMonth)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToAdjustment(it) }
            }
    }

    override suspend fun recordAdjustment(
        type: AdjustmentType,
        workerId: String,
        yearMonth: String,
        dateIso: String,
        amountMillimes: Long,
        motif: String,
        note: String?,
        author: String
    ): Unit = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val data = hashMapOf<String, Any>(
            "id" to id,
            "userId" to uid,
            "workerId" to workerId,
            "monthIso" to yearMonth,
            "type" to type.name,
            "amountMillimes" to amountMillimes.toInt(),
            "reason" to motif,
            "recordedBy" to author,
            "createdAt" to FieldValue.serverTimestamp()
        )
        adjustmentsCollection.document(id).set(data).await()
        Unit
    }

    override fun getPaymentsForMonth(yearMonth: String): Flow<List<PaymentRecord>> {
        return paymentsCollection
            .whereEqualTo("monthIso", yearMonth)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToPayment(it) }
            }
    }

    override suspend fun recordPayment(
        workerId: String,
        yearMonth: String,
        dateIso: String,
        amountMillimes: Long,
        method: String,
        payer: String,
        note: String?
    ): Unit = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val data = hashMapOf<String, Any>(
            "id" to id,
            "userId" to uid,
            "workerId" to workerId,
            "monthIso" to yearMonth,
            "amountMillimes" to amountMillimes.toInt(),
            "paymentMethod" to method,
            "paidBy" to payer,
            "createdAt" to FieldValue.serverTimestamp()
        )
        paymentsCollection.document(id).set(data).await()
        Unit
    }

    override fun getAnnouncements(): Flow<List<Announcement>> {
        return announcementsCollection
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { docToAnnouncement(it) }
                    .sortedByDescending { it.createdAtIso }
            }
    }

    override suspend fun createAnnouncement(
        title: String,
        content: String,
        audience: String,
        expirationDateIso: String,
        author: String
    ): Unit = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val data = hashMapOf<String, Any>(
            "id" to id,
            "userId" to uid,
            "title" to title,
            "content" to content,
            "authorName" to author,
            "isUrgent" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        announcementsCollection.document(id).set(data).await()
        Unit
    }

    override suspend fun deleteAnnouncement(id: String): Unit = withContext(Dispatchers.IO) {
        announcementsCollection.document(id).delete().await()
        Unit
    }

    // --- Helper converters ---
    private fun docToWorker(doc: DocumentSnapshot): Worker? {
        val id = doc.getString("id") ?: return null
        val firstName = doc.getString("firstName") ?: ""
        val lastName = doc.getString("lastName") ?: ""
        val role = doc.getString("role") ?: "Ouvrière"
        val startDateIso = doc.getString("startDateIso") ?: "2026-01-01"
        val isActive = doc.getBoolean("isActive") ?: true
        val initials = "${firstName.take(1)}${lastName.take(1)}".uppercase()

        return Worker(
            id = id,
            firstName = firstName,
            lastName = lastName,
            initials = initials,
            currentRateMillimes = 3500L,
            role = role,
            isActive = isActive,
            hireDateIso = startDateIso
        )
    }

    private fun docToAttendanceEntry(doc: DocumentSnapshot): AttendanceEntry? {
        val workerId = doc.getString("workerId") ?: return null
        val dateIso = doc.getString("dateIso") ?: return null
        val statusStr = doc.getString("status") ?: AttendanceStatus.BLANK.name
        val status = runCatching { AttendanceStatus.valueOf(statusStr) }.getOrDefault(AttendanceStatus.BLANK)
        val minutes = doc.getLong("minutesWorked")?.toInt() ?: 0
        val note = doc.getString("note")

        return AttendanceEntry(
            workerId = workerId,
            dateIso = dateIso,
            status = status,
            hours = minutes / 60,
            minutes = minutes % 60,
            note = note
        )
    }

    private fun docToRate(doc: DocumentSnapshot): HourlyRateRecord? {
        val id = doc.getString("id") ?: return null
        val workerId = doc.getString("workerId") ?: return null
        val effDate = doc.getString("effectiveDateIso") ?: return null
        val rate = doc.getLong("hourlyRateMillimes") ?: 0L

        return HourlyRateRecord(
            id = 0L,
            workerId = workerId,
            effectiveDateIso = effDate,
            rateMillimes = rate,
            motif = "Initial"
        )
    }

    private fun docToAdvance(doc: DocumentSnapshot): AdvanceRecord? {
        val id = doc.getString("id") ?: return null
        val workerId = doc.getString("workerId") ?: return null
        val dateIso = doc.getString("dateIso") ?: return null
        val amount = doc.getLong("amountMillimes") ?: 0L
        val reason = doc.getString("reason") ?: ""
        val author = doc.getString("recordedBy") ?: ""

        return AdvanceRecord(
            id = id,
            workerId = workerId,
            dateIso = dateIso,
            amountMillimes = amount,
            motif = reason,
            author = author
        )
    }

    private fun docToAdjustment(doc: DocumentSnapshot): PayrollAdjustment? {
        val id = doc.getString("id") ?: return null
        val workerId = doc.getString("workerId") ?: return null
        val monthIso = doc.getString("monthIso") ?: return null
        val typeStr = doc.getString("type") ?: AdjustmentType.PRIME.name
        val type = runCatching { AdjustmentType.valueOf(typeStr) }.getOrDefault(AdjustmentType.PRIME)
        val amount = doc.getLong("amountMillimes") ?: 0L
        val reason = doc.getString("reason") ?: ""
        val author = doc.getString("recordedBy") ?: ""

        return PayrollAdjustment(
            id = id,
            workerId = workerId,
            yearMonth = monthIso,
            type = type,
            amountMillimes = amount,
            motif = reason,
            dateIso = "${monthIso}-01",
            author = author,
            note = null
        )
    }

    private fun docToPayment(doc: DocumentSnapshot): PaymentRecord? {
        val id = doc.getString("id") ?: return null
        val workerId = doc.getString("workerId") ?: return null
        val monthIso = doc.getString("monthIso") ?: return null
        val amount = doc.getLong("amountMillimes") ?: 0L
        val method = doc.getString("paymentMethod") ?: "Espèces"
        val paidBy = doc.getString("paidBy") ?: "Gérante"

        return PaymentRecord(
            id = id,
            workerId = workerId,
            yearMonth = monthIso,
            amountMillimes = amount,
            paymentMethod = method,
            dateIso = "${monthIso}-01",
            payerName = paidBy,
            note = null
        )
    }

    private fun docToAnnouncement(doc: DocumentSnapshot): Announcement? {
        val id = doc.getString("id") ?: return null
        val title = doc.getString("title") ?: ""
        val content = doc.getString("content") ?: ""
        val author = doc.getString("authorName") ?: ""
        val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()

        return Announcement(
            id = id,
            title = title,
            content = content,
            targetAudience = "Tout l'atelier",
            expirationDateIso = "",
            createdAtIso = createdAt.toDate().toString(),
            author = author
        )
    }
}
