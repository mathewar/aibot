package com.aibot.crm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aibot.crm.ui.screens.HomeScreen
import com.aibot.crm.ui.screens.PersonDetailScreen
import com.aibot.crm.ui.theme.CrmTheme
import com.aibot.crm.ui.viewmodel.CrmViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CrmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrmTheme {
                CrmApp(viewModel)
            }
        }
    }
}

@Composable
private fun CrmApp(viewModel: CrmViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onPersonClick = { id -> navController.navigate("person/$id") },
            )
        }
        composable(
            route = "person/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            PersonDetailScreen(
                personId = id,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
