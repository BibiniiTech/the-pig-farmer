# Walkthrough - Improved Staff Access & Invitation Flow

I have improved the "Allow App Access" functionality across the Android and Web applications, focusing on robustness, performance, and transparency.

## Changes Made

### 1. Data Model & Security
- **Model Update**: Added `inviteStatus` field to `StaffMember` (Android) and the equivalent interface (Web) to track invitation progress (`none`, `pending`, `sent`, `failed`).
- **Security Rules**: Tightened Firestore rules for `staff_registry`. Managers can now only create/update registry entries where the `managerUid` matches their own `uid`.
- **String Resources**: Added new localized strings for invitation status (`inviteStatus`, `invite_sent`, `invite_pending`, `invite_failed`, `resend_invite`).

### 2. Android App Enhancements
- **Atomic Operations**: Refactored `HumanResourceViewModel` to use `writeBatch`. Now, adding/updating a staff member and their registry entry happens in a single atomic transaction.
- **Robust Invitations**: The invitation flow now updates the `inviteStatus` in Firestore, providing real-time feedback to the manager.
- **Resend Logic**: Added `resendInvitation` to allow managers to retry failed invitations or resend them if the staff member missed the initial email.
- **UI Feedback**: The `StaffMemberItem` now displays the invitation status and a "Resend" button when applicable.

### 3. Web App Enhancements
- **Atomic Operations**: Implemented `writeBatch` in `HumanResourcesPage` for consistent behavior with the Android app.
- **Improved UX**: The UI now shows the invitation status below the "App Access" checkbox and includes a "Resend" link.
- **Resend Support**: Added `handleResendInvite` to trigger invitations from the web dashboard.

## Verification Results

### Firestore Synchronization
- [x] Verified that adding a staff member with access creates both the staff document and the registry entry atomically.
- [x] Verified that disabling access removes the registry entry and updates the staff document atomically.

### Invitation Workflow
- [x] Verified `inviteStatus` transitions: `pending` -> `sent` (on success) or `failed` (on error).
- [x] Verified that the "Resend" button correctly triggers a new invitation email and updates the status.

### Security
- [x] Verified that the updated Firestore rules prevent unauthorized users from creating entries in the `staff_registry`.

> [!TIP]
> The invitation status helps managers troubleshoot access issues. If a staff member says they haven't received an email, the manager can now check the status and click "Resend" without having to delete and re-add the staff member.
