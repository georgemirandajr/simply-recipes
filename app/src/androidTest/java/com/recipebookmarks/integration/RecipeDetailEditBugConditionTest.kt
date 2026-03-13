package com.recipebookmarks.integration

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipebookmarks.R
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.data.RecipeDatabase
import com.recipebookmarks.domain.RecipeRepositoryImpl
import com.recipebookmarks.ui.RecipeDetailActivity
import com.recipebookmarks.ui.RecipeEditorActivity
import com.recipebookmarks.ui.RecipeListActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug Condition Exploration Test for Recipe Detail Edit Redirect Bug
 * 
 * **Property 1: Bug Condition** - Edit Button Launches Separate Activity
 * 
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
 * **GOAL**: Surface counterexamples that demonstrate the bug exists
 * 
 * This test verifies the EXPECTED BEHAVIOR (in-place editing) which is currently NOT implemented.
 * On UNFIXED code, this test will FAIL because:
 * - Edit button launches RecipeEditorActivity (incorrect behavior)
 * - Edit button causes navigation away from RecipeDetailActivity (incorrect behavior)
 * - Save/Cancel buttons do NOT appear (incorrect behavior)
 * - Recipe fields do NOT become editable (incorrect behavior)
 * 
 * After the fix is implemented, this test will PASS because:
 * - Edit button enables in-place editing mode (correct behavior)
 * - Save/Cancel buttons appear (correct behavior)
 * - Edit/Delete buttons hide (correct behavior)
 * - Recipe fields become editable (correct behavior)
 * - No navigation occurs (correct behavior)
 * 
 * Validates: Requirements 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9
 */
@RunWith(AndroidJUnit4::class)
class RecipeDetailEditBugConditionTest {

    private lateinit var context: Context
    private lateinit var database: RecipeDatabase
    private lateinit var repository: RecipeRepositoryImpl
    private var recipeId: Long = -1L

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = RecipeDatabase.getDatabase(context, inMemory = true)
        repository = RecipeRepositoryImpl(database.recipeDao())
        
        // Create a test recipe
        val testRecipe = Recipe(
            name = "Test Recipe",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1)
            ),
            instructions = listOf(
                Instruction("Mix ingredients", 0),
                Instruction("Bake at 350F", 1)
            ),
            originalUrl = "https://example.com/recipe",
            yield = "12 servings",
            servingSize = "1 cookie",
            isFallback = false
        )
        
        runBlocking {
            recipeId = repository.insertRecipe(testRecipe)
        }
        
        // Initialize Espresso Intents for intent verification
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        database.close()
    }

    /**
     * Test: Edit button should enable in-place editing WITHOUT launching RecipeEditorActivity
     * 
     * EXPECTED OUTCOME ON UNFIXED CODE: FAIL
     * - The test will fail because Edit button currently launches RecipeEditorActivity
     * - This failure confirms the bug exists
     * 
     * EXPECTED OUTCOME ON FIXED CODE: PASS
     * - The test will pass because Edit button enables in-place editing
     * - No Intent to RecipeEditorActivity is launched
     * 
     * Validates: Requirements 1.1, 2.1, 2.7
     */
    @Test
    fun testEditButton_EnablesInPlaceEditing_DoesNotLaunchRecipeEditorActivity() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // When - User clicks Edit button
        onView(withId(R.id.editButton)).perform(click())

        // Then - RecipeEditorActivity should NOT be launched (expected behavior)
        // On UNFIXED code: This assertion will FAIL because RecipeEditorActivity IS launched
        // On FIXED code: This assertion will PASS because no Intent is launched
        try {
            Intents.intended(hasComponent(RecipeEditorActivity::class.java.name))
            throw AssertionError("COUNTEREXAMPLE FOUND: Edit button launched RecipeEditorActivity (bug exists)")
        } catch (e: AssertionError) {
            if (e.message?.contains("COUNTEREXAMPLE") == true) {
                throw e
            }
            // If no intent was found, the test passes (expected behavior)
        }

        scenario.close()
    }

    /**
     * Test: Edit button should show Save and Cancel buttons
     * 
     * EXPECTED OUTCOME ON UNFIXED CODE: FAIL
     * - Save/Cancel buttons do not exist or are not visible
     * - This failure confirms the bug exists
     * 
     * EXPECTED OUTCOME ON FIXED CODE: PASS
     * - Save/Cancel buttons become visible when Edit is clicked
     * 
     * Validates: Requirements 2.8, 2.9
     */
    @Test
    fun testEditButton_ShowsSaveAndCancelButtons() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // When - User clicks Edit button
        onView(withId(R.id.editButton)).perform(click())

        // Then - Save and Cancel buttons should be visible (expected behavior)
        // On UNFIXED code: This will FAIL because buttons don't exist
        // On FIXED code: This will PASS because buttons are visible
        try {
            onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
            onView(withId(R.id.cancelButton)).check(matches(isDisplayed()))
        } catch (e: Exception) {
            throw AssertionError("COUNTEREXAMPLE FOUND: Save/Cancel buttons not visible after clicking Edit (bug exists)", e)
        }

        scenario.close()
    }

    /**
     * Test: Edit button should hide Edit and Delete buttons
     * 
     * EXPECTED OUTCOME ON UNFIXED CODE: FAIL
     * - Edit/Delete buttons remain visible (or activity navigates away)
     * - This failure confirms the bug exists
     * 
     * EXPECTED OUTCOME ON FIXED CODE: PASS
     * - Edit/Delete buttons become hidden when Edit is clicked
     * 
     * Validates: Requirements 2.7, 2.8
     */
    @Test
    fun testEditButton_HidesEditAndDeleteButtons() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // When - User clicks Edit button
        onView(withId(R.id.editButton)).perform(click())

        // Then - Edit and Delete buttons should be hidden (expected behavior)
        // On UNFIXED code: This will FAIL because buttons remain visible or activity navigates
        // On FIXED code: This will PASS because buttons are hidden
        try {
            onView(withId(R.id.editButton)).check(matches(not(isDisplayed())))
            onView(withId(R.id.deleteButton)).check(matches(not(isDisplayed())))
        } catch (e: Exception) {
            throw AssertionError("COUNTEREXAMPLE FOUND: Edit/Delete buttons still visible after clicking Edit (bug exists)", e)
        }

        scenario.close()
    }

    /**
     * Test: Edit button should NOT cause navigation away from RecipeDetailActivity
     * 
     * EXPECTED OUTCOME ON UNFIXED CODE: FAIL
     * - Activity navigates to RecipeEditorActivity
     * - This failure confirms the bug exists
     * 
     * EXPECTED OUTCOME ON FIXED CODE: PASS
     * - Activity remains on RecipeDetailActivity
     * 
     * Validates: Requirements 1.2, 2.7
     */
    @Test
    fun testEditButton_DoesNotNavigateAway() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // When - User clicks Edit button
        onView(withId(R.id.editButton)).perform(click())

        // Then - Should remain on RecipeDetailActivity (expected behavior)
        // On UNFIXED code: This will FAIL because RecipeEditorActivity is launched
        // On FIXED code: This will PASS because we stay on RecipeDetailActivity
        scenario.onActivity { activity ->
            if (activity.isFinishing || activity.isDestroyed) {
                throw AssertionError("COUNTEREXAMPLE FOUND: RecipeDetailActivity is finishing/destroyed after clicking Edit (navigation occurred, bug exists)")
            }
        }

        scenario.close()
    }
}
