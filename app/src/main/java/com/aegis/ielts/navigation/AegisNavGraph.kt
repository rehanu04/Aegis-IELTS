package com.aegis.ielts.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aegis.ielts.features.home.ui.AegisHomeScreen
import com.aegis.ielts.features.speaking.SpeakingViewModel
import com.aegis.ielts.features.speaking.ui.IeltsSpeakingAssessmentScreen
import com.aegis.ielts.features.reading.ReadingViewModel
import com.aegis.ielts.features.reading.ui.ReadingAssessmentScreen
import com.aegis.ielts.features.listening.ListeningViewModel
import com.aegis.ielts.features.listening.ui.ListeningAssessmentScreen
import com.aegis.ielts.features.writing.WritingViewModel
import com.aegis.ielts.features.writing.ui.WritingAssessmentScreen

/**
 * Type-safe navigation destination graph for Aegis IELTS.
 *
 * Route schema: "[module]/{testId}"
 *  - testId is a UUID string generated at the home screen and passed to each
 *    module screen to scope telemetry and evaluation sessions.
 *
 * Phases 3–5 fill in the empty composable blocks for Reading, Listening, Writing.
 */
sealed class AegisDestination(val route: String) {

    object Home : AegisDestination("home")

    object Speaking : AegisDestination("speaking/{testId}") {
        fun createRoute(testId: String) = "speaking/$testId"
        const val ARG_TEST_ID = "testId"
    }

    object Reading : AegisDestination("reading/{testId}") {
        fun createRoute(testId: String) = "reading/$testId"
        const val ARG_TEST_ID = "testId"
    }

    object Listening : AegisDestination("listening/{testId}") {
        fun createRoute(testId: String) = "listening/$testId"
        const val ARG_TEST_ID = "testId"
    }

    object Writing : AegisDestination("writing/{testId}") {
        fun createRoute(testId: String) = "writing/$testId"
        const val ARG_TEST_ID = "testId"
    }
}

/**
 * Root Compose NavHost for the application.
 * Starts at [AegisDestination.Home]. All module routes embed {testId}.
 */
@Composable
fun AegisNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController    = navController,
        startDestination = AegisDestination.Home.route
    ) {

        // ── Home ──────────────────────────────────────────────────────────
        composable(route = AegisDestination.Home.route) {
            AegisHomeScreen(
                onNavigateToSpeaking  = { testId ->
                    navController.navigate(AegisDestination.Speaking.createRoute(testId))
                },
                onNavigateToReading   = { testId ->
                    navController.navigate(AegisDestination.Reading.createRoute(testId))
                },
                onNavigateToListening = { testId ->
                    navController.navigate(AegisDestination.Listening.createRoute(testId))
                },
                onNavigateToWriting   = { testId ->
                    navController.navigate(AegisDestination.Writing.createRoute(testId))
                }
            )
        }

        // ── Speaking ──────────────────────────────────────────────────────
        composable(
            route     = AegisDestination.Speaking.route,
            arguments = listOf(
                navArgument(AegisDestination.Speaking.ARG_TEST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val testId    = backStackEntry.arguments
                ?.getString(AegisDestination.Speaking.ARG_TEST_ID)
                .orEmpty()
            val viewModel : SpeakingViewModel = hiltViewModel()
            IeltsSpeakingAssessmentScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel      = viewModel,
                testId         = testId
            )
        }

        // ── Reading (Phase 3) ─────────────────────────────────────────────
        composable(
            route     = AegisDestination.Reading.route,
            arguments = listOf(
                navArgument(AegisDestination.Reading.ARG_TEST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val testId = backStackEntry.arguments
                ?.getString(AegisDestination.Reading.ARG_TEST_ID)
                .orEmpty()
            val viewModel: ReadingViewModel = hiltViewModel()
            ReadingAssessmentScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel      = viewModel,
                testId         = testId
            )
        }

        // ── Listening (Phase 4) ───────────────────────────────────────────
        composable(
            route     = AegisDestination.Listening.route,
            arguments = listOf(
                navArgument(AegisDestination.Listening.ARG_TEST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val testId = backStackEntry.arguments
                ?.getString(AegisDestination.Listening.ARG_TEST_ID)
                .orEmpty()
            val viewModel: ListeningViewModel = hiltViewModel()
            ListeningAssessmentScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel      = viewModel,
                testId         = testId
            )
        }

        // ── Writing (Phase 5) ─────────────────────────────────────────────
        composable(
            route     = AegisDestination.Writing.route,
            arguments = listOf(
                navArgument(AegisDestination.Writing.ARG_TEST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val testId = backStackEntry.arguments
                ?.getString(AegisDestination.Writing.ARG_TEST_ID)
                .orEmpty()
            val viewModel: WritingViewModel = hiltViewModel()
            WritingAssessmentScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel      = viewModel,
                testId         = testId
            )
        }
    }
}
