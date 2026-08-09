package com.example.booktracker.app.journal

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.random.Random

enum class NoiseType {
    NONE, WHITE, BROWN, RAIN, FIRE, OCEAN
}

/**
 * Purely local, zero-cost generated soundscapes using AudioTrack.
 * Runs in a coroutine and generates noise on the fly, saving bundle size and network bandwidth.
 */
class SoundscapeEngine {
    private var audioTrack: AudioTrack? = null
    private var noiseJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    var currentNoiseType: NoiseType = NoiseType.NONE
        private set
        
    private var volume: Float = 0.5f

    fun setVolume(vol: Float) {
        volume = vol
        audioTrack?.setVolume(vol)
    }

    fun play(type: NoiseType) {
        if (currentNoiseType == type && noiseJob?.isActive == true) return
        stop()
        
        currentNoiseType = type
        if (type == NoiseType.NONE) return
        
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(8192)
        
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(audioFormat)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            
        audioTrack?.setVolume(volume)
        audioTrack?.play()
        
        noiseJob = scope.launch {
            val buffer = ShortArray(bufferSize / 2)
            var lastBrown = 0.0
            var lastRain = 0.0
            var oceanTime = 0.0
            
            while (isActive) {
                for (i in buffer.indices) {
                    val white = Random.nextDouble(-1.0, 1.0)
                    val sample = when (type) {
                        NoiseType.WHITE -> white
                        NoiseType.BROWN -> {
                            lastBrown = (lastBrown + (0.02 * white)) / 1.02
                            lastBrown * 3.5 
                        }
                        NoiseType.RAIN -> {
                            // Steeper low-pass filter for heavier sound
                            lastRain = (lastRain + (0.04 * white)) / 1.04
                            val rain = lastRain * 4.0
                            // Add occasional light drops (filtered white noise)
                            rain + if (Random.nextDouble() < 0.05) white * 0.1 else 0.0
                        }
                        NoiseType.FIRE -> {
                            // Low rumble
                            lastBrown = (lastBrown + (0.015 * white)) / 1.015
                            var fire = lastBrown * 2.5
                            // Occasional crackles/pops
                            if (Random.nextDouble() < 0.0003) {
                                fire += Random.nextDouble(0.6, 1.0) * (if (Random.nextBoolean()) 1 else -1)
                            }
                            fire
                        }
                        NoiseType.OCEAN -> {
                            lastBrown = (lastBrown + (0.02 * white)) / 1.02
                            oceanTime += 1.0 / sampleRate
                            // 8-second wave period
                            val lfo = (Math.sin(oceanTime * 2 * Math.PI / 8.0) + 1.0) / 2.0 
                            lastBrown * 3.5 * (0.2 + 0.8 * lfo)
                        }
                        else -> 0.0
                    }
                    
                    val clamped = sample.coerceIn(-1.0, 1.0)
                    buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
                }
                
                audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            }
        }
    }

    fun stop() {
        noiseJob?.cancel()
        noiseJob = null
        currentNoiseType = NoiseType.NONE
        
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore IllegalStateException if already released
        } finally {
            audioTrack = null
        }
    }
    
    fun release() {
        stop()
        scope.cancel()
    }
}
