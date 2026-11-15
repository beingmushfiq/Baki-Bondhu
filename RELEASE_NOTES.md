# Release Notes — Baki Bondhu (বাকী বন্ধু) v2.0.0

> **Release Tag**: `v2.0.0`  
> **Commit**: `40ae3734ce2e7f257fce5e04de480a65c8ed1fbb`  
> **Date**: 16 November 2026  
> **Package ID**: `com.aistudio.bakibondhu.kxdznp`  
> **Owner & Engineering**: DevCenterPoint ([devcenterpoint.com](https://devcenterpoint.com))

---

## 📱 Google Play Store / Storefront Release Notes

### 🇧🇩 বাংলা (Bengali — User Facing)
```
বাকী বন্ধু (Baki Bondhu) ২.০.০ সংস্করণে আপনাকে স্বাগতম!

• নির্ভুল পাইসা ভিত্তিক হিসাব: ফ্লোটিং পয়েন্টের কোনো গরমিল ছাড়াই পাইসা (১ টাকা = ১০০ পাইসা) পর্যন্ত সঠিক আর্থিক হিসাব।
• ভয়েস ইনপুটে সুরক্ষা: মুখে বলার সময় টাকার পরিমাণ বাদ পড়লে স্বয়ংক্রিয় সতর্কবার্তা এবং দ্রুত টাকার পরিমাণ সিলেক্ট করার সুবিধা।
• সম্পূর্ণ অফলাইন-ফার্স্ট: ইন্টারনেট ছাড়াই নতুন কাস্টমার তৈরি, বাকী ও জমার হিসাব রাখা এবং রিপোর্ট দেখার নিশ্চয়তা।
• নিরাপদ ক্লাউড ব্যাকআপ ও প্রিভিউ: গুগল দিয়ে লগইন করে ক্লাউডে ব্যাকআপ নেওয়া এবং রিস্টোর করার আগে কাস্টমার ও লেনদেনের তুলনামূলক চিত্র দেখে স্মার্ট মার্জ করার সুবিধা।
• সাপ্তাহিক ও দৈনিক আর্থিক রিপোর্ট: বাংলাদেশি দোকানদারদের উপযোগী শনিবার-ভিত্তিক বাণিজ্যিক সপ্তাহ অনুযায়ী লাভ-ক্ষতি, বাকী আদায় হার এবং ক্যাশ ড্রয়ার মিলানোর সার্বিক হিসাব।
• ১-ট্যাপ হোয়াটসঅ্যাপ রিমাইন্ডার: বকেয়া টাকার জন্য কাস্টমারকে বিকাশ ও নগদ নম্বরসহ সম্মানজনক বাংলায় তাগাদা মেসেজ পাঠানোর সুবিধা।
• নতুন দোকানদার অনবোর্ডিং: প্রথমবার ব্যবহারে দোকানের নাম, প্রোপাইটার ও পেমেন্ট নম্বর সহজেই সেটআপ করার উইজার্ড।
```

### 🇬🇧 English (User Facing)
```
What's New in Version 2.0.0:

• Zero-Drift Financial Precision: All balances and transactions calculated in minor units (1 BDT = 100 Paisa), eliminating floating-point rounding errors.
• Safe Natural Bengali Voice Ledger: Speaks colloquial Bengali with conversational number words and zero-amount hallucination guards.
• 100% Offline-First Architecture: Record transactions, manage accounts, and view cash flow reports without internet connectivity.
• Cloud Sync & Safe Restore Preview: Securely back up to Google Cloud Firestore with a pre-restore inspection modal featuring Smart Merge and Full Replace options.
• Commercial Retail Reports: 11 core financial metrics with Saturday-aligned weekly cycles and daily cash drawer reconciliation.
• 1-Tap WhatsApp & SMS Reminders: Send polite, culturally tailored Bengali debt reminders with bKash and Nagad payment details.
• First-Run Merchant Onboarding: Quick setup wizard to configure shop branding and mobile financial services in seconds.
```

---

## 🚀 Comprehensive Changelog (v2.0.0)

### 1. 💰 Financial Core & Minor Currency Units
- **`Money.kt` Math Engine**:
  - Implemented 64-bit integer (`Long`) arithmetic denominated in **Paisa** ($1\text{ BDT} = 100\text{ Paisa}$).
  - Fully immune to IEEE 754 floating-point inaccuracies.
  - Guaranteed immutable non-breaking space currency formatting (`৳\u00A0১,২৫০`).
  - Implemented Bengali Unicode numeral converters (`০-৯` $\leftrightarrow$ `0-9`).

### 2. 🗄️ Database Architecture (Room SQLite Version 5)
- **Non-Destructive Migration (`MIGRATION_4_5`)**:
  - Safely migrated existing v4 tables (`Customers` and `LedgerEntries`) without dropping tables or losing a single transaction record.
  - Automatically converted existing historical `Double` balances into `Long` Paisa via `ROUND(balance * 100)`.
  - Generated persistent 32-character hexadecimal UUIDs for all historical and newly created customers and ledger entries.
- **Relational Integrity**:
  - Replaced `CASCADE` deletes with `ForeignKey.RESTRICT`, preventing accidental ledger or customer orphaned deletions.
  - Added soft-deletion tombstones (`isDeleted`) for two-way cloud reconciliation.
  - Added indexes on `customerId`, `customerUuid`, `uuid`, `timestamp`, and `isDeleted` for sub-millisecond queries.

### 3. 🔐 Modern Google Authentication & Identity
- **AndroidX Credential Manager**:
  - Upgraded Google Sign-In to use `androidx.credentials` and `GetSignInWithGoogleOption` with raw nonce security.
  - Configured Web Client ID (`704564450702-d0ns15b9aipnslup9cvgos3qvp15chmq.apps.googleusercontent.com`) dynamically via Android string resources.
  - Implemented recursive `ContextWrapper` unwrapping to reliably extract `ComponentActivity` for dialogs and sheets.
- **First-Class Guest Mode**:
  - The application is 100% operational offline without requiring any account login. Sign-in is strictly optional for Google Cloud backup.

### 4. ☁️ Chunked Firestore Sync & Safe Restore Preview
- **Safe Batch Chunking (400 items)**:
  - Eliminated Firestore's 500-write quota crash by batching all document mutations into chunks of 400.
- **Two-Phase Restore Inspection**:
  - `CloudRestorePreviewDialog`: Inspects cloud metadata prior to writing local SQLite data, showing local vs. cloud counts.
  - **Smart Merge Mode**: Non-destructive UUID reconciliation preserving local offline entries.
  - **Full Replace Mode**: Atomic wipe and restoration for new device migration.

### 5. 🎙️ Natural Bengali NLP Voice Parsing
- **Grammar & Dialect Flexibility**:
  - Understands everyday phrases: *"রহিম ৪৫০ বাকী"*, *"করিম ভাই ২০০ টাকা জমা"*, *"সুমন পাঁচশো পরিশোধ"*, *"নগদ বিক্রি ১২০০ টাকা"*.
  - Converts spoken number words (*এক হাজার*, *পাঁচশো*, *দেড় হাজার*, *আড়াই হাজার*) and digits into exact amounts.
  - Strips Bengali grammatical case markers (`ের`, `কে`, `রে`, `এর`).
- **Zero-Amount Hallucination Guard**:
  - Flags `isAmountMissing = true` when an amount is omitted, prompting the merchant with an amber warning banner and quick amount chips.

### 6. 📊 11-Metric Financial Dashboard & Saturday Retail Cycle
- **Commercial Retail Week**:
  - Tailored start-of-week boundaries to **Saturday midnight** in Asia/Dhaka time (`DateRangeHelper.kt`), matching the traditional Bangladeshi retail week.
- **Reports Dashboard (`ReportsScreen.kt`)**:
  1. Today's Repayments (*আজকের আদায়*)
  2. Today's Credit Sales (*আজকের বাকি বিক্রি*)
  3. Today's Cash Sales (*আজকের নগদ বিক্রি*)
  4. Net Daily Balance Change (*বাকি বৃদ্ধি / হ্রাস*)
  5. Saturday-aligned Weekly Credit Sales (*সাপ্তাহিক বাকি*)
  6. Saturday-aligned Weekly Repayments (*সাপ্তাহিক আদায়*)
  7. Weekly Recovery Rate % (*আদায় হার %*)
  8. Monthly Credit Sales & Repayments (*মাসিক হিসাব*)
  9. Total Outstanding Baki (*সর্বমোট বাকি পাওনা*)
  10. Urgent Overdue Accounts (*জরুরি তাগাদা তালিকা*)
  11. Oldest Uncollected Due Age in days (*সর্বোচ্চ বাকি বয়স*)
- **Cash Drawer Reconciliation**:
  - Calculates physical till balance:
    $$\text{Cash in Drawer} = \text{Repayments} + \text{Cash Sales}$$

### 7. 📲 Digital Receipts & 1-Tap Reminders
- **WhatsApp Direct Protocol**:
  - Normalizes phone numbers to standard format (`88017...`).
  - Pre-fills culturally respectful Bengali reminders with shop name, owner name, debt balance, and bKash/Nagad payment info.
  - Native SMS and Chooser fallback for offline customers.

### 8. 📚 Technical Documentation Suite
- `docs/ARCHITECTURE.md`: Modern Android architecture, MVI/MVVM, and reactive StateFlow patterns.
- `docs/DATABASE.md`: Room schema v5, minor units, and `MIGRATION_4_5` implementation details.
- `docs/AUTHENTICATION.md`: Credential Manager lifecycle, Google Auth, and Guest mode.
- `docs/SYNCHRONIZATION.md`: Chunked batch writes, pre-restore inspection, and conflict resolution.
- `docs/DESIGN_SYSTEM.md`: "Modern Bengali Ledger" aesthetic tokens, color palettes, and retail ergonomics.
- `docs/QA_AND_TESTING.md`: Test strategy, Robolectric guidelines, and validation commands.

---

## 🧪 Verification & Quality Assurance Summary

- **Unit & Robolectric Test Suite**: **100% Passing (42 of 42 tests)**
  - `BanglaVoiceParserTest`: 9/9 passed
  - `DatabaseMigrationTest`: 1/1 passed
  - `ExampleRobolectricTest`: 23/23 passed
  - `ExampleUnitTest`: 6/6 passed
  - `MoneyTest`: 3/3 passed
- **Debug APK Build**:
  - Generated at `app/build/outputs/apk/debug/app-debug.apk` (28.5 MB)
