# Walkthrough: Fixed Feed Requirement PDF Export Zeros

I have fixed the issue where the Feed Requirement PDF export was showing `0.0` for totals when calculations were performed for multiple days.

## Changes Made

### [app]

#### [PdfGenerator.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/util/PdfGenerator.kt)

Modified `generateFeedRequirementPdf` to support both naming conventions used for total feed requirements in the `FeedViewModel`:
- Added support for the `"Daily Total"` key, which is used when the calculation period is greater than 1 day.
- Maintained support for `"Total Daily Requirement"`, which is used for single-day calculations.

```kotlin
// Before
val totalDaily = requirements["Total Daily Requirement"] ?: 0.0

// After
val totalDaily = requirements["Total Daily Requirement"] ?: requirements["Daily Total"] ?: 0.0
```

## Verification Results

### Code Inspection
The `FeedViewModel.kt` uses different keys for the total sum depending on the duration:
- `days == 1`: `"Total Daily Requirement"`
- `days > 1`: `"Daily Total"`

By updating `PdfGenerator.kt` to check for both, the PDF now correctly retrieves the data from the requirements map provided by the ViewModel.

### Manual Verification Recommendation
1. Open the **Feed Calculator**.
2. Run a calculation for **7 days**.
3. Export to PDF and verify the **Total** row correctly shows the daily sum and the 7-day total.
