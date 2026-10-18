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
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "slonecdaniil1922@gmail.com";

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

test("Unauthenticated user: cannot read markers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("markers").get());
});

test("Unauthenticated user: cannot create marker", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("markers").doc("marker_1").set({
    title: "Test",
    description: "Desc",
    category: "tourism",
    latitude: 50.45,
    longitude: 30.52,
    userId: ALICE_UID,
    userEmail: "alice@test.com",
    authorName: "Alice",
    status: "pending",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: can create valid pending marker", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com" }).firestore();
  await assertSucceeds(aliceDb.collection("markers").doc("marker_alice").set({
    title: "Kyiv Golden Gate",
    description: "Historic landmark",
    category: "history",
    latitude: 50.4488,
    longitude: 30.5133,
    userId: ALICE_UID,
    userEmail: "alice@test.com",
    authorName: "Alice",
    status: "pending",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: cannot bypass moderation by creating approved marker directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com" }).firestore();
  await assertFails(aliceDb.collection("markers").doc("marker_bypass").set({
    title: "Fake Approved",
    description: "Trying to self-approve",
    category: "other",
    latitude: 50.0,
    longitude: 30.0,
    userId: ALICE_UID,
    userEmail: "alice@test.com",
    authorName: "Alice",
    status: "approved",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: cannot impersonate another author UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com" }).firestore();
  await assertFails(aliceDb.collection("markers").doc("marker_impersonate").set({
    title: "Spoofed marker",
    description: "Desc",
    category: "tourism",
    latitude: 50.0,
    longitude: 30.0,
    userId: BOB_UID, // Spoofed!
    userEmail: "bob@test.com",
    authorName: "Bob",
    status: "pending",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Public reads: anyone authenticated can read approved marker", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("markers").doc("marker_approved").set({
      title: "Public Approved Marker",
      description: "Visible to all",
      category: "nature",
      latitude: 50.45,
      longitude: 30.52,
      userId: BOB_UID,
      userEmail: "bob@test.com",
      authorName: "Bob",
      status: "approved",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("markers").doc("marker_approved").get());
});

test("Privacy: user cannot read another user's pending marker", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("markers").doc("marker_pending_bob").set({
      title: "Bob's pending draft",
      description: "Not approved yet",
      category: "nature",
      latitude: 50.45,
      longitude: 30.52,
      userId: BOB_UID,
      userEmail: "bob@test.com",
      authorName: "Bob",
      status: "pending",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("markers").doc("marker_pending_bob").get());
});

test("Admin: moderator can read pending markers and approve them", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("markers").doc("marker_to_moderate").set({
      title: "Place needing review",
      description: "Check it out",
      category: "food",
      latitude: 50.45,
      longitude: 30.52,
      userId: BOB_UID,
      userEmail: "bob@test.com",
      authorName: "Bob",
      status: "pending",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(adminDb.collection("markers").doc("marker_to_moderate").get());

  await assertSucceeds(adminDb.collection("markers").doc("marker_to_moderate").update({
    status: "approved",
    moderatorNote: "Approved! Great location.",
    moderatedBy: ADMIN_EMAIL,
    updatedAt: new Date()
  }));
});

test("Author: author cannot change status to approved", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("markers").doc("marker_pending_alice").set({
      title: "Alice marker",
      description: "Desc",
      category: "tourism",
      latitude: 50.45,
      longitude: 30.52,
      userId: ALICE_UID,
      userEmail: "alice@test.com",
      authorName: "Alice",
      status: "pending",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("markers").doc("marker_pending_alice").update({
    status: "approved"
  }));
});

test("Moderator 1 (daniilslonec2000@gmail.com): can approve pending marker", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("markers").doc("marker_mod_1").set({
      title: "Place to review by daniilslonec2000",
      description: "Desc",
      category: "food",
      latitude: 50.45,
      longitude: 30.52,
      userId: BOB_UID,
      userEmail: "bob@test.com",
      authorName: "Bob",
      status: "pending",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const modDb = testEnv.authenticatedContext("mod_user_1", { email: "daniilslonec2000@gmail.com" }).firestore();
  await assertSucceeds(modDb.collection("markers").doc("marker_mod_1").get());
  await assertSucceeds(modDb.collection("markers").doc("marker_mod_1").update({
    status: "approved",
    moderatorNote: "Схвалено модератором daniilslonec2000",
    moderatedBy: "daniilslonec2000@gmail.com",
    updatedAt: new Date()
  }));
});

test("Moderator 2 (tancikozenden@gmail.com): can reject pending marker with note", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("markers").doc("marker_mod_2").set({
      title: "Place to review by tancikozenden",
      description: "Desc",
      category: "food",
      latitude: 50.45,
      longitude: 30.52,
      userId: BOB_UID,
      userEmail: "bob@test.com",
      authorName: "Bob",
      status: "pending",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const modDb = testEnv.authenticatedContext("mod_user_2", { email: "tancikozenden@gmail.com" }).firestore();
  await assertSucceeds(modDb.collection("markers").doc("marker_mod_2").get());
  await assertSucceeds(modDb.collection("markers").doc("marker_mod_2").update({
    status: "rejected",
    moderatorNote: "Не відповідає правилам",
    moderatedBy: "tancikozenden@gmail.com",
    updatedAt: new Date()
  }));
});

