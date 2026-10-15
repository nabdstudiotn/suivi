const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection("companies").doc(COMPANY_A).set({
        id: COMPANY_A,
        name: "Atelier A",
        ownerId: ALICE_UID,
        ownerName: "Alice Directrice",
        phone: "+21600000000",
        createdAt: new Date(),
      });
      await db.collection("companies").doc(COMPANY_B).set({
        id: COMPANY_B,
        name: "Atelier B",
        ownerId: CHARLIE_UID,
        ownerName: "Charlie",
        createdAt: new Date(),
      });
      await db.collection("companies").doc(COMPANY_A).collection("members").doc(ALICE_UID).set({
        id: `${COMPANY_A}_${ALICE_UID}`,
        companyId: COMPANY_A,
        userId: ALICE_UID,
        displayName: "Alice Directrice",
        role: "DIRECTRICE",
        status: "ACTIVE",
        canPostGroup: true,
        canDirectMessage: true,
        createdAt: new Date(),
      });
      await db.collection("companies").doc(COMPANY_A).collection("members").doc(BOB_UID).set({
        id: `${COMPANY_A}_${BOB_UID}`,
        companyId: COMPANY_A,
        userId: BOB_UID,
        displayName: "Bob Ouvrière",
        role: "OUVRIERE",
        status: "ACTIVE",
        canPostGroup: false,
        canDirectMessage: true,
        createdAt: new Date(),
      });
    });
  }
});

// 1. Unauthenticated checks
test("Unauthenticated user: cannot read worker collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workers").get());
});

test("Unauthenticated user: cannot write worker", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set({
      id: "w1",
      userId: ALICE_UID,
      firstName: "Fatma",
      lastName: "Trabelsi",
      role: "Couturière",
      startDateIso: "2026-01-01",
      isActive: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

// 2. Cross-user isolation checks
test("Cross-user access: Alice cannot read Bob's workers", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("workers").doc("w_bob").set({
      id: "w_bob",
      userId: BOB_UID,
      firstName: "Amira",
      lastName: "Ben Ali",
      role: "Piqueuse",
      startDateIso: "2026-01-01",
      isActive: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("workers").doc("w_bob").get());
});

test("Cross-user access: Alice cannot write into Bob's workers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("workers").doc("w1").set({
      id: "w1",
      userId: BOB_UID,
      firstName: "Malika",
      lastName: "Sassi",
      role: "Couturière",
      startDateIso: "2026-01-01",
      isActive: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

// 3. User can manage own workers
test("Authenticated user: Alice can create and read her own workers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set({
      id: "w1",
      userId: ALICE_UID,
      firstName: "Fatma",
      lastName: "Trabelsi",
      role: "Couturière",
      startDateIso: "2026-01-01",
      isActive: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").get()
  );
});

// 4. Schema validation bounds
test("Schema validation: Fails if worker is missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set({
      id: "w1",
      userId: ALICE_UID,
      // missing firstName & lastName
      role: "Couturière",
      isActive: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Schema validation: Alice can log attendance for her worker", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att1").set({
      id: "att1",
      userId: ALICE_UID,
      workerId: "w1",
      dateIso: "2026-10-09",
      status: "WORKED",
      minutesWorked: 480,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

// 5. Communication: Group and Direct Message Rules
const COMPANY_A = "company_nabd_01";
const COMPANY_B = "company_other_99";
const CHARLIE_UID = "charlie_789";

test("Communication: Unauthenticated user cannot read company messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("companies").doc(COMPANY_A).collection("conversations").doc("conv1").collection("messages").get()
  );
});

test("Communication: Alice can post message with matching senderId and companyId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("conversations").doc("conv_grp").collection("messages").doc("msg1").set({
      id: "msg1",
      conversationId: "conv_grp",
      companyId: COMPANY_A,
      type: "GROUP",
      senderId: ALICE_UID,
      senderName: "Alice Directrice",
      senderRole: "DIRECTRICE",
      text: "Consigne d'atelier importante",
      clientTimestamp: Date.now(),
      createdAt: new Date(),
    })
  );
});

