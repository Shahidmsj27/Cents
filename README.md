# Cents

Cents is a **100 % offline** Android expense tracker that reads transactional SMS, parses and categorises spending locally, and provides a Compose dashboard for financial insights. **No server, no analytics, no network permission.**

## Features

- **SMS-based tracking** — scans inbox messages after explicit `READ_SMS` + `RECEIVE_SMS` permission. Each SMS is SHA-256 hashed to prevent duplicates; raw body is discarded by default.
- **Deterministic parsing** — normalises message text, detects debit/credit/bill/reversal, extracts amount, balance, account hint, and merchant via regex rules. No ML, no data leaves the device.
- **Spam / noise rejection** — blocks link-containing messages, lottery/prize spam, 10-digit senders without bank keywords, EPFO/PF notices, generic credit-card notices, and OTP-only messages.
- **Category breakdown** — transparent keyword rules (Food, Transport, Bills, Shopping, etc.) with progress bars showing spending share. Tap a category to drill into filtered transactions.
- **Custom tagging** — tap any transaction to re-categorise it. Optionally create a persistent rule that applies to all past and future transactions from that merchant or sender.
- **Filter by type** — tap **Spent** or **Received** cards to toggle outflows (Debit + Bill) or inflows (Credit + Reversal).
- **Sort transactions** — sort by newest/oldest first, or highest/lowest amount.
- **Date range filters** — Daily, Weekly, Monthly, Yearly, or **Custom** date range via `DatePicker`. Sync any period individually.
- **Theme engine** — choose from **System**, **Light**, **Dark**, **Monochrome**, **Forest** (green earth), or **Ocean** (blue). Persisted across restarts.
- **Dark mode** — follows system or forced theme, with full Material 3 dark colour scheme.
- **Reversal support** — refunds and reversals shown as positive (green) entries in the Received view.

## Privacy

- No internet permission in `AndroidManifest.xml`.
- No analytics, crash reporting, or telemetry.
- All data stays in a local Room SQLite database.
- Raw SMS text is stored only if `keepRawSms` is enabled (off by default).

## Tech stack

| Layer        | Library                          |
|--------------|----------------------------------|
| Language     | Kotlin 2.0.21                    |
| UI           | Jetpack Compose + Material 3     |
| Persistence  | Room 2.6.1 (SQLite)              |
| Build        | AGP 8.7.3, Gradle 8.13, KSP     |
| Min / Target | SDK 23 / 35                      |

## Architecture

```
SmsScanner / SmsReceiver
    └─ TransactionRepository.processSms()
            ├─ TransactionParser.parse()
            ├─ CategoryRuleDao.getAllRules()  → user overrides
            ├─ SmsHash.create()              → dedup
            └─ TransactionDao.insert()

MainViewModel
    └─ combine(transactions, scanState, dateWindow,
               selectedCategory, selectedTypeFilter, sortOrder)
         └─ MainUiState (filtered, sorted)

MainActivity
    ├─ SettingsDialog        → theme picker (6 themes)
    ├─ DateFilterCard        → preset + custom range
    ├─ SummaryCards          → Spent / Received (clickable)
    ├─ CategoryBreakdown     → tap to drill down
    ├─ TransactionRow        → tap to tag
    └─ TagDialog             → recategorize + persist rule
```

## Usage

1. Grant SMS permission when prompted.
2. Select a date range (Daily / Weekly / Monthly / Yearly / Custom).
3. Tap **Sync** — old entries for that period are replaced with fresh SMS data.
4. Browse the category breakdown and transaction list.
5. Tap **Spent** or **Received** cards to filter by type.
6. Tap **Sort: Newest first** to change transaction order.
7. Tap the gear icon (⚙) next to the title to change theme.
8. Tap any transaction to re-categorise it and optionally create a persistent rule.

## Build & install

```bash
git clone https://github.com/YOUR_USER/cents.git
cd cents

# Create local.properties with your SDK path:
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows equivalent for `local.properties`:
```properties
sdk.dir=C:\\Users\\YOU\\AppData\\Local\\Android\\Sdk
```

## Upload to GitHub (recommended procedure)

```bash
# 1. Create a repository on github.com (empty, no README, no .gitignore, no licence)
#    Name it e.g. "cents" or "cents-expense-tracker".

# 2. Initialise git locally
git init
git add .
git commit -m "Initial commit: Cents offline expense tracker"

# 3. Link your remote and push
git remote add origin https://github.com/YOUR_USER/cents.git
git branch -M main
git push -u origin main
```

**Or use GitHub CLI** (`gh`):
```bash
gh repo create cents --public --source=. --remote=origin --push
```

### Before pushing, check

- `.gitignore` — the Android Studio template already ignores `build/`, `.gradle/`, `local.properties`, and `*.iml`. Verify the file exists.
- `local.properties` — contains your local SDK path; **already gitignored** so it won't be pushed.
- `gradle.properties` — ensure no personal paths or credentials.
- `app/google-services.json` — not used; Cents has no Firebase or Google services.

### Licence

No licence file is included by default. If you want others to use, modify, and distribute the code, add a licence (MIT is common for Android projects):
```bash
curl -L https://www.tldrlegal.com/download/MIT -o LICENSE
```

## Disclaimers

- Cents is provided **as-is** without warranty. The parser may miss or misclassify transactions.
- Always verify the parsed amounts and categories against your bank statements.
- The app reads SMS via `content://sms/inbox`. Only transactional messages are parsed and stored; raw body text is discarded by default.
