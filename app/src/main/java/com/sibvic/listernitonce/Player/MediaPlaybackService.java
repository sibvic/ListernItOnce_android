package com.sibvic.listernitonce.Player;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
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
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaNotificationManager notificationManager;
    MediaSessionCompatCallback callback;
    Player player = new Player(this);

    @Override
    public void onCreate() {
        super.onCreate();

        handler.postDelayed(updateTimeTask, 1000);

        mediaSession = new MediaSessionCompat(this, "lio");
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        callback = new MediaSessionCompatCallback(this, player);
        mediaSession.setCallback(callback);

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());

        try {
            notificationManager = new MediaNotificationManager(this);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if (player.isPlaying()) {
            player.stop();
        }
        super.onDestroy();
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
                .setSubtitle("")
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
        mediaSession.setMetadata(builder.build());
        mediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_PLAYING));
        mediaSession.setActive(true);
        notificationManager.startNotification();
        startService(new Intent(getApplicationContext(), MediaPlaybackService.class));
        Log.d("lio", String.format("Starting playing of %1$s", file.getTitle()));
    }

    @NonNull
    private PlaybackStateCompat buildState(MediaFile file, int state) {
        return stateBuilder.setState(state, file.getCurrentPosition(), 1).build();
    }

    @Override
    public void onPaused(MediaFile file) {
        mediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_PAUSED));
        Log.d("lio", String.format("Pausing of %1$s", file.getTitle()));
    }

    @Override
    public void onResumed(MediaFile file) {
        mediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_PLAYING));
        Log.d("lio", String.format("Resuming of %1$s", file.getTitle()));
    }

    @Override
    public void onStopped(MediaFile file) {
        mediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_STOPPED));
        mediaSession.setActive(false);
        notificationManager.stopNotification();
        stopSelf();
        Log.d("lio", String.format("Stopping of %1$s", file.getTitle()));
    }

    private int findFileIndex(String fileId) {
        ArrayList<MediaFile> files = callback.getFiles();
        for (int i = 0; i < files.size(); ++i) {
            if (files.get(i).getFile().getAbsolutePath().equals(fileId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onCompleted(MediaFile file) {
        //HACK. Use some kind of notification about item remove/list update.
        file.setCurrentPosition(file.getLength());
        mediaSession.setPlaybackState(buildState(file, PlaybackStateCompat.STATE_SKIPPING_TO_NEXT));

        int indexOfFile = findFileIndex(file.getFile().getAbsolutePath());
        handleCompletedFile(file);
        if (indexOfFile == -1) {
            return;
        }
        callback.removeFile(indexOfFile);
        ArrayList<MediaFile> files = callback.getFiles();
        if (indexOfFile <= files.size() - 1) {
            MediaFile nextFile = files.get(indexOfFile);
            callback.onPlayFromMediaId(nextFile.getFile().getAbsolutePath(), null);
        }
    }

    private void handleCompletedFile(MediaFile file) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("delete_after_play", false)) {
            deleteFile(file);
        }
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
            handler.postDelayed(this, 800);
            MediaFile currentFile = player.getMediaFile();
            if (currentFile == null) {
                return;
            }
            if (player.isPlaying()) {
                mediaSession.setPlaybackState(buildState(currentFile, PlaybackStateCompat.STATE_PLAYING));
            }
            else {
                mediaSession.setPlaybackState(buildState(currentFile, PlaybackStateCompat.STATE_PAUSED));
            }
        }
    };
}
