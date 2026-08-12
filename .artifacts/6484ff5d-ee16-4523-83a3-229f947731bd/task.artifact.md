# Task: Improve "Allow App Access" Functionality & Performance

- `[x]` **Phase 1: Common Changes**
    - `[x]` Add `inviteStatus` to `StaffMember` model (Android)
    - `[x]` Update `firestore.rules` for registry security
- `[x]` **Phase 2: Android Implementation**
    - `[x]` Update `HumanResourceViewModel.kt` (Batching, robust invites, resend logic)
    - `[x]` Update `HumanResourceScreen.kt` (UI status, Resend button)
- `[x]` **Phase 3: Web Implementation**
    - `[x]` Update `StaffMember` interface in `web/src/app/dashboard/hr/page.tsx`
    - `[x]` Update `HumanResourcesPage` component logic and UI
- `[x]` **Phase 4: Verification**
    - `[x]` Verify atomic updates (Android)
    - `[x]` Verify atomic updates (Web)
    - `[x]` Test invitation statuses (sent, failed, resend)
    - `[x]` Verify Firestore security rules
