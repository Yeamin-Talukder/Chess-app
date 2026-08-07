package com.example.chess.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.chess.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun playPattern(timings: LongArray, amplitudes: IntArray = intArrayOf()) {
        CoroutineScope(Dispatchers.IO).launch {
            if (vibrator == null || !vibrator.hasVibrator()) return@launch
            
            // Check user setting
            val isEnabled = settingsRepository.hapticsEnabled.first()
            if (!isEnabled) return@launch

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (amplitudes.isNotEmpty() && vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }

    fun playMove() {
        playPattern(longArrayOf(0, 5))
    }

    fun playCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playPattern(longArrayOf(0, 12, 20, 12), intArrayOf(0, 120, 0, 180))
        } else {
            playPattern(longArrayOf(0, 12, 20, 12))
        }
    }

    fun playIllegalMove() {
        playPattern(longArrayOf(0, 40, 40, 40, 40, 40))
    }

    fun playCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playPattern(longArrayOf(0, 50, 50, 100), intArrayOf(0, 255, 0, 255))
        } else {
            playPattern(longArrayOf(0, 50, 50, 100))
        }
    }

    fun playCheckmate() {
        playPattern(longArrayOf(0, 100, 50, 100, 50, 100, 50, 300))
    }

    fun playVictory() {
        playCheckmate()
    }

    fun playDraw() {
        playPattern(longArrayOf(0, 200, 100, 100, 100, 50))
    }

    fun playPromotion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playPattern(
                longArrayOf(0, 30, 30, 30, 30, 30, 30, 50),
                intArrayOf(0, 50, 0, 100, 0, 150, 0, 255)
            )
        } else {
            playPattern(longArrayOf(0, 30, 30, 30, 30, 30, 30, 50))
        }
    }
}
