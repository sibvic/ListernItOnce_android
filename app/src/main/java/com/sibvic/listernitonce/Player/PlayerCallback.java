package com.sibvic.listernitonce.Player;

import com.sibvic.listernitonce.Media.MediaFile;

/**
 * Notifies listener about playback events.
 */
interface PlayerCallback {
    void onStarted(MediaFile file);
    void onPaused(MediaFile file);
    void onResumed(MediaFile file);
    void onStopped(MediaFile file);
    void onCompleted(MediaFile file);
}
