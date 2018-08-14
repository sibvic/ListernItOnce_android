package com.sibvic.listernitonce.Player

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

import com.sibvic.listernitonce.Media.FileFactory
import com.sibvic.listernitonce.Media.MediaFile

import java.io.File
import java.util.ArrayList

/**
 * Callback for the media session.
 */

internal class MediaSessionCompatCallback(private val _context: Context, //private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
                                          private val player: Player) : MediaSessionCompat.Callback() {
    val files = ArrayList<MediaFile>()
    private var targetFolder: String? = "/"
    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { Log.d("lio", "") }

    private fun refreshFiles() {
        Log.d("lio", "Refreshing files: " + targetFolder!!)
        files.clear()
        if (targetFolder != "") {
            val directory = File(targetFolder!!)
            FileFactory.addFilesFromFolder(files, directory)
        }
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        Log.d("lio", "Play: " + mediaId!!)
        val fileToPlay = findFileById(mediaId)
        if (fileToPlay != null) {
            player.play(fileToPlay)
        } else {
            Log.d("lio", "File not found: $mediaId")
        }
    }

    private fun findFileById(mediaId: String?): MediaFile? {
        for (file in files) {
            if (file.file.absolutePath == mediaId) {
                return file
            }
        }
        return null
    }

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
        Log.d("lio", "Setting new target folder: " + mediaId!!)
        targetFolder = mediaId
        super.onPrepareFromMediaId(mediaId, extras)
        refreshFiles()
    }

    override fun onPlay() {
        val am = _context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = am.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player.resume()
            //registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
        }
    }

    override fun onStop() {
        val am = _context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.abandonAudioFocus(afChangeListener)
        //unregisterReceiver(myNoisyAudioStreamReceiver);
        player.stop()
    }

    override fun onPause() {
        player.pause()
    }

    fun removeFile(index: Int) {
        files.removeAt(index)
    }
}
