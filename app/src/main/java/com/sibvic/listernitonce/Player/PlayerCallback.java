package com.sibvic.listernitonce.Player;

import com.sibvic.listernitonce.Media.MediaFile;

/**
 * Notifies listener about playback events.
 */
public interface PlayerCallback {
    void onStarted(MediaFile file);
    void onPaused(MediaFile file);
    void onResumed(MediaFile file);
    void onStopped(MediaFile file);
}
