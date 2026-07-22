# Implementation Plan - Remove Paystack Test Payment Methods

The goal is to transition the web application's Paystack integration from Test Mode to Live Mode by ensuring only live API keys are used and removing any test-related fallbacks.

## User Review Required

> [!IMPORTANT]
> I have identified that the current code in `BillingPage.tsx` allows falling back to a test key if the environment variable `NEXT_PUBLIC_PAYSTACK_PUBLIC_KEY` starts with `pk_`. I will restrict this to only allow keys starting with `pk_live_`.

> [!WARNING]
> Please verify that the following Plan Codes are your **Live** Plan Codes from the Paystack Dashboard. If these are Test Plan Codes, payments will fail in Live Mode.
> - Monthly: `PLN_0fhg14kc86tn8qs`
> - Annual: `PLN_sk44tcyegocprdu`

## Proposed Changes

### Web Application

#### [MODIFY] [BillingPage.tsx](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/app/dashboard/billing/page.tsx)
- Update the `PUBLIC_KEY` resolution logic to strictly require a `pk_live_` prefix for any environment-provided key.
- Maintain the existing live fallback key as a safety measure.
- Ensure any accidental "test" strings or placeholders are ignored.

#### [MODIFY] [.env.local](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/.env.local) (Local reference)
- Update the placeholder for `NEXT_PUBLIC_PAYSTACK_PUBLIC_KEY` to reflect that it should be a live key.

## Verification Plan

### Automated Tests
- I cannot run live payment tests, but I will verify the code logic via inspection.

### Manual Verification
1. Open the Billing page in the web app.
2. Click "Upgrade".
3. Verify that the Paystack modal **does not** show "Test Card" or "Test Bank" options. It should only show real payment methods (Card, Transfer, etc., depending on your Paystack settings).
4. Verify that the "Test Mode" watermark is absent from the checkout modal.
