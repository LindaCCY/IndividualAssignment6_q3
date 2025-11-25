package com.example.individualassignment6_q3

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10
import kotlin.random.Random

/**
 * ViewModel for the Sound Meter application
 * Handles audio recording, amplitude measurement, and decibel calculation
 */
class SoundMeterViewModel : ViewModel() {

    // State variables that trigger UI recomposition when changed

    /** Current decibel level (0-120 dB) */
    var decibelLevel by mutableStateOf(0.0)
        private set

    /** Whether the app is currently recording/measuring sound */
    var isRecording by mutableStateOf(false)
        private set

    /** Whether the current noise level exceeds the threshold */
    var isThresholdExceeded by mutableStateOf(false)
        private set

    /** Whether microphone permission has been granted */
    var hasPermission by mutableStateOf(false)
        private set

    /** Debug message displayed in the UI for troubleshooting */
    var debugMessage by mutableStateOf("")
        private set

    // Private variables for audio recording

    /** MediaRecorder for capturing audio from the microphone */
    private var mediaRecorder: MediaRecorder? = null

    /** Coroutine job for continuous sound level monitoring */
    private var recordingJob: Job? = null

    /** Temporary file where MediaRecorder writes audio data */
    private var outputFile: File? = null

    /** Threshold in dB above which an alert is triggered (75 dB = loud conversation) */
    private val threshold = 75.0

    /**
     * Whether to use simulation mode (generates random sound levels)
     * Set to true for demo/testing when microphone doesn't work
     * Set to false to use actual microphone input
     */
    private val useSimulation = true

    /**
     * Updates the permission status
     * Called when permission is granted or denied
     */
    fun checkPermission(hasPermission: Boolean) {
        this.hasPermission = hasPermission
    }

    /**
     * Starts recording and measuring sound levels
     * Uses simulation mode if useSimulation is true, otherwise uses real microphone
     */
    fun startRecording(context: android.content.Context) {
        // Check if permission was granted
        if (!hasPermission) {
            debugMessage = "No permission"
            Log.e("SoundMeter", "No permission")
            return
        }

        // Prevent starting multiple recordings
        if (isRecording) return

        // Use simulation mode if enabled
        if (useSimulation) {
            startSimulation()
            return
        }

        // Double-check permission at runtime (required for Android 6.0+)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            debugMessage = "Permission not granted"
            Log.e("SoundMeter", "Permission check failed")
            return
        }

        try {
            // Create a temporary file for MediaRecorder output
            // MediaRecorder requires an output file even though we only use getMaxAmplitude()
            outputFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.3gp")

            // Initialize MediaRecorder based on Android version
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires passing context
                MediaRecorder(context)
            } else {
                // Older Android versions use deprecated constructor
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                try {
                    // Configure MediaRecorder
                    setAudioSource(MediaRecorder.AudioSource.MIC)  // Use microphone
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)  // 3GP format
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)  // AMR audio codec
                    setOutputFile(outputFile!!.absolutePath)  // Set output file

                    prepare()  // Prepare MediaRecorder
                    start()    // Start recording

                    debugMessage = "MediaRecorder started successfully"
                    Log.d("SoundMeter", "MediaRecorder started")
                } catch (e: Exception) {
                    debugMessage = "Error: ${e.message}"
                    Log.e("SoundMeter", "Error in setup: ${e.message}", e)
                    throw e
                }
            }

            isRecording = true

            // Start a coroutine to continuously read amplitude values
            recordingJob = viewModelScope.launch(Dispatchers.IO) {
                var readCount = 0

                // Continue reading while recording
                while (isActive && isRecording) {
                    try {
                        // Get current amplitude (0-32767)
                        // This is the peak amplitude since last call
                        val amplitude = mediaRecorder?.maxAmplitude ?: 0
                        readCount++

                        debugMessage = "Read #$readCount - Amp: $amplitude"
                        Log.d("SoundMeter", "Amplitude: $amplitude (read #$readCount)")

                        // Convert amplitude to decibels
                        val db = amplitudeToDb(amplitude)

                        Log.d("SoundMeter", "Decibels: $db")

                        // Update UI state
                        decibelLevel = db
                        isThresholdExceeded = db > threshold

                    } catch (e: Exception) {
                        debugMessage = "Read error: ${e.message}"
                        Log.e("SoundMeter", "Error reading amplitude: ${e.message}")
                    }

                    // Wait 100ms before next reading (10 updates per second)
                    delay(100)
                }
            }
        } catch (e: Exception) {
            debugMessage = "Start error: ${e.message}"
            Log.e("SoundMeter", "Error starting recording: ${e.message}", e)
            stopRecording()
        }
    }

    /**
     * Simulation mode: generates random sound levels for demo/testing
     * Useful when the actual microphone doesn't work on emulator or device
     */
    private fun startSimulation() {
        isRecording = true
        debugMessage = "SIMULATION MODE - Making random sounds"
        Log.d("SoundMeter", "Starting simulation mode")

        // Start a coroutine to generate random sound levels
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            var time = 0

            while (isActive && isRecording) {
                // Simulate realistic sound patterns

                // Base ambient noise (35-45 dB - quiet room)
                val baseNoise = Random.nextDouble(35.0, 45.0)

                // Add occasional peaks (simulating speech/sounds)
                val peak = if (Random.nextDouble() > 0.7) {
                    // 30% chance of a loud sound (20-40 dB increase)
                    Random.nextDouble(20.0, 40.0)
                } else {
                    // 70% chance of quiet (0-10 dB increase)
                    Random.nextDouble(0.0, 10.0)
                }

                // Total dB level (clamped between 30-100 dB)
                val db = (baseNoise + peak).coerceIn(30.0, 100.0)

                debugMessage = "SIMULATION - Time: ${time}s"

                // Update UI state
                decibelLevel = db
                isThresholdExceeded = db > threshold

                time++
                delay(100)  // Update every 100ms
            }
        }
    }

    /**
     * Converts amplitude (0-32767) to decibels (dB)
     *
     * Formula: dB = 20 * log10(amplitude / reference)
     *
     * @param amplitude Raw amplitude value from MediaRecorder (0-32767)
     * @return Decibel level (30-120 dB)
     */
    private fun amplitudeToDb(amplitude: Int): Double {
        return if (amplitude > 0) {
            // Calculate ratio compared to max amplitude (32767)
            val ratio = amplitude.toDouble() / 32767.0

            // Convert to decibels using logarithmic scale
            // Multiply by 10000 to scale appropriately
            val decibels = 20 * log10(ratio * 10000 + 1)

            // Add offset and clamp to realistic range (30-120 dB)
            (decibels + 20).coerceIn(30.0, 120.0)
        } else {
            // Silence = 30 dB (quiet room)
            30.0
        }
    }

    /**
     * Stops recording and releases resources
     */
    fun stopRecording() {
        isRecording = false

        // Cancel the coroutine job
        recordingJob?.cancel()
        recordingJob = null

        // Stop and release MediaRecorder
        try {
            mediaRecorder?.apply {
                stop()     // Stop recording
                release()  // Release audio resources
            }
        } catch (e: Exception) {
            Log.e("SoundMeter", "Error stopping recorder: ${e.message}")
        }
        mediaRecorder = null

        // Delete temporary audio file
        outputFile?.delete()
        outputFile = null

        // Reset UI state
        decibelLevel = 0.0
        isThresholdExceeded = false
        debugMessage = "Stopped"
        Log.d("SoundMeter", "Recording stopped")
    }

    /**
     * Clean up when ViewModel is destroyed
     * Ensures MediaRecorder is properly stopped and released
     */
    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}