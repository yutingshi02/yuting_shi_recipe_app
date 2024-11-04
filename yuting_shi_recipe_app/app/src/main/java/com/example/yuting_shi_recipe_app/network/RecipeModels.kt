package com.example.yuting_shi_recipe_app.network

import com.squareup.moshi.Json

data class RecipeResponse(
    @Json(name = "results") val recipes: List<Recipe>
)

data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val instructions: String?,
    @Json(name = "extendedIngredients") val ingredients: List<Ingredient>?,
    @Json(name = "analyzedInstructions") val analyzedInstructions: List<AnalyzedInstruction>? = null
) {
    fun getFormattedInstructions(): String {
        if (!analyzedInstructions.isNullOrEmpty()) {
            return analyzedInstructions
                .flatMap { it.steps }
                .joinToString("\n\n") { "${it.number}. ${it.step}" }
        }
        return instructions?.let { htmlText ->
            htmlText.replace("<ol>", "")
                .replace("</ol>", "")
                .replace("<li>", "\n• ")
                .replace("</li>", "")
                .trim()
        } ?: "No instructions available"
    }
}

data class Ingredient(
    @Json(name = "originalName") val name: String,
    val amount: Double,
    val unit: String,
    @Json(name = "image") val image: String? = null
)

data class AnalyzedInstruction(
    val name: String,
    val steps: List<Step>
)

data class Step(
    val number: Int,
    val step: String,
    val ingredients: List<StepIngredient>? = null,
    val equipment: List<Equipment>? = null
)

data class StepIngredient(
    val id: Int,
    val name: String,
    val image: String?
)

data class Equipment(
    val id: Int,
    val name: String,
    val image: String?
)