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

test("Unauthenticated user: cannot read user profiles or ledger", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Alice: can create and read her own profile and customer ledger", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      shopName: "Alice Store",
      autoBackupEnabled: true
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      userId: ALICE_UID,
      name: "Rahim",
      balance: 500,
      phone: "01700000000"
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      customerName: "Rahim",
      type: "BAKI_SALE",
      amount: 500,
      timestamp: Date.now()
    })
  );
});

test("Bob: cannot read or write Alice's data", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).set({ userId: BOB_UID }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get());
});
