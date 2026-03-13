# Implementation Plan: Recipe Bookmarking App

## Overview

This implementation plan breaks down the Android recipe bookmarking app into discrete coding tasks. The app will be built using Kotlin with Android Jetpack components (Room, LiveData/Flow, ViewModel), following clean architecture principles with clear separation between UI, business logic, and data layers.

The implementation follows an incremental approach: starting with core data models and persistence, then building the display functionality, followed by import capabilities, and finally adding advanced features like scaling, search, filtering, and manual entry.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Create Android project with Kotlin support
  - Add dependencies: Room, Coroutines, Lifecycle components, Retrofit/OkHttp, Jsoup for HTML parsing, Kotest for property testing
  - Configure build.gradle with required plugins (kapt for Room)
  - Set up package structure: data, domain, ui, utils
  - _Requirements: All requirements depend on proper project setup_

- [ ] 2. Implement core data models and database
  - [x] 2.1 Create data model classes
    - Implement Recipe, Ingredient, Instruction, NutritionInfo, ScaledIngredient data classes
    - Implement Category enum with BREAKFAST, LUNCH, DINNER, DESSERT, DRINK, SAUCE, UNCATEGORIZED
    - Add Room annotations (@Entity, @PrimaryKey, @TypeConverters)
    - Create type converters for List<Ingredient>, List<Instruction>, NutritionInfo, Category
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 4.1, 5.1, 5.2, 6.1, 9.1, 9.2_

  - [ ]* 2.2 Write property test for recipe persistence round-trip
    - **Property 1: Recipe persistence round-trip**
    - **Validates: Requirements 7.1, 7.2, 7.3**

  - [x] 2.3 Create Room database and DAO
    - Implement RecipeDatabase with Room annotations
    - Implement RecipeDao with CRUD operations: insert, update, delete, getAll, getById, searchByName, getByCategory
    - Use Flow return types for reactive queries
    - _Requirements: 7.1, 7.2, 7.3, 1.1, 10.2, 9.7_

  - [ ]* 2.4 Write property test for recipe list completeness
    - **Property 2: Recipe list completeness**
    - **Validates: Requirements 1.1**

- [ ] 3. Implement repository layer
  - [x] 3.1 Create RecipeRepository interface and implementation
    - Implement getAllRecipes(), getRecipeById(), searchRecipes(), getRecipesByCategory()
    - Implement insertRecipe(), updateRecipe(), deleteRecipe()
    - Add importFromUrl() method signature (implementation in later task)
    - Use DAO as data source with proper coroutine scoping
    - _Requirements: 1.1, 1.2, 7.1, 7.2, 7.3, 9.7, 10.2_

  - [ ]* 3.2 Write unit tests for repository operations
    - Test CRUD operations with mock DAO
    - Test Flow emissions on data changes
    - _Requirements: 1.1, 7.1, 7.2, 7.3_

