# Tasks

- [x] Create implementation plan for PDF Generator refactor
- [x] Implement core refactoring infrastructure in `PdfGenerator.kt`
    - [x] Create `runPdfTask` helper
    - [x] Create DSL-style extension functions (`tr`, `addSection`, `addHeaderRow`, etc.)
- [x] Refactor individual PDF generation functions
    - [x] `generateFeedRequirementPdf`
    - [x] `generateFormulationPdf`
    - [x] `generateFinancialReportPdf`
    - [x] `generateHerdReportPdf`
    - [x] `generateFeedReportPdf`
    - [x] `generateHRReportPdf`
- [x] Verify changes
    - [x] Run `analyze_file` on `PdfGenerator.kt`
    - [x] Cross-check logic with original implementation
