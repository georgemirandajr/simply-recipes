package com.recipebookmarks.integration

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipebookmarks.R
import com.recipebookmarks.data.Category
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.NutritionInfo
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.data.RecipeDatabase
import com.recipebookmarks.domain.RecipeRepositoryImpl
import com.recipebookmarks.ui.RecipeDetailActivity
import com.recipebookmarks.ui.RecipeListActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Preservation Property Tests for Recipe Detail Edit Redirect Bug
 * 
 * **Property 2: Preservation** - Non-Edit Interactions Unchanged
 * 
 * **IMPORTANT**: These tests follow observation-first methodology
 * - Tests are written to capture observed behavior on UNFIXED code
 * - Tests verify all non-Edit-button interactions work correctly
 * - Tests should PASS on UNFIXED code (confirms baseline behavior)
 * - Tests should PASS on FIXED code (confirms no regressions)
 * 
 * This test suite verifies that all interactions OTHER than clicking the Edit button
 * continue to work exactly as before after the fix is implemented.
 * 
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9
 */
@RunWith(AndroidJUnit4::class)
class RecipeDetailPreservationPropertyTest {

    private lateinit var context: Context
    private lateinit var database: RecipeDatabase
    private lateinit var repository: RecipeRepositoryImpl
    private var recipeId: Long = -1L

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = RecipeDatabase.getDatabase(context, inMemory = true)
        repository = RecipeRepositoryImpl(database.recipeDao())
        
