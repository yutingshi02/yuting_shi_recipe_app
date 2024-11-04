package com.example.yuting_shi_recipe_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yuting_shi_recipe_app.network.Recipe
import com.example.yuting_shi_recipe_app.network.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> get() = _recipes

    private val _lastQuery = MutableStateFlow("Birthday Cake")
    val lastQuery: StateFlow<String> = _lastQuery

    private val _lastSelectedCuisines = MutableStateFlow<Set<String>>(emptySet())
    val lastSelectedCuisines: StateFlow<Set<String>> = _lastSelectedCuisines

    private val _lastSelectedDiets = MutableStateFlow<Set<String>>(emptySet())
    val lastSelectedDiets: StateFlow<Set<String>> = _lastSelectedDiets

    private val _lastMaxCalories = MutableStateFlow(2000)
    val lastMaxCalories: StateFlow<Int> = _lastMaxCalories

    private var currentOffset = 0
    private var isLoading = false

    init {
        searchRecipes("Birthday Cake", reset = true)
    }

    fun updateLastQuery(query: String) {
        _lastQuery.value = query
    }

    fun updateLastSelectedCuisines(cuisines: Set<String>) {
        _lastSelectedCuisines.value = cuisines
    }

    fun updateLastSelectedDiets(diets: Set<String>) {
        _lastSelectedDiets.value = diets
    }

    fun updateLastMaxCalories(calories: Int) {
        _lastMaxCalories.value = calories
    }

    fun searchRecipes(
        query: String,
        cuisine: String? = null,
        diet: String? = null,
        maxCalories: Int? = null,
        reset: Boolean = false
    ) {
        if (isLoading) return

        if (reset) {
            currentOffset = 0
            _recipes.value = emptyList()

            // Update last search state
            updateLastQuery(query)
            updateLastSelectedCuisines(cuisine?.split(",")?.toSet() ?: emptySet())
            updateLastSelectedDiets(diet?.split(",")?.toSet() ?: emptySet())
            updateLastMaxCalories(maxCalories ?: 2000)
        }

        viewModelScope.launch {
            isLoading = true
            repository.searchRecipes(query, cuisine, diet, maxCalories, currentOffset).collect { response ->
                _recipes.value = _recipes.value + response.recipes
                currentOffset += response.recipes.size
            }
            isLoading = false
        }
    }

    fun getRecipeById(id: String, onResult: (Recipe?) -> Unit) {
        viewModelScope.launch {
            val recipe = repository.getRecipeById(id)
            onResult(recipe)
        }
    }
}