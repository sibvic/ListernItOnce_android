package com.sibvic.listernitonce.Player;

import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.sibvic.listernitonce.Media.FileFactory;
import com.sibvic.listernitonce.Media.MediaFile;

import java.io.File;
import java.util.ArrayList;

/**
 * Callback for the media session.
 */

class MediaSessionCompatCallback extends MediaSessionCompat.Callback {

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private Player _player;
    private ArrayList<MediaFile> _files = new ArrayList<>();
    private String _targetFolder = "/";

    MediaSessionCompatCallback(Context context, Player player) {
        this._context = context;
        this._player = player;
    }

    ArrayList<MediaFile> get_files() {
        return _files;
    }

    private void refreshFiles() {
        Log.d("lio", "Refreshing files: " + _targetFolder);
        _files.clear();
        if (!_targetFolder.equals("")) {
            File directory = new File(_targetFolder);
            FileFactory.addFilesFromFolder(_files, directory);
        }
    }

    private Context _context;
    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d("lio", "");
        }
    };

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        Log.d("lio", "Play: " + mediaId);
        MediaFile fileToPlay = findFileById(mediaId);
        if (fileToPlay != null) {
            _player.play(fileToPlay);
        }
        else {
            Log.d("lio", "File not found: " + mediaId);
        }
    }

    private MediaFile findFileById(String mediaId) {
        for (MediaFile file : _files) {
            if (file.getFile().getAbsolutePath().equals(mediaId)) {
                return file;
            }
        }
        return null;
    }

    @Override
    public void onPrepareFromMediaId(String mediaId, Bundle extras) {
        Log.d("lio", "Setting new target folder: " + mediaId);
        _targetFolder = mediaId;
        super.onPrepareFromMediaId(mediaId, extras);
        refreshFiles();
    }

    @Override
    public void onPlay() {
        AudioManager am = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            _player.resume();
            //registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
        }
    }

    @Override
    public void onStop() {
        AudioManager am = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(afChangeListener);
        //unregisterReceiver(myNoisyAudioStreamReceiver);
        _player.stop();
    }

    @Override
    public void onPause() {
        _player.pause();
    }

    void removeFile(int index) {
        _files.remove(index);
    }
}
