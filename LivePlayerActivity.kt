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
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultDataSource
import com.sitotv.iptv.databinding.ActivityLivePlayerBinding
import com.sitotv.iptv.models.Channel
import android.os.Handler
import android.os.Looper

class LivePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivePlayerBinding
    private var player: ExoPlayer? = null
    private var channel: Channel? = null
    private val hideHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLivePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        @Suppress("DEPRECATION")
        channel = intent.getParcelableExtra("channel")
        binding.tvChannelName.text = channel?.name ?: ""

        setupPlayer()
        autoHideControls()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo

            val url = channel?.url ?: return
            val mediaItem = MediaItem.fromUri(url)
            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.playWhenReady = true

            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = "Erreur: ${error.message}"
                    binding.progressBar.visibility = View.GONE
                }

                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        Player.STATE_READY -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                        }
                        else -> {}
                    }
                }
            })
        }
    }

    private fun autoHideControls() {
        showControls()
        hideHandler.postDelayed({ hideControls() }, 4000)
    }

    private fun showControls() {
        binding.controlsOverlay.visibility = View.VISIBLE
        hideHandler.removeCallbacksAndMessages(null)
        hideHandler.postDelayed({ hideControls() }, 4000)
    }

    private fun hideControls() {
        binding.controlsOverlay.visibility = View.GONE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        showControls()
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player?.isPlaying == true) player?.pause() else player?.play()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
        hideHandler.removeCallbacksAndMessages(null)
    }
}
