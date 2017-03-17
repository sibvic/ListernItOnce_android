package com.sibvic.listernitonce.Player;

import android.media.MediaPlayer;

import com.sibvic.listernitonce.Media.FileFactory;
import com.sibvic.listernitonce.Media.FileInformationWriter;
import com.sibvic.listernitonce.Media.MediaFile;

import java.util.TimerTask;

/**
 * Updates position of the media file every second
 */
class UpdatePlaybackPositionTimerTask extends TimerTask {
    private MediaPlayer player;
    private MediaFile file;

    UpdatePlaybackPositionTimerTask(MediaPlayer player) {
        this.player = player;
    }

    public void setFile(MediaFile file) {
        this.file = file;
    }

    private int tickCounter = 0;

    @Override
    public void run() {
        MediaFile currentFile = file;
        if (currentFile != null) {
            long lastPosition = currentFile.getCurrentPosition();
            int currentPosition = player.getCurrentPosition() / 1000;
            if (lastPosition != currentPosition) {
                currentFile.setCurrentPosition(currentPosition);
            }
            tickCounter++;
            if (tickCounter == 10) {
                tickCounter = 0;
                FileInformationWriter.saveInformation(currentFile);
            }
        }
    }
}
