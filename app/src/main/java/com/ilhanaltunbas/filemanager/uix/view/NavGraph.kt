package com.ilhanaltunbas.filemanager.uix.view

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ilhanaltunbas.filemanager.uix.viewmodel.DetailViewModel
import com.ilhanaltunbas.filemanager.uix.viewmodel.MainViewModel

@Composable
fun NavGraph(mainViewModel: MainViewModel,
             detailViewModel: DetailViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "mainPage") {
        composable(
            "mainPage"
        ) {
            MainPage(navController = navController,mainViewModel)
        }
        composable(
            "detailPage/{id}/{name}",
            arguments = listOf(
                navArgument("id"){type = NavType.IntType},
                navArgument("name"){type = NavType.StringType}
            )
        ) {
            val id = it.arguments?.getInt("id")
            val name = it.arguments?.getString("name")
            DetailPage(navController = navController,id,name,detailViewModel)
        }
    }

}