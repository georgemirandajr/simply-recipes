package com.recipebookmarks.ui

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.recipebookmarks.R
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.data.RecipeDatabase
import com.recipebookmarks.domain.RecipeRepositoryImpl
import kotlinx.coroutines.launch

/**
 * Activity for editing recipe details.
 * Allows editing recipe name for all recipes.
 * For fallback recipes (isFallback=true), allows editing all fields including ingredients and instructions.
 * For non-fallback recipes (isFallback=false), only allows editing the recipe name.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12, 7.13
 */
class RecipeEditorActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RecipeEditorViewModel
    private lateinit var recipeNameEditText: EditText
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var instructionsContainer: LinearLayout
    private lateinit var addIngredientButton: Button
    private lateinit var addInstructionButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    
    private var currentRecipe: Recipe? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_editor)
        
        // Get recipe ID from intent extras
        val recipeId = intent.getLongExtra(EXTRA_RECIPE_ID, -1L)
        
        // Initialize ViewModel
        val database = RecipeDatabase.getDatabase(applicationContext)
        val repository = RecipeRepositoryImpl(database.recipeDao())
        val viewModelFactory = RecipeEditorViewModelFactory(repository, recipeId)
        viewModel = ViewModelProvider(this, viewModelFactory)[RecipeEditorViewModel::class.java]
        
        // Initialize views
        recipeNameEditText = findViewById(R.id.recipeNameEditText)
        ingredientsContainer = findViewById(R.id.ingredientsContainer)
        instructionsContainer = findViewById(R.id.instructionsContainer)
        addIngredientButton = findViewById(R.id.addIngredientButton)
        addInstructionButton = findViewById(R.id.addInstructionButton)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
        
        // Observe recipe and populate fields
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipe.collect { recipe ->
                    recipe?.let {
                        currentRecipe = it
                        populateFields(it)
                        configureEditingRestrictions(it)
                    }
                }
            }
        }
        
        // Set up button listeners
        addIngredientButton.setOnClickListener { addIngredientField() }
        addInstructionButton.setOnClickListener { addInstructionField() }
        saveButton.setOnClickListener { saveRecipe() }
        cancelButton.setOnClickListener { finish() }
    }
    
    /**
     * Populates all fields with recipe data.
     * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
     */
    private fun populateFields(recipe: Recipe) {
        // Set recipe name
        recipeNameEditText.setText(recipe.name)
        
        // Clear existing fields
        ingredientsContainer.removeAllViews()
        instructionsContainer.removeAllViews()
        
        // Populate ingredients
        recipe.ingredients.sortedBy { it.order }.forEach { ingredient ->
            addIngredientField(ingredient)
        }
        
        // Populate instructions
        recipe.instructions.sortedBy { it.order }.forEach { instruction ->
            addInstructionField(instruction)
        }
    }
    
    /**
     * Configures which fields are editable based on whether this is a fallback recipe.
     * Fallback recipes: all fields editable
     * Non-fallback recipes: only name editable
     * 
     * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.13
     */
    private fun configureEditingRestrictions(recipe: Recipe) {
        if (recipe.isFallback) {
            // Enable all fields for fallback recipes
            recipeNameEditText.isEnabled = true
            addIngredientButton.visibility = View.VISIBLE
            addInstructionButton.visibility = View.VISIBLE
            
            // Enable all ingredient fields
            for (i in 0 until ingredientsContainer.childCount) {
                val ingredientLayout = ingredientsContainer.getChildAt(i) as LinearLayout
                enableAllChildViews(ingredientLayout, true)
            }
            
            // Enable all instruction fields
            for (i in 0 until instructionsContainer.childCount) {
                val instructionLayout = instructionsContainer.getChildAt(i) as LinearLayout
                enableAllChildViews(instructionLayout, true)
            }
        } else {
            // Only allow name editing for non-fallback recipes
            recipeNameEditText.isEnabled = true
            addIngredientButton.visibility = View.GONE
            addInstructionButton.visibility = View.GONE
            
            // Disable all ingredient fields
            for (i in 0 until ingredientsContainer.childCount) {
                val ingredientLayout = ingredientsContainer.getChildAt(i) as LinearLayout
                enableAllChildViews(ingredientLayout, false)
            }
            
            // Disable all instruction fields
            for (i in 0 until instructionsContainer.childCount) {
                val instructionLayout = instructionsContainer.getChildAt(i) as LinearLayout
                enableAllChildViews(instructionLayout, false)
            }
        }
    }
    
    /**
     * Helper method to enable/disable all child views in a layout.
     */
    private fun enableAllChildViews(layout: LinearLayout, enabled: Boolean) {
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            child.isEnabled = enabled
            if (child is LinearLayout) {
                enableAllChildViews(child, enabled)
            }
        }
    }
    
    /**
     * Adds a new ingredient field or populates with existing ingredient data.
     * Creates EditText fields for name, quantity, and unit.
     * Adds a remove button for each ingredient.
     * 
     * Requirements: 7.6, 7.7, 7.8
     */
    private fun addIngredientField(ingredient: Ingredient? = null) {
        val ingredientLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }
        
        // Ingredient name field
        val nameLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        val nameLabel = TextView(this).apply {
            text = "Ingredient:"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.3f
            )
        }
        
        val nameEditText = EditText(this).apply {
            hint = "Name"
            setText(ingredient?.name ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.7f
            )
            tag = "ingredient_name"
        }
        
        nameLayout.addView(nameLabel)
        nameLayout.addView(nameEditText)
        
        // Quantity and unit fields
        val quantityUnitLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
        }
        
        val quantityEditText = EditText(this).apply {
            hint = "Quantity"
            setText(ingredient?.quantity?.toString() ?: "")
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.4f
            )
            tag = "ingredient_quantity"
        }
        
        val unitEditText = EditText(this).apply {
            hint = "Unit"
            setText(ingredient?.unit ?: "")
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.4f
            )
            tag = "ingredient_unit"
        }
        
        val removeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_delete)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                ingredientsContainer.removeView(ingredientLayout)
            }
        }
        
        quantityUnitLayout.addView(quantityEditText)
        quantityUnitLayout.addView(unitEditText)
        quantityUnitLayout.addView(removeButton)
        
        ingredientLayout.addView(nameLayout)
        ingredientLayout.addView(quantityUnitLayout)
        
        ingredientsContainer.addView(ingredientLayout)
    }
    
    /**
     * Adds a new instruction field or populates with existing instruction data.
     * Creates EditText field for instruction text.
     * Adds a remove button for each instruction.
     * 
     * Requirements: 7.9, 7.10, 7.11
     */
    private fun addInstructionField(instruction: Instruction? = null) {
        val instructionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }
        
        val instructionEditText = EditText(this).apply {
            hint = "Instruction step"
            setText(instruction?.text ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            gravity = Gravity.TOP
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            tag = "instruction_text"
        }
        
        val removeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_delete)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                instructionsContainer.removeView(instructionLayout)
            }
        }
        
        instructionLayout.addView(instructionEditText)
        instructionLayout.addView(removeButton)
        
        instructionsContainer.addView(instructionLayout)
    }
    
    /**
     * Collects data from fields, validates, and saves the recipe.
     * Requirements: 7.12
     */
    private fun saveRecipe() {
        val recipe = currentRecipe ?: return
        
        // Collect recipe name
        val recipeName = recipeNameEditText.text.toString().trim()
        
        // Validate recipe name
        if (recipeName.isEmpty()) {
            Toast.makeText(this, "Recipe name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Collect ingredients
        val ingredients = mutableListOf<Ingredient>()
        for (i in 0 until ingredientsContainer.childCount) {
            val ingredientLayout = ingredientsContainer.getChildAt(i) as LinearLayout
            
            // Get name from first child (nameLayout)
            val nameLayout = ingredientLayout.getChildAt(0) as LinearLayout
            val nameEditText = nameLayout.findViewWithTag<EditText>("ingredient_name")
            val name = nameEditText?.text?.toString()?.trim() ?: ""
            
            // Get quantity and unit from second child (quantityUnitLayout)
            val quantityUnitLayout = ingredientLayout.getChildAt(1) as LinearLayout
            val quantityEditText = quantityUnitLayout.findViewWithTag<EditText>("ingredient_quantity")
            val unitEditText = quantityUnitLayout.findViewWithTag<EditText>("ingredient_unit")
            
            val quantityStr = quantityEditText?.text?.toString()?.trim() ?: ""
            val unit = unitEditText?.text?.toString()?.trim() ?: ""
            
            // Validate ingredient fields
            if (name.isEmpty()) {
                Toast.makeText(this, "Ingredient name cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (quantityStr.isEmpty()) {
                Toast.makeText(this, "Ingredient quantity cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }
            
            val quantity = quantityStr.toDoubleOrNull()
            if (quantity == null || quantity <= 0) {
                Toast.makeText(this, "Invalid ingredient quantity", Toast.LENGTH_SHORT).show()
                return
            }
            
            ingredients.add(Ingredient(name, quantity, unit, i))
        }
        
        // Collect instructions
        val instructions = mutableListOf<Instruction>()
        for (i in 0 until instructionsContainer.childCount) {
            val instructionLayout = instructionsContainer.getChildAt(i) as LinearLayout
            val instructionEditText = instructionLayout.findViewWithTag<EditText>("instruction_text")
            val text = instructionEditText?.text?.toString()?.trim() ?: ""
            
            // Validate instruction text
            if (text.isEmpty()) {
                Toast.makeText(this, "Instruction text cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }
            
            instructions.add(Instruction(text, i))
        }
        
        // Create updated recipe
        val updatedRecipe = recipe.copy(
            name = recipeName,
            ingredients = ingredients,
            instructions = instructions,
            updatedAt = System.currentTimeMillis()
        )
        
        // Save recipe
        viewModel.saveRecipe(updatedRecipe)
        
        // Navigate back
        finish()
    }
    
    companion object {
        const val EXTRA_RECIPE_ID = "recipe_id"
    }
}
