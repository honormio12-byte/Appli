package com.sitotv.iptv.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.sitotv.iptv.R
import com.sitotv.iptv.SitoTVApp
import com.sitotv.iptv.databinding.DialogAddPlaylistBinding
import com.sitotv.iptv.models.PlaylistEntity
import com.sitotv.iptv.utils.M3UParser
import kotlinx.coroutines.launch

/**
 * Dialog to add a playlist via:
 *   1. M3U URL  (paste a link)
 *   2. M3U File (pick from storage)
 *   3. Xtream Codes (server + user + pass)
 */
class AddPlaylistDialog : DialogFragment() {

    private var _binding: DialogAddPlaylistBinding? = null
    private val binding get() = _binding!!
    private val db by lazy { SitoTVApp.instance.database }

    // File picker result
    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handleM3UFile(uri) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.75).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(R.color.bg_card)

        // Tab switching
        binding.tabM3uUrl.setOnClickListener { showTab(Tab.M3U_URL) }
        binding.tabM3uFile.setOnClickListener { showTab(Tab.M3U_FILE) }
        binding.tabXtream.setOnClickListener { showTab(Tab.XTREAM) }

        // Buttons
        binding.btnPickFile.setOnClickListener { openFilePicker() }
        binding.btnAdd.setOnClickListener { onAddClicked() }
        binding.btnCancel.setOnClickListener { dismiss() }

        showTab(Tab.M3U_URL)
    }

    private fun showTab(tab: Tab) {
        binding.layoutM3uUrl.visibility = if (tab == Tab.M3U_URL) View.VISIBLE else View.GONE
        binding.layoutM3uFile.visibility = if (tab == Tab.M3U_FILE) View.VISIBLE else View.GONE
        binding.layoutXtream.visibility = if (tab == Tab.XTREAM) View.VISIBLE else View.GONE

        listOf(binding.tabM3uUrl, binding.tabM3uFile, binding.tabXtream).forEach { it.alpha = 0.5f }
        when (tab) {
            Tab.M3U_URL -> binding.tabM3uUrl.alpha = 1f
            Tab.M3U_FILE -> binding.tabM3uFile.alpha = 1f
            Tab.XTREAM -> binding.tabXtream.alpha = 1f
        }
        currentTab = tab
    }

    private var currentTab = Tab.M3U_URL
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePicker.launch(Intent.createChooser(intent, "Choisir fichier M3U"))
    }

    private fun handleM3UFile(uri: Uri) {
        selectedFileUri = uri
        selectedFileName = uri.lastPathSegment ?: "playlist.m3u"
        binding.tvSelectedFile.text = selectedFileName
        binding.tvSelectedFile.visibility = View.VISIBLE
    }

    private fun onAddClicked() {
        val name = binding.etPlaylistName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etPlaylistName.error = "Nom requis"
            return
        }

        when (currentTab) {
            Tab.M3U_URL -> addM3UUrl(name)
            Tab.M3U_FILE -> addM3UFile(name)
            Tab.XTREAM -> addXtream(name)
        }
    }

    private fun addM3UUrl(name: String) {
        val url = binding.etM3uUrl.text.toString().trim()
        if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            binding.etM3uUrl.error = "URL invalide (doit commencer par http:// ou https://)"
            return
        }

        lifecycleScope.launch {
            binding.btnAdd.isEnabled = false
            binding.progressAdd.visibility = View.VISIBLE

            // Quick validation: try to load a few channels
            val channels = SitoTVApp.instance.streamRepository.loadM3UFromUrl(url)
            if (channels.isEmpty()) {
                binding.tvAddError.text = "Impossible de charger la playlist (URL incorrecte ou liste vide)"
                binding.tvAddError.visibility = View.VISIBLE
                binding.btnAdd.isEnabled = true
                binding.progressAdd.visibility = View.GONE
                return@launch
            }

            db.playlistDao().insert(
                PlaylistEntity(name = name, url = url, type = "m3u_url")
            )
            Toast.makeText(requireContext(), "${channels.size} chaines chargees!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun addM3UFile(name: String) {
        val uri = selectedFileUri
        if (uri == null) {
            Toast.makeText(requireContext(), "Veuillez choisir un fichier", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.btnAdd.isEnabled = false
            binding.progressAdd.visibility = View.VISIBLE

            try {
                val stream = requireContext().contentResolver.openInputStream(uri)!!
                val channels = M3UParser.parseFromStream(stream)

                if (channels.isEmpty()) {
                    binding.tvAddError.text = "Fichier M3U invalide ou vide"
                    binding.tvAddError.visibility = View.VISIBLE
                    binding.btnAdd.isEnabled = true
                    binding.progressAdd.visibility = View.GONE
                    return@launch
                }

                // Store URI as url for file playlists
                db.playlistDao().insert(
                    PlaylistEntity(name = name, url = uri.toString(), type = "m3u_file")
                )
                Toast.makeText(requireContext(), "${channels.size} chaines chargees!", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                binding.tvAddError.text = "Erreur: ${e.message}"
                binding.tvAddError.visibility = View.VISIBLE
                binding.btnAdd.isEnabled = true
                binding.progressAdd.visibility = View.GONE
            }
        }
    }

    private fun addXtream(name: String) {
        val server = binding.etXtreamServer.text.toString().trim()
        val user   = binding.etXtreamUser.text.toString().trim()
        val pass   = binding.etXtreamPass.text.toString().trim()

        if (server.isEmpty()) { binding.etXtreamServer.error = "Requis"; return }
        if (user.isEmpty())   { binding.etXtreamUser.error = "Requis"; return }
        if (pass.isEmpty())   { binding.etXtreamPass.error = "Requis"; return }

        lifecycleScope.launch {
            binding.btnAdd.isEnabled = false
            binding.progressAdd.visibility = View.VISIBLE

            val valid = SitoTVApp.instance.streamRepository.validateXtream(server, user, pass)
            if (!valid) {
                binding.tvAddError.text = "Connexion echouee. Verifiez vos identifiants."
                binding.tvAddError.visibility = View.VISIBLE
                binding.btnAdd.isEnabled = true
                binding.progressAdd.visibility = View.GONE
                return@launch
            }

            db.playlistDao().insert(
                PlaylistEntity(
                    name = name,
                    url = server,
                    type = "xtream",
                    username = user,
                    password = pass
                )
            )
            Toast.makeText(requireContext(), "Xtream connecte!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class Tab { M3U_URL, M3U_FILE, XTREAM }
}
