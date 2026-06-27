package com.sitotv.iptv.activities

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sitotv.iptv.databinding.ActivityVodPlayerBinding
import com.sitotv.iptv.models.Channel
import android.os.Handler
import android.os.Looper

class VodPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVodPlayerBinding
    private var player: ExoPlayer? = null
    private var channel: Channel? = null
    private val hideHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVodPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        @Suppress("DEPRECATION")
        channel = intent.getParcelableExtra("channel")
        binding.tvTitle.text = channel?.name ?: ""

        setupPlayer()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo

            val url = channel?.url ?: return
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.playWhenReady = true

            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = "Erreur de lecture: ${error.message}"
                    binding.progressBar.visibility = View.GONE
                }

                override fun onPlaybackStateChanged(state: Int) {
                    binding.progressBar.visibility = if (state == Player.STATE_BUFFERING)
                        View.VISIBLE else View.GONE
                }
            })
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player?.isPlaying == true) player?.pause() else player?.play()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                player?.seekTo((player?.currentPosition ?: 0) + 10_000)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                player?.seekTo(maxOf(0, (player?.currentPosition ?: 0) - 10_000))
                true
            }
            KeyEvent.KEYCODE_BACK -> { finish(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
