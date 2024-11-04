package com.example.yuting_shi_recipe_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.rememberImagePainter
import com.example.yuting_shi_recipe_app.network.Recipe
import com.example.yuting_shi_recipe_app.network.RecipeRepository
import com.example.yuting_shi_recipe_app.network.SpoonacularApi
import com.example.yuting_shi_recipe_app.viewmodel.RecipeViewModel
import com.example.yuting_shi_recipe_app.viewmodel.RecipeViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiService = SpoonacularApi.retrofitService
        val repository = RecipeRepository(apiService)
        val viewModelFactory = RecipeViewModelFactory(repository)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "search") {
                    composable("search") {
                        RecipeSearchScreen(navController = navController, viewModel = viewModel(factory = viewModelFactory))
                    }
                    composable("detail/{recipeId}") { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString("recipeId")
                        recipeId?.let { id ->
                            RecipeDetailScreen(
                                viewModel = viewModel(factory = viewModelFactory),
                                recipeId = id,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row -> row.maxOf { it.height } }
        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width
                }
                y += row.maxOf { it.height }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSearchScreen(navController: NavHostController, viewModel: RecipeViewModel) {
    val recipes by viewModel.recipes.collectAsState()
    val lastQuery by viewModel.lastQuery.collectAsState()
    val lastSelectedCuisines by viewModel.lastSelectedCuisines.collectAsState()
    val lastSelectedDiets by viewModel.lastSelectedDiets.collectAsState()
    val lastMaxCalories by viewModel.lastMaxCalories.collectAsState()

    var query by remember { mutableStateOf(lastQuery) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var selectedCuisines by remember { mutableStateOf(lastSelectedCuisines) }
    var selectedDiets by remember { mutableStateOf(lastSelectedDiets) }
    var maxCalories by remember { mutableStateOf(lastMaxCalories) }
    var isInitialLoad by remember { mutableStateOf(true) }

    LaunchedEffect(recipes) {
        if (recipes.isNotEmpty()) {
            isInitialLoad = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe Search", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Recipe") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Text("×", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showFiltersDialog = true }) {
                        Text("Filters")
                    }
                    if (selectedCuisines.isNotEmpty() || selectedDiets.isNotEmpty() || maxCalories < 2000) {
                        Text(
                            buildString {
                                append(" (")
                                if (selectedCuisines.isNotEmpty()) append("${selectedCuisines.size} cuisines")
                                if (selectedDiets.isNotEmpty()) {
                                    if (selectedCuisines.isNotEmpty()) append(", ")
                                    append("${selectedDiets.size} diets")
                                }
                                if (maxCalories < 2000) {
                                    if (selectedCuisines.isNotEmpty() || selectedDiets.isNotEmpty()) append(", ")
                                    append("$maxCalories cal")
                                }
                                append(")")
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (selectedCuisines.isNotEmpty() || selectedDiets.isNotEmpty() || maxCalories < 2000) {
                    TextButton(onClick = {
                        selectedCuisines = emptySet()
                        selectedDiets = emptySet()
                        maxCalories = 2000
                        viewModel.searchRecipes(query, "", "", 2000, reset = true)
                    }) {
                        Text("Clear Filters")
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.searchRecipes(
                        query,
                        selectedCuisines.joinToString(","),
                        selectedDiets.joinToString(","),
                        maxCalories,
                        reset = true
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search")
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isInitialLoad -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                recipes.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "No recipes found",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No recipes for specified filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showFiltersDialog = true }
                            ) {
                                Text("Adjust Filters")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn {
                        itemsIndexed(recipes) { index, recipe ->
                            RecipeListItem(recipe = recipe) {
                                navController.navigate("detail/${recipe.id}")
                            }
                        }
                    }
                }
            }
        }

        if (showFiltersDialog) {
            FiltersDialog(
                selectedCuisines = selectedCuisines,
                onCuisinesChanged = { selectedCuisines = it },
                selectedDiets = selectedDiets,
                onDietsChanged = { selectedDiets = it },
                maxCalories = maxCalories,
                onMaxCaloriesChanged = { maxCalories = it },
                onDismiss = { showFiltersDialog = false }
            )
        }
    }
}

@Composable
fun FiltersDialog(
    selectedCuisines: Set<String>,
    onCuisinesChanged: (Set<String>) -> Unit,
    selectedDiets: Set<String>,
    onDietsChanged: (Set<String>) -> Unit,
    maxCalories: Int,
    onMaxCaloriesChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val cuisineOptions = listOf(
        "American", "Caribbean", "Chinese", "French", "Greek", "Indian", "Irish", "Italian", "Japanese", "Korean", "Mediterranean", "Mexican", "Thai", "Vietnamese"
    )

    val dietOptions = listOf(
        "Gluten Free", "Vegetarian", "Vegan", "Pescatarian", "Paleo", "Whole30"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Filters") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text("Cuisines", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    cuisineOptions.forEach { cuisine ->
                        FilterChip(
                            selected = cuisine in selectedCuisines,
                            onClick = {
                                val newSelection = selectedCuisines.toMutableSet()
                                if (cuisine in selectedCuisines) {
                                    newSelection.remove(cuisine)
                                } else {
                                    newSelection.add(cuisine)
                                }
                                onCuisinesChanged(newSelection)
                            },
                            label = { Text(cuisine) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Dietary Restrictions", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dietOptions.forEach { diet ->
                        FilterChip(
                            selected = diet in selectedDiets,
                            onClick = {
                                val newSelection = selectedDiets.toMutableSet()
                                if (diet in selectedDiets) {
                                    newSelection.remove(diet)
                                } else {
                                    newSelection.add(diet)
                                }
                                onDietsChanged(newSelection)
                            },
                            label = { Text(diet) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Maximum Calories: $maxCalories", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = maxCalories.toFloat(),
                    onValueChange = { onMaxCaloriesChanged(it.toInt()) },
                    valueRange = 0f..2000f,
                    steps = 20
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun RecipeListItem(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Image(
            painter = rememberImagePainter(recipe.image),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .padding(end = 8.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Column {
            Text(recipe.title, style = MaterialTheme.typography.titleMedium)
            Text("Click for more details", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(viewModel: RecipeViewModel, recipeId: String, navController: NavHostController) {
    var recipe by remember { mutableStateOf<Recipe?>(null) }

    LaunchedEffect(recipeId) {
        viewModel.getRecipeById(recipeId) { fetchedRecipe ->
            recipe = fetchedRecipe
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.title ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        if (recipe == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Image(
                    painter = rememberImagePainter(recipe!!.image),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!recipe!!.ingredients.isNullOrEmpty()) {
                    Text(
                        "Ingredients",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    recipe!!.ingredients?.forEach { ingredient ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("•", modifier = Modifier.padding(end = 8.dp))
                            Text(
                                "${ingredient.amount} ${ingredient.unit} ${ingredient.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (!recipe!!.instructions.isNullOrEmpty()) {
                    Text(
                        "Instructions",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        recipe!!.getFormattedInstructions(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}