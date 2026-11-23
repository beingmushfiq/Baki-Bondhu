# 🚀 Baki Bondhu (বাকী বন্ধু) — Release v2.2.3 (Stable)

## Release Title
**v2.2.3 (Stable Release) — The Complete Voice-First Retail Ledger, Zero-Drift Minor-Unit Accounting & Cloud Recovery Engine**
*(বাংলা: সংস্করণ ২.২.৩ (স্ট্যাবল রিলিজ) — প্রাকৃতিক বাংলা ভয়েস খাতা, পয়সা-ভিত্তিক নির্ভুল হিসাব, ক্লাউড ব্যাকআপ ও রিস্টোর ইঞ্জিন)*

---

## 📌 Executive Summary
**Baki Bondhu v2.2.3** represents the definitive stable milestone for Bangladesh's premier offline-first, voice-driven retail ledger and cash drawer application. Engineered specifically for micro, small, and medium retail merchants (মুদি দোকান, ফার্মেসি, চা-স্টল, কাপড়ের দোকান, ও পাইকারি আড়ত), this release combines lightning-fast colloquial Bengali speech entry, zero-drift minor unit (Paisa) arithmetic, seamless Singapore Firebase Realtime Database cloud backup, robust in-app updates with cross-host redirect support, and complete foreign-key safe ledger recovery.

---

## 🇧🇩 রিলিজ হাইলাইটস (বাংলা)

### ১. ☁️ ক্লাউড ব্যাকআপ ও নিরাপদ রিস্টোর ইঞ্জিন (Foreign Key Code 1811 Fix)
- **রিস্টোর ফিক্স:** ব্যাকআপ রিস্টোরের সময় SQLite Foreign Key ত্রুটি (`code 1811`) সম্পূর্ণ সমাধান করা হয়েছে। লোকাল কাস্টমারদের ইন-প্লেস আপডেট এবং ট্রানজেকশনের ফরেন কি নিখুঁতভাবে ম্যাপিং করার মাধ্যমে খাতার ধারাবাহিকতা ১০০% নিশ্চিত করা হয়েছে।
- **সিঙ্গাপুর ফায়ারবেস আরটিডিবি (RTDB):** প্রজেক্ট `gen-lang-client-0530887953`-এর জন্য সিঙ্গাপুর রিজিয়ন (`asia-southeast1`) ফায়ারবেস রিয়েলটাইম ডাটাবেজের সাথে সরাসরি ইন্টিগ্রেশন। মাত্র ০.৫ সেকেন্ডের মধ্যে সমগ্র খাতা, কাস্টমার এবং সেটিংস নিরাপদভাবে ক্লাউডে সিঙ্ক হয়।
- **স্মার্ট মার্জ ও ফুল ওভাররাইট চয়েস:** নতুন ফোনে খাতা ফিরিয়ে আনা কিংবা ফোনে থাকা হিসাবের সাথে ক্লাউড হিসাব মার্জ করার স্পষ্ট ডায়ালগ ও প্রিভিউ সুবিধা।

### ২. 🎙️ খাঁটি বাংলা ভয়েস খাতা (Bengali Voice Ledger)
- **কথ্য ভাষার নিখুঁত পার্সিং:** “দেড় হাজার”, “আড়াইশো”, “পাঁচশো”, “এক হাজার পঞ্চাশ” কিংবা যেকোনো আঞ্চলিক কথ্য বাংলা সংখ্যা তাৎক্ষণিকভাবে শনাক্ত করে।
- **ভুল এন্ট্রি প্রতিরোধ গার্ড:** ক্রেতার নাম বলে টাকার অঙ্ক না বললে আন্দাজে শূন্য লিখে না—বরং সতর্কবার্তা দিয়ে পুনরায় স্পষ্ট করে বলতে অনুরোধ করে।
- **শব্দ তরঙ্গের ভিজ্যুয়ালাইজার:** মাইক অন থাকলে লাইভ সাউন্ডওয়েভ অ্যানিমেশন এবং সফল এন্ট্রিতে পিওএস চিম সাউন্ড।

