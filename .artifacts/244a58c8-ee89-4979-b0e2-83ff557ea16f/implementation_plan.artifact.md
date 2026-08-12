# Harmonize Web and Android Data Sharing

Ensure that both the SmartSwine Android app and the Next.js Web app share the same Firebase data consistently, preventing data loss and ensuring feature parity.

## User Review Required

> [!IMPORTANT]
> **Data Model Consolidation**: I will be adding several nutritional fields to the Android `FeedIngredient` model and several health-related fields to the Web `Pig` model. Please confirm if there are any specific fields you'd like to exclude or rename.

> [!WARNING]
> **Field Renaming**: I propose renaming `metabolicEnergy` to `metabolizableEnergy` in the Android app to match the Web app and scientific standards. This will require a small migration or using Firestore aliases if data already exists with the old name.

## Proposed Changes

### [Android] Data Models & Repositories

Harmonize Android models with Web models to ensure no data is lost during `set()` operations and that settings are shared.

#### [MODIFY] [Pig.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/model/Pig.kt)
- Ensure all fields used in Web are present.
- Verify Firestore property names for boolean fields (using `is` prefix).

#### [MODIFY] [FeedIngredient.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/model/FeedIngredient.kt)
- Add missing nutritional fields: `sodium`, `chloride`, `potassium`, `sulfur`, `fat`, `cystine`, `threonine`, `tryptophan`, `arginine`, etc.
- Rename or alias `metabolicEnergy` to `metabolizableEnergy`.

#### [MODIFY] [TaskItem.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/model/TaskItem.kt) & [HealthRecord.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/model/HealthRecord.kt)
- Review and ensure parity with Web (though Android is currently more complete).

#### [MODIFY] [AuthViewModel.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/auth/AuthViewModel.kt)
- Update `UserProfile` data class to include the `settings` object.
- Use `update()` or `set(..., SetOptions.merge())` for profile updates to avoid wiping out Web-only fields.

#### [MODIFY] [HerdRepository.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/data/HerdRepository.kt)
- Refactor `updatePig` to use `update()` or `merge` to prevent accidental deletion of Web-specific fields.

---

### [Web] Data Models & Synchronization

Centralize and expand interfaces to match Android's comprehensive models, preventing data loss during updates.

#### [NEW] [types.ts](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/lib/types.ts)
- Create a central file for `Pig`, `UserProfile`, `FeedIngredient`, `FinancialRecord`, `HealthRecord`, and `TaskItem` interfaces.

#### [MODIFY] Web Pages & Components
- Update all files using local interfaces to use the centralized types.
- **Pig Model**: Add `isCastrated`, `isTeethClipped`, `isTailDocked`, `isWeaned`, `ironInjections`, `castrationDate`, `lastBreedingDate`, `lastBoarTag`, `hasFarrowed`.
- **HealthRecord**: Add `medication`, `cost`, `taskId`.
- **TaskItem**: Add `healthRecordIds`.
- Ensure `setDoc` calls in Web use `{ merge: true }` where appropriate.

---

### [Firebase] Configuration & Rules

#### [VERIFY] Environment Variables
- Ensure `web/.env.local` (if it exists or is created) points to project `the-pig-farmer-78728`.
- Confirm `google-services.json` in Android matches.

## Verification Plan

### Automated Tests
- Build both apps to ensure no compilation errors after model changes.
- Run `FeedRepository` unit tests if available.

### Manual Verification
- **Data Parity**: Add a pig in the Android app with health details (castrated, teeth clipped) and verify it shows up in the Web app without losing those fields when edited on the web.
- **Settings Sync**: Change a setting (e.g., weaning days) on the Web app and verify it is reflected in the Android app's `UserProfile`.
- **Ingredients Sync**: Verify that the expanded nutritional profile for feed ingredients is visible and editable in both apps.
