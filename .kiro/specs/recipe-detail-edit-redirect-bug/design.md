# Recipe Detail Edit Redirect Bug - Bugfix Design

## Overview

This bugfix addresses the incorrect behavior where clicking the "Edit" button in RecipeDetailActivity launches a separate RecipeEditorActivity screen. The fix will implement in-place editing within RecipeDetailActivity itself, allowing users to edit recipe fields without leaving the detail view. The solution involves adding edit mode state management, toggling UI elements between read-only and editable modes, and implementing Save/Cancel functionality with proper data persistence and rollback capabilities.

The fix is minimal and targeted: it modifies only RecipeDetailActivity and its ViewModel to support edit mode, while preserving all existing functionality for viewing, scaling, categorization, and deletion.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - when the user clicks the "Edit" button in RecipeDetailActivity
- **Property (P)**: The desired behavior when Edit is clicked - enable in-place editing with Save/Cancel buttons, without navigation
- **Preservation**: All existing view-mode behaviors (scaling, categorization, deletion, link opening) that must remain unchanged
- **Edit Mode**: A UI state where recipe fields become editable and Save/Cancel buttons are displayed
- **View Mode**: The default UI state where recipe fields are read-only and the Edit button is displayed
- **RecipeDetailActivity**: The activity in `app/src/main/java/com/recipebookmarks/ui/RecipeDetailActivity.kt` that displays recipe details
- **RecipeDetailViewModel**: The ViewModel in `app/src/main/java/com/recipebookmarks/ui/RecipeDetailViewModel.kt` that manages recipe data and state
- **Original Recipe Data**: A snapshot of recipe data before entering edit mode, used for Cancel functionality

## Bug Details

### Bug Condition

The bug manifests when a user clicks the "Edit" button in RecipeDetailActivity. The current implementation incorrectly launches RecipeEditorActivity (a separate screen), causing unwanted navigation away from the detail view.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type ButtonClickEvent
  OUTPUT: boolean
  
  RETURN input.buttonId == R.id.editButton
         AND input.activity == RecipeDetailActivity
         AND currentBehavior == LAUNCH_RECIPE_EDITOR_ACTIVITY