        // Create a test recipe with all fields populated
        val testRecipe = Recipe(
            name = "Chocolate Chip Cookies",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1),
                Ingredient("Butter", 0.5, "cup", 2),
                Ingredient("Eggs", 2.0, "whole", 3)
            ),
            instructions = listOf(
                Instruction("Preheat oven to 350F", 0),
                Instruction("Mix dry ingredients", 1),
                Instruction("Add wet ingredients", 2),
                Instruction("Bake for 12 minutes", 3)
            ),
            originalUrl = "https://example.com/cookies",
            yield = "24 cookies",
            servingSize = "2 cookies",
            nutritionInfo = NutritionInfo(
                calories = 150,
                protein = "2g",
                carbohydrates = "20g",
                fat = "7g",
                fiber = "1g",
                sugar = "10g"
            ),
            category = Category.DESSERT,
            isFallback = false
        )
        
        runBlocking {
            recipeId = repository.insertRecipe(testRecipe)
        }
        
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        database.close()
    }

    /**
     * Property Test: Scaling factor changes update ingredient quantities correctly
     * 
     * Tests that changing scaling factor (1.0x, 1.5x, 2.0x) updates ingredient quantities
     * according to the scaling calculation.
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirement 3.3 - Scaling factor changes continue to work
     */
    @Test
    fun testScalingFactorChanges_UpdateIngredientQuantities() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // When - User changes scaling factor to 1.5x
        onView(withId(R.id.scaling1_5xRadioButton)).perform(click())

        // Then - Ingredients should be scaled by 1.5x
        // Original: 2.0 cups Flour -> Scaled: 3.0 cups Flour
        onView(withText(containsString("3"))).check(matches(isDisplayed()))
        onView(withText(containsString("cups Flour"))).check(matches(isDisplayed()))

        // When - User changes scaling factor to 2.0x
        onView(withId(R.id.scaling2xRadioButton)).perform(click())

        // Then - Ingredients should be scaled by 2.0x
        // Original: 2.0 cups Flour -> Scaled: 4.0 cups Flour
        onView(withText(containsString("4"))).check(matches(isDisplayed()))
        onView(withText(containsString("cups Flour"))).check(matches(isDisplayed()))

        // When - User changes scaling factor back to 1.0x
        onView(withId(R.id.scaling1xRadioButton)).perform(click())

        // Then - Ingredients should return to original quantities
        // Original: 2.0 cups Flour
        onView(withText(containsString("2"))).check(matches(isDisplayed()))
        onView(withText(containsString("cups Flour"))).check(matches(isDisplayed()))

        scenario.close()
    }

    /**
     * Property Test: Category changes via spinner update recipe category
     * 
     * Tests that changing the category via the spinner updates the recipe category
     * and persists the change to the database.
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirement 3.4 - Category changes continue to work
     */
    @Test
    fun testCategoryChanges_UpdateRecipeCategory() {
        // Given - RecipeDetailActivity is displayed with DESSERT category
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // When - User changes category to BREAKFAST (position 0)
        onView(withId(R.id.categorySpinner)).perform(click())
        onData(anything()).atPosition(0).perform(click())

        // Then - Category should be updated in database
        runBlocking {
            val updatedRecipe = repository.getRecipeByIdSync(recipeId)
            assert(updatedRecipe?.category == Category.BREAKFAST) {
                "Expected category to be BREAKFAST, but was ${updatedRecipe?.category}"
            }
        }

        // When - User changes category to LUNCH (position 1)
        onView(withId(R.id.categorySpinner)).perform(click())
        onData(anything()).atPosition(1).perform(click())

        // Then - Category should be updated in database
        runBlocking {
            val updatedRecipe = repository.getRecipeByIdSync(recipeId)
            assert(updatedRecipe?.category == Category.LUNCH) {
                "Expected category to be LUNCH, but was ${updatedRecipe?.category}"
            }
        }

        scenario.close()
    }

    /**
     * Property Test: Delete button shows confirmation dialog
     * 
     * Tests that clicking the delete button displays a confirmation dialog
     * with the recipe name and confirm/cancel actions.
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirement 3.2 - Delete button continues to show confirmation dialog
     */
    @Test
    fun testDeleteButton_ShowsConfirmationDialog() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // When - User clicks delete button
        onView(withId(R.id.deleteButton)).perform(click())

        // Then - Confirmation dialog should be displayed
        onView(withText(containsString("Chocolate Chip Cookies")))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        // And - Dialog should have confirm and cancel buttons
        onView(withText(R.string.delete_confirm))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        
        onView(withText(R.string.delete_cancel))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        // Cleanup - Cancel the dialog
        onView(withText(R.string.delete_cancel))
            .inRoot(isDialog())
            .perform(click())

        scenario.close()
    }

    /**
     * Property Test: Original recipe link opens in browser
     * 
     * Tests that clicking the original recipe link opens the URL in a web browser
     * using Intent.ACTION_VIEW.
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirement 3.5 - Original recipe link continues to open in browser
     */
    @Test
    fun testOriginalRecipeLink_OpensInBrowser() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // When - User clicks the original recipe link
        onView(withId(R.id.originalLinkTextView)).perform(click())

        // Then - Intent with ACTION_VIEW should be launched with the URL
        Intents.intended(allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData("https://example.com/cookies")
        ))

        scenario.close()
    }

    /**
     * Property Test: View mode displays all recipe data correctly
     * 
     * Tests that all recipe data is displayed correctly in view mode:
     * - Recipe name
     * - Yield and serving size
     * - Ingredients with quantity, unit, and name
     * - Instructions as numbered steps
     * - Nutrition information
     * - Original recipe link
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirements 3.1, 3.6, 3.7, 3.8, 3.9 - View mode display continues to work
     */
    @Test
    fun testViewMode_DisplaysAllRecipeDataCorrectly() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // Then - Recipe name should be displayed
        onView(withId(R.id.recipeNameTextView))
            .check(matches(withText("Chocolate Chip Cookies")))

        // And - Yield should be displayed
        onView(withId(R.id.yieldTextView))
            .check(matches(withText(containsString("24 cookies"))))

        // And - Serving size should be displayed
        onView(withId(R.id.servingSizeTextView))
            .check(matches(withText(containsString("2 cookies"))))

        // And - Ingredients should be displayed with quantity, unit, and name
        onView(withText(containsString("2 cups Flour")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("1 cup Sugar")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("0.5 cup Butter")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("2 whole Eggs")))
            .check(matches(isDisplayed()))

        // And - Instructions should be displayed as numbered steps
        onView(withText(containsString("1. Preheat oven to 350F")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("2. Mix dry ingredients")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("3. Add wet ingredients")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("4. Bake for 12 minutes")))
            .check(matches(isDisplayed()))

        // And - Nutrition information should be displayed
        onView(withText(containsString("Calories: 150")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("Protein: 2g")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("Carbohydrates: 20g")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("Fat: 7g")))
            .check(matches(isDisplayed()))

        // And - Original recipe link should be displayed
        onView(withId(R.id.originalLinkTextView))
            .check(matches(withText("https://example.com/cookies")))

        scenario.close()
    }

    /**
     * Property Test: Ingredients display in correct order with all details
     * 
     * Tests that ingredients are displayed in the order specified by the recipe
     * and include quantity, unit, and name for each ingredient.
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirement 3.6 - Ingredient display continues to work correctly
     */
    @Test
    fun testIngredients_DisplayInCorrectOrderWithAllDetails() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // Then - All ingredients should be displayed with quantity, unit, and name
        // Order: Flour (0), Sugar (1), Butter (2), Eggs (3)
        onView(withText(containsString("2 cups Flour")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("1 cup Sugar")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("0.5 cup Butter")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("2 whole Eggs")))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    /**
     * Property Test: Instructions display as numbered sequential steps
     * 
     * Tests that instructions are displayed in sequential order as numbered steps.
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirement 3.7 - Instruction display continues to work correctly
     */
    @Test
    fun testInstructions_DisplayAsNumberedSequentialSteps() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // Then - Instructions should be displayed as numbered steps in order
        onView(withText("1. Preheat oven to 350F"))
            .check(matches(isDisplayed()))
        onView(withText("2. Mix dry ingredients"))
            .check(matches(isDisplayed()))
        onView(withText("3. Add wet ingredients"))
            .check(matches(isDisplayed()))
        onView(withText("4. Bake for 12 minutes"))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    /**
     * Property Test: Nutrition information displays correctly or shows unavailable message
     * 
     * Tests that nutrition information is displayed when available, or shows
     * "unavailable" message when not available.
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirement 3.8 - Nutrition display continues to work correctly
     */
    @Test
    fun testNutritionInfo_DisplaysCorrectlyOrShowsUnavailable() {
        // Given - RecipeDetailActivity is displayed with nutrition info
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // Then - Nutrition information should be displayed
        onView(withText(containsString("Calories: 150")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("Protein: 2g")))
            .check(matches(isDisplayed()))

        scenario.close()

        // Given - Recipe without nutrition info
        val recipeWithoutNutrition = Recipe(
            name = "Simple Recipe",
            ingredients = listOf(Ingredient("Salt", 1.0, "tsp", 0)),
            instructions = listOf(Instruction("Add salt", 0)),
            originalUrl = null,
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            category = null,
            isFallback = false
        )
        
        val recipeId2 = runBlocking {
            repository.insertRecipe(recipeWithoutNutrition)
        }

        val intent2 = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId2)
        }
        val scenario2 = ActivityScenario.launch<RecipeDetailActivity>(intent2)

        // Then - "Nutritional data unavailable" message should be displayed
        onView(withId(R.id.nutritionInfoTextView))
            .check(matches(withText(R.string.nutrition_unavailable)))

        scenario2.close()
    }

    /**
     * Property Test: Yield and serving size display when available
     * 
     * Tests that yield and serving size are displayed when available in the recipe.
     * 
     * EXPECTED OUTCOME: PASS on unfixed code (baseline behavior)
     * EXPECTED OUTCOME: PASS on fixed code (preserved behavior)
     * 
     * Validates: Requirement 3.9 - Yield and serving size display continues to work
     */
    @Test
    fun testYieldAndServingSize_DisplayWhenAvailable() {
        // Given - RecipeDetailActivity is displayed
        val intent = Intent(context, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId)
        }
        val scenario = ActivityScenario.launch<RecipeDetailActivity>(intent)

        // Then - Yield should be displayed
        onView(withId(R.id.yieldTextView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.yieldTextView))
            .check(matches(withText(containsString("24 cookies"))))

        // And - Serving size should be displayed
        onView(withId(R.id.servingSizeTextView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.servingSizeTextView))
            .check(matches(withText(containsString("2 cookies"))))

        scenario.close()
    }
}
