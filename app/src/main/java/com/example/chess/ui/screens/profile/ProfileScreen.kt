package com.example.chess.ui.screens.profile

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chess.ui.components.PremiumCard
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            ProfileAvatar(
                avatar = profile.avatar,
                username = profile.username,
                modifier = Modifier.size(120.dp).clickable { showEditDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = profile.username,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = profile.country,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Grid
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(label = "Games", value = profile.gamesPlayed.toString())
                    StatItem(label = "Wins", value = profile.wins.toString())
                    StatItem(label = "Losses", value = profile.losses.toString())
                    StatItem(label = "Draws", value = profile.draws.toString())
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val winRateStr = String.format(Locale.getDefault(), "%.1f%%", profile.winRate)
                    StatItem(label = "Win Rate", value = winRateStr)
                    
                    val hours = profile.totalPlayTime / (1000 * 60 * 60)
                    val minutes = (profile.totalPlayTime / (1000 * 60)) % 60
                    StatItem(label = "Play Time", value = "${hours}h ${minutes}m")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val puzzleRating by viewModel.puzzleRating.collectAsState(initial = 1500)
            val puzzleBestStreak by viewModel.puzzleBestStreak.collectAsState(initial = 0)
            val puzzleCurrentStreak by viewModel.puzzleCurrentStreak.collectAsState(initial = 0)
            val puzzleSolved by viewModel.puzzleSolvedCount.collectAsState(initial = 0)
            val puzzleFailed by viewModel.puzzleFailedCount.collectAsState(initial = 0)
            
            val totalPuzzles = puzzleSolved + puzzleFailed
            val accuracy = if (totalPuzzles > 0) (puzzleSolved.toFloat() / totalPuzzles) * 100f else 0f
            
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("Puzzle Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(label = "Rating", value = puzzleRating.toString())
                    StatItem(label = "Solved", value = puzzleSolved.toString())
                    val accStr = String.format(Locale.getDefault(), "%.1f%%", accuracy)
                    StatItem(label = "Accuracy", value = accStr)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(label = "Failed", value = puzzleFailed.toString())
                    StatItem(label = "Best Streak", value = puzzleBestStreak.toString())
                    StatItem(label = "Streak", value = puzzleCurrentStreak.toString())
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Favorite Board", style = MaterialTheme.typography.bodyLarge)
                    Text(profile.favoriteBoard, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Favorite Pieces", style = MaterialTheme.typography.bodyLarge)
                    Text(profile.favoritePieces, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentUsername = profile.username,
            currentCountry = profile.country,
            currentAvatar = profile.avatar,
            onDismiss = { showEditDialog = false },
            onSave = { username, country, avatar ->
                viewModel.updateProfileInfo(username, country, avatar)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentUsername: String,
    currentCountry: String,
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var country by remember { mutableStateOf(currentCountry) }
    var avatar by remember { mutableStateOf(currentAvatar) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        avatar = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(avatar = avatar, username = username, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Profile photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Use a photo from your device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(onClick = { photoPicker.launch(arrayOf("image/*")) }) { Text("Choose photo") }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text("Or choose an icon", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val emojis = listOf("♔", "♕", "♖", "♗", "♘", "♙", "♚", "♛", "♜", "♝", "♞", "♟", "G")
                    items(emojis) { emoji ->
                        val isSelected = avatar == emoji
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { avatar = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(username, country, avatar) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ProfileAvatar(avatar: String, username: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(avatar) {
        if (avatar.startsWith("content://")) {
            runCatching {
                context.contentResolver.openInputStream(android.net.Uri.parse(avatar))?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        } else null
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "$username profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Text(
                text = avatar.takeUnless { it.startsWith("content://") }?.ifBlank { username.take(1).uppercase() }
                    ?: username.take(1).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
