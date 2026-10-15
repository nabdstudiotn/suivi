package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY lastName ASC, firstName ASC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY lastName ASC, firstName ASC")
    fun getActiveWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun getWorkerByIdFlow(id: String): Flow<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getWorkerById(id: String): WorkerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(worker: WorkerEntity)

    @Query("UPDATE workers SET isActive = :isActive, returnDateIso = :returnDateIso, archivedDateIso = :archivedDateIso WHERE id = :workerId")
    suspend fun updateActiveStatus(workerId: String, isActive: Boolean, returnDateIso: String?, archivedDateIso: String?)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteWorker(id: String)

    @Query("SELECT COUNT(*) FROM workers")
    suspend fun countWorkers(): Int
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE dateIso = :dateIso")
    fun getAttendanceForDate(dateIso: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId ORDER BY dateIso DESC")
    fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE dateIso LIKE :monthPrefix || '%'")
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY dateIso DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(attendance: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteAttendance(id: String)
}

@Dao
interface HourlyRateDao {
    @Query("SELECT * FROM hourly_rate_history WHERE workerId = :workerId ORDER BY effectiveDateIso DESC")
    fun getRatesForWorker(workerId: String): Flow<List<HourlyRateHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: HourlyRateHistoryEntity): Long
}

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advances WHERE dateIso LIKE :monthPrefix || '%' ORDER BY dateIso DESC")
    fun getAdvancesForMonth(monthPrefix: String): Flow<List<AdvanceEntity>>

    @Query("SELECT * FROM advances WHERE workerId = :workerId ORDER BY dateIso DESC")
    fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(advance: AdvanceEntity)
}

@Dao
interface PayrollAdjustmentDao {
    @Query("SELECT * FROM payroll_adjustments WHERE yearMonth = :yearMonth")
    fun getAdjustmentsForMonth(yearMonth: String): Flow<List<PayrollAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adj: PayrollAdjustmentEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE yearMonth = :yearMonth")
    fun getPaymentsForMonth(yearMonth: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY createdAtIso DESC")
    fun getAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY clientTimestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE syncStatus = 'PENDING'")
    suspend fun getPendingMessages(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :id")
    suspend fun getMessageById(id: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET syncStatus = :status, serverTimestamp = :serverTimestamp, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, serverTimestamp: Long?, errorMessage: String?)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun countMessagesForConversation(conversationId: String): Int
}

@Dao
interface ChatConversationDao {
    @Query("SELECT * FROM chat_conversations WHERE companyId = :companyId ORDER BY lastMessageTime DESC")
    fun getConversationsForCompany(companyId: String): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ChatConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ChatConversationEntity)
}

@Dao
interface CompanyMemberDao {
    @Query("SELECT * FROM company_members WHERE companyId = :companyId ORDER BY displayName ASC")
    fun getMembersForCompany(companyId: String): Flow<List<CompanyMemberEntity>>

    @Query("SELECT * FROM company_members WHERE companyId = :companyId AND status = 'ACTIVE' ORDER BY displayName ASC")
    fun getActiveMembersForCompany(companyId: String): Flow<List<CompanyMemberEntity>>

    @Query("SELECT * FROM company_members WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun getMembershipsForUser(userId: String): Flow<List<CompanyMemberEntity>>

    @Query("SELECT * FROM company_members WHERE userId = :userId AND status = 'ACTIVE' ORDER BY createdAtEpochMs DESC")
    fun getActiveMembershipsForUser(userId: String): Flow<List<CompanyMemberEntity>>

    @Query("SELECT * FROM company_members WHERE companyId = :companyId AND userId = :userId")
    suspend fun getMember(companyId: String, userId: String): CompanyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(member: CompanyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<CompanyMemberEntity>)

    @Query("UPDATE company_members SET status = :status WHERE companyId = :companyId AND userId = :userId")
    suspend fun updateMemberStatus(companyId: String, userId: String, status: String)

    @Query("DELETE FROM company_members WHERE companyId = :companyId AND userId = :userId")
    suspend fun deleteMember(companyId: String, userId: String)
}

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    fun getCompany(id: String): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE companyCode = :code LIMIT 1")
    suspend fun getCompanyByCode(code: String): CompanyEntity?

    @Query("SELECT * FROM companies WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR companyCode LIKE '%' || :query || '%'")
    fun searchCompanies(query: String): Flow<List<CompanyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(company: CompanyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(companies: List<CompanyEntity>)
}

@Dao
interface JoinRequestDao {
    @Query("SELECT * FROM join_requests WHERE companyId = :companyId ORDER BY createdAtEpochMs DESC")
    fun getRequestsForCompany(companyId: String): Flow<List<JoinRequestEntity>>

    @Query("SELECT * FROM join_requests WHERE companyId = :companyId AND status = 'PENDING' ORDER BY createdAtEpochMs DESC")
    fun getPendingRequestsForCompany(companyId: String): Flow<List<JoinRequestEntity>>