### ৩. 🎯 ১ পয়সাও গরমিল নেই (Zero-Drift Minor Unit Accounting)
- **পূর্ণসংখ্যা পয়সা ম্যাথ:** কম্পিউটারের ফ্লোটিং-পয়েন্ট দশমিক ভুলের ঝুঁকি চিরতরে দূর করতে প্রতিটি লেনদেন ও ব্যালেন্স অভ্যন্তরীণভাবে পয়সায় (`Long` integers, ১ টাকা = ১০০ পয়সা) সংরক্ষিত হয়।
- **ডাবল-এন্ট্রি হিসাব নিশ্চয়তা:** কাস্টমার ব্যালেন্স স্বয়ংক্রিয়ভাবে খাতার লেজার এন্ট্রি থেকে রিকনসাইল করা হয়, ফলে হাজার হাজার লেনদেনের পরেও ১ পয়সাও এদিক-সেদিক হয় না।

### ৪. 🔄 ইন-অ্যাপ অটো আপডেট ইঞ্জিন (OkHttp 302 Redirect Support)
- **প্যাকেজ পার্সিং ফিক্স:** সেটিংস থেকে সরাসরি নতুন সংস্করণ ডাউনলোডের পর অ্যান্ড্রয়েডে *"Problem parsing package"* সমস্যার চিরস্থায়ী সমাধান।
- **GitHub Release S3 Redirect:** গিটহাব থেকে এডাব্লিউএস S3 অবজেক্ট স্টোরেজে রিডাইরেক্ট হওয়া ফাইলগুলোকে সফলভাবে ডাউনলোড করার জন্য `OkHttpClient` (`followRedirects = true`) যুক্ত করা হয়েছে।
- **অ্যান্ড্রয়েড ১৫ সামঞ্জস্য:** আধুনিক অ্যান্ড্রয়েড সিস্টেমের সাথে শতভাগ মানানসই করতে স্ট্যাবল `targetSdk = 35` নির্ধারণ করা হয়েছে।

### ৫. 🛡️ কাস্টমার ম্যানেজমেন্ট, নাম এডিট ও আর্কাইভ জোন
- **কাস্টমার নাম পরিবর্তন:** তালিকা থেকে সরাসরি ক্রেতার নাম এডিট করার সুবিধা, যা পূর্বের সমস্ত ভাউচার ও ট্রানজেকশনে সাথে সাথে আপডেট হয়।
- **সফট-ডিলিট ও আর্কাইভ জোন:** বকেয়া থাকা অবস্থায় অসাবধানতাবশত কাস্টমার ডিলিট রোধে সতর্কবার্তা। ডিলিট করা কাস্টমার সংরক্ষিত থাকে 'আর্কাইভ জোন'-এ, যেখান থেকে ১-ক্লিকে ফিরিয়ে আনা যায়।
- **একই নামের সমাধান চিপস:** একই নামের একাধিক ক্রেতা থাকলে বিভ্রান্তি এড়াতে ১-ট্যাপে নাম পার্থক্যকারী চিপস (+ (২), + (নতুন), + (বাজার)) যুক্ত করার সুবিধা।

### ৬. 📱 ১-ট্যাপ হোয়াটসঅ্যাপ তাগাদা ও ক্যাশ ড্রয়ার ট্র্যাকিং
- **সম্মানজনক বাংলা বার্তা:** বকেয়া টাকার জন্য কাস্টমারের নাম, মোট বাকি এবং আপনার দোকানের বিকাশ ও নগদ নম্বরসহ স্বয়ংক্রিয়ভাবে তৈরি ভদ্র ও মার্জিত তাগাদা মেসেজ।
- **ক্যাশ ড্রয়ার রিকনসিলিয়েশন:** সকালে দোকান খোলার সময় ড্রয়ারের শুরুর ক্যাশ নির্ধারণ এবং দিনশেষে নগদ বিক্রি ও বকেয়া আদায়ের লাইভ হিসাব মিলিয়ে ক্যাশ ড্রয়ার লক/ক্লোজ করার সুবিধা।

---

## 🇬🇧 Release Highlights (English)

### 1. ☁️ Cloud Backup & Robust Recovery Engine (Foreign Key Code 1811 Fix)
- **Restore Architecture Fix:** Completely resolved SQLite `FOREIGN KEY constraint failed (code 1811)` during cloud restore. Existing customers are updated in place to preserve local primary keys and prevent SQLite `REPLACE` from triggering cascading `ON DELETE RESTRICT` lockouts.
- **Singapore RTDB Integration:** Connected directly to the dedicated Singapore region (`asia-southeast1`) Firebase Realtime Database instance. Employs atomic `updateChildren` sync, completing full store backups in sub-second times with robust 8-second diagnostics.
- **Smart Merge vs Full Overwrite:** Offers shopkeepers full control during restore—either merging cloud records safely with local device data or performing a clean, foreign-key ordered overwrite.

