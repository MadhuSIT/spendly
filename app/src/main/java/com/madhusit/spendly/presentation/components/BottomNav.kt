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
    NavigationBar {
        items.forEach { (route, item) ->
            NavigationBarItem(
                modifier = Modifier.testTag("bottom_nav_$route"),
                selected = currentRoute == route,
                onClick = { navController.navigate(route) { launchSingleTop = true; restoreState = true } },
                icon = { Icon(item.second, contentDescription = item.first) },
                label = { Text(item.first) }
            )
        }
    }
}