- [ ] 4. Implement recipe list UI
  - [x] 4.1 Create RecipeListActivity with RecyclerView
    - Implement activity layout with RecyclerView, search input field, category filter spinner
    - Add kitchen-icon.png to drawable resources (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
    - Display kitchen-icon.png as header branding element
    - Create RecipeListAdapter with ViewHolder for recipe items
    - Display recipe name and category tag in list items
    - Show "Uncategorized" for recipes without category
    - _Requirements: 1.1, 1.3, 9.9, 9.10, 10.1_

  - [x] 4.2 Create RecipeListViewModel
    - Implement ViewModel with LiveData/StateFlow for recipe list
    - Add search query and category filter state
    - Observe repository and apply filters
    - _Requirements: 1.1, 9.7, 10.2_

  - [x] 4.3 Wire RecipeListActivity to ViewModel
    - Observe recipe list LiveData and update RecyclerView
    - Implement click listener to navigate to RecipeDetailActivity
    - Pass recipe ID via Intent extras
    - _Requirements: 1.2_

  - [ ]* 4.4 Write property test for recipe selection navigation
    - **Property 3: Recipe selection navigation**
    - **Validates: Requirements 1.2**

  - [ ]* 4.5 Write property test for recipe display includes name
    - **Property 4: Recipe display includes name**
    - **Validates: Requirements 1.3**

- [x] 5. Checkpoint - Ensure basic recipe list display works
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement recipe detail UI
  - [x] 6.1 Create RecipeDetailActivity layout
    - Design layout with sections: recipe name, yield/serving info, ingredients list, instructions list, nutrition info, original link, scaling controls
    - Use ScrollView for scrollable content
    - Add TextViews for recipe name, yield, serving size
    - Add RecyclerView or LinearLayout for ingredients list
    - Add RecyclerView or LinearLayout for instructions list
    - Add section for nutrition info with conditional visibility
    - Add clickable TextView/Button for original link
    - Add scaling factor selector (RadioGroup or Spinner with 1.0x, 1.5x, 2.0x options)
    - Add notification TextView in instruction section for scaling warnings
    - _Requirements: 1.2, 2.1, 2.2, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 6.1, 6.3, 8.1, 8.4, 8.5_

  - [x] 6.2 Create RecipeDetailViewModel
    - Implement ViewModel to load recipe by ID from repository
    - Add scaling factor state (default to 1.0x)
    - Expose recipe data and scaled ingredients as LiveData/StateFlow
    - _Requirements: 1.2, 8.6_

  - [x] 6.3 Implement ingredient display with order preservation
    - Display ingredients in order specified by Ingredient.order field
    - Show quantity, unit, and name for each ingredient
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ]* 6.4 Write property tests for ingredient display
    - **Property 5: Ingredient list completeness**
    - **Property 6: Ingredient display includes quantity and unit**
    - **Property 7: Ingredient order preservation**
    - **Validates: Requirements 2.1, 2.2, 2.3**

  - [x] 6.5 Implement yield and serving size display
    - Display yield information when available
    - Display serving size information when available
    - Format yield as number of servings or quantity
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ]* 6.6 Write property tests for yield display
    - **Property 8: Yield information display**
    - **Property 9: Serving size information display**
    - **Property 10: Yield format display**
    - **Validates: Requirements 3.1, 3.2, 3.3**

  - [x] 6.7 Implement nutrition information display
    - Show nutrition info section when NutritionInfo is not null
    - Display calories, protein, carbohydrates, fat, fiber, sugar
    - Show "Nutritional data unavailable" message when NutritionInfo is null
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ]* 6.8 Write property tests for nutrition display
    - **Property 11: Nutrition information display when available**
    - **Property 12: Nutrition unavailable indication**
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [x] 6.9 Implement instruction display with order preservation
    - Display instructions in order specified by Instruction.order field
    - Show each instruction as a distinct numbered step
    - _Requirements: 5.1, 5.2, 5.3_

  - [ ]* 6.10 Write property tests for instruction display
    - **Property 13: Instruction list completeness**
    - **Property 14: Instruction order preservation**
    - **Property 15: Instruction step distinction**
    - **Validates: Requirements 5.1, 5.2, 5.3**

  - [x] 6.11 Implement original link display and click handling
    - Display original URL as clickable element when not null
    - Implement click listener to open URL in browser using Intent.ACTION_VIEW
    - _Requirements: 6.1, 6.2, 6.3_

  - [ ]* 6.12 Write property tests for original link
    - **Property 16: Original link display**
    - **Property 17: Original link opens browser**
    - **Property 18: Original link is clickable**
    - **Validates: Requirements 6.1, 6.2, 6.3**

