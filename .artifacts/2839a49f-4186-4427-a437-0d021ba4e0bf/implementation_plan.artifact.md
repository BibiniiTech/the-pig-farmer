# Bug Fix: Feed Requirement PDF Export Zeros

This plan addresses the issue where the "Feed Requirements" PDF report shows `0.0` for the total weights when the calculation is for more than one day.

## User Review Required

> [!IMPORTANT]
> The current PDF layout for Feed Requirements uses a 3-column table: **Growth Stage**, **Daily (kg)**, and **[X] Days (kg)**. I will ensure the "Total" row correctly populates these columns by looking up the correct keys from the calculation results.

## Proposed Changes

### [app]

#### [MODIFY] [PdfGenerator.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/util/PdfGenerator.kt)

- Update `generateFeedRequirementPdf` to correctly retrieve the total daily requirement regardless of whether the calculation is for a single day or multiple days.
- Support both keys `"Total Daily Requirement"` (used for 1 day) and `"Daily Total"` (used for >1 day) from `FeedViewModel`.

```kotlin
// In generateFeedRequirementPdf
val totalDaily = requirements["Total Daily Requirement"] ?: requirements["Daily Total"] ?: 0.0
```

## Verification Plan

### Automated Tests
- I will verify the logic in `PdfGenerator.kt` by checking the key retrieval.

### Manual Verification
- Deploy the app.
- Go to **Feed -> Feed Calculator**.
- Enter some animal counts.
- Select **7 days**.
- Click **Generate Report**.
- Click **Export PDF**.
- Verify that the "Total" row in the PDF contains the correct daily and 7-day totals instead of `0.0`.