END FUNCTION
```

### Examples

- User views a recipe and clicks "Edit" → System launches RecipeEditorActivity (INCORRECT)
- User views a recipe and clicks "Edit" → System navigates to a new screen (INCORRECT)
- User wants to quickly edit recipe title → Must navigate to separate screen and back (INCORRECT)
- User clicks "Edit" → Expected: fields become editable on same screen with Save/Cancel buttons (CORRECT)

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Viewing recipe details in read-only mode must continue to work exactly as before
- Scaling ingredients with 1.0x, 1.5x, 2.0x factors must continue to work
- Changing recipe category via spinner must continue to work
- Deleting recipes with confirmation dialog must continue to work
- Opening original recipe links in browser must continue to work
- Displaying ingredients, instructions, nutrition info, yield, and serving size must continue to work

**Scope:**
All inputs and interactions that do NOT involve clicking the "Edit" button should be completely unaffected by this fix. This includes:
- All view-mode interactions (scrolling, reading, clicking links)
- Scaling factor changes via radio buttons
- Category changes via spinner
- Delete button clicks and confirmation dialog
- Navigation back to recipe list

## Hypothesized Root Cause

Based on the bug description and code analysis, the root cause is:

1. **Incorrect Intent Launch**: The `setupEditAndDeleteButtons()` method in RecipeDetailActivity creates an Intent to launch RecipeEditorActivity when the Edit button is clicked, rather than toggling edit mode within the current activity.

2. **Missing Edit Mode State**: RecipeDetailActivity lacks any state management for edit vs view modes. There is no boolean flag or StateFlow to track whether the user is currently editing.

3. **Missing UI Toggle Logic**: The layout uses static TextViews for displaying recipe data. There is no logic to swap these with EditText fields or make them editable when entering edit mode.

4. **Missing Save/Cancel Buttons**: The layout only has Edit and Delete buttons. There are no Save and Cancel buttons defined, and no logic to show/hide button groups based on mode.

5. **Missing Data Snapshot Logic**: There is no mechanism to preserve the original recipe data before editing, which is required to implement Cancel functionality that discards changes.

## Correctness Properties

Property 1: Bug Condition - In-Place Edit Mode Activation

_For any_ button click event where the Edit button is clicked in RecipeDetailActivity, the fixed implementation SHALL enable in-place editing mode on the same screen, making recipe fields editable, displaying Save and Cancel buttons, hiding the Edit and Delete buttons, and NOT launching RecipeEditorActivity or navigating away from the current view.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9**

Property 2: Preservation - Non-Edit Interactions

_For any_ user interaction that is NOT clicking the Edit button (scaling changes, category changes, delete button, link clicks, scrolling), the fixed code SHALL produce exactly the same behavior as the original code, preserving all existing view-mode functionality.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `app/src/main/java/com/recipebookmarks/ui/RecipeDetailViewModel.kt`

**Function**: RecipeDetailViewModel class

**Specific Changes**:
1. **Add Edit Mode State**: Add a `MutableStateFlow<Boolean>` to track whether the activity is in edit mode (default: false)
   - `private val _isEditMode = MutableStateFlow(false)`
   - `val isEditMode: StateFlow<Boolean> = _isEditMode`

2. **Add Original Recipe Snapshot**: Add a `MutableStateFlow<Recipe?>` to store the original recipe data before editing
   - `private val _originalRecipe = MutableStateFlow<Recipe?>(null)`

3. **Add enterEditMode() Function**: Create a function that captures the current recipe as a snapshot and sets edit mode to true
   - Captures `recipe.value` into `_originalRecipe`
   - Sets `_isEditMode.value = true`

4. **Add saveChanges() Function**: Create a function that persists the current recipe data and exits edit mode
   - Calls `repository.updateRecipe()` with current recipe
   - Sets `_isEditMode.value = false`
   - Clears `_originalRecipe`

5. **Add cancelChanges() Function**: Create a function that restores the original recipe data and exits edit mode
   - Restores `_originalRecipe.value` to the recipe StateFlow (may require refactoring recipe to MutableStateFlow)
   - Sets `_isEditMode.value = false`
   - Clears `_originalRecipe`

**File**: `app/src/main/res/layout/activity_recipe_detail.xml`

**Specific Changes**:
1. **Add Save and Cancel Buttons**: Add two new buttons in the button container LinearLayout
   - `android:id="@+id/saveButton"` with text "@string/save_button"
   - `android:id="@+id/cancelButton"` with text "@string/cancel_button"
   - Initially set `android:visibility="gone"` for both

2. **Convert Recipe Name to EditText**: Replace `recipeNameTextView` TextView with an EditText that can toggle between enabled/disabled
   - Keep the same ID for minimal code changes
   - Or add a separate EditText and toggle visibility between TextView and EditText

3. **Convert Yield and Serving Size to EditText**: Replace `yieldTextView` and `servingSizeTextView` with EditText fields
   - Keep the same IDs or add separate EditText fields with visibility toggling

4. **Make Ingredients Editable**: The ingredients are dynamically added TextViews. Will need to change the `displayIngredients()` method to create EditText fields when in edit mode

5. **Make Instructions Editable**: The instructions are dynamically added TextViews. Will need to change the `displayInstructions()` method to create EditText fields when in edit mode

6. **Make Nutrition Info Editable**: Replace `nutritionInfoTextView` with EditText or add separate EditText fields for each nutrition field

**File**: `app/src/main/java/com/recipebookmarks/ui/RecipeDetailActivity.kt`

**Function**: setupEditAndDeleteButtons()

**Specific Changes**:
1. **Replace Edit Button Logic**: Remove the Intent creation and startActivity() call
   - Replace with: `viewModel.enterEditMode()`

2. **Add Save Button Listener**: Wire up the Save button to call `viewModel.saveChanges()`

3. **Add Cancel Button Listener**: Wire up the Cancel button to call `viewModel.cancelChanges()`

4. **Add Edit Mode Observer**: Add a StateFlow collector in onCreate() that observes `viewModel.isEditMode`
   - When true: show Save/Cancel buttons, hide Edit/Delete buttons, enable all editable fields
   - When false: show Edit/Delete buttons, hide Save/Cancel buttons, disable all editable fields

**Function**: displayIngredients()

**Specific Changes**:
1. **Check Edit Mode**: Query `viewModel.isEditMode.value` to determine whether to create TextViews or EditTexts
2. **Create EditText in Edit Mode**: When in edit mode, create EditText fields for ingredient name, quantity, and unit
3. **Add Remove Buttons**: Add delete buttons for each ingredient row when in edit mode
4. **Add "Add Ingredient" Button**: Show a button to add new ingredient rows when in edit mode

**Function**: displayInstructions()

**Specific Changes**:
1. **Check Edit Mode**: Query `viewModel.isEditMode.value` to determine whether to create TextViews or EditTexts
2. **Create EditText in Edit Mode**: When in edit mode, create EditText fields for instruction text
3. **Add Remove Buttons**: Add delete buttons for each instruction row when in edit mode
4. **Add "Add Instruction" Button**: Show a button to add new instruction rows when in edit mode

**Function**: displayYieldAndServingSize()

**Specific Changes**:
1. **Toggle Field Editability**: Enable/disable the yield and serving size EditText fields based on edit mode

**Function**: displayNutritionInfo()

**Specific Changes**:
1. **Toggle Field Editability**: Enable/disable nutrition info EditText fields based on edit mode
2. **Parse Nutrition Fields**: When saving, parse the EditText values back into NutritionInfo object

**New Function**: collectEditedRecipeData()

**Specific Changes**:
1. **Create New Function**: Add a function that collects all data from EditText fields and constructs an updated Recipe object
2. **Parse Ingredients**: Iterate through ingredientsListLayout children and extract ingredient data
3. **Parse Instructions**: Iterate through instructionsListLayout children and extract instruction data
4. **Parse Nutrition**: Extract nutrition data from EditText fields
5. **Validate Data**: Ensure required fields are not empty and numeric fields are valid
6. **Return Recipe**: Return the updated Recipe object or null if validation fails

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code (Edit button launches RecipeEditorActivity), then verify the fix works correctly (Edit button enables in-place editing) and preserves existing behavior (all other interactions unchanged).

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm that clicking Edit launches RecipeEditorActivity instead of enabling in-place editing.

**Test Plan**: Write instrumentation tests that click the Edit button in RecipeDetailActivity and assert that RecipeEditorActivity is launched. Run these tests on the UNFIXED code to observe failures and confirm the root cause.

**Test Cases**:
1. **Edit Button Launches Editor Test**: Click Edit button, assert RecipeEditorActivity is launched (will pass on unfixed code, should fail on fixed code)
2. **Edit Button Navigation Test**: Click Edit button, assert navigation away from RecipeDetailActivity (will pass on unfixed code, should fail on fixed code)
3. **No In-Place Edit Test**: Click Edit button, assert recipe fields remain as TextViews (will pass on unfixed code, should fail on fixed code)
4. **No Save/Cancel Buttons Test**: Click Edit button, assert Save/Cancel buttons are not visible (will pass on unfixed code, should fail on fixed code)

**Expected Counterexamples**:
- Edit button click launches RecipeEditorActivity via Intent
- User is navigated to a separate screen
- Recipe fields remain read-only TextViews
- Save and Cancel buttons do not appear

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds (Edit button clicked), the fixed function produces the expected behavior (in-place editing enabled).

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := handleEditButtonClick_fixed(input)
  ASSERT isEditModeEnabled(result)
  ASSERT saveCancelButtonsVisible(result)
  ASSERT editDeleteButtonsHidden(result)
  ASSERT recipeFieldsEditable(result)
  ASSERT noNavigationOccurred(result)
END FOR
```

