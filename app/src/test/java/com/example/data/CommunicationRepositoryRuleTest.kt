package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.SuiviDatabase
import com.example.domain.model.ConversationType
import com.example.domain.model.MemberRole
import com.example.domain.model.MessageSyncStatus
import com.example.domain.model.buildDirectConversationId
import com.example.domain.model.buildGroupConversationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CommunicationRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var database: SuiviDatabase
    private val companyId = "company_atelier_test"

    @Before
    fun setUpLocalDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SuiviDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownLocalDb() {
        database.close()
    }

    @Test
    fun unauthenticatedAccess_failsFast() {
        runBlocking {
            auth.signOut()
            val repo = CommunicationRepository(database, firestore, auth)
            val result = repo.sendMessage(
                companyId = companyId,
                conversationId = buildGroupConversationId(companyId),
                text = "Tentative non authentifiée",
                type = ConversationType.GROUP
            )
            assertTrue(result.isFailure)
        }
    }

    @Test
    fun directrice_canSendGroupMessage_andSyncsToRoomAndFirestore() {
        runBlocking {
            val uid = signInTestUser("directrice_test@example.com")
            assertNotNull(uid)

            val repo = CommunicationRepository(database, firestore, auth)
            // Ensure membership as Directrice
            val member = repo.ensureCurrentMember(
                companyId = companyId,
                defaultDisplayName = "Directrice Test",
                defaultRole = MemberRole.DIRECTRICE
            )
            assertEquals(uid, member.userId)
            assertEquals(MemberRole.DIRECTRICE, member.role)
            assertTrue(member.canPostGroup)

            val groupId = buildGroupConversationId(companyId)
            val result = repo.sendMessage(
                companyId = companyId,
                conversationId = groupId,
                text = "Consigne officielle d'atelier",
                type = ConversationType.GROUP
            )

            assertTrue(result.isSuccess)
            val sent = result.getOrNull()
            assertNotNull(sent)
            assertEquals("Consigne officielle d'atelier", sent!!.text)
            assertEquals(MessageSyncStatus.SYNCED, sent.syncStatus)

            // Verify Room cache has the message
            val roomMessages = repo.observeMessages(groupId).first()
            assertEquals(1, roomMessages.size)
            assertEquals(sent.id, roomMessages[0].id)
            assertEquals(MessageSyncStatus.SYNCED, roomMessages[0].syncStatus)
        }
    }

    @Test
    fun worker_withoutPostingPermission_cannotSendGroupMessage() {
        runBlocking {
            val uid = signInTestUser("ouvriere_test@example.com")
            val repo = CommunicationRepository(database, firestore, auth)

            // Register as Ouvrière without group posting rights
            repo.ensureCurrentMember(
                companyId = companyId,
                defaultDisplayName = "Ouvrière Test",
                defaultRole = MemberRole.OUVRIERE
            )

            val groupId = buildGroupConversationId(companyId)
            val result = repo.sendMessage(
                companyId = companyId,
                conversationId = groupId,
                text = "Tentative ouvrière sans permission",
                type = ConversationType.GROUP
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SecurityException)
        }
    }

    @Test
    fun directMessaging_betweenCompanyMembers_isRecordedWithHonestSync() {
        runBlocking {
            // User 1: Alice
            val aliceUid = signInTestUser("alice_worker@example.com")
            val repoAlice = CommunicationRepository(database, firestore, auth)
            val aliceMember = repoAlice.ensureCurrentMember(
                companyId = companyId,
                defaultDisplayName = "Alice",
                defaultRole = MemberRole.DIRECTRICE
            )

            // User 2: Bob (registered in company)
            val bobMember = com.example.domain.model.CompanyMember(
                companyId = companyId,
                userId = "bob_uid_456",
                displayName = "Bob",
                role = MemberRole.OUVRIERE
            )
            database.companyMemberDao().insertOrUpdate(
                com.example.data.local.CompanyMemberEntity.fromDomain(bobMember)
            )

            val conv = repoAlice.createOrGetDirectConversation(companyId, bobMember)
            val expectedConvId = buildDirectConversationId(companyId, aliceUid, "bob_uid_456")
            assertEquals(expectedConvId, conv.id)

            val result = repoAlice.sendMessage(
                companyId = companyId,
                conversationId = conv.id,
                text = "Bonjour Bob, vérifie la commande",
                type = ConversationType.DIRECT,
                recipientId = "bob_uid_456"
            )

            assertTrue(result.isSuccess)
            val msg = result.getOrNull()
            assertNotNull(msg)
            assertEquals(MessageSyncStatus.SYNCED, msg!!.syncStatus)
            assertEquals("bob_uid_456", msg.recipientId)
        }
    }

    @Test
    fun idempotentRetry_doesNotDuplicateMessage() {
        runBlocking {
            val uid = signInTestUser("retry_user@example.com")
            val repo = CommunicationRepository(database, firestore, auth)
            repo.ensureCurrentMember(
                companyId = companyId,
                defaultDisplayName = "Directrice Retry",
                defaultRole = MemberRole.DIRECTRICE
            )

            val groupId = buildGroupConversationId(companyId)
            val result = repo.sendMessage(
                companyId = companyId,
                conversationId = groupId,
                text = "Message pour test idempotence",
                type = ConversationType.GROUP
            )
            assertTrue(result.isSuccess)
            val msg = result.getOrNull()!!

            // Retry the same message explicitly
            val retry1 = repo.retryMessage(msg.id)
            assertTrue(retry1.isSuccess)

            val retry2 = repo.retryMessage(msg.id)
            assertTrue(retry2.isSuccess)

            // Room still only contains 1 message
            val messagesInRoom = repo.observeMessages(groupId).first()
            assertEquals(1, messagesInRoom.size)
            assertEquals(msg.id, messagesInRoom[0].id)
        }
    }
}
