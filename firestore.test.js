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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 5000);
}

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

test("Unauthenticated caller cannot read or write user profile or subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("blocked_numbers").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("blocked_numbers").doc("b1").set({
      userId: ALICE_UID,
      normalizedNumber: "01712345678",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Authenticated user can create, read, and update their own UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const ts = pastTimestamp();

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      autoCallRecordingEnabled: false,
      callRecordingScope: "ALL_CALLS",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(profileRef.get());

  await assertSucceeds(
    profileRef.update({
      autoCallRecordingEnabled: true,
      callRecordingScope: "UNKNOWN_CONTACTS_ONLY",
      updatedAt: pastTimestamp(),
    })
  );
});

test("UserProfile rejects ghost fields, invalid enum, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      autoCallRecordingEnabled: false,
      callRecordingScope: "ALL_CALLS",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid recording scope enum rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      autoCallRecordingEnabled: false,
      callRecordingScope: "INVALID_SCOPE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Seed Alice profile via admin
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      autoCallRecordingEnabled: false,
      callRecordingScope: "ALL_CALLS",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Bob cannot read or overwrite Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Blocked numbers: owner CRUD succeeds, cross-user and immutable createdAt tampering fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("blocked_numbers")
    .doc("block_1");

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      normalizedNumber: "+8801712345678",
      displayLabel: "Spam",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("blocked_numbers").get()
  );

  // Update label while preserving createdAt succeeds
  await assertSucceeds(
    docRef.update({
      displayLabel: "Telemarketer",
      updatedAt: pastTimestamp(),
    })
  );

  // Tampering createdAt fails
  await assertFails(
    docRef.update({
      displayLabel: "Tampered",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Bob cannot read or delete Alice's blocked numbers
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("blocked_numbers").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("blocked_numbers").doc("block_1").delete()
  );
});

test("Favorite contacts and contact notes enforce ownership and bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorite_contacts").doc("fav_1").set({
      userId: ALICE_UID,
      lookupKey: "lookup_1",
      displayName: "Sadia Akter",
      phoneNumber: "+8801712345678",
      speedDialSlot: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Speed dial slot out of range (10) fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorite_contacts").doc("fav_2").set({
      userId: ALICE_UID,
      lookupKey: "lookup_2",
      displayName: "Bob",
      phoneNumber: "12345",
      speedDialSlot: 10,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contact_notes").doc("note_1").set({
      userId: ALICE_UID,
      lookupKey: "lookup_1",
      noteText: "Call after 5 PM",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's favorites or notes
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorite_contacts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("contact_notes").get()
  );
});
