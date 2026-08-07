package com.example.chess.ui.screens.multiplayer

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.chess.game.board.PieceColor
import com.example.chess.network.wifi.WifiState
import com.example.chess.ui.components.GameSetupDialog
import com.example.chess.ui.components.PremiumCard
import com.example.chess.ui.components.PrimaryButton
import com.example.chess.ui.screens.game.GameConfig
import com.example.chess.ui.screens.game.PlayerColorChoice

private fun checkWifiReady(context: Context): Boolean {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    if (!wifiManager.isWifiEnabled) return false
    
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiLobbyScreen(
    viewModel: WifiGameViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGame: () -> Unit
) {
    val state by viewModel.connectionState.collectAsState()
    val discoveredServices by viewModel.discoveredServices.collectAsState()
    
    val context = LocalContext.current
    var isWifiReady by remember { mutableStateOf(checkWifiReady(context)) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isWifiReady = checkWifiReady(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isWifiReady) {
        if (isWifiReady) {
            viewModel.startLobby()
        }
    }

    LaunchedEffect(state) {
        if (state is WifiState.Connected) {
            onNavigateToGame()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.wifiController.stopDiscovery()
        }
    }

    val showSetupDialog = remember { mutableStateOf(false) }
    var selectedOpponent by remember { mutableStateOf<NsdServiceInfo?>(null) }

    if (showSetupDialog.value && selectedOpponent != null) {
        GameSetupDialog(
            showDialog = showSetupDialog,
            onConfirm = { config ->
                val myColor = when (config.playerColorChoice) {
                    PlayerColorChoice.WHITE -> PieceColor.WHITE
                    PlayerColorChoice.BLACK -> PieceColor.BLACK
                    PlayerColorChoice.RANDOM -> listOf(PieceColor.WHITE, PieceColor.BLACK).random()
                    else -> PieceColor.WHITE
                }
                viewModel.sendChallenge(
                    selectedOpponent!!, 
                    config.timeMillis, 
                    config.incrementMillis, 
                    myColor
                )
                showSetupDialog.value = false
            }
        )
    }

    if (state is WifiState.ChallengePending) {
        val pending = state as WifiState.ChallengePending
        LocalMatchRequestDialog(
            opponentName = pending.fromOpponent,
            onAccept = { viewModel.acceptChallenge(pending.fromOpponent) },
            onDecline = viewModel::rejectChallenge
        )
    }

    if (state is WifiState.ChallengeRejected) {
        AlertDialog(
            onDismissRequest = { viewModel.startLobby() },
            title = { Text("Challenge Rejected") },
            text = { Text("The opponent rejected your challenge.") },
            confirmButton = {
                TextButton(onClick = { viewModel.startLobby() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Local match", fontWeight = FontWeight.Bold)
                        Text(
                            "Play someone on your Wi-Fi",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.wifiController.disconnect()
                        onNavigateBack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (isWifiReady) viewModel.startLobby() },
                        enabled = isWifiReady
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh players")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            when {
                !isWifiReady -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NetworkStatusIcon(connected = false)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Connect to Wi-Fi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Local matches need both players on the same wireless network.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Open Wi-Fi settings") }
                    }
                }
                state is WifiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NetworkStatusIcon(connected = false)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Couldn’t find players", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text((state as WifiState.Error).message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.startLobby() }, shape = RoundedCornerShape(14.dp)) { Text("Try again") }
                    }
                }
                state is WifiState.Challenging -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Challenge sent", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Waiting for ${(state as WifiState.Challenging).opponentName} to accept your match.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = { viewModel.startLobby() }, shape = RoundedCornerShape(14.dp)) { Text("Cancel") }
                    }
                }
                else -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            NetworkStatusIcon(connected = true, compact = true)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Discovering nearby players", fontWeight = FontWeight.Bold)
                                Text("Only players on your local Wi-Fi can join", style = MaterialTheme.typography.bodySmall)
                            }
                            if (discoveredServices.isEmpty()) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Available players", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Tap Play to choose the time control and color.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))
                        
                    if (discoveredServices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("Looking for opponents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Keep this screen open while other players join.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(discoveredServices, key = { it.serviceName }) { service ->
                                    val fallbackName = service.serviceName.removePrefix("ChessPeer_")
                                    val opponentName = service.attributes?.get("username")?.let { String(it, Charsets.UTF_8) }?.ifBlank { fallbackName } ?: fallbackName
                                    val wins = service.attributes?.get("wins")?.let { String(it, Charsets.UTF_8) } ?: "0"
                                    val losses = service.attributes?.get("losses")?.let { String(it, Charsets.UTF_8) } ?: "0"
                                    
                                    PlayerLobbyCard(
                                        opponentName = opponentName,
                                        wins = wins,
                                        losses = losses,
                                        avatar = service.attributes?.get("avatar")?.let { String(it) } ?: "",
                                        onChallenge = {
                                            selectedOpponent = service
                                            showSetupDialog.value = true
                                        }
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerLobbyCard(
    opponentName: String,
    wins: String,
    losses: String,
    avatar: String,
    onChallenge: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(durationMillis = 400)
        ) + fadeIn(animationSpec = tween(durationMillis = 400))
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Box
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatar.takeIf { it.isNotBlank() } ?: opponentName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = opponentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("Ready for a local match", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Wins
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Wins",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFC107) // Gold Color
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = wins,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Losses
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Losses",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = losses,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Challenge Button
                Button(
                    onClick = onChallenge,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Challenge",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Play", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NetworkStatusIcon(connected: Boolean, compact: Boolean = false) {
    val size = if (compact) 40.dp else 72.dp
    val container = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer
    val content = if (connected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer
    Box(
        modifier = Modifier.size(size).background(container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            modifier = Modifier.size(if (compact) 22.dp else 36.dp),
            tint = content
        )
    }
}

@Composable
private fun LocalMatchRequestDialog(
    opponentName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Dialog(onDismissRequest = onDecline) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.86f, animationSpec = tween(260))
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Match request", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$opponentName wants to play a local match with you.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Same Wi-Fi network", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) { Text("Accept match", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
                        Text("Decline", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
