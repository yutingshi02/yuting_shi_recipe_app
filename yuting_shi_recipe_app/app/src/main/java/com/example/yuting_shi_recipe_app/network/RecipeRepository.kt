package com.example.yuting_shi_recipe_app.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class RecipeRepository(private val apiService: SpoonacularApiService) {

    fun searchRecipes(
        query: String,
        cuisine: String? = null,
        diet: String? = null,
        maxCalories: Int? = null,
        offset: Int = 0
    ): Flow<RecipeResponse> = flow {
        val response = apiService.searchRecipes(query, cuisine, diet, maxCalories)
        emit(response)
    }.catch { e ->
        e.printStackTrace()
    }

    suspend fun getRecipeById(id: String): Recipe? {
        return try {
            val response = apiService.getRecipeById(id)
            println("Recipe details response: $response")
            response
        } catch (e: Exception) {
            println("Error fetching recipe details: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}