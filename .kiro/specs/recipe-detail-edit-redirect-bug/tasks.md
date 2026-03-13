# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Edit Button Launches Separate Activity
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: For deterministic bugs, scope the property to the concrete failing case(s) to ensure reproducibility
  - Test that clicking Edit button in RecipeDetailActivity launches RecipeEditorActivity (Bug Condition from design)
  - Test that clicking Edit button causes navigation away from RecipeDetailActivity
  - Test that clicking Edit button does NOT enable in-place editing mode
  - Test that clicking Edit button does NOT show Save/Cancel buttons
  - The test assertions should match the Expected Behavior Properties from design (in-place editing, Save/Cancel buttons visible, no navigation)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples found to understand root cause (e.g., "Edit button launches Intent to RecipeEditorActivity instead of toggling edit mode")
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Edit Interactions Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-buggy inputs (all interactions except Edit button)
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements
  - Test scaling factor changes (1.0x, 1.5x, 2.0x) update ingredient quantities correctly
  - Test category changes via spinner update recipe category
  - Test delete button shows confirmation dialog
  - Test original recipe link opens in browser
  - Test view mode displays all recipe data correctly (ingredients, instructions, nutrition, yield, serving size)
  - Property-based testing generates many test cases for stronger guarantees
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9_

- [x] 3. Fix for recipe detail edit redirect bug

  - [x] 3.1 Add edit mode state management to RecipeDetailViewModel
    - Add `MutableStateFlow<Boolean>` for edit mode tracking (default: false)
    - Add `MutableStateFlow<Recipe?>` for original recipe snapshot
    - Add `enterEditMode()` function to capture recipe snapshot and set edit mode to true
    - Add `saveChanges()` function to persist changes and exit edit mode
    - Add `cancelChanges()` function to restore original data and exit edit mode
    - _Bug_Condition: isBugCondition(input) where input.buttonId == R.id.editButton AND input.activity == RecipeDetailActivity_
    - _Expected_Behavior: Enable in-place editing mode, show Save/Cancel buttons, hide Edit/Delete buttons, make fields editable, no navigation_
    - _Preservation: All non-Edit-button interactions (scaling, category, delete, link opening, view mode display) must remain unchanged_
    - _Requirements: 2.1, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12, 2.13, 2.14, 2.15, 2.16_

  - [x] 3.2 Add Save and Cancel buttons to layout
    - Add Save button with id `saveButton` to activity_recipe_detail.xml
    - Add Cancel button with id `cancelButton` to activity_recipe_detail.xml
    - Set initial visibility to `gone` for both buttons
    - Add string resources for button labels
    - _Requirements: 2.8, 2.9_

  - [x] 3.3 Convert recipe fields to support edit mode
    - Convert recipe name TextView to EditText or add separate EditText with visibility toggling
    - Convert yield TextView to EditText or add separate EditText
    - Convert serving size TextView to EditText or add separate EditText
    - Modify displayIngredients() to create EditText fields when in edit mode
    - Modify displayInstructions() to create EditText fields when in edit mode
    - Modify displayNutritionInfo() to support editable fields when in edit mode
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 3.4 Update RecipeDetailActivity to handle edit mode
    - Replace Edit button logic in setupEditAndDeleteButtons() to call viewModel.enterEditMode() instead of launching Intent
    - Add Save button listener to call viewModel.saveChanges()
    - Add Cancel button listener to call viewModel.cancelChanges()
    - Add StateFlow collector for viewModel.isEditMode to toggle UI elements
    - When edit mode true: show Save/Cancel, hide Edit/Delete, enable fields
    - When edit mode false: show Edit/Delete, hide Save/Cancel, disable fields
    - _Requirements: 2.1, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12, 2.13, 2.14, 2.15, 2.16_

  - [x] 3.5 Implement data collection and validation
    - Create collectEditedRecipeData() function to gather data from EditText fields
    - Parse ingredients from EditText fields into Ingredient objects
    - Parse instructions from EditText fields into Instruction objects
    - Parse nutrition info from EditText fields into NutritionInfo object
    - Validate required fields are not empty
    - Validate numeric fields contain valid numbers
    - Return updated Recipe object or null if validation fails
    - _Requirements: 2.10, 2.13_

  - [x] 3.6 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - In-Place Edit Mode Enabled
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - Verify Edit button enables in-place editing without launching RecipeEditorActivity
    - Verify Save/Cancel buttons appear when Edit is clicked
    - Verify Edit/Delete buttons hide when Edit is clicked
    - Verify recipe fields become editable when Edit is clicked
    - Verify no navigation occurs when Edit is clicked
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9_

  - [x] 3.7 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-Edit Interactions Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Verify scaling factor changes still work correctly
    - Verify category changes still work correctly
    - Verify delete button still shows confirmation dialog
    - Verify original recipe link still opens in browser
    - Verify view mode still displays all recipe data correctly
    - Confirm all tests still pass after fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9_

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
