package com.sibvic.listernitonce

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView

import com.sibvic.listernitonce.Player.MediaPlaybackService

class FilesListActivity : AppCompatActivity() {
    internal var adapter: FileListAdapter? = null
    internal var player: MediaBrowserCompat? = null
    internal var listItems = FileInfoList()
    internal var currentlyPlaying: FileInfo? = null
    private val getPermissionId = 0

    private var optionsChangeListener: SharedPreferences.OnSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "target_path") {
            val settings = PreferenceManager
                    .getDefaultSharedPreferences(this@FilesListActivity)
            val targetFolder = settings.getString("target_path", "")
            MediaControllerCompat.getMediaController(this@FilesListActivity)
                    .transportControls
                    .prepareFromMediaId(targetFolder, null)
        }
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            // Get the token for the MediaSession
            val token = player!!.sessionToken

            // Create a MediaControllerCompat
            val mediaController: MediaControllerCompat
            try {
                mediaController = MediaControllerCompat(this@FilesListActivity, // Context
                        token)
            } catch (e: RemoteException) {
                e.printStackTrace()
                return
            }

            // Save the controller
            MediaControllerCompat.setMediaController(this@FilesListActivity, mediaController)
            val settings = PreferenceManager
                    .getDefaultSharedPreferences(this@FilesListActivity)

            val target_folder = settings.getString("target_path", "")
            val transportControls = mediaController.transportControls
            transportControls?.prepareFromMediaId(target_folder, null)

            // Finish building the UI
            buildTransportControls()
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }
    }

    internal var controllerCallback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            updateTitle(metadata)
            val fileId = metadata!!.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            currentlyPlaying = listItems.findFileById(fileId)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (currentlyPlaying == null) {
                val metadata = MediaControllerCompat
                        .getMediaController(this@FilesListActivity).metadata
                if (metadata != null) {
                    onMetadataChanged(metadata)
                }
            }
            updatePlayPauseButton(state!!)
            updateProgress(state)
        }
    }

    private val _handler = Handler()

    private val _showOptionsTask = Runnable { showOptions() }

    private val mSubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String,
                                      children: List<MediaBrowserCompat.MediaItem>) {
            listItems.clear()
            for (item in children) {
                val description = item.description
                val extras = description.extras
                val title = description.title
                val duration = extras?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0
                val current_position = extras?.getLong("current_position") ?: 0
                val fileInfo = FileInfo(item.mediaId,
                        title?.toString() ?: "",
                        duration.toInt(),
                        current_position.toInt())
                listItems.add(fileInfo)
            }

            adapter = FileListAdapter(this@FilesListActivity, listItems.toArrayList())
            adapter?.notifyDataSetChanged()

            val listView = findViewById<View>(android.R.id.list) as ListView
            listView.adapter = adapter
            listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                val fileToPlay = adapter?.getItem(position)
                playFile(fileToPlay!!)
            }
        }

        override fun onError(id: String) {}
    }

    internal fun isPermissionGranted(name: String): Boolean {
        return ContextCompat.checkSelfPermission(this, name) != PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files_list)

        if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
                || !isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || !isPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE),
                    getPermissionId)
        } else {
            InitPlayer()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            getPermissionId -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    InitPlayer()
                    player!!.connect()
                }
            }
        }
    }

    private fun InitPlayer() {
        player = MediaBrowserCompat(this,
                ComponentName(this, MediaPlaybackService::class.java),
                connectionCallbacks, null)
        player!!.subscribe("root", mSubscriptionCallback)
        handlePreferences()
    }

    override fun onStart() {
        super.onStart()
        if (player != null) {
            player!!.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        if (MediaControllerCompat.getMediaController(this@FilesListActivity) != null) {
            MediaControllerCompat.getMediaController(this@FilesListActivity).unregisterCallback(controllerCallback)
        }
        if (player != null) {
            player!!.disconnect()
        }
    }

    internal fun buildTransportControls() {
        val playPauseButton = findViewById<View>(R.id.play_pause) as ImageButton
        // Attach a listener to the button
        playPauseButton.setOnClickListener {
            // Since this is a play/pause button, you'll need to test the current state
            // and choose the action accordingly

            val pbState = MediaControllerCompat.getMediaController(this@FilesListActivity).playbackState.state
            if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                MediaControllerCompat.getMediaController(this@FilesListActivity).transportControls.pause()
            } else {
                MediaControllerCompat.getMediaController(this@FilesListActivity).transportControls.play()
            }
        }

        val mediaController = MediaControllerCompat.getMediaController(this@FilesListActivity)

        // Display the initial state
        updateTitle(mediaController.metadata)
        updatePlayPauseButton(mediaController.playbackState)

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)
    }

    private fun updatePlayPauseButton(pbState: PlaybackStateCompat) {
        val playPauseButton = findViewById<View>(R.id.play_pause) as ImageButton
        val state = pbState.state
        when (state) {
            PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_STOPPED -> {
                playPauseButton.isEnabled = false
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                playPauseButton.isEnabled = true
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
            }
            PlaybackStateCompat.STATE_PLAYING -> {
                playPauseButton.isEnabled = true
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            }
            else -> {
                Log.d("lio", "Unknown state " + Integer.toString(state))
            }
        }
    }

    private fun updateTitle(metadata: MediaMetadataCompat?) {
        val currentFileName = findViewById<View>(R.id.current_file_name) as TextView
        val title = formatTitle(metadata)
        currentFileName.text = title
    }

    private fun formatTitle(metadata: MediaMetadataCompat?): String {
        if (metadata == null) {
            return "---"
        }
        val title = metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE).toString()
        return if (title == "") {
            "---"
        } else {
            title
        }
    }

    private fun updateProgress(state: PlaybackStateCompat?) {
        if (currentlyPlaying == null) {
            return
        }
        currentlyPlaying!!.currentPosition = state!!.position.toInt()
        val indexOfFile = listItems.indexOf(currentlyPlaying!!)
        if (indexOfFile != -1) {
            val listView = findViewById<View>(android.R.id.list) as ListView
            if (state.state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT && currentlyPlaying!!.length >= currentlyPlaying!!.currentPosition) {
                listItems.remove(indexOfFile)
                adapter?.remove(currentlyPlaying)
                adapter?.notifyDataSetChanged()
            } else {
                val v = listView.getChildAt(indexOfFile - listView.firstVisiblePosition)
                if (v != null) {
                    adapter?.updateRow(indexOfFile, v)
                }
            }
        }
    }

    private fun handlePreferences() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        settings.registerOnSharedPreferenceChangeListener(optionsChangeListener)
        val target_folder = settings.getString("target_path", "")
        if (target_folder == "") {
            _handler.postDelayed(_showOptionsTask, 100)
        }
    }

    private fun showOptions() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun playFile(fileToPlay: FileInfo) {
        val transportControls = MediaControllerCompat.getMediaController(this@FilesListActivity).transportControls
        transportControls.playFromMediaId(fileToPlay.id, null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_files_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                showOptions()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
