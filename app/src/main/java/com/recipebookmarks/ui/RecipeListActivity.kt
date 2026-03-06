package com.recipebookmarks.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.recipebookmarks.R
import com.recipebookmarks.data.Category
import com.recipebookmarks.data.RecipeDatabase
import com.recipebookmarks.domain.RecipeRepositoryImpl
import kotlinx.coroutines.launch

class RecipeListActivity : AppCompatActivity() {
    
    private lateinit var searchInput: EditText
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var emptyResultsText: android.widget.TextView
    private lateinit var adapter: RecipeListAdapter

    // Initialize ViewModel with repository
    private val viewModel: RecipeListViewModel by viewModels {
        val database = RecipeDatabase.getDatabase(applicationContext)
        val repository = RecipeRepositoryImpl(database.recipeDao())
        RecipeListViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_list)

        // Initialize views
        searchInput = findViewById(R.id.searchInput)
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView)
        emptyResultsText = findViewById(R.id.emptyResultsText)

        // Set up RecyclerView
        recipeRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Set up RecyclerView adapter with click listener
        adapter = RecipeListAdapter { recipe ->
            // Navigate to RecipeDetailActivity with recipe ID
            val intent = Intent(this, RecipeDetailActivity::class.java).apply {
                putExtra(EXTRA_RECIPE_ID, recipe.id)
            }
            startActivity(intent)
        }
        recipeRecyclerView.adapter = adapter

        // Set up category filter spinner
        setupCategoryFilter()

        // Set up search input listener
        setupSearchInput()

        // Observe recipe list from ViewModel
        observeRecipes()
    }

    private fun setupCategoryFilter() {
        // Create list of categories with "All Categories" option
        val categories = mutableListOf(getString(R.string.all_categories))
        categories.addAll(Category.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } })

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        categoryFilterSpinner.adapter = spinnerAdapter

        // Add spinner selection listener to filter recipes
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    // "All Categories" selected - clear filter
                    viewModel.clearCategoryFilter()
                } else {
                    // Specific category selected
                    val category = Category.values()[position - 1]
                    viewModel.setCategoryFilter(category)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.clearCategoryFilter()
            }
        }
    }

    private fun setupSearchInput() {
        searchInput.addTextChangedListener { text ->
            // Update ViewModel search query
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun observeRecipes() {
        // Observe recipe list from ViewModel and update RecyclerView
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipes.collect { recipes ->
                    adapter.submitList(recipes)
                    
                    // Show/hide empty results message
                    if (recipes.isEmpty()) {
                        emptyResultsText.visibility = View.VISIBLE
                        recipeRecyclerView.visibility = View.GONE
                    } else {
                        emptyResultsText.visibility = View.GONE
                        recipeRecyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id"
    }
}
