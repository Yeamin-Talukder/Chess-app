package com.example.chess.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val soundVolume by viewModel.soundVolume.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val boardColors by viewModel.boardColors.collectAsState()
    val pieceStyle by viewModel.pieceStyle.collectAsState()
    val animationSpeed by viewModel.animationSpeed.collectAsState()
    val boardRotation by viewModel.boardRotation.collectAsState()
    val coordinates by viewModel.coordinates.collectAsState()
    val legalMoveHighlight by viewModel.legalMoveHighlight.collectAsState()
    val language by viewModel.language.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategory(title = "Appearance")
            SettingsDropdownItem(
                title = "Board Colors",
                subtitle = "Square color palette",
                currentValue = boardColors,
                options = listOf("Wood", "Green", "Blue", "Glass", "Tournament"),
                onValueChange = { viewModel.setBoardColors(it) }
            )
            SettingsDropdownItem(
                title = "Piece Style",
                subtitle = "Chess piece design",
                currentValue = pieceStyle,
                options = listOf("Classic", "Modern", "Minimalist", "3D"),
                onValueChange = { viewModel.setPieceStyle(it) }
            )
            SettingsSliderItem(
                title = "Animation Speed",
                value = animationSpeed,
                onValueChange = { viewModel.setAnimationSpeed(it) }
            )
            
            SettingsCategory(title = "Gameplay")
            SettingsSwitchItem(
                title = "Show Legal Moves", 
                subtitle = "Highlight possible moves when a piece is selected", 
                checked = legalMoveHighlight, 
                onCheckedChange = { viewModel.setLegalMoveHighlight(it) }
            )
            SettingsSwitchItem(
                title = "Board Rotation", 
                subtitle = "Auto-flip board each turn for local play", 
                checked = boardRotation, 
                onCheckedChange = { viewModel.setBoardRotation(it) }
            )
            SettingsSwitchItem(
                title = "Show Coordinates", 
                subtitle = "Display ranks (1-8) and files (a-h)", 
                checked = coordinates, 
                onCheckedChange = { viewModel.setCoordinates(it) }
            )
            
            SettingsCategory(title = "Feedback")
            SettingsSwitchItem(
                title = "Haptic Feedback", 
                subtitle = "Vibrate on move and capture", 
                checked = hapticsEnabled, 
                onCheckedChange = { viewModel.setHapticsEnabled(it) }
            )
            SettingsSwitchItem(
                title = "Sound Effects", 
                subtitle = "Play sounds for moves, checks", 
                checked = soundEnabled, 
                onCheckedChange = { viewModel.setSoundEnabled(it) }
            )
            if (soundEnabled) {
                SettingsSliderItem(
                    title = "Sound Volume",
                    value = soundVolume,
                    onValueChange = { viewModel.setSoundVolume(it) }
                )
            }
            
            Spacer(modifier = Modifier.padding(24.dp))
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = null // Handled by row click
        )
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    currentValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
