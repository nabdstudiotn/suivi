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
