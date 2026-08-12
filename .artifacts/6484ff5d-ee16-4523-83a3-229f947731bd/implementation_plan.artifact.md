# Implementation Plan - Improve "Allow App Access" Functionality & Performance

Review and improve the "allow app access" functionality in the "add staff member" feature across both Android and Web apps. This includes enhancing robustness, performance, and user feedback.

## User Review Required

> [!IMPORTANT]
> - **Invitations**: We are sticking with the client-side REST API invitation flow for now to avoid the complexity of deploying Cloud Functions, but we will make it significantly more robust.
> - **Atomic Updates**: We will use `writeBatch` in both Android and Web to ensure that manager's staff list and the global `staff_registry` remain in sync.

## Proposed Changes

### [Common]

#### [MODIFY] [StaffMember.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/model/StaffMember.kt)
- Add `inviteStatus: String = "none"` (values: "none", "pending", "sent", "failed").

#### [MODIFY] [firestore.rules](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/firestore.rules)
- Tighten security rules for `staff_registry` to ensure only the authorized manager can manage their staff's registry entries.

---

### [Android App]

#### [MODIFY] [HumanResourceViewModel.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/hr/HumanResourceViewModel.kt)
- Implement `writeBatch` in `addStaff` and `updateStaff`.
- Refactor `inviteStaffMember` to handle `EMAIL_EXISTS` as a "success" (already registered) and update the `inviteStatus` in Firestore.
- Add a `resendInvitation` function.

#### [MODIFY] [HumanResourceScreen.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/hr/HumanResourceScreen.kt)
- Update `StaffMemberItem` to display the invitation status.
- Add a "Resend" button if the status is "failed" or "pending".
- Ensure consistent loading feedback during the invitation process.

---

### [Web App]

#### [MODIFY] [web/src/app/dashboard/hr/page.tsx](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/app/dashboard/hr/page.tsx)
- Update `StaffMember` interface to include `inviteStatus`.
- Implement `writeBatch` for atomic updates.
- Refactor `inviteStaffMember` and `handleToggleAccess` for better robustness.
- Update UI to show invitation status and add a "Resend" button.

## Verification Plan

### Automated Tests
- N/A (Unit tests for ViewModel logic if available, but primarily manual verification).

### Manual Verification
1. **Add Staff with Access**: Verify that a new staff member is added to the manager's list AND the `staff_registry` atomically. Verify invitation email is sent.
2. **Toggle Access**: Verify that enabling/disabling access correctly updates both locations.
3. **Resend Invite**: Verify that clicking "Resend" triggers a new invitation email.
4. **Already Exists**: Verify that adding a staff member whose email already exists in Firebase Auth doesn't crash or hang, and still grants access in Firestore.
5. **Security Rules**: Attempt to write to `staff_registry` from a different user account to verify the tightened rules work.