test("Communication: Impersonation rejected when senderId does not match auth.uid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("companies").doc(COMPANY_A).collection("conversations").doc("conv_grp").collection("messages").doc("msg_fake").set({
      id: "msg_fake",
      conversationId: "conv_grp",
      companyId: COMPANY_A,
      type: "GROUP",
      senderId: BOB_UID, // Alice tries to pretend she is Bob
      senderName: "Bob",
      senderRole: "OUVRIERE",
      text: "Message usurpé",
      clientTimestamp: Date.now(),
      createdAt: new Date(),
    })
  );
});

test("Communication: Cross-company write rejected when payload companyId mismatches path", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("companies").doc(COMPANY_A).collection("conversations").doc("conv_grp").collection("messages").doc("msg_cross").set({
      id: "msg_cross",
      conversationId: "conv_grp",
      companyId: COMPANY_B, // Mismatched company ID
      type: "GROUP",
      senderId: ALICE_UID,
      senderName: "Alice",
      senderRole: "DIRECTRICE",
      text: "Tentative cross company",
      clientTimestamp: Date.now(),
      createdAt: new Date(),
    })
  );
});

test("Communication: Charlie cannot read private direct message between Alice and Bob", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("companies").doc(COMPANY_A).collection("conversations").doc("dm_alice_bob").collection("messages").doc("dm_msg1").set({
      id: "dm_msg1",
      conversationId: "dm_alice_bob",
      companyId: COMPANY_A,
      type: "DIRECT",
      senderId: ALICE_UID,
      senderName: "Alice",
      senderRole: "DIRECTRICE",
      recipientId: BOB_UID,
      participantUids: [ALICE_UID, BOB_UID],
      text: "Message confidentiel pour Bob",
      clientTimestamp: Date.now(),
      createdAt: new Date(),
    });
  });

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("companies").doc(COMPANY_A).collection("conversations").doc("dm_alice_bob").collection("messages").doc("dm_msg1").get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("companies").doc(COMPANY_A).collection("conversations").doc("dm_alice_bob").collection("messages").doc("dm_msg1").get()
  );
});

// 6. Announcements & Read Receipts Security Rules
test("Announcements: Alice Directrice can publish valid workshop announcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = Date.now();
  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_001").set({
      id: "ann_001",
      companyId: COMPANY_A,
      authorId: ALICE_UID,
      authorName: "Alice Directrice",
      authorRole: "DIRECTRICE",
      type: "INFORMATION",
      title: "Réunion générale de l'atelier",
      content: "Une réunion d'atelier aura lieu demain à 08:30.",
      priority: "NORMALE",
      targetAudience: "TOUS",
      startEpochMs: now,
      endEpochMs: now + 7 * 86400000,
      durationDays: 7,
      status: "ACTIVE",
      version: 1,
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );
});

test("Announcements: Cross-company announcement publication rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = Date.now();
  await assertFails(
    aliceDb.collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_cross").set({
      id: "ann_cross",
      companyId: COMPANY_B, // Mismatched company
      authorId: ALICE_UID,
      authorName: "Alice Directrice",
      authorRole: "DIRECTRICE",
      type: "INFORMATION",
      title: "Annonce interdite",
      content: "Test cross company",
      priority: "NORMALE",
      targetAudience: "TOUS",
      startEpochMs: now,
      endEpochMs: now + 86400000,
      durationDays: 1,
      status: "ACTIVE",
      version: 1,
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );
});

test("Announcements: Impersonating authorId is rejected", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = Date.now();
  await assertFails(
    bobDb.collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_fake").set({
      id: "ann_fake",
      companyId: COMPANY_A,
      authorId: ALICE_UID, // Bob claims to be Alice
      authorName: "Alice Directrice",
      authorRole: "DIRECTRICE",
      type: "SECURITE",
      title: "Consigne fausse",
      content: "Test imposture",
      priority: "URGENTE",
      targetAudience: "TOUS",
      startEpochMs: now,
      endEpochMs: now + 86400000,
      durationDays: 1,
      status: "ACTIVE",
      version: 1,
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );
});

