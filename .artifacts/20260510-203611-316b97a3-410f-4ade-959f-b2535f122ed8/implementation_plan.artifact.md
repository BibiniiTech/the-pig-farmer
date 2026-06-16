# Implementation Plan - Translate Training Page

Translate the Training page and all its contents (tips and videos) into all supported languages (English, French, Chinese, Spanish, Filipino, Vietnamese, Thai, Portuguese, and Hindi).

## User Review Required

- The translations were generated using my internal knowledge. Please verify them if you have specific preferences for any language.

## Proposed Changes

### Localization Utilities

#### [TranslationUtils.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/thepigfarmer/utils/TranslationUtils.kt)

- Add new keys for Training page headers, tips, and video titles to all `getXXTranslations()` methods.
- Keys to add:
    - `training_title`
    - `quick_farming_tips`
    - `video_tutorials`
    - `tip_1_title` to `tip_5_title`
    - `tip_1_content` to `tip_5_content`
    - `video_1_title` to `video_4_title`

### UI Components

#### [TrainingScreen.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/thepigfarmer/ui/training/TrainingScreen.kt)

- Import `stringResource` from `com.example.thepigfarmer.utils`.
- Replace hardcoded strings in `tips` and `videos` lists with `stringResource` calls.
- Replace hardcoded headers ("TRAINING", "Quick Farming Tips", "Video Tutorials") with `stringResource` calls.

---

## Verification Plan

### Automated Tests
- No automated tests are available for translations, but I will run `analyze_file` to ensure no syntax errors are introduced.

### Manual Verification
- I will use `render_compose_preview` for `TrainingScreenPreview` to see how the English version looks (default).
- I will check the code to ensure all hardcoded strings are removed.
- I will use `adb shell input tap` to navigate to the Training page in the running app and change languages to verify (if the device is available and I can navigate).
- Since I cannot easily change the system locale or app language via ADB without knowing the UI details of the settings, I'll rely on code inspection and preview.
