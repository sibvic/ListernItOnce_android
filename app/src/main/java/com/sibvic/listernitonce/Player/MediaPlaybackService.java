package com.sibvic.listernitonce.Player;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.sibvic.listernitonce.Media.MediaFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Player service
 */
public class MediaPlaybackService extends MediaBrowserServiceCompat implements PlayerCallback {
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    MediaSessionCompatCallback callback;
    Player mPlayer = new Player(this);

    @Override
    public void onCreate() {
        super.onCreate();

        handler.postDelayed(updateTimeTask, 1000);

        // Create a MediaSessionCompat
        mMediaSession = new MediaSessionCompat(this, "lio");

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mMediaSession.setPlaybackState(mStateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        callback = new MediaSessionCompatCallback(this, mPlayer);
        mMediaSession.setCallback(callback);

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mMediaSession.getSessionToken());
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        ArrayList<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        for (MediaFile file : callback.getFiles()) {
            items.add(createMediaItem(file));
        }
        result.sendResult(items);
    }

    @NonNull
    private MediaBrowserCompat.MediaItem createMediaItem(MediaFile file) {
        Bundle bundle = new Bundle();
        bundle.putLong("current_position", file.getCurrentPosition());
        bundle.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, file.getLength());
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(file.getFile().getAbsolutePath())
                .setTitle(file.getTitle())
                .setExtras(bundle)
                .build();
        return new MediaBrowserCompat.MediaItem(description, 0);
    }

    @Override
    public void onStarted(MediaFile file) {
        MediaMetadataCompat.Builder builder = new  MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, file.getTitle());
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, file.getLength());
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, file.getFile().getAbsolutePath());
        builder.putLong("current_position", file.getCurrentPosition());
        mMediaSession.setMetadata(builder.build());
        mMediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_PLAYING));
    }

    @NonNull
    private PlaybackStateCompat buildState(MediaFile file, int state) {
        return mStateBuilder.setState(state, file.getCurrentPosition(), 1).build();
    }

    @Override
    public void onPaused(MediaFile file) {
        mMediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_PAUSED));
    }

    @Override
    public void onResumed(MediaFile file) {
        mMediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_PLAYING));
    }

    @Override
    public void onStopped(MediaFile file) {
        if (file.getLength() > 0 && file.getCurrentPosition() >= file.getLength()) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (preferences.getBoolean("delete_after_play", false)) {
                deleteFile(file);
            }
        }
        mMediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_STOPPED));
    }

    private void deleteFile(MediaFile file) {
        if (!file.getFile().delete()) {
            Log.d("lio", String.format("Failed to delete %1$s", file.getTitle()));
        }
        if (!file.getMetaInformationFile().delete()) {
            Log.d("lio", String.format("Failed to delete %1$s meta information",
                    file.getTitle()));
        }
    }

    private Handler handler = new Handler();
    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            handler.postDelayed(this, 1000);
            MediaFile currentFile = mPlayer.getMediaFile();
            if (currentFile == null) {
                return;
            }
            if (mPlayer.isPlaying()) {
                mMediaSession.setPlaybackState(buildState(currentFile, PlaybackStateCompat.STATE_PLAYING));
            }
            else {
                mMediaSession.setPlaybackState(buildState(currentFile, PlaybackStateCompat.STATE_PAUSED));
            }
        }
    };
}
