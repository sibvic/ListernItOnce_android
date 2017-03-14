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
    private Player mPlayer;
    private ArrayList<MediaFile> mFiles = new ArrayList<>();
    private String targetFolder = "/";

    MediaSessionCompatCallback(Context context, Player player) {
        this.mContext = context;
        this.mPlayer = player;
    }

    ArrayList<MediaFile> getFiles() {
        return mFiles;
    }

    private void refreshFiles() {
        mFiles.clear();
        if (!targetFolder.equals("")) {
            File directory = new File(targetFolder);
            FileFactory.addFilesFromFolder(mFiles, directory);
        }
    }

    private Context mContext;
    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    };
//    private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
//    private MediaStyleNotification myPlayerNotification;
    //private MediaBrowserService service;

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        MediaFile fileToPlay = findFileById(mediaId);
        if (fileToPlay != null) {
            mPlayer.play(fileToPlay);
        }
        else {
            Log.d("lio", "File not found: " + mediaId);
        }
    }

    private MediaFile findFileById(String mediaId) {
        for (MediaFile file : mFiles) {
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
        AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mPlayer.resume();
            //registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
        }
    }

    @Override
    public void onStop() {
        AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(afChangeListener);
        //unregisterReceiver(myNoisyAudioStreamReceiver);
        mPlayer.stop();
    }

    @Override
    public void onPause() {
        mPlayer.pause();
        //unregisterReceiver(myNoisyAudioStreamReceiver, intentFilter);
    }
}
