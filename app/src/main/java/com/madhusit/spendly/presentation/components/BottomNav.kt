package com.madhusit.spendly.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNav(navController: NavHostController) {
    val backStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = backStackEntry?.destination?.route
    val items = listOf(
        "home" to ("Home" to Icons.Default.Home),
        "transactions" to ("Transactions" to Icons.Default.ReceiptLong),
        "accounts" to ("Accounts" to Icons.Default.AccountBalanceWallet),
        "insights" to ("Insights" to Icons.Default.Insights)
    )
    // Top-level tab routes — anything else is a detail/modal screen
    val topLevelRoutes = setOf("home", "transactions", "accounts", "insights")
    val selectedTab = if (currentRoute in topLevelRoutes) currentRoute else "home"

    NavigationBar {
        items.forEach { (route, item) ->
            NavigationBarItem(
                modifier = Modifier.testTag("bottom_nav_$route"),
                selected = selectedTab == route,
                onClick = {
                    navController.navigate(route) {
                        // Pop current sub-screens. For Home tab, pop home itself too so it
                        // re-enters fresh (avoids launchSingleTop no-op when home is underneath).
                        popUpTo("home") {
                            inclusive = route == "home"
                            saveState = route != "home"
                        }
                        launchSingleTop = true
                        restoreState = route != "home"
                    }
                },
                icon = { Icon(item.second, contentDescription = item.first) },
                label = { Text(item.first) }
            )
        }
    }
}