test("Receipts: Recipient can record their own read receipt upon opening", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_test").set({
      id: "ann_test",
      companyId: COMPANY_A,
      authorId: ALICE_UID,
      authorName: "Alice Directrice",
      authorRole: "DIRECTRICE",
      type: "INFORMATION",
      title: "Annonce pour reçu",
      content: "Contenu",
      priority: "NORMALE",
      targetAudience: "TOUS",
      startEpochMs: Date.now(),
      endEpochMs: Date.now() + 86400000,
      durationDays: 1,
      status: "ACTIVE",
      version: 1,
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = Date.now();
  await assertSucceeds(
    bobDb.collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_test").collection("receipts").doc(BOB_UID).set({
      companyId: COMPANY_A,
      announcementId: "ann_test",
      recipientUserId: BOB_UID,
      recipientName: "Bob Ouvrier",
      recipientRole: "OUVRIERE",
      firstReadAtEpochMs: now,
      lastReadAtEpochMs: now,
      announcementVersion: 1,
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );
});

test("Receipts: Coworkers cannot read each other's receipts (only Author and Recipient can)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_private").set({
      id: "ann_private",
      companyId: COMPANY_A,
      authorId: ALICE_UID,
      authorName: "Alice Directrice",
      authorRole: "DIRECTRICE",
      type: "INFORMATION",
      title: "Annonce privée",
      content: "Contenu",
      priority: "NORMALE",
      targetAudience: "TOUS",
      startEpochMs: Date.now(),
      endEpochMs: Date.now() + 86400000,
      durationDays: 1,
      status: "ACTIVE",
      version: 1,
      createdAt: new Date(),
      updatedAt: new Date()
    });
    await context.firestore().collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_private").collection("receipts").doc(BOB_UID).set({
      companyId: COMPANY_A,
      announcementId: "ann_private",
      recipientUserId: BOB_UID,
      recipientName: "Bob Ouvrier",
      recipientRole: "OUVRIERE",
      firstReadAtEpochMs: Date.now(),
      lastReadAtEpochMs: Date.now(),
      announcementVersion: 1,
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  // Charlie is a coworker, not author and not recipient -> must FAIL
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_private").collection("receipts").doc(BOB_UID).get()
  );

  // Alice is the Directrice/Author of the announcement -> can read receipt list
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_private").collection("receipts").doc(BOB_UID).get()
  );

  // Bob is the recipient -> can read their own receipt
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("companies").doc(COMPANY_A).collection("announcements").doc("ann_private").collection("receipts").doc(BOB_UID).get()
  );
});

test("Audit Log: Alice Directrice can record audit log in her company", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("audit_logs").doc("audit_01").set({
      id: "audit_01",
      companyId: COMPANY_A,
      action: "POINTAGE_SAVED",
      actorId: ALICE_UID,
      actorName: "Alice Directrice",
      actorRole: "DIRECTRICE",
      details: "Pointage enregistré pour 10 opératrices",
      timestampEpochMs: Date.now(),
      createdAt: new Date()
    })
  );
});

test("Audit Log: Cross-company audit log is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("companies").doc(COMPANY_A).collection("audit_logs").doc("audit_cross").set({
      id: "audit_cross",
      companyId: "other_company_xyz",
      action: "POINTAGE_SAVED",
      actorId: ALICE_UID,
      actorName: "Alice Directrice",
      actorRole: "DIRECTRICE",
      details: "Pointage",
      timestampEpochMs: Date.now(),
      createdAt: new Date()
    })
  );
});

test("Audit Log: Impersonating actorId is rejected", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("companies").doc(COMPANY_A).collection("audit_logs").doc("audit_fake").set({
      id: "audit_fake",
      companyId: COMPANY_A,
      action: "POINTAGE_SAVED",
      actorId: ALICE_UID,
      actorName: "Alice Fake",
      actorRole: "DIRECTRICE",
      details: "Pointage",
      timestampEpochMs: Date.now(),
      createdAt: new Date()
    })
  );
});