- [x] 7. Checkpoint - Ensure recipe detail display works
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Implement ingredient scaling functionality
  - [x] 8.1 Create ScalingCalculator interface and implementation
    - Implement scaleIngredients() method
    - Calculate scaled quantities by multiplying original quantity by scaling factor multiplier
    - Return List<ScaledIngredient> with both original and scaled quantities
    - _Requirements: 8.1, 8.2_

  - [ ]* 8.2 Write property test for ingredient scaling calculation
    - **Property 19: Ingredient scaling calculation**
    - **Validates: Requirements 8.2**

  - [x] 8.3 Integrate scaling into RecipeDetailViewModel
    - Add scaling factor state management
    - Call ScalingCalculator when scaling factor changes
    - Expose scaled ingredients to UI
    - _Requirements: 8.2, 8.3, 8.5, 8.6_

  - [x] 8.4 Update RecipeDetailActivity to display scaled quantities
    - Show scaled quantities instead of original when scaling factor is not 1.0x
    - Update scaling factor selector to change ViewModel state
    - Display current scaling factor prominently
    - Show scaling notification in instruction section when factor is not 1.0x
    - _Requirements: 8.3, 8.4, 8.5_

  - [ ]* 8.5 Write property tests for scaling display
    - **Property 20: Scaled quantities display**
    - **Property 21: Scaling notification display**
    - **Property 22: Current scaling factor display**
    - **Property 23: Default scaling factor**
    - **Validates: Requirements 8.3, 8.4, 8.5, 8.6**

- [ ] 9. Implement category management
  - [x] 9.1 Add category assignment UI to RecipeDetailActivity
    - Add category selector (Spinner or dropdown) to detail view
    - Populate with Category enum values
    - Show current category selection
    - _Requirements: 9.2, 9.4, 9.9_

  - [x] 9.2 Implement category update in RecipeDetailViewModel
    - Add updateCategory() method
    - Call repository.updateRecipe() with modified category
    - _Requirements: 9.2, 9.3, 9.4, 9.5_

  - [ ]* 9.3 Write property test for category assignment and persistence
    - **Property 24: Category assignment and persistence**
    - **Validates: Requirements 9.2, 9.3, 9.4, 9.5**

  - [x] 9.4 Implement category filtering in RecipeListActivity
    - Add category filter Spinner to list view
    - Include "All Categories" option
    - Update RecipeListViewModel to filter by selected category
    - _Requirements: 9.6, 9.7, 9.8_

  - [ ]* 9.5 Write property tests for category filtering
    - **Property 25: Category filter accuracy**
    - **Property 26: No filter shows all recipes**
    - **Property 27: Category display in list**
    - **Property 28: Uncategorized indication**
    - **Validates: Requirements 9.7, 9.8, 9.9, 9.10**

- [ ] 10. Implement search functionality
  - [x] 10.1 Add search input handling to RecipeListViewModel
    - Add search query state
    - Implement case-insensitive substring matching
    - Update recipe list based on search query
    - _Requirements: 10.2, 10.4_

  - [x] 10.2 Wire search input to ViewModel in RecipeListActivity
    - Add TextWatcher to search EditText
    - Update ViewModel search query on text changes
    - Clear search when input is empty
    - _Requirements: 10.1, 10.3, 10.6_

  - [x] 10.3 Implement empty search results handling
    - Show "No results found" message when filtered list is empty
    - Hide message when results exist
    - _Requirements: 10.5_

  - [ ]* 10.4 Write property tests for search functionality
    - **Property 29: Search filter accuracy**
    - **Property 30: Case-insensitive search**
    - **Property 31: Empty search results message**
    - **Validates: Requirements 10.2, 10.4, 10.5**

- [ ] 11. Checkpoint - Ensure search and filtering work
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 12. Implement recipe parsing from HTML
  - [x] 12.1 Create RecipeParser interface and implementation
    - Add Jsoup dependency for HTML parsing
    - Implement parseRecipe() method to extract recipe data from HTML
    - Support common recipe schema formats (JSON-LD, Microdata, RDFa)
    - Extract name, ingredients, instructions, yield, serving size, nutrition info
    - Return ParseResult.Success or ParseResult.Failure
    - _Requirements: 11.5_

  - [ ]* 12.2 Write property test for recipe parsing
    - **Property 35: Recipe parsing extracts required fields**
    - **Validates: Requirements 11.5**

  - [ ]* 12.3 Write unit tests for RecipeParser
    - Test parsing valid recipe HTML with all fields
    - Test parsing HTML with missing optional fields
    - Test parsing invalid HTML returns ParseResult.Failure
    - Test parsing HTML without recipe data returns ParseResult.Failure
    - _Requirements: 11.5_

