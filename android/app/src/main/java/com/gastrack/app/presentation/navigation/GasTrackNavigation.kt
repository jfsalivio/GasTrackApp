package com.gastrack.app.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.*
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.gastrack.app.presentation.components.*
import com.gastrack.app.presentation.screens.*
import com.gastrack.app.presentation.viewmodels.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object LoginRoute
@Serializable data object RegistrationRoute
@Serializable data object ForgotRoute
@Serializable data class ResetRoute(val token: String)
@Serializable data class LegalRoute(val file: String)
@Serializable data class ModuleRoute(val module: String)

private val labels = linkedMapOf("dashboard" to "Dashboard", "pos" to "POS Terminal", "inventory" to "Inventory", "products" to "Products", "sales" to "Sales", "restocking" to "Restocking", "orders" to "Order and Delivery", "suppliers" to "Suppliers", "report" to "Reports and Compliance", "data" to "Data", "users" to "Users", "settings" to "Settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GasTrackApp(resetToken: String?, session: SessionViewModel = hiltViewModel()) {
    val state by session.state.collectAsStateWithLifecycle()
    val online by session.online.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            session.refreshSession()
            while (true) { delay(30_000); session.refreshSession() }
        }
    }
    var consumedReset by rememberSaveableReset() // survives recomposition; reset links take priority
    if (!resetToken.isNullOrBlank() && !consumedReset) {
        Scaffold { padding -> Box(Modifier.padding(padding)) { PasswordScreen(hiltViewModel(), resetToken) { consumedReset = true } } }
        return
    }
    if (state.loading) { Scaffold { Box(Modifier.padding(it)) { LoadingPanel("Verifying your account…") } }; return }
    val actor = state.user
    if (actor == null) {
        val nav = rememberNavController()
        Scaffold(topBar = { Column(Modifier.statusBarsPadding()) {
            if (!online) Text("Offline · Check your connection", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
            state.error?.let { ErrorPanel(it, session::refreshSession) }
        } }) { padding ->
            NavHost(nav, startDestination = LoginRoute, modifier = Modifier.padding(padding)) {
                composable<LoginRoute> { LoginScreen(hiltViewModel(), { nav.navigate(RegistrationRoute) }, { nav.navigate(ForgotRoute) }, session::refreshSession) }
                composable<RegistrationRoute> { RegistrationScreen(hiltViewModel(), { nav.popBackStack() }, session::refreshSession, { nav.navigate(LegalRoute(it)) }) }
                composable<ForgotRoute> { PasswordScreen(hiltViewModel(), null) { nav.popBackStack() } }
                composable<LegalRoute> { LegalScreen(it.toRoute<LegalRoute>().file) }
            }
        }
        return
    }
    val allowed = labels.keys.filter { it in actor.modules }
    if (allowed.isEmpty()) {
        Scaffold { padding -> Column(Modifier.padding(padding).padding(16.dp)) { Text("No module access is assigned. Contact an administrator."); TextButton(onClick = session::logout) { Text("Sign out") } } }
        return
    }
    key(actor.id, allowed) {
        val nav = rememberNavController()
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val entry by nav.currentBackStackEntryAsState()
        val current = entry?.toRoute<ModuleRoute>()?.module ?: allowed.first()
        ModalNavigationDrawer(drawerState = drawer, drawerContent = {
            ModalDrawerSheet(Modifier.verticalScroll(rememberScrollState())) {
                Text("GasTrack", Modifier.padding(24.dp), style = MaterialTheme.typography.headlineMedium)
                Text("${actor.firstName} ${actor.lastName}", Modifier.padding(horizontal = 24.dp))
                allowed.forEach { id -> NavigationDrawerItem(label = { Text(labels.getValue(id)) }, selected = current == id, onClick = {
                    // Modules share a destination ID, so restoring by ID can restore
                    // another module's arguments instead of the requested module.
                    nav.navigate(ModuleRoute(id)) {
                        popUpTo(nav.graph.startDestinationId)
                        launchSingleTop = true
                    }
                    scope.launch { drawer.close() }
                }) }
                TextButton(onClick = session::logout, modifier = Modifier.padding(16.dp)) { Text("Sign out") }
            }
        }) {
            Scaffold(topBar = { Column {
                TopAppBar(title = { Text(labels[current].orEmpty()) }, navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Open navigation") } })
                if (!online) Text("Offline · Requests will resume when you retry", Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.error)
                state.error?.let { ErrorPanel(it, session::refreshSession) }
            } }) { padding ->
                NavHost(nav, startDestination = ModuleRoute(allowed.first()), modifier = Modifier.padding(padding)) {
                    composable<ModuleRoute> { backStack ->
                        val id = backStack.toRoute<ModuleRoute>().module
                        if (id !in allowed) Text("Access unavailable.") else when (id) {
                            "dashboard" -> DashboardScreen(hiltViewModel())
                            "inventory" -> InventoryHub()
                            "products" -> ProductsScreen(hiltViewModel())
                            "restocking" -> RestockingHub()
                            "orders" -> FulfillmentScreen(actor.isDriver)
                            else -> ManagementScreen(id)
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun rememberSaveableReset() = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
@Composable private fun LegalScreen(file: String) {
    val context = LocalContext.current
    val text = remember(file) { context.assets.open("legal/$file").bufferedReader().use { it.readText() } }
    Text(text, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp))
}
@Composable private fun FulfillmentScreen(driver: Boolean) {
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(if (driver) "Deliveries" else "Pickups") }
    val tabs = if (driver) listOf("Deliveries") else listOf("Pickups", "Deliveries", "Orders", "Customers")
    Column {
        ScrollableTabRow(tabs.indexOf(tab).coerceAtLeast(0), edgePadding = 8.dp) {
            tabs.forEach { label -> Tab(selected = tab == label, onClick = { tab = label }, text = { Text(label) }) }
        }
        when (tab) {
            "Pickups" -> PickupsScreen(hiltViewModel())
            "Deliveries" -> DeliveriesScreen(hiltViewModel(), driver)
            "Orders" -> OrdersScreen(hiltViewModel())
            "Customers" -> CustomersScreen(hiltViewModel())
        }
    }
}
@Composable private fun RestockingHub() {
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    Column {
        TabRow(tab) { listOf("Recommendations", "Purchase orders").forEachIndexed { index, label -> Tab(tab == index, { tab = index }, text = { Text(label) }) } }
        if (tab == 0) RestockingScreen(hiltViewModel()) else RecordsScreen("purchase-orders", hiltViewModel(key = "purchase-orders"))
    }
}
@Composable private fun InventoryHub() {
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    Column {
        ScrollableTabRow(tab, edgePadding = 8.dp) { listOf("Inventory", "Transactions", "Transfers").forEachIndexed { index, label -> Tab(tab == index, { tab = index }, text = { Text(label) }) } }
        when (tab) {
            0 -> InventoryScreen(hiltViewModel())
            1 -> RecordsScreen("inventory-transactions", hiltViewModel(key = "inventory-transactions"))
            else -> RecordsScreen("inventory-transfers", hiltViewModel(key = "inventory-transfers"))
        }
    }
}
