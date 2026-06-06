package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DreamEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    viewModel: MainViewModel,
    onPlayDream: (DreamEntity) -> Unit
) {
    val dreams by viewModel.allDreams.collectAsState()
    val pinCode by viewModel.securityPin.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()

    var showPinSetup by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CosmicDeepSpace, CosmicMidnightCard)
                )
            )
            .testTag("journal_screen_root")
    ) {
        if (pinCode != null && !isUnlocked) {
            // Protected PIN Entry View
            PinProtectionPad(
                onVerify = { input -> viewModel.verifyPin(input) }
            )
        } else {
            // Unlocked Journal Main Content
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Dream Vault",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = CosmicTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${dreams.size} surreal stories archived",
                                fontSize = 14.sp,
                                color = CosmicTextSecondary
                            )
                        }

                        // Toggle PIN Lock Protection Action
                        IconButton(
                            onClick = { showPinSetup = true },
                            modifier = Modifier
                                .background(CosmicMidnightSurface, CircleShape)
                                .testTag("secure_settings_button")
                        ) {
                            Icon(
                                imageVector = if (pinCode != null) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Security PIN settings",
                                tint = if (pinCode != null) CosmicSecondaryCyan else CosmicTextSecondary
                            )
                        }
                    }
                }
            ) { padding ->
                if (dreams.isEmpty()) {
                    EmptyVaultState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(20.dp, 10.dp, 20.dp, 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dreams, key = { it.id }) { dream ->
                            DreamArchiveCard(
                                dream = dream,
                                onClick = { onPlayDream(dream) },
                                onDelete = { viewModel.deleteDream(dream.id) }
                            )
                        }
                    }
                }
            }
        }

        // Pin Configuration Setup overlay dialog
        if (showPinSetup) {
            PinSetupOverlay(
                currentPin = pinCode,
                onSave = { newPin ->
                    viewModel.setSecurityPin(newPin)
                    showPinSetup = false
                },
                onDismiss = { showPinSetup = false }
            )
        }
    }
}

@Composable
fun PinProtectionPad(onVerify: (String) -> Boolean) {
    var pinText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "🔒 Lock",
            tint = CosmicPrimaryPurple,
            modifier = Modifier
                .size(64.dp)
                .shadow(12.dp, CircleShape)
                .background(CosmicMidnightSurface, CircleShape)
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SUB-CONSCIOUS VENEER",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CosmicTextPrimary,
            letterSpacing = 1.sp
        )

        Text(
            text = "Your private archive is locked.",
            fontSize = 14.sp,
            color = CosmicTextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Passcode indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { i ->
                val filled = i < pinText.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isError) CosmicGlowRed
                            else if (filled) CosmicSecondaryCyan
                            else CosmicMidnightSurface
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // PIN Keypad grid
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "Del")
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            for (row in 0..3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0..2) {
                        val key = keys[row * 3 + col]
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CosmicMidnightSurface)
                                .clickable {
                                    isError = false
                                    when (key) {
                                        "Clear" -> pinText = ""
                                        "Del" -> if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                        else -> if (pinText.length < 4) {
                                            pinText += key
                                            if (pinText.length == 4) {
                                                val ok = onVerify(pinText)
                                                if (!ok) {
                                                    isError = true
                                                    pinText = ""
                                                }
                                            }
                                        }
                                    }
                                }
                                .testTag("pin_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = if (key.length > 2) 12.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (key.length > 2) CosmicTextSecondary else CosmicTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DreamArchiveCard(
    dream: DreamEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showConfirmDelete = true }
            )
            .testTag("dream_card_${dream.id}"),
        colors = CardDefaults.cardColors(
            containerColor = CosmicMidnightCard.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(CosmicPrimaryPurple.copy(0.3f), Color.Transparent)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dream.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Mood Indicator chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (dream.mood.lowercase()) {
                                "calm" -> CosmicGlowGreen.copy(alpha = 0.2f)
                                "terrifying" -> CosmicGlowRed.copy(alpha = 0.2f)
                                else -> CosmicSecondaryCyan.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dream.mood.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = when (dream.mood.lowercase()) {
                            "calm" -> CosmicGlowGreen
                            "terrifying" -> CosmicGlowRed
                            else -> CosmicSecondaryCyan
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = dream.originalText,
                fontSize = 13.sp,
                color = CosmicTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dream.style,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CosmicPrimaryPurple
                )

                val sdf = remember { SimpleDateFormat("MMM dd, yyyy · H:mm", Locale.getDefault()) }
                Text(
                    text = sdf.format(Date(dream.timestamp)),
                    fontSize = 11.sp,
                    color = CosmicTextMuted
                )
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Erase Dream?", color = CosmicTextPrimary) },
            text = { Text("This story will be wiped from your subconscious vault permanently.", color = CosmicTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    },
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Erase", color = CosmicGlowRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancel", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicMidnightSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun EmptyVaultState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = "Empty",
            tint = CosmicTextMuted,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your dream catalog is empty",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CosmicTextPrimary
        )
        Text(
            text = "Record dynamic dreams immediately after waking up to construct visual stories.",
            fontSize = 13.sp,
            color = CosmicTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PinSetupOverlay(
    currentPin: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentPin == null) "Set Subconscious Lock" else "Erase Security Lock",
                color = CosmicTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (currentPin == null) "Choose a 4-digit numeric code to protect your dream journal. Only you will possess the key."
                           else "Confirm removal of passcode safety envelope on past dream archives.",
                    color = CosmicTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (currentPin == null) {
                    TextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) enteredPin = it },
                        placeholder = { Text("4-Digit PIN") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicMidnightCard,
                            unfocusedContainerColor = CosmicMidnightCard,
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary,
                            focusedIndicatorColor = CosmicPrimaryPurple
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_input_field")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentPin != null) {
                        onSave(null)
                    } else if (enteredPin.length == 4) {
                        onSave(enteredPin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimaryPurple),
                modifier = Modifier.testTag("save_pin_button")
            ) {
                Text(if (currentPin == null) "Enable PIN" else "Remove Security Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CosmicTextSecondary)
            }
        },
        containerColor = CosmicMidnightSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

