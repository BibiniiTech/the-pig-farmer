# Walkthrough: Enhanced Notifications & Weight Update Reminders

I have improved the notification systems for both the Android and Web applications, adding reminders for animal weight updates and low feed stock alerts.

## Changes Made

### 1. Unified Notification Logic
Both platforms now mirror each other's notification logic, ensuring a consistent user experience.

### 2. Android App Enhancements
- **Data Model**: Updated `Pig.kt` to include `lastWeightDate`.
- **Logic**:
    - `HerdViewModel` now automatically records the `lastWeightDate` whenever a "Weight Check" health record is added.
    - `NotificationWorker` has been expanded to run twice daily and check for:
        - **Task Deadlines**: -2 to +2 day window.
        - **Weight Updates**: Pigs not weighed in over 30 days.
        - **Low Stock**: Feed items below their minimum threshold.
- **Multilingual Support**: Added notification strings for English, Spanish, and Swahili in `TranslationUtils.kt`.

### 3. Web App Enhancements
- **Translations**: Added a new `Notifications` section in `en.json` with keys for weight and stock reminders.
- **Weight Tracking**: Updated `WeightCheckerPage.tsx` and `TaskCompletionModal.tsx` to save the `lastWeightDate` to Firestore using the `dd/MM/yyyy` format.
- **Real-time Notifications**:
    - `DesktopHeader.tsx` now listens to `pigs` and `feed_inventory` collections.
    - The notification bell badge now shows the sum of all active tasks, weight alerts, and stock alerts.
- **Categorized Drawer**: The notification drawer in `DashboardPage.tsx` now organizes alerts into:
    - **Upcoming Activities** (Tasks)
    - **Weight Update Needed** (Amber alerts)
    - **Low Stock Alert** (Rose alerts)

## Verification Results

### Android
- `NotificationWorker` logic confirmed to calculate day differences correctly using `DateUtils.parseInternal`.
- Weight reminders trigger if `today - lastWeightDate >= 30 days`.

### Web
- Notification Drawer dynamically updates based on Firestore state.
- Clicking Weight/Stock alerts navigates the user directly to the relevant dashboard section.

> [!TIP]
> To test the weight reminder immediately, you can manually set a pig's `lastWeightDate` to a date 31 days ago in the Firestore console.
