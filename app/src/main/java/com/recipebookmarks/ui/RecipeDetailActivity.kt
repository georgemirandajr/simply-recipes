package com.recipebookmarks.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.recipebookmarks.R
import com.recipebookmarks.data.Category
import com.recipebookmarks.data.RecipeDatabase
import com.recipebookmarks.domain.RecipeRepositoryImpl
import com.recipebookmarks.domain.ScalingFactor
import kotlinx.coroutines.launch

/**
 * Activity for displaying detailed recipe information.
 * Displays ingredients with order preservation, showing quantity, unit, and name.
 * Displays yield and serving size information when available.
 * Displays instructions in sequential order as numbered steps.
 * Displays original recipe link as clickable element when available.
 * Supports ingredient scaling with 1.0x, 1.5x, and 2.0x factors.
 * Supports category assignment and modification.
 * 
 * Requirements: 1.2, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 8.3, 8.4, 8.5, 9.2, 9.4, 9.9
 */
class RecipeDetailActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RecipeDetailViewModel
    private lateinit var recipeNameTextView: TextView
    private lateinit var recipeNameEditText: android.widget.EditText
    private lateinit var ingredientsListLayout: LinearLayout
    private lateinit var instructionsListLayout: LinearLayout
    private lateinit var yieldTextView: TextView
    private lateinit var yieldEditText: android.widget.EditText
    private lateinit var servingSizeTextView: TextView
    private lateinit var servingSizeEditText: android.widget.EditText
    private lateinit var nutritionInfoTextView: TextView
    private lateinit var nutritionInfoEditText: android.widget.EditText
    private lateinit var originalLinkTextView: TextView
    private lateinit var scalingRadioGroup: RadioGroup
    private lateinit var scalingWarningTextView: TextView
    private lateinit var categorySpinner: Spinner
    private lateinit var editButton: android.widget.Button
    private lateinit var deleteButton: android.widget.Button
    private lateinit var saveButton: android.widget.Button
    private lateinit var cancelButton: android.widget.Button
    private lateinit var fallbackMessageTextView: TextView
    
    private var isUpdatingCategorySpinner = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)
        
        // Get recipe ID from intent extras
        val recipeId = intent.getLongExtra(RecipeListActivity.EXTRA_RECIPE_ID, -1L)
        
        // Initialize ViewModel
        val database = RecipeDatabase.getDatabase(applicationContext)
        val repository = RecipeRepositoryImpl(database.recipeDao())
        val scalingCalculator = com.recipebookmarks.domain.ScalingCalculatorImpl()
        val viewModelFactory = RecipeDetailViewModelFactory(repository, scalingCalculator, recipeId)
        viewModel = ViewModelProvider(this, viewModelFactory)[RecipeDetailViewModel::class.java]
        
        // Initialize views
        recipeNameTextView = findViewById(R.id.recipeNameTextView)
        recipeNameEditText = findViewById(R.id.recipeNameEditText)
        ingredientsListLayout = findViewById(R.id.ingredientsListLayout)
        instructionsListLayout = findViewById(R.id.instructionsListLayout)
        yieldTextView = findViewById(R.id.yieldTextView)
        yieldEditText = findViewById(R.id.yieldEditText)
        servingSizeTextView = findViewById(R.id.servingSizeTextView)
        servingSizeEditText = findViewById(R.id.servingSizeEditText)
        nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView)
        nutritionInfoEditText = findViewById(R.id.nutritionInfoEditText)
        originalLinkTextView = findViewById(R.id.originalLinkTextView)
        scalingRadioGroup = findViewById(R.id.scalingRadioGroup)
        scalingWarningTextView = findViewById(R.id.scalingWarningTextView)
        categorySpinner = findViewById(R.id.categorySpinner)
        editButton = findViewById(R.id.editButton)
        deleteButton = findViewById(R.id.deleteButton)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
        fallbackMessageTextView = findViewById(R.id.fallbackMessageTextView)
        
        // Disable Edit and Delete buttons until recipe is loaded
        // This ensures enterEditMode() has a valid recipe to work with
        editButton.isEnabled = false
        deleteButton.isEnabled = false
        
        // Set up scaling factor selector
        // Requirement 8.5: Update scaling factor selector to change ViewModel state
        setupScalingControls()
        
        // Set up category selector
        // Requirements 9.2, 9.4, 9.9: Add category selector, populate with Category enum values, show current category
        setupCategorySelector()
        
        // Set up edit and delete buttons
        // Requirements 3.3, 7.1, 8.2: Wire edit and delete buttons
        setupEditAndDeleteButtons()
        
        // Observe recipe data and update yield/serving size
        // Requirements 3.1, 3.2, 3.3: Display yield and serving size information
        lifecycleScope.launch {
            viewModel.recipe.collect { recipe ->
                recipe?.let {
                    updateRecipeNameDisplay(it)
                    displayYieldAndServingSize(it)
                    displayNutritionInfo(it)
                    displayInstructions(it)
                    displayOriginalLink(it)
                    updateCategorySpinner(it.category)
                    displayFallbackMessage(it)
                    // Enable Edit and Delete buttons once recipe is loaded
                    // This ensures the buttons are only clickable when there's a recipe to edit/delete
                    editButton.isEnabled = true
                    deleteButton.isEnabled = true
                }
            }
        }
        
        // Observe scaled ingredients and update UI
        // Requirements 2.1, 2.2, 2.3: Display ingredients with order preservation
        // Requirement 8.3: Show scaled quantities instead of original when scaling factor is not 1.0x
        lifecycleScope.launch {
            viewModel.scaledIngredients.collect { scaledIngredients ->
                displayIngredients(scaledIngredients)
            }
        }
        
        // Observe scaling factor and update UI
        // Requirement 8.4: Show scaling notification when factor is not 1.0x
        // Requirement 8.5: Display current scaling factor prominently
        lifecycleScope.launch {
            viewModel.scalingFactor.collect { scalingFactor ->
                updateScalingWarning(scalingFactor)
            }
        }
        
        // Observe edit mode state and toggle UI elements
        // Requirements 2.1, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12, 2.13, 2.14, 2.15, 2.16
        lifecycleScope.launch {
            viewModel.isEditMode.collect { isEditMode ->
                toggleEditMode(isEditMode)
            }
        }
    }
    
    /**
     * Sets up scaling controls and wires them to ViewModel.
     * Requirement 8.5: Update scaling factor selector to change ViewModel state
     */
    private fun setupScalingControls() {
        scalingRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val scalingFactor = when (checkedId) {
                R.id.scaling1xRadioButton -> ScalingFactor.SINGLE
                R.id.scaling1_5xRadioButton -> ScalingFactor.ONE_AND_HALF
                R.id.scaling2xRadioButton -> ScalingFactor.DOUBLE
                else -> ScalingFactor.SINGLE
            }
            viewModel.setScalingFactor(scalingFactor)
        }
    }
    
    /**
     * Updates the recipe name display based on edit mode.
     * Requirement 2.2: Support edit mode for recipe name
     */
    private fun updateRecipeNameDisplay(recipe: com.recipebookmarks.data.Recipe) {
        val isEditMode = viewModel.isEditMode.value
        
        if (isEditMode) {
            // Edit mode: show EditText field
            recipeNameTextView.visibility = android.view.View.GONE
            recipeNameEditText.visibility = android.view.View.VISIBLE
            recipeNameEditText.setText(recipe.name)
        } else {
            // View mode: show TextView field
            recipeNameEditText.visibility = android.view.View.GONE
            recipeNameTextView.visibility = android.view.View.VISIBLE
            recipeNameTextView.text = recipe.name
        }
    }
    
    /**
     * Sets up category selector and wires it to ViewModel.
     * Requirements 9.2, 9.4: Add category selector, populate with Category enum values
     */
    private fun setupCategorySelector() {
        // Create list of categories excluding UNCATEGORIZED (it's the default/null state)
        val categories = listOf(
            Category.BREAKFAST,
            Category.LUNCH,
            Category.DINNER,
            Category.DESSERT,
            Category.DRINK,
            Category.SAUCE,
            Category.APPETIZER,
            Category.SIDE,
            Category.SOUP,
            Category.SALAD,
            Category.UNCATEGORIZED
        )
        
        // Create display names for categories
        val categoryNames = categories.map { category ->
            when (category) {
                Category.BREAKFAST -> getString(R.string.category_breakfast)
                Category.LUNCH -> getString(R.string.category_lunch)
                Category.DINNER -> getString(R.string.category_dinner)
                Category.DESSERT -> getString(R.string.category_dessert)
                Category.DRINK -> getString(R.string.category_drink)
                Category.SAUCE -> getString(R.string.category_sauce)
                Category.APPETIZER -> getString(R.string.category_appetizer)
                Category.SIDE -> getString(R.string.category_side)
                Category.SOUP -> getString(R.string.category_soup)
                Category.SALAD -> getString(R.string.category_salad)
                Category.UNCATEGORIZED -> getString(R.string.uncategorized)
            }
        }
        
        // Set up adapter
        val adapter = ArrayAdapter(this, R.layout.spinner_item, categoryNames)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        categorySpinner.adapter = adapter
        
        // Set up selection listener
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Avoid triggering update when we're programmatically setting the spinner
                if (!isUpdatingCategorySpinner) {
                    val selectedCategory = categories[position]
                    viewModel.updateCategory(selectedCategory)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    /**
     * Updates the category spinner to show the current category selection.
     * Requirement 9.9: Show current category selection
     */
    private fun updateCategorySpinner(category: Category?) {
        isUpdatingCategorySpinner = true
        
        val selectedCategory = category ?: Category.UNCATEGORIZED
        val position = when (selectedCategory) {
            Category.BREAKFAST -> 0
            Category.LUNCH -> 1
            Category.DINNER -> 2
            Category.DESSERT -> 3
            Category.DRINK -> 4
            Category.SAUCE -> 5
            Category.APPETIZER -> 6
            Category.SIDE -> 7
            Category.SOUP -> 8
            Category.SALAD -> 9
            Category.UNCATEGORIZED -> 10
        }
        
        categorySpinner.setSelection(position)
        isUpdatingCategorySpinner = false
    }
    
    /**
     * Updates the scaling warning visibility based on the current scaling factor.
     * Requirement 8.4: Show scaling notification in instruction section when factor is not 1.0x
     */
    private fun updateScalingWarning(scalingFactor: ScalingFactor) {
        // Show warning when scaling factor is not 1.0x
        if (scalingFactor != ScalingFactor.SINGLE) {
            scalingWarningTextView.visibility = android.view.View.VISIBLE
        } else {
            scalingWarningTextView.visibility = android.view.View.GONE
        }
    }
    
    /**
     * Displays yield and serving size information when available.
     * Requirement 3.1: Display yield information when available
     * Requirement 3.2: Display serving size information when available
     * Requirement 3.3: Format yield as number of servings or quantity
     * Requirements 2.2, 2.3: Support edit mode for yield and serving size
     */
    private fun displayYieldAndServingSize(recipe: com.recipebookmarks.data.Recipe) {
        val isEditMode = viewModel.isEditMode.value
        
        if (isEditMode) {
            // Edit mode: show EditText fields
            yieldTextView.visibility = android.view.View.GONE
            servingSizeTextView.visibility = android.view.View.GONE
            
            // Display yield EditText if available
            if (recipe.yield != null && recipe.yield.isNotBlank()) {
                yieldEditText.setText(recipe.yield)
                yieldEditText.visibility = android.view.View.VISIBLE
            } else {
                yieldEditText.setText("")
                yieldEditText.visibility = android.view.View.VISIBLE
            }
            
            // Display serving size EditText if available
            if (recipe.servingSize != null && recipe.servingSize.isNotBlank()) {
                servingSizeEditText.setText(recipe.servingSize)
                servingSizeEditText.visibility = android.view.View.VISIBLE
            } else {
                servingSizeEditText.setText("")
                servingSizeEditText.visibility = android.view.View.VISIBLE
            }
        } else {
            // View mode: show TextView fields
            yieldEditText.visibility = android.view.View.GONE
            servingSizeEditText.visibility = android.view.View.GONE
            
            // Display yield information if available
            if (recipe.yield != null && recipe.yield.isNotBlank()) {
                yieldTextView.text = "Yield: ${recipe.yield}"
                yieldTextView.visibility = android.view.View.VISIBLE
            } else {
                yieldTextView.visibility = android.view.View.GONE
            }
            
            // Display serving size information if available
            if (recipe.servingSize != null && recipe.servingSize.isNotBlank()) {
                servingSizeTextView.text = "Serving Size: ${recipe.servingSize}"
                servingSizeTextView.visibility = android.view.View.VISIBLE
            } else {
                servingSizeTextView.visibility = android.view.View.GONE
            }
        }
    }
    
    /**
     * Displays ingredients in order specified by Ingredient.order field.
     * Shows quantity, unit, and name for each ingredient.
     * Requirements 2.1, 2.2, 2.3: Display ingredients with order preservation
     * Requirement 2.4: Support edit mode for ingredients
     */
    private fun displayIngredients(scaledIngredients: List<com.recipebookmarks.data.ScaledIngredient>) {
        // Clear existing ingredient views
        ingredientsListLayout.removeAllViews()
        
        // Sort ingredients by order field to preserve recipe-specified order
        // Requirement 2.3: Display ingredients in the order specified by the recipe
        val sortedIngredients = scaledIngredients.sortedBy { it.order }
        
        val isEditMode = viewModel.isEditMode.value
        
        if (isEditMode) {
            // Edit mode: create EditText fields for each ingredient
            sortedIngredients.forEach { ingredient ->
                val ingredientEditText = android.widget.EditText(this).apply {
                    setText(formatIngredient(ingredient))
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                    gravity = Gravity.START
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                    hint = "Quantity Unit Name"
                }
                ingredientsListLayout.addView(ingredientEditText)
            }
        } else {
            // View mode: create TextView fields for each ingredient
            // Requirement 2.1: Show the complete ingredients list
            sortedIngredients.forEach { ingredient ->
                val ingredientTextView = TextView(this).apply {
                    // Requirement 2.2: Display each ingredient with its quantity and unit of measurement
                    text = formatIngredient(ingredient)
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                    gravity = Gravity.START
                }
                ingredientsListLayout.addView(ingredientTextView)
            }
        }
    }
    
    /**
     * Formats an ingredient for display with quantity, unit, and name.
     * Requirement 2.2: Display each ingredient with its quantity and unit of measurement
     */
    private fun formatIngredient(ingredient: com.recipebookmarks.data.ScaledIngredient): String {
        val quantity = formatQuantity(ingredient.scaledQuantity)
        return "$quantity ${ingredient.unit} ${ingredient.name}"
    }
    
    /**
     * Formats a quantity value for display, removing unnecessary decimal places.
     */
    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            String.format("%.2f", quantity).trimEnd('0').trimEnd('.')
        }
    }
    
    /**
     * Displays nutrition information when available, or shows unavailable message.
     * Requirement 4.1: Display nutritional data when provided
     * Requirement 4.2: Indicate when nutritional data is unavailable
     * Requirement 4.3: Show calories and macronutrients
     * Requirement 2.6: Support edit mode for nutrition info
     */
    private fun displayNutritionInfo(recipe: com.recipebookmarks.data.Recipe) {
        val nutritionInfo = recipe.nutritionInfo
        val isEditMode = viewModel.isEditMode.value
        
        if (isEditMode) {
            // Edit mode: show EditText field
            nutritionInfoTextView.visibility = android.view.View.GONE
            nutritionInfoEditText.visibility = android.view.View.VISIBLE
            
            if (nutritionInfo == null) {
                nutritionInfoEditText.setText("")
            } else {
                // Format nutrition info for editing
                val nutritionText = buildString {
                    nutritionInfo.calories?.let { 
                        append("Calories: $it\n")
                    }
                    nutritionInfo.protein?.let { 
                        append("Protein: $it\n")
                    }
                    nutritionInfo.carbohydrates?.let { 
                        append("Carbohydrates: $it\n")
                    }
                    nutritionInfo.fat?.let { 
                        append("Fat: $it\n")
                    }
                    nutritionInfo.fiber?.let { 
                        append("Fiber: $it\n")
                    }
                    nutritionInfo.sugar?.let { 
                        append("Sugar: $it")
                    }
                }.trim()
                
                nutritionInfoEditText.setText(nutritionText)
            }
        } else {
            // View mode: show TextView field
            nutritionInfoEditText.visibility = android.view.View.GONE
            nutritionInfoTextView.visibility = android.view.View.VISIBLE
            
            if (nutritionInfo == null) {
                // Requirement 4.2: Show "Nutritional data unavailable" when NutritionInfo is null
                nutritionInfoTextView.text = getString(R.string.nutrition_unavailable)
            } else {
                // Requirement 4.1, 4.3: Display calories, protein, carbohydrates, fat, fiber, sugar
                val nutritionText = buildString {
                    nutritionInfo.calories?.let { 
                        append("Calories: $it\n")
                    }
                    nutritionInfo.protein?.let { 
                        append("Protein: $it\n")
                    }
                    nutritionInfo.carbohydrates?.let { 
                        append("Carbohydrates: $it\n")
                    }
                    nutritionInfo.fat?.let { 
                        append("Fat: $it\n")
                    }
                    nutritionInfo.fiber?.let { 
                        append("Fiber: $it\n")
                    }
                    nutritionInfo.sugar?.let { 
                        append("Sugar: $it")
                    }
                }.trim()
                
                // If all nutrition fields are null, show unavailable message
                if (nutritionText.isEmpty()) {
                    nutritionInfoTextView.text = getString(R.string.nutrition_unavailable)
                } else {
                    nutritionInfoTextView.text = nutritionText
                }
            }
        }
    }
    
    /**
     * Displays instructions in order specified by Instruction.order field.
     * Shows each instruction as a distinct numbered step.
     * 
     * Requirement 5.1: Show all preparation instructions
     * Requirement 5.2: Display instructions in sequential order
     * Requirement 5.3: Display each instruction as a distinct step
     * Requirement 2.5: Support edit mode for instructions
     */
    private fun displayInstructions(recipe: com.recipebookmarks.data.Recipe) {
        // Clear existing instruction views
        instructionsListLayout.removeAllViews()
        
        // Sort instructions by order field to preserve sequential order
        // Requirement 5.2: Display instructions in sequential order
        val sortedInstructions = recipe.instructions.sortedBy { it.order }
        
        val isEditMode = viewModel.isEditMode.value
        
        if (isEditMode) {
            // Edit mode: create EditText fields for each instruction
            sortedInstructions.forEachIndexed { index, instruction ->
                val instructionEditText = android.widget.EditText(this).apply {
                    setText(instruction.text)
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                    gravity = Gravity.START
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    hint = "Step ${index + 1}"
                    minLines = 2
                }
                instructionsListLayout.addView(instructionEditText)
            }
        } else {
            // View mode: create TextView fields for each instruction
            // Requirement 5.1: Show all preparation instructions
            // Requirement 5.3: Display each instruction as a distinct step
            sortedInstructions.forEachIndexed { index, instruction ->
                val instructionTextView = TextView(this).apply {
                    // Display as numbered step (1., 2., 3., etc.)
                    text = "${index + 1}. ${instruction.text}"
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                    gravity = Gravity.START
                }
                instructionsListLayout.addView(instructionTextView)
            }
        }
    }
    
    /**
     * Displays original recipe link as clickable element when available.
     * Opens URL in browser when clicked.
     * 
     * Requirement 6.1: Display original recipe link when available
     * Requirement 6.2: Open URL in web browser when selected
     * Requirement 6.3: Display link as clickable element
     */
    private fun displayOriginalLink(recipe: com.recipebookmarks.data.Recipe) {
        // Requirement 6.1: Show the original recipe link when not null
        if (recipe.originalUrl != null && recipe.originalUrl.isNotBlank()) {
            // Requirement 6.3: Display as clickable element
            originalLinkTextView.text = recipe.originalUrl
            originalLinkTextView.visibility = android.view.View.VISIBLE
            
            // Requirement 6.2: Open URL in browser using Intent.ACTION_VIEW
            originalLinkTextView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recipe.originalUrl))
                startActivity(intent)
            }
        } else {
            // Hide the link if no URL is available
            originalLinkTextView.visibility = android.view.View.GONE
        }
    }
    
    /**
     * Sets up edit and delete button click listeners.
     * Requirements 3.3, 7.1, 8.2: Wire edit and delete buttons
     * Requirements 2.1, 2.7, 2.8, 2.9: Enable in-place editing mode
     */
    private fun setupEditAndDeleteButtons() {
        // Requirement 2.1: Edit button enables in-place editing mode
        editButton.setOnClickListener {
            viewModel.enterEditMode()
        }
        
        // Requirement 8.2: Delete button shows confirmation dialog
        deleteButton.setOnClickListener {
            viewModel.recipe.value?.let { recipe ->
                showDeleteConfirmation(recipe)
            }
        }
        
        // Requirement 2.10: Save button persists changes
        saveButton.setOnClickListener {
            val updatedRecipe = collectEditedRecipeData()
            if (updatedRecipe != null) {
                viewModel.updateRecipe(updatedRecipe)
                viewModel.saveChanges()
            }
        }
        
        // Requirement 2.13: Cancel button discards changes
        cancelButton.setOnClickListener {
            viewModel.cancelChanges()
        }
    }
    
    /**
     * Displays fallback message when recipe is a fallback recipe.
     * Requirement 3.3: Display a message indicating this is a fallback recipe with limited data
     */
    private fun displayFallbackMessage(recipe: com.recipebookmarks.data.Recipe) {
        if (recipe.isFallback) {
            fallbackMessageTextView.visibility = android.view.View.VISIBLE
        } else {
            fallbackMessageTextView.visibility = android.view.View.GONE
        }
    }
    
    /**
     * Toggles UI elements based on edit mode state.
     * Requirements 2.8, 2.9, 2.11, 2.12, 2.15, 2.16: Toggle buttons and fields
     */
    private fun toggleEditMode(isEditMode: Boolean) {
        if (isEditMode) {
            // Edit mode: show Save/Cancel, hide Edit/Delete
            saveButton.visibility = android.view.View.VISIBLE
            cancelButton.visibility = android.view.View.VISIBLE
            editButton.visibility = android.view.View.GONE
            deleteButton.visibility = android.view.View.GONE
        } else {
            // View mode: show Edit/Delete, hide Save/Cancel
            editButton.visibility = android.view.View.VISIBLE
            deleteButton.visibility = android.view.View.VISIBLE
            saveButton.visibility = android.view.View.GONE
            cancelButton.visibility = android.view.View.GONE
        }
        
        // Trigger UI updates for all fields
        viewModel.recipe.value?.let { recipe ->
            updateRecipeNameDisplay(recipe)
            displayYieldAndServingSize(recipe)
            displayNutritionInfo(recipe)
            displayInstructions(recipe)
        }
        
        // Trigger ingredient display update
        viewModel.scaledIngredients.value.let { scaledIngredients ->
            displayIngredients(scaledIngredients)
        }
    }
    
    /**
     * Shows confirmation dialog for recipe deletion.
     * Requirements 8.3, 8.4, 8.5, 8.6, 8.9: Show confirmation dialog with recipe name
     */
    private fun showDeleteConfirmation(recipe: com.recipebookmarks.data.Recipe) {
        // Requirement 8.3: Display a confirmation dialog when user clicks delete button
        // Requirement 8.4: Display the recipe name being deleted in confirmation dialog
        val message = getString(R.string.delete_confirmation_message, recipe.name)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(message)
            // Requirement 8.5: Provide a confirm action in confirmation dialog
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                // Requirement 8.7: Remove the Recipe_Bookmark from database when user confirms deletion
                viewModel.deleteRecipe()
                // Requirement 8.9: Navigate user back to Recipe_List_View when recipe is deleted
                finish()
            }
            // Requirement 8.6: Provide a cancel action in confirmation dialog
            .setNegativeButton(R.string.delete_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * Collects edited recipe data from EditText fields and constructs an updated Recipe object.
     * Parses ingredients, instructions, and nutrition info from EditText fields.
     * Validates required fields and numeric values.
     * 
     * Requirements 2.10, 2.13: Collect and validate edited recipe data
     * 
     * @return Updated Recipe object if validation succeeds, null if validation fails
     */
    private fun collectEditedRecipeData(): com.recipebookmarks.data.Recipe? {
        val currentRecipe = viewModel.recipe.value ?: return null
        
        // Collect recipe name
        val recipeName = recipeNameEditText.text.toString().trim()
        if (recipeName.isEmpty()) {
            android.widget.Toast.makeText(this, "Recipe name cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
            return null
        }
        
        // Collect yield
        val yieldText = yieldEditText.text.toString().trim()
        val recipeYield = if (yieldText.isNotEmpty()) yieldText else null
        
        // Collect serving size
        val servingSizeText = servingSizeEditText.text.toString().trim()
        val recipeServingSize = if (servingSizeText.isNotEmpty()) servingSizeText else null
        
        // Parse ingredients from EditText fields
        val ingredients = parseIngredients()
        if (ingredients == null) {
            android.widget.Toast.makeText(this, "Invalid ingredient format", android.widget.Toast.LENGTH_SHORT).show()
            return null
        }
        
        // Parse instructions from EditText fields
        val instructions = parseInstructions()
        if (instructions == null) {
            android.widget.Toast.makeText(this, "Invalid instruction format", android.widget.Toast.LENGTH_SHORT).show()
            return null
        }
        
        // Parse nutrition info from EditText field
        val nutritionInfo = parseNutritionInfo()
        
        // Construct updated Recipe object
        return currentRecipe.copy(
            name = recipeName,
            yield = recipeYield,
            servingSize = recipeServingSize,
            ingredients = ingredients,
            instructions = instructions,
            nutritionInfo = nutritionInfo,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Parses ingredients from EditText fields in the ingredients layout.
     * Expected format: "quantity unit name" (e.g., "2 cups flour")
     * 
     * @return List of Ingredient objects if parsing succeeds, null if parsing fails
     */
    private fun parseIngredients(): List<com.recipebookmarks.data.Ingredient>? {
        val ingredients = mutableListOf<com.recipebookmarks.data.Ingredient>()
        
        for (i in 0 until ingredientsListLayout.childCount) {
            val child = ingredientsListLayout.getChildAt(i)
            if (child is android.widget.EditText) {
                val ingredientText = child.text.toString().trim()
                if (ingredientText.isEmpty()) {
                    continue // Skip empty ingredient fields
                }
                
                // Parse ingredient text: "quantity unit name"
                val parts = ingredientText.split(" ", limit = 3)
                if (parts.size < 3) {
                    return null // Invalid format
                }
                
                val quantityStr = parts[0]
                val unit = parts[1]
                val name = parts[2]
                
                // Validate quantity is a valid number
                val quantity = quantityStr.toDoubleOrNull()
                if (quantity == null) {
                    return null // Invalid quantity
                }
                
                ingredients.add(
                    com.recipebookmarks.data.Ingredient(
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        order = i
                    )
                )
            }
        }
        
        return ingredients
    }
    
    /**
     * Parses instructions from EditText fields in the instructions layout.
     * Each EditText represents one instruction step.
     * 
     * @return List of Instruction objects if parsing succeeds, null if parsing fails
     */
    private fun parseInstructions(): List<com.recipebookmarks.data.Instruction>? {
        val instructions = mutableListOf<com.recipebookmarks.data.Instruction>()
        
        for (i in 0 until instructionsListLayout.childCount) {
            val child = instructionsListLayout.getChildAt(i)
            if (child is android.widget.EditText) {
                val instructionText = child.text.toString().trim()
                if (instructionText.isEmpty()) {
                    continue // Skip empty instruction fields
                }
                
                instructions.add(
                    com.recipebookmarks.data.Instruction(
                        text = instructionText,
                        order = i
                    )
                )
            }
        }
        
        return instructions
    }
    
    /**
     * Parses nutrition info from EditText field.
     * Expected format: Multi-line text with "Label: Value" pairs
     * Example:
     *   Calories: 250
     *   Protein: 10g
     *   Carbohydrates: 30g
     *   Fat: 5g
     *   Fiber: 3g
     *   Sugar: 8g
     * 
     * @return NutritionInfo object if parsing succeeds, null if all fields are empty
     */
    private fun parseNutritionInfo(): com.recipebookmarks.data.NutritionInfo? {
        val nutritionText = nutritionInfoEditText.text.toString().trim()
        if (nutritionText.isEmpty()) {
            return null
        }
        
        var calories: Int? = null
        var protein: String? = null
        var carbohydrates: String? = null
        var fat: String? = null
        var fiber: String? = null
        var sugar: String? = null
        
        // Parse each line
        val lines = nutritionText.split("\n")
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            // Split by colon
            val parts = trimmedLine.split(":", limit = 2)
            if (parts.size != 2) continue
            
            val label = parts[0].trim().lowercase()
            val value = parts[1].trim()
            
            when {
                label.contains("calorie") -> {
                    calories = value.toIntOrNull()
                }
                label.contains("protein") -> {
                    protein = value
                }
                label.contains("carbohydrate") -> {
                    carbohydrates = value
                }
                label.contains("fat") -> {
                    fat = value
                }
                label.contains("fiber") -> {
                    fiber = value
                }
                label.contains("sugar") -> {
                    sugar = value
                }
            }
        }
        
        // Return null if all fields are null
        if (calories == null && protein == null && carbohydrates == null && 
            fat == null && fiber == null && sugar == null) {
            return null
        }
        
        return com.recipebookmarks.data.NutritionInfo(
            calories = calories,
            protein = protein,
            carbohydrates = carbohydrates,
            fat = fat,
            fiber = fiber,
            sugar = sugar
        )
    }
}