test("Sessions: Authenticated user can register and revoke device session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionId = "session_tab_01";
  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("sessions").doc(sessionId).set({
      id: sessionId,
      companyId: COMPANY_A,
      userId: ALICE_UID,
      userName: "Alice Directrice",
      deviceName: "Samsung Galaxy Tab A9",
      deviceType: "TABLET",
      lastActiveEpochMs: Date.now(),
      isRevoked: false,
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );

  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("sessions").doc(sessionId).update({
      isRevoked: true,
      updatedAt: new Date()
    })
  );
});

test("Permissions: Authenticated user can configure permissions for company", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("permissions").doc("chafa_default").set({
      companyId: COMPANY_A,
      canEditPastAttendance: true,
      canRecordAdvances: true,
      canPublishAnnouncements: false,
      canViewPayrollFinancials: false,
      canManageWorkers: true,
      canViewAuditLogs: true,
      canCloseArchive: true,
      lastUpdatedEpochMs: Date.now(),
      updatedAt: new Date()
    })
  );
});

test("Month Closures: Authenticated user can create and update month closure in company", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const yearMonth = "2026-05";
  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("month_closures").doc(yearMonth).set({
      companyId: COMPANY_A,
      yearMonth: yearMonth,
      status: "CLOTURE",
      closedBy: "Alice Directrice",
      closedByRole: "DIRECTRICE",
      closedAtEpochMs: Date.now(),
      revisionCount: 0,
      notes: "Clôture certifiée de fin de mois",
      updatedAt: new Date()
    })
  );

  // Update status to EN_REVISION
  await assertSucceeds(
    aliceDb.collection("companies").doc(COMPANY_A).collection("month_closures").doc(yearMonth).update({
      status: "EN_REVISION",
      revisionCount: 1,
      revisionReason: "Régularisation heures supplémentaires",
      updatedAt: new Date()
    })
  );
});

test("Month Closures: Cross-company month closure is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("companies").doc(COMPANY_A).collection("month_closures").doc("2026-05").set({
      companyId: COMPANY_B,
      yearMonth: "2026-05",
      status: "CLOTURE",
      closedBy: "Alice Directrice",
      closedByRole: "DIRECTRICE",
      closedAtEpochMs: Date.now(),
      revisionCount: 0,
      updatedAt: new Date()
    })
  );
});

test("Month Closures: Invalid status is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("companies").doc(COMPANY_A).collection("month_closures").doc("2026-05").set({
      companyId: COMPANY_A,
      yearMonth: "2026-05",
      status: "INVALID_STATUS",
      closedBy: "Alice Directrice",
      closedByRole: "DIRECTRICE",
      closedAtEpochMs: Date.now(),
      revisionCount: 0,
      updatedAt: new Date()
    })
  );
});




test("Membership security: an active worker cannot promote or suspend herself", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("companies").doc(COMPANY_A).collection("members").doc(BOB_UID).update({
      role: "DIRECTRICE",
      status: "ACTIVE",
    })
  );
});

test("Join request security: requester can create PENDING but cannot approve herself", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const request = {
    id: `req_${COMPANY_A}_${BOB_UID}`,
    companyId: COMPANY_A,
    companyName: "Atelier A",
    userId: BOB_UID,
    userEmail: "bob@example.com",
    userFullName: "Bob Ouvrière",
    userPhone: "+21611111111",
    requestedRole: "OUVRIERE",
    status: "PENDING",
    createdAtEpochMs: Date.now(),
    createdAt: new Date(),
  };
  await assertSucceeds(
    bobDb.collection("companies").doc(COMPANY_A).collection("join_requests").doc(request.id).set(request)
  );
  await assertFails(
    bobDb.collection("companies").doc(COMPANY_A).collection("join_requests").doc(request.id).update({
      status: "APPROVED",
      finalRole: "DIRECTRICE",
    })
  );
});

test("Company isolation: Alice cannot read members of a company where she is not a member", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("companies").doc(COMPANY_B).collection("members").doc(CHARLIE_UID).get()
  );
});