### 2. 🎙️ Natural Spoken Bengali Voice Ledger
- **Colloquial Bengali Parser:** Recognizes spoken compound amounts ("দেড় হাজার", "আড়াইশো", "পাঁচশো", numbers in words or digits) with high fidelity.
- **Accidental Zero Protection:** Proactively detects spoken phrases where monetary amounts were omitted and warns the user rather than erroneously logging zero.
- **Interactive Audio Feedback:** Real-time audio waveform visualizer and POS chime feedback upon confirmed entry.

### 3. 🎯 100% Exact Paisa Precision (Zero Rounding Drift)
- **Minor Currency Unit Engine:** All financial operations use 64-bit integer Paisa (`Long`, 1 BDT = 100 Paisa), completely banishing cumulative floating-point calculation drift.
- **Strict Ledger Audit Trail:** Ledger entries act as the single source of truth; customer balances are reconciled deterministically from active ledger rows.

### 4. 🔄 In-App Update Engine (OkHttp 302 Redirect Support)
- **Package Parsing Resolution:** Fixed the package installer failure when updating directly through the in-app settings sheet.
- **Cross-Host Redirect Handling:** Configured `OkHttpClient` (`followRedirects = true`, `followSslRedirects = true`) to handle GitHub release asset downloads redirecting to AWS S3 (`objects.githubusercontent.com`).
- **Android 15 Compatibility:** Standardized on stable `targetSdk = 35`, guaranteeing full backward compatibility from Android 7.0 (Nougat) up to Android 15/16.

### 5. 🛡️ Customer Management, Name Editing & Archive Zone
- **Customer Name Renaming:** Edit customer names on the fly with automatic real-time propagation across all historical transactions.
- **Soft-Delete Safeguards:** Warns if an outstanding balance exists before deletion and safely moves the record to the Archive Zone.
- **1-Click Restoration:** Instantly unarchive customers and transactions with automatic balance adjustments.
- **Disambiguation Chips:** 1-tap suffix chips (+ (2), + (New), + (Bazaar)) eliminate duplicate name confusion for non-technical users.

### 6. 📱 1-Tap WhatsApp Debt Recovery & Cash Till Reconciliation
- **Courteous Payment Reminders:** Pre-formats polite Bengali reminder messages containing the customer's balance and your shop's bKash/Nagad numbers, ready to send via WhatsApp or SMS.
- **Cash Till Reconciliation:** Record morning opening cash, observe live daily takings, and complete the day's cash till audit with zero bookkeeping headaches.

---

## 📊 Technical Specifications

| Parameter | Specification |
| :--- | :--- |
| **Release Version** | `v2.2.3` (Stable) |
| **Version Code** | `25` |
| **Minimum SDK** | Android 7.0 (API 24 - Nougat) |
| **Target SDK** | Android 15 (API 35 - Upside Down Cake / Vanilla Ice Cream) |
| **Compile SDK** | Android 35 |
| **Local Database** | Android Room SQLite v5 (Deterministic Migrations) |
| **Cloud Database** | Firebase Realtime Database (Region: `asia-southeast1` Singapore) |
| **Network Client** | OkHttp 4.12.0 (Automatic 302 Redirect Handling) |
| **APK Package Name** | `com.aistudio.bakibondhu.kxdznp` |
| **APK Binary Size** | ~28.9 MB |
| **Official Repository**| `https://github.com/beingmushfiq/Baki-Bondhu` |
| **Production Website** | `https://bakibondhu.devcenterpoint.com` |
| **Vendor / Sponsor** | DevCenterPoint (`https://devcenterpoint.com`) |

---

## 📦 Release Artifacts & Assets
- **Branded APK:** `bakibondhu-v2.2.3.apk` (Downloadable directly from web & in-app updater)
- **Universal APK:** `bakibondhu-latest.apk`
- **Web Landing Deployment Bundle:** `deploy_bakibondhu.zip` (Includes full SEO/AEO/GEO architecture, robots.txt, sitemap.xml, llms.txt, and .htaccess)