- [ ] 13. Implement network client for URL fetching
  - [x] 13.1 Create NetworkClient interface and implementation
    - Add OkHttp or Retrofit dependency
    - Implement fetchHtml() method to retrieve webpage content from URL
    - Set timeout to 30 seconds
    - Handle network errors and return appropriate error types
    - _Requirements: 11.4_

  - [ ]* 13.2 Write unit tests for NetworkClient
    - Test successful URL fetch
    - Test network timeout
    - Test connection failure
    - Test invalid URL
    - _Requirements: 11.4_

- [ ] 14. Implement URL import service
  - [x] 14.1 Create ImportService as IntentService or WorkManager Worker
    - Implement handleSharedUrls() method
    - Process each URL sequentially
    - Call NetworkClient.fetchHtml() for each URL
    - Call RecipeParser.parseRecipe() on fetched HTML
    - Call RecipeRepository.insertRecipe() on successful parse
    - Store original URL in Recipe.originalUrl field
    - Track success and failure counts
    - Continue processing remaining URLs after failures
    - Return ImportSummary with counts and failure details
    - _Requirements: 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.13_

  - [ ]* 14.2 Write property tests for import operations
    - **Property 32: Single URL acceptance**
    - **Property 33: Multiple URL acceptance**
    - **Property 34: URL triggers fetch**
    - **Property 36: Successful import adds to list**
    - **Property 37: Import preserves source URL**
    - **Property 42: Import continues after errors**
    - **Validates: Requirements 11.2, 11.3, 11.4, 11.6, 11.7, 11.13**

  - [x] 14.3 Implement error handling and user notifications
    - Show error message for inaccessible URLs (ImportError.URL_INACCESSIBLE)
    - Show error message for parse failures (ImportError.PARSE_FAILED)
    - Show success confirmation for single URL imports
    - Show import summary for multi-URL imports with success/failure counts
    - _Requirements: 11.9, 11.10, 11.11, 11.12_

  - [ ]* 14.4 Write property tests for import error handling
    - **Property 38: Import success confirmation**
    - **Property 39: Inaccessible URL error message**
    - **Property 40: Parse failure error message**
    - **Property 41: Multi-URL import summary accuracy**
    - **Validates: Requirements 11.9, 11.10, 11.11, 11.12**

- [ ] 15. Register app as Android share target
  - [x] 15.1 Add intent filter to AndroidManifest.xml
    - Register ImportService or dedicated activity as share target
    - Add intent filter for ACTION_SEND with text/plain MIME type
    - Add intent filter for ACTION_SEND_MULTIPLE for bookmark folder sharing
    - _Requirements: 11.1, 11.2, 11.3_

  - [x] 15.2 Create share handling activity or update ImportService
    - Extract shared URL(s) from Intent extras
    - Pass URLs to ImportService for processing
    - Show progress indicator during import
    - _Requirements: 11.2, 11.3_

