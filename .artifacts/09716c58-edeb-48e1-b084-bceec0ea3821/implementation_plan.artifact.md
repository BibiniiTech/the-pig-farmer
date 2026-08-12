# Phase 3 Implementation Plan: UI Decomposition & Security Refinement

This plan details the implementation of Phase 3, focusing on simplifying the core UI structures and centralizing security logic for better maintainability.

## Proposed Changes

### 1. Security Logic Centralization

Currently, security checks are performed directly within `MainActivity.kt` using `SecurityUtils`. We will encapsulate this into a `SecurityManager` class to provide a cleaner API and separate concerns.

#### [NEW] [SecurityManager.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/data/SecurityManager.kt)
- Create a `SecurityManager` class (singleton or provided via ViewModel/DI).
- Methods: `checkSecurity()`, `isRooted()`, `isSignatureValid()`, `isHookingFrameworkActive()`.
- Return a sealed class `SecurityStatus` (e.g., `Safe`, `Violation(message: String)`, `Warning(message: String)`).

### 2. MainActivity Refactoring

`MainActivity.kt` is currently over 1000 lines long, with a massive `setContent` block and internal `AppDrawer` implementation.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/MainActivity.kt)
- Extract the core application UI into a new Composable: `SmartSwineApp`.
- Extract `AppDrawer` into its own file: `C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/navigation/AppDrawer.kt`.
- Extract `NavHost` configuration into `C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/navigation/AppNavigation.kt`.
- Simplify `MainActivity` to only handle `onCreate`, basic initialization, and top-level security monitoring via `SecurityManager`.

### 3. DashboardScreen Decomposition

`DashboardScreen.kt` is quite large and handles many UI sections. We will extract sub-components into separate files.

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/dashboard/DashboardScreen.kt)
- Move `QuoteCard` to a shared UI folder or its own file.
- Move `ManagementCategory` and its rendering logic to `C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/dashboard/components/ManagementGrid.kt`.
- Extract the "Upcoming Activities" (the notification sheet content) into `C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/dashboard/components/UpcomingActivitiesList.kt`.

## Verification Plan

### Automated Tests
- Unit tests for `SecurityManager` logic (mocking `Context` and `PackageManager` where possible).
- Verify navigation state remains consistent after decomposition.

### Manual Verification
- Deploy to an emulator/device and verify all navigation routes work correctly.
- Verify the `AppDrawer` looks and behaves as before.
- Verify the `DashboardScreen` sections load and interact correctly.
- Trigger a simulated security violation to ensure the `SecurityManager` correctly handles and displays it.
