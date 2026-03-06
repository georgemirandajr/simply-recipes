package com.recipebookmarks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.recipebookmarks.R
import com.recipebookmarks.data.Recipe

class RecipeListAdapter(
    private val onRecipeClick: (Recipe) -> Unit,
    private val onDeleteClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeListAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view, onRecipeClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(
        itemView: View,
        private val onRecipeClick: (Recipe) -> Unit,
        private val onDeleteClick: (Recipe) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val recipeName: TextView = itemView.findViewById(R.id.recipeName)
        private val categoryTag: TextView = itemView.findViewById(R.id.categoryTag)
        private val fallbackIndicator: TextView = itemView.findViewById(R.id.fallbackIndicator)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)

        fun bind(recipe: Recipe) {
            recipeName.text = recipe.name
            
            // Display category tag or "Uncategorized" if no category
            categoryTag.text = recipe.category?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                ?: itemView.context.getString(R.string.uncategorized)
            
            // Show fallback indicator if this is a fallback recipe
            if (recipe.isFallback) {
                fallbackIndicator.visibility = View.VISIBLE
                fallbackIndicator.text = itemView.context.getString(R.string.fallback_indicator)
            } else {
                fallbackIndicator.visibility = View.GONE
            }
            
            // Set up delete button click listener
            deleteButton.setOnClickListener {
                onDeleteClick(recipe)
            }
            
            itemView.setOnClickListener {
                onRecipeClick(recipe)
            }
        }
    }

    private class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}
