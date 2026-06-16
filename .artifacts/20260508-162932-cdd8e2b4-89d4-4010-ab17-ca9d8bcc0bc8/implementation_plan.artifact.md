# Fix Offline Loading for Feed Formulator

The Feed Management module fails to load data when offline because the `FeedViewModel` attempts to synchronize default ingredients and requirements sequentially before starting the actual data loading. The synchronization uses `get().await()` and `set().await()` which can throw exceptions or hang when offline, preventing the subsequent data loading calls (which use offline-friendly snapshot listeners) from ever running.

## Proposed Changes

### Feed Management

#### [FeedViewModel.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/thepigfarmer/ui/feed/FeedViewModel.kt)

- Refactor `initializeData()` to call `loadIngredients()` and `loadRequirements()` immediately and independently.
- Move `repository.initializeDefaultIngredients()` and `repository.initializeDefaultRequirements()` into a separate `viewModelScope.launch` block with its own `try-catch`.
- Ensure exceptions during initialization are logged rather than overriding the main error state, allowing the app to function with cached data when offline.
- Improve `_isLoading` state management to handle multiple parallel loading operations more accurately.

```kotlin
    fun initializeData() {
        // Start loading data immediately - these use snapshot listeners and work offline
        loadIngredients()
        loadRequirements()

        // Try to sync/initialize defaults in the background
        viewModelScope.launch {
            try {
                repository.initializeDefaultIngredients()
                repository.initializeDefaultRequirements()
            } catch (e: Exception) {
                // Log and ignore initialization errors when offline
                android.util.Log.w("FeedViewModel", "Default data sync failed (likely offline): ${e.message}")
            }
        }
    }
```

#### [FeedRepository.kt](file:///C:/Users/GuyGuy/AndroidStudioProjects/ThePigFarmer/app/src/main/java/com/example/thepigfarmer/data/FeedRepository.kt)

- (Optional) I will check if I can make `initializeDefaultIngredients` more efficient by checking for collection existence first rather than 50+ individual checks, which is better for both online performance and offline resilience.

## Verification Plan

### Manual Verification
- I will simulate offline mode (though I cannot literally disable network, I can mock a failure or observe the code path).
- Since I cannot easily run the app in an emulator with network control here, I will verify the logic by:
    1.  Analyzing the `FeedViewModel` and `FeedRepository` code to ensure the fix correctly handles exceptions.
    2.  Checking that `loadIngredients()` and `loadRequirements()` are now called before the potentially failing initialization calls.
    3.  Verifying that `_isLoading` and `_error` states are not negatively impacted by initialization failures.

### Automated Tests
- I will check if there are existing unit tests for `FeedViewModel` and consider adding a test case for initialization failure if possible.
- If no unit tests exist, I will verify the fix by code inspection and ensuring it follows the pattern used in other working ViewModels (like `HerdViewModel`).