- [ ] 16. Checkpoint - Ensure URL import works end-to-end
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 17. Implement manual recipe entry UI
  - [ ] 17.1 Create ManualEntryActivity layout
    - Add EditText fields for recipe name, yield, serving size, original URL
    - Add RecyclerView or dynamic LinearLayout for ingredients list
    - Add RecyclerView or dynamic LinearLayout for instructions list
    - Add "Add Ingredient" button
    - Add "Add Instruction" button
    - Add "Save" and "Cancel" buttons
    - _Requirements: 12.3, 12.4, 12.5, 12.9, 12.15_

  - [ ] 17.2 Create ingredient entry UI components
    - Create ingredient item layout with EditText for name, quantity, unit
    - Add remove button for each ingredient item
    - Add drag handle for reordering ingredients
    - _Requirements: 12.6, 12.8, 12.13_

  - [ ] 17.3 Create instruction entry UI components
    - Create instruction item layout with EditText for instruction text
    - Add remove button for each instruction item
    - Add drag handle for reordering instructions
    - _Requirements: 12.10, 12.12, 12.14_

  - [ ] 17.4 Implement ManualEntryViewModel
    - Add state for recipe name, yield, serving size, original URL
    - Add state for ingredients list and instructions list
    - Implement addIngredient(), removeIngredient(), reorderIngredients()
    - Implement addInstruction(), removeInstruction(), reorderInstructions()
    - Implement validation logic for required fields
    - Implement saveRecipe() method
    - _Requirements: 12.6, 12.7, 12.8, 12.9, 12.10, 12.11, 12.12, 12.13, 12.14, 12.16, 12.17, 12.18_

  - [ ] 17.5 Wire ManualEntryActivity to ViewModel
    - Bind UI inputs to ViewModel state
    - Implement add/remove/reorder button click handlers
    - Implement save button with validation
    - Show validation error messages for missing required fields
    - Show success confirmation on successful save
    - Implement cancel button to close activity without saving
    - _Requirements: 12.16, 12.17, 12.18, 12.19, 12.20, 12.21, 12.22_

  - [ ]* 17.6 Write property tests for manual entry operations
    - **Property 45: Ingredient addition captures all fields**
    - **Property 46: Multiple items addition**
    - **Property 47: Item removal from lists**
    - **Property 48: List reordering preserves all items**
    - **Property 49: Optional URL entry**
    - **Validates: Requirements 12.6, 12.7, 12.8, 12.10, 12.11, 12.12, 12.13, 12.14, 12.15**

  - [ ]* 17.7 Write property tests for manual entry validation
    - **Property 50: Empty name validation failure**
    - **Property 51: Empty ingredients validation failure**
    - **Property 52: Empty instructions validation failure**
    - **Property 53: Validation error messages**
    - **Property 54: Valid manual entry adds to list**
    - **Property 55: Manual entry cancellation**
    - **Validates: Requirements 12.16, 12.17, 12.18, 12.19, 12.20, 12.22**

- [ ] 18. Integrate manual entry with import failure flow
  - [ ] 18.1 Update ImportService error handling
    - When URL fetch fails, show dialog with "Retry" and "Enter Manually" options
    - When parse fails, show dialog with "Enter Manually" option
    - Launch ManualEntryActivity when user selects manual entry
    - Pre-fill original URL field in ManualEntryActivity if available
    - _Requirements: 12.1, 12.2, 12.3_

  - [ ]* 18.2 Write property test for manual entry offered on failure
    - **Property 43: Manual entry offered on failure**
    - **Property 44: Manual entry triggers form display**
    - **Validates: Requirements 12.1, 12.2, 12.3**

- [ ] 19. Set up app icon and branding
  - [ ] 19.1 Add kitchen-icon.png to launcher icon resources
    - Copy kitchen-icon.png to mipmap-mdpi, mipmap-hdpi, mipmap-xhdpi, mipmap-xxhdpi, mipmap-xxxhdpi
    - Update AndroidManifest.xml to reference kitchen-icon as app icon
    - _Requirements: Visual branding requirement_

- [ ] 20. Final integration and polish
  - [ ] 20.1 Add navigation from RecipeListActivity to ManualEntryActivity
    - Add FloatingActionButton or menu item for "Add Recipe Manually"
    - Launch ManualEntryActivity on button click
    - _Requirements: 12.3_

  - [ ] 20.2 Implement recipe deletion functionality
    - Add delete option in RecipeDetailActivity (menu item or button)
    - Show confirmation dialog before deletion
    - Call repository.deleteRecipe() on confirmation
    - Navigate back to RecipeListActivity after deletion
    - _Requirements: Implied by CRUD operations_

  - [ ]* 20.3 Write integration tests for critical user flows
    - Test end-to-end flow: import URL → display in list → view detail → scale ingredients
    - Test end-to-end flow: manual entry → save → display in list → view detail
    - Test end-to-end flow: search recipes → select result → view detail
    - Test end-to-end flow: filter by category → select result → view detail
    - _Requirements: All requirements_

- [ ] 21. Final checkpoint - Ensure all features work end-to-end
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The implementation uses Kotlin with Android Jetpack components (Room, LiveData/Flow, ViewModel)
- Kotest property testing framework is used for property-based tests with minimum 100 iterations
- All property tests should include comment tags: `// Feature: recipe-bookmarks, Property {number}: {property_text}`
