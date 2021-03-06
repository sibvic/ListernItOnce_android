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
class Player implements MediaPlayer.OnCompletionListener {
    private MediaPlayer mediaPlayer;
    private MediaFile mediaFile;
    private PlayerCallback listener;
    private UpdatePlaybackPositionTimerTask positionUpdater;

    Player(PlayerCallback listener) {
        this.listener = listener;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        Timer timer = new Timer();
        positionUpdater = new UpdatePlaybackPositionTimerTask(mediaPlayer);
        timer.schedule(positionUpdater, 1000, 1000);
    }

    void play(MediaFile mediaFile) {
        if (this.mediaFile != null) {
            stop();
        }
        if (!mediaFile.getFile().exists()) {
            listener.onCompleted(mediaFile);
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
        Log.d("lio", String.format("starting %1$s", mediaFile.getTitle()));
        mediaPlayer.start();
        if (mediaFile.getCurrentPosition() > 0) {
            mediaPlayer.seekTo((int)mediaFile.getCurrentPosition() * 1000);
        }
        listener.onStarted(mediaFile);
    }

    private void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
        positionUpdater.setFile(mediaFile);
    }

    void pause() {
        if (mediaPlayer.isPlaying()) {
            Log.d("lio", String.format("pausing %1$s", mediaFile.getTitle()));
            mediaPlayer.pause();
            listener.onPaused(mediaFile);
        }
    }

    void resume() {
        if (!mediaPlayer.isPlaying() && mediaFile != null) {
            Log.d("lio", String.format("resuming %1$s", mediaFile.getTitle()));
            mediaPlayer.start();
            listener.onResumed(mediaFile);
        }
    }

    boolean isPlaying() {
        return mediaFile != null && mediaPlayer.isPlaying();
    }

    void stop() {
        if (mediaFile != null) {
            Log.d("lio", String.format("stopping %1$s", mediaFile.getTitle()));
            mediaPlayer.stop();
            MediaFile stoppedFile = mediaFile;
            setMediaFile(null);
            listener.onStopped(stoppedFile);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mediaFile != null) {
            Log.d("lio", String.format("%1$s stopped", mediaFile.getTitle()));
            mediaFile.setCurrentPosition(mediaFile.getLength());
            MediaFile finishedFile = mediaFile;
            setMediaFile(null);
            listener.onCompleted(finishedFile);
        }
    }

    public void release() {
        releaseMediaPlayer();
    }

    private void releaseMediaPlayer() {
        if (mediaFile != null) {
            setMediaFile(null);
        }
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    MediaFile getMediaFile() {
        return mediaFile;
    }
}
