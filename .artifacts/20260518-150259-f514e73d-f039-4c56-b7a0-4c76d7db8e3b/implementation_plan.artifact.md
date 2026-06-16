# PDF Generator Refactor Plan

Refactor the `PdfGenerator.kt` to simplify the code, reduce duplication, and improve translation support while maintaining the current design and contents.

## Proposed Changes

### [PdfGenerator.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/smartswine/util/PdfGenerator.kt)

#### Core Infrastructure
- Add `runPdfTask` to handle Coroutines, File I/O, and common PDF setup (iText boilerplate, headers, watermarks).
- Add DSL-style extension functions for:
    - `tr(key, lang, args)`: Short-hand for `Translator.getString`.
    - `Document.addSection(title)`: Consistent section headers.
    - `Table.addHeaderRow(headers, lang)`: Auto-translated and styled header cells.
    - `resolvePigIds(description, allPigs)`: Centralized logic for replacing raw IDs with Tag Numbers in text.

#### Refactoring Report Functions
Each of the following will be updated to use the new `runPdfTask` and DSL helpers:
- `generateFeedRequirementPdf`
- `generateFormulationPdf`
- `generateFinancialReportPdf`
- `generateHerdReportPdf`
- `generateFeedReportPdf`
- `generateHRReportPdf`

## Verification Plan

### Automated Verification
- Run `analyze_file` to check for any syntax errors or missing imports.
- Use `grep` to ensure all calls to `Translator.getString` have been simplified or consistently handled.

### Manual Verification
- I will perform a line-by-line comparison between the old and new code to ensure no data points (like running balances, totals, or nutritional analysis status colors) were omitted or changed in logic.
- Verify that the `lang` parameter is correctly propagated to all UI elements.
