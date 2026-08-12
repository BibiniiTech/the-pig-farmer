# Implementation Plan: Improved Notification System & Weight Update Reminders

Enhance the notification system on both Android and Web platforms to include reminders for updating animal weights and low stock alerts, while improving the existing task notification features.

## User Review Required

> [!IMPORTANT]
> The weight update reminder will be set to **30 days** since the last recorded weight or birth date.
> Low stock alerts will trigger when an item's quantity falls below its `minThreshold`.

- The Android background worker (`NotificationWorker`) will now run twice daily to check for all three types of notifications: Tasks, Weight, and Stock.
- The Web app will update its notification bell/drawer to show these new alert types in real-time.

## Proposed Changes

### [Android] Data Models & Translations

#### [MODIFY] [Pig.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/model/Pig.kt)
- Add `lastWeightDate: String = ""` to the data class.

#### [MODIFY] [TranslationUtils.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/utils/TranslationUtils.kt)
- Add strings for `weight_update_reminder`, `weight_update_title`, `low_stock_reminder`, and `low_stock_title` across all languages.

### [Android] Logic Updates

#### [MODIFY] [HerdViewModel.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/herd/HerdViewModel.kt)
- Update `handleSpecializedActivityLogic` to set `lastWeightDate` when a "Weight Check" health record is added.

#### [MODIFY] [NotificationWorker.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/utils/NotificationWorker.kt)
- Expand `doWork()` to fetch pigs and check `lastWeightDate`.
- Expand `doWork()` to fetch feed inventory and check `quantity` vs `minThreshold`.
- Create specific notification IDs for weight and stock alerts to prevent overwriting task notifications.

---

### [Web] Data Models & Translations

#### [MODIFY] [DashboardPage.tsx](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/app/dashboard/page.tsx) (and others)
- Update `Pig` interface to include `lastWeightDate?: string`.

#### [MODIFY] [en.json](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/messages/en.json) (and others)
- Add `weightUpdateReminder` and `lowStockReminder` keys.

### [Web] Logic Updates

#### [MODIFY] [page.tsx](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/app/dashboard/weight/page.tsx)
- Update the `updateDoc` call to include `lastWeightDate` when saving weight.

#### [MODIFY] [DesktopHeader.tsx](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/components/layouts/DesktopHeader.tsx)
- Add listeners for `pigs` and `feed_inventory` to calculate the total notification count.
- Implement logic to identify pigs needing weight updates (30+ days) and low stock items.

#### [MODIFY] [DashboardPage.tsx](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/app/dashboard/page.tsx)
- Update the Notification Drawer UI to categorize alerts into "Tasks", "Weight Reminders", and "Inventory Alerts".

## Verification Plan

### Automated Tests
- N/A (Manual verification on device/browser preferred for notification UI).

### Manual Verification
1.  **Weight Reminder**: Set a pig's `lastWeightDate` to 31 days ago in Firestore. Verify notification appears on Android and Web.
2.  **Low Stock**: Set a feed item's `quantity` below `minThreshold`. Verify notification appears.
3.  **Task Timeline**: Ensure existing task notifications (-2 to +2 days) still work correctly.
4.  **Action Link**: Verify that clicking a weight notification takes the user to the Weight Checker screen.
