package com.example

import com.example.domain.model.AtelierDate
import com.example.domain.model.CompanyMember
import com.example.domain.model.ConversationType
import com.example.domain.model.MemberRole
import com.example.domain.model.MembershipStatus
import com.example.domain.model.MessageSyncStatus
import com.example.domain.model.Worker
import com.example.domain.model.WorkerProfileStatus
import com.example.domain.model.buildDirectConversationId
import com.example.domain.model.buildGroupConversationId
import com.example.domain.model.normalizeForFrenchSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    assertEquals("Active", WorkerProfileStatus.ACTIVE.labelFrench)
    assertEquals("Archivée", WorkerProfileStatus.ARCHIVED.labelFrench)
    assertEquals("Brouillon", WorkerProfileStatus.PENDING_SAVE.labelFrench)
    assertEquals("Conflit à vérifier", WorkerProfileStatus.CONFLICT.labelFrench)
    assertEquals("Lecture seule", WorkerProfileStatus.READ_ONLY.labelFrench)
  }

  @Test
  fun testWorkerFullNameAndEmployment() {
    val worker = Worker(
      id = "worker-01",
      firstName = "Amira",
      lastName = "Trabelsi",
      hireDateIso = "2026-01-15",
      isActive = true
    )
    assertEquals("Amira Trabelsi", worker.fullName)
    assertEquals("AT", worker.initials)
    assertTrue(worker.isEmployedOn("2026-02-01"))
    assertFalse(worker.isEmployedOn("2026-01-10"))
  }

  @Test
  fun testDeterministicConversationIds() {
    val companyId = "atelier_nabd_01"
    val groupConvId = buildGroupConversationId(companyId)
    assertEquals("group_atelier_nabd_01_main", groupConvId)

    // Order of UIDs must produce the exact same direct conversation ID
    val convId1 = buildDirectConversationId(companyId, "uid_directrice", "uid_chafe")
    val convId2 = buildDirectConversationId(companyId, "uid_chafe", "uid_directrice")
    assertEquals(convId1, convId2)
    assertEquals("direct_atelier_nabd_01_uid_chafe_uid_directrice", convId1)
  }

  @Test
  fun testCompanyMemberRoleAndPermissions() {
    val directrice = CompanyMember(
      companyId = "c1",
      userId = "u1",
      displayName = "Mme Directrice",
      role = MemberRole.DIRECTRICE,
      status = MembershipStatus.ACTIVE
    )
    assertTrue(directrice.canPostGroup)
    assertTrue(directrice.canDirectMessage)

    val chafe = CompanyMember(
      companyId = "c1",
      userId = "u2",
      displayName = "Châfa Atelier",
      role = MemberRole.CHAFE,
      status = MembershipStatus.ACTIVE,
      canPostGroup = true,
      canDirectMessage = true
    )
    assertTrue(chafe.canPostGroup)
    assertTrue(chafe.canDirectMessage)

    val ouvriere = CompanyMember(
      companyId = "c1",
      userId = "u3",
      displayName = "Ouvrière Broderie",
      role = MemberRole.OUVRIERE,
      status = MembershipStatus.ACTIVE,
      canPostGroup = false,
      canDirectMessage = true
    )
    assertFalse(ouvriere.canPostGroup)
    assertTrue(ouvriere.canDirectMessage)
  }

  @Test
  fun testMessageSyncStatus() {
    assertEquals("En attente de synchronisation", MessageSyncStatus.PENDING.labelFrench)
    assertEquals("Synchronisé", MessageSyncStatus.SYNCED.labelFrench)
    assertEquals("Échec de l’envoi", MessageSyncStatus.FAILED.labelFrench)
  }

  @Test
  fun testAnnouncementAudienceEligibility() {
    val dir = CompanyMember("c1", "u1", "Directrice", MemberRole.DIRECTRICE)
    val chafe = CompanyMember("c1", "u2", "Châfa", MemberRole.CHAFE)
    val ouv = CompanyMember("c1", "u3", "Ouvrière", MemberRole.OUVRIERE)

    val now = System.currentTimeMillis()
    val annTous = com.example.domain.model.WorkshopAnnouncement(
      id = "a1", companyId = "c1", authorId = "u1", authorName = "Directrice", authorRole = MemberRole.DIRECTRICE,
      type = com.example.domain.model.AnnouncementType.INFORMATION, title = "Tous", content = "Txt",
      priority = com.example.domain.model.AnnouncementPriority.NORMALE, targetAudience = com.example.domain.model.AnnouncementAudience.TOUS,
      startEpochMs = now - 1000, endEpochMs = now + 86400000, durationDays = 1, status = com.example.domain.model.AnnouncementStatus.ACTIVE,
      createdAtEpochMs = now, updatedAtEpochMs = now
    )
    assertTrue(annTous.isAudienceEligible(dir))
    assertTrue(annTous.isAudienceEligible(chafe))
    assertTrue(annTous.isAudienceEligible(ouv))
    assertTrue(annTous.isCurrentlyActive)

    val annChafas = annTous.copy(targetAudience = com.example.domain.model.AnnouncementAudience.CHAFAS_ONLY)
    assertTrue(annChafas.isAudienceEligible(dir))
    assertTrue(annChafas.isAudienceEligible(chafe))
    assertFalse(annChafas.isAudienceEligible(ouv))

    val annOuvrieres = annTous.copy(targetAudience = com.example.domain.model.AnnouncementAudience.OUVRIERES_ONLY)
    assertTrue(annOuvrieres.isAudienceEligible(dir))
    assertFalse(annOuvrieres.isAudienceEligible(chafe))
    assertTrue(annOuvrieres.isAudienceEligible(ouv))
  }

  @Test
  fun testAnnouncementReadStatsSummary() {
    val stats = com.example.domain.model.AnnouncementReadStats(
      announcementId = "ann-1",
      targetAudienceCount = 12,
      readCount = 8,
      unreadCount = 4,
      readers = emptyList(),
      unreadMembers = emptyList()
    )
    assertEquals("Lu par 8 / 12", stats.summaryTextFrench)
    assertEquals("Non lu par 4", stats.unreadSummaryTextFrench)
  }
}