**Test Cases**:
1. **In-Place Edit Mode Test**: Click Edit button, assert edit mode is enabled without navigation
2. **Save/Cancel Buttons Test**: Click Edit button, assert Save and Cancel buttons become visible
3. **Edit/Delete Buttons Hidden Test**: Click Edit button, assert Edit and Delete buttons become hidden
4. **Fields Editable Test**: Click Edit button, assert recipe name, yield, ingredients, instructions become editable
5. **Save Persists Changes Test**: Edit recipe name, click Save, assert changes are persisted to database
6. **Cancel Discards Changes Test**: Edit recipe name, click Cancel, assert changes are discarded and original data restored
7. **Save Exits Edit Mode Test**: Click Save, assert edit mode is disabled and Edit button reappears
8. **Cancel Exits Edit Mode Test**: Click Cancel, assert edit mode is disabled and Edit button reappears

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold (any interaction other than Edit button), the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT handleInteraction_original(input) = handleInteraction_fixed(input)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because it generates many test cases automatically across the input domain, catches edge cases that manual unit tests might miss, and provides strong guarantees that behavior is unchanged for all non-Edit-button interactions.

**Test Plan**: Observe behavior on UNFIXED code first for scaling, categorization, deletion, and link opening, then write property-based tests capturing that behavior.

**Test Cases**:
1. **Scaling Preservation Test**: Observe that scaling works correctly on unfixed code, then verify it continues working after fix
2. **Category Change Preservation Test**: Observe that category changes work correctly on unfixed code, then verify they continue working after fix
3. **Delete Button Preservation Test**: Observe that delete confirmation dialog works correctly on unfixed code, then verify it continues working after fix
4. **Link Opening Preservation Test**: Observe that original recipe links open in browser on unfixed code, then verify they continue working after fix
5. **View Mode Display Preservation Test**: Observe that all recipe data displays correctly in view mode on unfixed code, then verify it continues displaying correctly after fix
6. **Ingredient Display Preservation Test**: Verify ingredients display with correct order, quantity, unit, and name
7. **Instruction Display Preservation Test**: Verify instructions display as numbered sequential steps
8. **Nutrition Display Preservation Test**: Verify nutrition info displays correctly or shows unavailable message

### Unit Tests

- Test edit mode state transitions (view → edit → view)
- Test Save button persists changes to database
- Test Cancel button restores original data
- Test button visibility toggling based on edit mode
- Test field editability toggling based on edit mode
- Test data collection from EditText fields
- Test validation of edited data (empty fields, invalid numbers)
- Test that Edit button no longer launches RecipeEditorActivity

### Property-Based Tests

- Generate random recipe data and verify edit mode can be entered and exited correctly
- Generate random edits and verify Save persists them correctly
- Generate random edits and verify Cancel discards them correctly
- Generate random scaling factors and verify they work in both view and edit modes
- Generate random category changes and verify they work in both view and edit modes

### Integration Tests

- Test full edit flow: view recipe → click Edit → modify fields → click Save → verify persistence
- Test full cancel flow: view recipe → click Edit → modify fields → click Cancel → verify rollback
- Test edit mode with scaling: enter edit mode → change scaling factor → verify ingredients update correctly
- Test edit mode with category: enter edit mode → change category → verify category updates correctly
- Test switching between recipes: edit recipe A → navigate to recipe B → verify recipe A changes are saved/discarded correctly
