package com.dmitrysukhov.music

import android.app.Application
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BpmSoundApp() }
    }
}

@Composable
fun BpmSoundApp() {
    val viewModel: BpmViewModel = viewModel()
    val isPlaying by viewModel.isPlaying
    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { viewModel.togglePlay() }, modifier = Modifier.padding(16.dp)) {
            Text(if (isPlaying) "Stop" else "Start", fontSize = 24.sp)
        }
    }
}

class BpmViewModel(application: Application) : AndroidViewModel(application) {
    private val kickPlayer = MediaPlayer.create(application, R.raw.kick_a)
    private val snarePlayer = MediaPlayer.create(application, R.raw.snare_g)
    private val hihatPlayer = MediaPlayer.create(application, R.raw.hihat5)
    private val melodyPlayers = listOf(
        MediaPlayer.create(application, R.raw.c1), MediaPlayer.create(application, R.raw.d),
        MediaPlayer.create(application, R.raw.e), MediaPlayer.create(application, R.raw.f),
        MediaPlayer.create(application, R.raw.g), MediaPlayer.create(application, R.raw.a),
        MediaPlayer.create(application, R.raw.h), MediaPlayer.create(application, R.raw.c)
    )

    private var mainTimer: CountDownTimer? = null
    private val intervalMs = 300L  // Интервал 200 BPM
    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying
    private var beatCount = 0

    fun togglePlay() {
        if (_isPlaying.value) stopSound() else startSound()
    }

    private fun startSound() {
        _isPlaying.value = true
        beatCount = 0
        mainTimer = object : CountDownTimer(Long.MAX_VALUE, intervalMs) {
            override fun onTick(millisUntilFinished: Long) {
                playBeat()
                playMelodyNote()
                beatCount = (beatCount + 1) % 4
            }

            override fun onFinish() {}
        }.start()
    }

    private fun playBeat() {
        when (beatCount) {
            0 -> {
                launchSound(kickPlayer)
                launchSound(hihatPlayer)
            }

            1, 3 -> launchSound(hihatPlayer)

            2 -> {
                launchSound(snarePlayer)
                launchSound(hihatPlayer)
            }
        }
    }

    private fun playMelodyNote() {
        val randomNote = melodyPlayers.random()
        val noteDuration = when (Random.nextInt(4)) {
            0 -> intervalMs  // четвертная
            1 -> intervalMs * 2  // половинка
            2 -> intervalMs / 2  // восьмая
            else -> 0L  // пауза
        }
        if (noteDuration > 0) launchSound(randomNote)
    }

    private fun launchSound(player: MediaPlayer?) {
        player?.let {
            viewModelScope.launch {
                it.seekTo(0)
                it.start()
            }
        }
    }

    private fun stopSound() {
        _isPlaying.value = false
        mainTimer?.cancel()
        beatCount = 0
        listOf(kickPlayer, snarePlayer, hihatPlayer).forEach { it.stopAndReset() }
        melodyPlayers.forEach { it.stopAndReset() }
    }

    override fun onCleared() {
        super.onCleared()
        mainTimer?.cancel()
        listOf(kickPlayer, snarePlayer, hihatPlayer).forEach { it.release() }
        melodyPlayers.forEach { it.release() }
    }
}

private fun MediaPlayer.stopAndReset() {
    pause()
    seekTo(0)
}