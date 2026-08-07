package com.example.chess.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.chess.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val soundPool: SoundPool

    // Sound IDs
    private var moveSoundId: Int = 0
    private var captureSoundId: Int = 0
    private var castleSoundId: Int = 0
    private var promotionSoundId: Int = 0
    private var checkSoundId: Int = 0
    private var checkmateSoundId: Int = 0
    private var victorySoundId: Int = 0
    private var drawSoundId: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        // Placeholder for loading actual sound files from res/raw once available
        /*
        moveSoundId = soundPool.load(context, R.raw.move, 1)
        captureSoundId = soundPool.load(context, R.raw.capture, 1)
        castleSoundId = soundPool.load(context, R.raw.castle, 1)
        promotionSoundId = soundPool.load(context, R.raw.promotion, 1)
        checkSoundId = soundPool.load(context, R.raw.check, 1)
        checkmateSoundId = soundPool.load(context, R.raw.checkmate, 1)
        victorySoundId = soundPool.load(context, R.raw.victory, 1)
        drawSoundId = soundPool.load(context, R.raw.draw, 1)
        */
    }

    private fun playSound(soundId: Int) {
        if (soundId == 0) {
            Log.d("SoundManager", "Sound not loaded or file missing.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val isEnabled = settingsRepository.soundEnabled.first()
            if (!isEnabled) return@launch

            val volume = settingsRepository.soundVolume.first()
            
            soundPool.play(soundId, volume, volume, 1, 0, 1f)
        }
    }

    fun playMove() = playSound(moveSoundId)
    fun playCapture() = playSound(captureSoundId)
    fun playCastle() = playSound(castleSoundId)
    fun playPromotion() = playSound(promotionSoundId)
    fun playCheck() = playSound(checkSoundId)
    fun playCheckmate() = playSound(checkmateSoundId)
    fun playVictory() = playSound(victorySoundId)
    fun playDraw() = playSound(drawSoundId)
}
