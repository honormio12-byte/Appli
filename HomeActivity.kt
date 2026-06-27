package com.sitotv.iptv.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sitotv.iptv.R
import com.sitotv.iptv.SitoTVApp
import com.sitotv.iptv.adapters.ChannelAdapter
import com.sitotv.iptv.adapters.CategoryAdapter
import com.sitotv.iptv.adapters.PlaylistAdapter
import com.sitotv.iptv.databinding.ActivityHomeBinding
import com.sitotv.iptv.fragments.AddPlaylistDialog
import com.sitotv.iptv.models.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val db by lazy { SitoTVApp.instance.database }
    private val repo by lazy { SitoTVApp.instance.streamRepository }

    private val channelAdapter = ChannelAdapter { channel -> playChannel(channel) }
    private val categoryAdapter = CategoryAdapter { cat -> filterByCategory(cat) }

    private var allChannels: List<Channel> = emptyList()
    private var currentMode = HomeMode.LIVE
    private var currentPlaylistId: Int = -1
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observePlaylists()
    }

    private fun setupUI() {
        // Channels grid
        binding.rvChannels.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 4)
            adapter = channelAdapter
        }

        // Categories sidebar
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = categoryAdapter
        }

        // Bottom navigation
        binding.btnLive.setOnClickListener { setMode(HomeMode.LIVE) }
        binding.btnMovies.setOnClickListener { setMode(HomeMode.MOVIES) }
        binding.btnSeries.setOnClickListener { setMode(HomeMode.SERIES) }
        binding.btnPlaylists.setOnClickListener { showPlaylistPanel() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Add playlist button
        binding.btnAddPlaylist.setOnClickListener {
            AddPlaylistDialog().show(supportFragmentManager, "add_playlist")
        }

        // Search
        binding.etSearch.setOnEditorActionListener { tv, _, _ ->
            filterBySearch(tv.text.toString())
            true
        }

        setMode(HomeMode.LIVE)
    }

    private fun observePlaylists() {
        lifecycleScope.launch {
            db.playlistDao().getAllPlaylists().collectLatest { playlists ->
                if (playlists.isEmpty()) {
                    showEmptyState()
                } else {
                    binding.tvNoPlaylist.visibility = View.GONE
                    if (currentPlaylistId == -1 && playlists.isNotEmpty()) {
                        selectPlaylist(playlists.first())
                    }
                }
            }
        }
    }

    private fun selectPlaylist(playlist: PlaylistEntity) {
        currentPlaylistId = playlist.id
        binding.tvCurrentPlaylist.text = playlist.name
        loadChannels(playlist)
    }

    private fun loadChannels(playlist: PlaylistEntity) {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvChannels.visibility = View.GONE

            try {
                allChannels = when (playlist.type) {
                    "m3u_url" -> repo.loadM3UFromUrl(playlist.url)
                    "xtream" -> when (currentMode) {
                        HomeMode.LIVE -> repo.getXtreamLiveStreams(playlist.url, playlist.username, playlist.password)
                        HomeMode.MOVIES -> repo.getXtreamVod(playlist.url, playlist.username, playlist.password)
                        HomeMode.SERIES -> repo.getXtreamSeries(playlist.url, playlist.username, playlist.password)
                    }
                    else -> emptyList()
                }

                // Filter by type for M3U
                if (playlist.type == "m3u_url") {
                    allChannels = when (currentMode) {
                        HomeMode.LIVE -> allChannels.filter { it.type == StreamType.LIVE }
                        HomeMode.MOVIES -> allChannels.filter { it.type == StreamType.MOVIE }
                        HomeMode.SERIES -> allChannels.filter { it.type == StreamType.SERIES }
                    }
                }

                updateCategories()
                channelAdapter.submitList(allChannels)
                db.playlistDao().updateLastSync(playlist.id, System.currentTimeMillis())

            } catch (e: Exception) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = getString(R.string.error_loading)
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.rvChannels.visibility = View.VISIBLE
            }
        }
    }

    private fun updateCategories() {
        val cats = allChannels
            .groupBy { it.group }
            .map { (grp, chs) -> Category(name = grp.ifEmpty { "Autres" }, count = chs.size) }
            .sortedByDescending { it.count }
        categoryAdapter.submitList(listOf(Category(name = getString(R.string.all_categories))) + cats)
    }

    private fun filterByCategory(cat: Category) {
        val filtered = if (cat.name == getString(R.string.all_categories)) {
            allChannels
        } else {
            allChannels.filter { it.group == cat.name }
        }
        channelAdapter.submitList(filtered)
    }

    private fun filterBySearch(query: String) {
        if (query.isEmpty()) {
            channelAdapter.submitList(allChannels)
            return
        }
        val filtered = allChannels.filter { it.name.contains(query, ignoreCase = true) }
        channelAdapter.submitList(filtered)
    }

    private fun setMode(mode: HomeMode) {
        currentMode = mode
        // Update button states
        listOf(binding.btnLive, binding.btnMovies, binding.btnSeries).forEach {
            it.alpha = 0.5f
        }
        when (mode) {
            HomeMode.LIVE -> binding.btnLive.alpha = 1.0f
            HomeMode.MOVIES -> binding.btnMovies.alpha = 1.0f
            HomeMode.SERIES -> binding.btnSeries.alpha = 1.0f
        }
        // Reload with current playlist
        if (currentPlaylistId != -1) {
            lifecycleScope.launch {
                db.playlistDao().getById(currentPlaylistId)?.let { loadChannels(it) }
            }
        }
    }

    private fun showPlaylistPanel() {
        binding.playlistPanel.visibility = if (binding.playlistPanel.visibility == View.VISIBLE)
            View.GONE else View.VISIBLE
    }

    private fun showEmptyState() {
        binding.tvNoPlaylist.visibility = View.VISIBLE
        binding.rvChannels.visibility = View.GONE
    }

    private fun playChannel(channel: Channel) {
        val intent = when (channel.type) {
            StreamType.LIVE -> Intent(this, LivePlayerActivity::class.java)
            else -> Intent(this, VodPlayerActivity::class.java)
        }
        intent.putExtra("channel", channel)
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showPlaylistPanel()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

enum class HomeMode { LIVE, MOVIES, SERIES }
