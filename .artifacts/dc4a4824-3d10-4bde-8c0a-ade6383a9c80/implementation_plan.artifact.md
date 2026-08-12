# Subscription Price Update Plan

The goal is to update all subscription price references in the SmartSwine Android and Web applications to reflect the new pricing: **$2 per month** and **$10 per annum**.

## User Review Required

> [!IMPORTANT]
> This change updates hardcoded display prices and fallback strings. The actual transaction amounts in Google Play and Paystack must be updated separately by the user, as stated in the request.

## Proposed Changes

### Android App

#### [MODIFY] [PaywallScreen.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/ui/settings/PaywallScreen.kt)
- Update the `PaywallScreenPreview` prices from $4.99/$49.99 to $2.00/$10.00.

#### [MODIFY] [TranslationUtils.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/utils/TranslationUtils.kt)
- Update all occurrences of `default_monthly_price` to show "$2" or equivalent in all translated languages.
- Update all occurrences of `default_annual_price` to show "$10" or equivalent in all translated languages.

#### [MODIFY] [app_strings.csv](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app_strings.csv) & [app_strings_all.csv](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app_strings_all.csv)
- Update the master string files to reflect the new prices.

---

### Web App

#### [MODIFY] [page.tsx](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/web/src/app/dashboard/billing/page.tsx)
- Update `paystackConfig` amount from 500/4500 cents to 200/1000 cents.
- Update display prices from $5.00/$45.00 to $2.00/$10.00.

---

### Project Root

#### [MODIFY] [quotes.txt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/quotes.txt) (If applicable)
- I will check if `quotes.txt` or other text files contain price references.

## Verification Plan

### Automated Tests
- N/A (UI display change)

### Manual Verification
- Render Compose Preview for `PaywallScreen`.
- Verify the Web App billing page UI.
- Verify that Paystack button passes the correct cents (200 for monthly, 1000 for annual).
