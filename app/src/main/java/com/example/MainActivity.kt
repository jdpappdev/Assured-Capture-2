package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.InspectionRepository
import com.example.ui.screens.IssueDetailScreen
import com.example.ui.screens.PhotoDetailScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.ReportsHomeScreen
import com.example.ui.theme.AssuredCaptureTheme
import com.example.ui.theme.BackgroundNeutral
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: InspectionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        repository = InspectionRepository(database.reportDao(), applicationContext)

        // Seed realistic inspection data on first run for field inspector Nic
        lifecycleScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        setContent {
            AssuredCaptureTheme {
                val navController = rememberNavController()
                val reports by repository.allReports.collectAsState(initial = emptyList())

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundNeutral
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "reports_home"
                    ) {
                        // 1. Reports Home
                        composable("reports_home") {
                            ReportsHomeScreen(
                                reports = reports,
                                repository = repository,
                                onReportClick = { reportId ->
                                    navController.navigate("report_detail/$reportId")
                                }
                            )
                        }

                        // 2. Report Detail
                        composable(
                            route = "report_detail/{reportId}",
                            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
                            ReportDetailScreen(
                                reportId = reportId,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onIssueClick = { issueId ->
                                    navController.navigate("issue_detail/$issueId")
                                },
                                onReportDeleted = {
                                    navController.popBackStack("reports_home", inclusive = false)
                                }
                            )
                        }

                        // 3. Issue Detail
                        composable(
                            route = "issue_detail/{issueId}",
                            arguments = listOf(navArgument("issueId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
                            IssueDetailScreen(
                                issueId = issueId,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onPhotoClick = { photoId ->
                                    navController.navigate("photo_detail/$photoId")
                                },
                                onIssueDeleted = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 4. Photo Detail / Edit
                        composable(
                            route = "photo_detail/{photoId}",
                            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
                            PhotoDetailScreen(
                                photoId = photoId,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onPhotoDeleted = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