    @Query("SELECT * FROM join_requests WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun getRequestsForUser(userId: String): Flow<List<JoinRequestEntity>>

    @Query("SELECT * FROM join_requests WHERE id = :id LIMIT 1")
    fun getRequestById(id: String): Flow<JoinRequestEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(request: JoinRequestEntity)

    @Query("UPDATE join_requests SET status = :status, reviewedByUid = :reviewerId, reviewedByName = :reviewerName, reviewedAtEpochMs = :reviewedAt, assignedWorkerId = :workerId WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, reviewerId: String, reviewerName: String, reviewedAt: Long, workerId: String?)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE uid = :uid LIMIT 1")
    fun getUserAccount(uid: String): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserAccountEntity)
}

@Dao
interface WorkshopAnnouncementDao {
    @Query("SELECT * FROM workshop_announcements WHERE companyId = :companyId ORDER BY startEpochMs DESC")
    fun getAnnouncementsForCompany(companyId: String): Flow<List<WorkshopAnnouncementEntity>>

    @Query("SELECT * FROM workshop_announcements WHERE id = :id")
    suspend fun getAnnouncementById(id: String): WorkshopAnnouncementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(announcement: WorkshopAnnouncementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<WorkshopAnnouncementEntity>)

    @Query("UPDATE workshop_announcements SET status = :status, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("UPDATE workshop_announcements SET isReadLocally = 1 WHERE id = :id")
    suspend fun markLocallyRead(id: String)
}

@Dao
interface AnnouncementReceiptDao {
    @Query("SELECT * FROM announcement_receipts WHERE announcementId = :announcementId ORDER BY firstReadAtEpochMs ASC")
    fun getReceiptsForAnnouncement(announcementId: String): Flow<List<AnnouncementReceiptEntity>>

    @Query("SELECT * FROM announcement_receipts WHERE announcementId = :announcementId AND recipientUserId = :userId")
    suspend fun getReceiptForUser(announcementId: String, userId: String): AnnouncementReceiptEntity?

    @Query("SELECT * FROM announcement_receipts WHERE syncStatus = 'PENDING'")
    suspend fun getPendingReceipts(): List<AnnouncementReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(receipt: AnnouncementReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipts(receipts: List<AnnouncementReceiptEntity>)

    @Query("UPDATE announcement_receipts SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}

@Dao
interface AnnouncementAuditDao {
    @Query("SELECT * FROM announcement_audit_logs WHERE announcementId = :announcementId ORDER BY timestampEpochMs DESC")
    fun getAuditLogsForAnnouncement(announcementId: String): Flow<List<AnnouncementAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: AnnouncementAuditEntity)
}

@Dao
interface AuditEventDao {
    @Query("SELECT * FROM workshop_audit_events WHERE companyId = :companyId ORDER BY timestampEpochMs DESC")
    fun getAuditEvents(companyId: String): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM workshop_audit_events WHERE companyId = :companyId AND actorRole = :role ORDER BY timestampEpochMs DESC")
    fun getAuditEventsByRole(companyId: String, role: String): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(event: AuditEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AuditEventEntity>)

    @Query("SELECT * FROM workshop_audit_events WHERE syncStatus = 'PENDING'")
    suspend fun getPendingEvents(): List<AuditEventEntity>

    @Query("UPDATE workshop_audit_events SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}

@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profiles WHERE companyId = :companyId LIMIT 1")
    fun getCompanyProfile(companyId: String): Flow<CompanyProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: CompanyProfileEntity)
}

@Dao
interface ChafaPermissionsDao {
    @Query("SELECT * FROM chafa_permissions WHERE companyId = :companyId LIMIT 1")
    fun getPermissions(companyId: String): Flow<ChafaPermissionsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(permissions: ChafaPermissionsEntity)
}

@Dao
interface DeviceSessionDao {
    @Query("SELECT * FROM device_sessions WHERE companyId = :companyId ORDER BY lastActiveEpochMs DESC")
    fun getSessions(companyId: String): Flow<List<DeviceSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: DeviceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<DeviceSessionEntity>)

    @Query("UPDATE device_sessions SET isRevoked = 1 WHERE id = :id")
    suspend fun revokeSession(id: String)
}

@Dao
interface MonthClosureDao {
    @Query("SELECT * FROM month_closures WHERE companyId = :companyId ORDER BY yearMonth DESC")
    fun getAllClosures(companyId: String): Flow<List<MonthClosureEntity>>

    @Query("SELECT * FROM month_closures WHERE companyId = :companyId AND yearMonth = :yearMonth LIMIT 1")
    fun getClosureForMonth(companyId: String, yearMonth: String): Flow<MonthClosureEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(closure: MonthClosureEntity)
}


