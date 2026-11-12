const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: rules,
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

test("Unauthenticated user cannot read or write anything", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc(`users/${ALICE_UID}`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/tasks`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/tasks/task1`).set({
      userId: ALICE_UID,
      title: "Task 1",
      dueDate: "2026-11-20",
      isCompleted: false,
      createdAt: new Date(),
    })
  );
});

test("Alice can create and read her own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      displayName: "أحمد محمود",
      email: "ahmed@example.com",
      createdAt: new Date(),
    })
  );
  const doc = await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}`).get());
  // Bob cannot read Alice's profile
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}`).get());
});

test("Alice can create and read her own tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/tasks/task_1`).set({
      userId: ALICE_UID,
      title: "تسليم التقرير المالي",
      description: "مراجعة مع المحاسب",
      priority: "HIGH",
      dueDate: "2026-11-20",
      category: "مشاريع",
      status: "IN_PROGRESS",
      isCompleted: false,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/tasks`).get());

  // Bob cannot read Alice's task or tasks collection
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/tasks/task_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/tasks`).get());
});

test("Bob cannot write to Alice's tasks or forge userId", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob trying to write to Alice's subcollection
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/tasks/bob_task`).set({
      userId: ALICE_UID,
      title: "Malicious task",
      dueDate: "2026-11-20",
      isCompleted: false,
      createdAt: new Date(),
    })
  );

  // Bob trying to write to Alice's subcollection with his own userId
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/tasks/bob_task_2`).set({
      userId: BOB_UID,
      title: "Malicious task",
      dueDate: "2026-11-20",
      isCompleted: false,
      createdAt: new Date(),
    })
  );
});

test("Alice cannot create transaction with invalid negative amount", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_inv`).set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: -50.0, // Invalid negative amount
      date: "2026-11-11",
      category: "طعام",
      createdAt: new Date(),
    })
  );
});

test("Alice can create valid financial transaction and update it safely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.doc(`users/${ALICE_UID}/transactions/tx_1`);
  const now = new Date();

  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 150.0,
      date: "2026-11-11",
      category: "طعام وبقالة",
      description: "مشتريات خضار",
      paymentMethod: "نقدي",
      createdAt: now,
    })
  );

  // Valid update
  await assertSucceeds(
    txRef.update({
      amount: 180.0,
      description: "مشتريات خضار وفاكهة",
      updatedAt: new Date(),
    })
  );

  // Invalid update: trying to mutate createdAt or userId
  await assertFails(
    txRef.update({
      userId: BOB_UID,
    })
  );
});

test("Alice can create utility bill and Bob cannot modify it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const billRef = aliceDb.doc(`users/${ALICE_UID}/bills/bill_1`);

  await assertSucceeds(
    billRef.set({
      userId: ALICE_UID,
      name: "فاتورة الإنترنت",
      billNumber: "023588910",
      amount: 320.0,
      dueDate: "2026-11-15",
      isPaid: false,
      iconName: "wifi",
      createdAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/bills/bill_1`).update({
      isPaid: true,
    })
  );
});
