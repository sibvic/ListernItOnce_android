package com.sibvic.listernitonce.Player;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.sibvic.listernitonce.FilesListActivity;
import com.sibvic.listernitonce.R;

/**
 * Notifications for the MediaSession
 */

public class MediaNotificationManager extends BroadcastReceiver {
    MediaPlaybackService service;

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "com.sibvic.listernitonce.player.pause";
    public static final String ACTION_PLAY = "com.sibvic.listernitonce.player.play";
    public static final String ACTION_STOP_CASTING = "com.sibvic.listernitonce.player.stop_cast";

    private MediaSessionCompat.Token sessionToken;
    private MediaControllerCompat controller;
    private MediaControllerCompat.TransportControls transportControls;

    private PlaybackStateCompat playbackState;
    private MediaMetadataCompat metadata;

    private final NotificationManagerCompat notificationManager;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent stopCastIntent;

    private final int notificationColor;

    private boolean started = false;
    private int lastPlaybackState = 0;

    public MediaNotificationManager(MediaPlaybackService service) throws RemoteException {
        super();
        this.service = service;
        updateSessionToken();

        notificationColor = Color.DKGRAY;

        notificationManager = NotificationManagerCompat.from(service);

        String pkg = service.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopCastIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_STOP_CASTING).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        notificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!started) {
            metadata = controller.getMetadata();
            playbackState = controller.getPlaybackState();
            lastPlaybackState = playbackState.getState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
                controller.registerCallback(mCb);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_STOP_CASTING);
                service.registerReceiver(this, filter);

                service.startForeground(NOTIFICATION_ID, notification);
                started = true;
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (started) {
            started = false;
            controller.unregisterCallback(mCb);
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                service.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            service.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case ACTION_PAUSE:
                transportControls.pause();
                break;
            case ACTION_PLAY:
                transportControls.play();
                break;
            case ACTION_STOP_CASTING:
                Intent i = new Intent(context, MediaPlaybackService.class);
//                i.setAction(MediaPlaybackService.ACTION_CMD);
//                i.putExtra(MediaPlaybackService.CMD_NAME, MediaPlaybackService.CMD_STOP_CASTING);
                service.startService(i);
                break;
            default:
                break;
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token freshToken = service.getSessionToken();
        if (sessionToken == null && freshToken != null
                || sessionToken != null && !sessionToken.equals(freshToken)) {
            if (controller != null) {
                controller.unregisterCallback(mCb);
            }
            sessionToken = freshToken;
            if (sessionToken != null) {
                controller = new MediaControllerCompat(service, sessionToken);
                transportControls = controller.getTransportControls();
                if (started) {
                    controller.registerCallback(mCb);
                }
            }
        }
    }

    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        Intent openUI = new Intent(service, FilesListActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //openUI.putExtra(FilesListActivity.EXTRA_START_FULLSCREEN, true);
        if (description != null) {
            //openUI.putExtra(FilesListActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, description);
        }
        return PendingIntent.getActivity(service, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private final MediaControllerCompat.Callback mCb = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            playbackState = state;
            if (lastPlaybackState == state.getState()) {
                return;
            }
            lastPlaybackState = state.getState();
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED
                    || state.getState() == PlaybackStateCompat.STATE_NONE) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat changedMetadata) {
            metadata = changedMetadata;
            Notification notification = createNotification();
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            try {
                updateSessionToken();
            } catch (RemoteException ignored) {

            }
        }
    };

    private Notification createNotification() {
        if (metadata == null || playbackState == null) {
            return null;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(service);

        addPlayPauseAction(notificationBuilder);
        //addArt(notificationBuilder);

        MediaDescriptionCompat description = metadata.getDescription();

        int playPauseButtonPosition = 1;
        notificationBuilder
                .setStyle(new android.support.v7.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(
                                new int[]{playPauseButtonPosition})  // show only play/pause in compact view
                        .setMediaSession(sessionToken))
                .setSmallIcon(R.drawable.ic_info_black_24dp)//TODO: set proper icon
                .setColor(notificationColor)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle());

//        if (controller != null && controller.getExtras() != null) {
//            String castName = controller.getExtras().getString(MediaPlaybackService.EXTRA_CONNECTED_CAST);
//            if (castName != null) {
//                String castInfo = service.getResources()
//                        .getString(R.string.casting_to_device, castName);
//                notificationBuilder.setSubText(castInfo);
//                notificationBuilder.addAction(android.R.drawable.ic_media_pause,
//                        service.getString(R.string.pause), stopCastIntent);
//            }
//        }

        return notificationBuilder.build();
    }

    private void addArt(NotificationCompat.Builder notificationBuilder) {
        MediaDescriptionCompat description = metadata.getDescription();
        String fetchArtUrl = null;
        Bitmap art = null;
        if (description.getIconUri() != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            String artUrl = description.getIconUri().toString();
            //art = AlbumArtCache.getInstance().getBigImage(artUrl);
            if (art == null) {
                fetchArtUrl = artUrl;
                // use a placeholder art while the remote art is being downloaded
//                art = BitmapFactory.decodeResource(service.getResources(),
//                        R.drawable.ic_default_art);
            }
        }
//        notificationBuilder
//                .setSmallIcon(R.drawable.ic_notification)
//                .setLargeIcon(art);
        if (fetchArtUrl != null) {
            fetchBitmapFromURLAsync(fetchArtUrl, notificationBuilder);
        }
    }

    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        String label;
        int icon;
        PendingIntent intent;
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = service.getString(R.string.pause);
            icon = android.R.drawable.ic_media_pause;
            intent = pauseIntent;
        } else {
            label = service.getString(R.string.play);
            icon = android.R.drawable.ic_media_play;
            intent = playIntent;
        }
        builder.addAction(new NotificationCompat.Action(icon, label, intent));
    }

    private void fetchBitmapFromURLAsync(final String bitmapUrl,
                                         final NotificationCompat.Builder builder) {
//        AlbumArtCache.getInstance().fetch(bitmapUrl, new AlbumArtCache.FetchListener() {
//            @Override
//            public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
//                if (metadata != null && metadata.getDescription().getIconUri() != null &&
//                        metadata.getDescription().getIconUri().toString().equals(artUrl)) {
//                    // If the media is still the same, update the notification:
//                    builder.setLargeIcon(bitmap);
//                    notificationManager.notify(NOTIFICATION_ID, builder.build());
//                }
//            }
//        });
    }
}
