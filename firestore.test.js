const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function validProfile(uid, ts = pastDate()) {
  return {
    userId: uid,
    displayName: "Shekhar Kumar",
    tagline: "Dreamer • Fighter • Achiever",
    dailyGoalText: "Be 1% Better Than Yesterday",
    level: 1,
    xp: 120,
    streakDays: 5,
    totalStudyMinutes: 240,
    totalNaamJapCount: 1080,
    todayNaamJapCount: 108,
    todayMalas: 1,
    activeMantra: "Radha Radha",
    todayWaterGlasses: 4,
    todaySleepHours: 7.5,
    motivationMode: true,
    lastActiveDate: "2026-11-25",
    createdAt: ts,
    updatedAt: ts,
  };
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

test("Unauthenticated users are denied across all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(db.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("subjects").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("workouts").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("sadhanaLogs").get());
});

test("UserProfile create, partial update, shadow field denial, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, created))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Valid partial update (e.g., Naam Jap increment + dailyGoalText)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      todayNaamJapCount: 216,
      todayMalas: 2,
      totalNaamJapCount: 1188,
      dailyGoalText: "Master Maths & 5 Malas Today",
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost/shadow field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 200,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject cross-user read/write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      xp: 9999,
      updatedAt: pastDate(500),
    })
  );
});

test("LifeTasks CRUD, validation bounds, and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(8000);

  const tasksRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks");

  await assertSucceeds(
    tasksRef.doc("task_1").set({
      userId: ALICE_UID,
      title: "Complete Maths Chapter",
      category: "STUDY",
      completed: false,
      dueDate: "Today",
      priority: "HIGH",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(tasksRef.get());
  await assertSucceeds(
    tasksRef.doc("task_1").update({
      completed: true,
      updatedAt: pastDate(1000),
    })
  );

  // Reject @DocumentId "id" key injection
  await assertFails(
    tasksRef.doc("task_bad").set({
      id: "task_bad",
      userId: ALICE_UID,
      title: "Bad payload",
      category: "STUDY",
      completed: false,
      dueDate: "Today",
      priority: "LOW",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid enum category
  await assertFails(
    tasksRef.doc("task_enum").set({
      userId: ALICE_UID,
      title: "Invalid cat",
      category: "INVALID",
      completed: false,
      dueDate: "Today",
      priority: "LOW",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user list & delete blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").delete()
  );

  await assertSucceeds(tasksRef.doc("task_1").delete());
});

test("Subjects, Notes, Workouts, and SadhanaLogs CRUD parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(5000);
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  // Subject
  await assertSucceeds(
    userDoc.collection("subjects").doc("sub_maths").set({
      userId: ALICE_UID,
      name: "Mathematics",
      progressPercent: 80,
      studyMinutes: 120,
      accentKey: "CRIMSON",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    userDoc.collection("subjects").doc("sub_maths").update({
      progressPercent: 85,
      studyMinutes: 145,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    userDoc.collection("subjects").doc("sub_maths").update({
      progressPercent: 150,
      updatedAt: pastDate(500),
    })
  );

  // Note
  await assertSucceeds(
    userDoc.collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Ohm's Law & Circuits",
      subjectName: "Electricity",
      content: "V = I * R. Power P = V * I.",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Workout
  await assertSucceeds(
    userDoc.collection("workouts").doc("wk_1").set({
      userId: ALICE_UID,
      title: "Morning 5K Run",
      activityType: "RUNNING",
      durationMinutes: 30,
      distanceKm: 5.0,
      caloriesBurned: 320,
      dateLabel: "Today",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Sadhana Log
  await assertSucceeds(
    userDoc.collection("sadhanaLogs").doc("sad_1").set({
      userId: ALICE_UID,
      mantra: "Radha Radha",
      count: 108,
      malas: 1,
      meditationMinutes: 15,
      dateLabel: "Today",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(userDoc.collection("subjects").get());
  await assertSucceeds(userDoc.collection("notes").get());
  await assertSucceeds(userDoc.collection("workouts").get());
  await assertSucceeds(userDoc.collection("sadhanaLogs").get());
});
