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
    private Player player;
    private ArrayList<MediaFile> files = new ArrayList<>();
    private String targetFolder = "/";

    MediaSessionCompatCallback(Context context, Player player) {
        this.context = context;
        this.player = player;
    }

    ArrayList<MediaFile> getFiles() {
        return files;
    }

    private void refreshFiles() {
        files.clear();
        if (!targetFolder.equals("")) {
            File directory = new File(targetFolder);
            FileFactory.addFilesFromFolder(files, directory);
        }
    }

    private Context context;
    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    };
//    private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
//    private MediaStyleNotification myPlayerNotification;

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        MediaFile fileToPlay = findFileById(mediaId);
        if (fileToPlay != null) {
            player.play(fileToPlay);
        }
        else {
            Log.d("lio", "File not found: " + mediaId);
        }
    }

    private MediaFile findFileById(String mediaId) {
        for (MediaFile file : files) {
            if (file.getFile().getAbsolutePath().equals(mediaId)) {
                return file;
            }
        }
        return null;
    }

    @Override
    public void onPrepareFromMediaId(String mediaId, Bundle extras) {
        super.onPrepareFromMediaId(mediaId, extras);
        targetFolder = mediaId;
        refreshFiles();
    }

    @Override
    public void onPlay() {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player.resume();
            //registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
        }
    }

    @Override
    public void onStop() {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(afChangeListener);
        //unregisterReceiver(myNoisyAudioStreamReceiver);
        player.stop();
    }

    @Override
    public void onPause() {
        player.pause();
        //unregisterReceiver(myNoisyAudioStreamReceiver, intentFilter);
    }

    void removeFile(int index) {
        files.remove(index);
    }
}
