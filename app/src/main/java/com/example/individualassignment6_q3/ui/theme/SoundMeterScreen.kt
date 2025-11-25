package com.example.individualassignment6_q3

import android.Manifest
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Main screen for the Sound Meter app
 * Displays real-time decibel levels with visual indicators
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SoundMeterScreen(
    modifier: Modifier = Modifier,
    viewModel: SoundMeterViewModel = viewModel()
) {
    val context = LocalContext.current

    // Handle microphone permission using Accompanist library
    val microphonePermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    // Update ViewModel when permission status changes
    LaunchedEffect(microphonePermissionState.status.isGranted) {
        viewModel.checkPermission(microphonePermissionState.status.isGranted)
    }

    // Main content layout
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Sound Meter",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Debug message (optional - shows in simulation mode)
        if (viewModel.debugMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewModel.debugMessage,
                fontSize = 12.sp,
                color = Color.Red,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Decibel Display Card
        // Background color changes to red when threshold is exceeded
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (viewModel.isThresholdExceeded) {
                    Color(0xFFFFCDD2)  // Light red when loud
                } else {
                    MaterialTheme.colorScheme.surfaceVariant  // Normal color
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large decibel number
                Text(
                    text = "${viewModel.decibelLevel.toInt()}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isThresholdExceeded) {
                        Color(0xFFD32F2F)  // Dark red when loud
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                // "dB" label
                Text(
                    text = "dB",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Visual Sound Meter (progress bar)
        SoundMeterVisualizer(
            decibelLevel = viewModel.decibelLevel,
            isThresholdExceeded = viewModel.isThresholdExceeded
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Alert Message Card (only shown when threshold exceeded)
        if (viewModel.isThresholdExceeded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD32F2F)  // Red background
                )
            ) {
                Text(
                    text = "⚠️ Noise level exceeds safe threshold!",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Control Button
        if (!viewModel.hasPermission) {
            // Show permission request button if not granted
            Button(
                onClick = { microphonePermissionState.launchPermissionRequest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Grant Microphone Permission")
            }
        } else {
            // Show start/stop recording button
            Button(
                onClick = {
                    if (viewModel.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isRecording) {
                        Color(0xFFD32F2F)  // Red when recording (stop button)
                    } else {
                        MaterialTheme.colorScheme.primary  // Blue when not recording (start)
                    }
                )
            ) {
                // Icon (microphone or stop)
                Icon(
                    imageVector = if (viewModel.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Button text
                Text(
                    text = if (viewModel.isRecording) "Stop Recording" else "Start Recording",
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Threshold indicator text
        Text(
            text = "Threshold: 75 dB",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Visual progress bar showing sound level
 * Changes color based on sound intensity
 */
@Composable
fun SoundMeterVisualizer(
    decibelLevel: Double,
    isThresholdExceeded: Boolean
) {
    // Animate the progress value for smooth transitions
    // Convert 0-120 dB to 0-1 progress value
    val progress by animateFloatAsState(
        targetValue = (decibelLevel / 120.0).toFloat().coerceIn(0f, 1f),
        label = "progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Label
        Text(
            text = "Sound Level",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar container (background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Progress bar fill (foreground)
            // Width changes based on sound level
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)  // Animated width
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        // Color changes based on sound level
                        when {
                            isThresholdExceeded -> Color(0xFFD32F2F)  // Red: > 75 dB
                            progress > 0.6f -> Color(0xFFFFA726)       // Orange: > 72 dB
                            progress > 0.3f -> Color(0xFFFFCA28)       // Yellow: > 36 dB
                            else -> Color(0xFF66BB6A)                  // Green: <= 36 dB
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scale labels (0 dB to 120 dB)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0 dB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("120 dB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}