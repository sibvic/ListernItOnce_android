package com.sibvic.listernitonce.Player;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.sibvic.listernitonce.Media.MediaFile;

import java.io.IOException;
import java.util.Timer;

/**
 * Plays MediaFile
 * Player starts playing from the last position.
 * Updates current position in the MediaFile
 */
public class Player implements MediaPlayer.OnCompletionListener {
    private MediaPlayer mediaPlayer;
    private MediaFile mediaFile;
    private PlayerCallback listener;
    private UpdatePlaybackPositionTimerTask positionUpdater;

    public Player(PlayerCallback listener) {
        this.listener = listener;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        Timer timer = new Timer();
        positionUpdater = new UpdatePlaybackPositionTimerTask(mediaPlayer);
        timer.schedule(positionUpdater, 1000, 1000);
    }

    public void play(MediaFile mediaFile) {
        if (this.mediaFile != null) {
            stop();
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(mediaFile.getFile().getPath());
            mediaPlayer.prepare();
        } catch (IOException e) {
            //TODO:
            e.printStackTrace();
            return;
        }
        setMediaFile(mediaFile);
        Log.d("lio", String.format("starting %1$s", mediaFile.getFileName()));
        mediaPlayer.start();
        if (mediaFile.getCurrentPosition() > 0) {
            mediaPlayer.seekTo((int)mediaFile.getCurrentPosition() * 1000);
        }
        this.mediaFile.setLength(mediaPlayer.getDuration() / 1000);
        listener.onStarted(mediaFile);
    }

    private void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
        positionUpdater.setFile(mediaFile);
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            Log.d("lio", String.format("pausing %1$s", mediaFile.getFileName()));
            mediaPlayer.pause();
            listener.onPaused(mediaFile);
        }
    }

    public void resume() {
        if (!mediaPlayer.isPlaying() && mediaFile != null) {
            Log.d("lio", String.format("resuming %1$s", mediaFile.getFileName()));
            mediaPlayer.start();
            listener.onResumed(mediaFile);
        }
    }

    public boolean isPlaying() {
        if (mediaFile == null) {
            return false;
        }
        return mediaPlayer.isPlaying();
    }

    public void stop() {
        if (mediaFile != null) {
            Log.d("lio", String.format("stopping %1$s", mediaFile.getFileName()));
            mediaPlayer.stop();
            listener.onStopped(mediaFile);
            setMediaFile(null);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //TODO: test
        if (mediaFile != null) {
            Log.d("lio", String.format("%1$s stopped", mediaFile.getFileName()));
            listener.onStopped(mediaFile);
            setMediaFile(null);
        }
    }

    public void release() {
        releaseMediaPlayer();
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }
}
