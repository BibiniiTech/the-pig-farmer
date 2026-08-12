# Walkthrough: Harmonized Data Sharing

I have successfully harmonized the data models between the Android and Web apps to ensure seamless data sharing and prevent data loss.

## Changes Made

### Android App
- **User Profile Parity**: Updated the `UserProfile` model in `AuthViewModel.kt` to include the `UserSettings` object. This ensures that app settings (weaning days, currency, etc.) are correctly read and written by both platforms.
- **Enhanced Feed Ingredients**: Updated `FeedRepository.kt` to correctly parse all nutritional fields (fat, sodium, amino acids, etc.) from the default JSON, matching the Web app's detailed profile.
- **Safe Update Patterns**: Refactored `HerdRepository.kt` and `AuthViewModel.kt` to use `SetOptions.merge()` when updating documents. This prevents Android from accidentally wiping out web-only fields.

### Web App
- **Centralized Type Definitions**: Created [types.ts](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/lib/types.ts) to serve as the single source of truth for all data models.
- **Expanded Pig Model**: The web app now recognizes all health and status fields used by Android (castration, teeth clipping, tail docking, weaned status, iron injections, etc.).
- **Consistent Updating**: Updated `setDoc` calls across all major pages (Herd, Activities, Task Completion) to use `{ merge: true }`, ensuring that updates from the web don't overwrite unrelated data.

## Verification Results

### Automated Tests
- [x] Android: `gradle :app:assembleDebug` completed successfully.
- [x] Web: Verified type consistency manually across all updated components.

### Manual Verification Required
- [ ] **Data Sync**: Add a pig on Android with specialized health markers and verify they are preserved after an edit on the Web dashboard.
- [ ] **Settings Sync**: Verify that changing the "Weaning Days" or "Currency" on one platform updates the other.

> [!TIP]
> Both apps now use `SetOptions.merge()` for profile and pig updates. This is the safest way to ensure that future features added to only one platform won't break data for the other.
