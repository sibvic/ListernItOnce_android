package com.sibvic.listernitonce.Player

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.RemoteException
import android.support.v4.app.NotificationCompat.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.KeyEvent

import com.sibvic.listernitonce.FilesListActivity
import com.sibvic.listernitonce.R

import android.content.Context.TELEPHONY_SERVICE

/**
 * Notifications for the MediaSession
 */

class MediaNotificationManager @Throws(RemoteException::class)
constructor(internal var service: MediaPlaybackService) : BroadcastReceiver() {

    private var sessionToken: MediaSessionCompat.Token? = null
    private var controller: MediaControllerCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null

    private var playbackState: PlaybackStateCompat? = null
    private var metadata: MediaMetadataCompat? = null

    private val notificationManager: NotificationManagerCompat

    private val pauseIntent: PendingIntent
    private val playIntent: PendingIntent

    private val notificationColor: Int

    private var started = false
    private var lastPlaybackState = 0

    internal var wasPlaying = false

    internal var phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                wasPlaying = playbackState!!.state == PlaybackStateCompat.STATE_PLAYING
                if (wasPlaying) {
                    transportControls!!.pause()
                }
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                if (wasPlaying) {
                    transportControls!!.play()
                    wasPlaying = false
                }
            }
            super.onCallStateChanged(state, incomingNumber)
        }
    }

    private val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            playbackState = state
            if (lastPlaybackState == state.state) {
                return
            }
            lastPlaybackState = state.state
            if (state.state == PlaybackStateCompat.STATE_STOPPED || state.state == PlaybackStateCompat.STATE_NONE) {
                stopNotification()
            } else {
                val notification = createNotification()
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }

        override fun onMetadataChanged(changedMetadata: MediaMetadataCompat?) {
            metadata = changedMetadata
            val notification = createNotification()
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            try {
                updateSessionToken()
            } catch (ignored: RemoteException) {

            }

        }
    }

    init {
        updateSessionToken()

        notificationColor = Color.DKGRAY

        notificationManager = NotificationManagerCompat.from(service)

        val pkg = service.packageName
        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        notificationManager.cancelAll()
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before [.stopNotification] is called.
     */
    fun startNotification() {
        if (!started) {
            metadata = controller!!.metadata
            playbackState = controller!!.playbackState
            lastPlaybackState = playbackState!!.state

            // The notification must be updated after setting started to true
            val notification = createNotification()
            if (notification != null) {
                controller!!.registerCallback(callback)
                val filter = IntentFilter(Intent.ACTION_MEDIA_BUTTON)
                filter.addAction(ACTION_PAUSE)
                filter.addAction(ACTION_PLAY)
                service.registerReceiver(this, filter)

                service.startForeground(NOTIFICATION_ID, notification)
                started = true
            }
            val mgr = service.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            mgr?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    fun stopNotification() {
        if (started) {
            started = false
            controller!!.unregisterCallback(callback)
            try {
                notificationManager.cancel(NOTIFICATION_ID)
                service.unregisterReceiver(this)
            } catch (ex: IllegalArgumentException) {
                // ignore if the receiver is not registered.
            }

            service.stopForeground(true)
            val mgr = service.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            mgr?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_PAUSE) {
            transportControls!!.pause()
            return
        } else if (action == ACTION_PLAY) {
            transportControls!!.play()
            return
        }

        val key = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        if (key.action == KeyEvent.ACTION_UP) {
            val keycode = key.keyCode
            if (keycode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                transportControls!!.pause()
            } else if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                transportControls!!.play()
            } else if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (controller!!.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                    transportControls!!.pause()
                } else if (controller!!.playbackState.state == PlaybackStateCompat.STATE_PAUSED) {
                    transportControls!!.play()
                }
            }
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see [android.media.session.MediaController.Callback.onSessionDestroyed])
     */
    @Throws(RemoteException::class)
    private fun updateSessionToken() {
        val freshToken = service.sessionToken
        if (sessionToken == null && freshToken != null || sessionToken != null && sessionToken != freshToken) {
            if (controller != null) {
                controller!!.unregisterCallback(callback)
            }
            sessionToken = freshToken
            if (sessionToken != null) {
                controller = MediaControllerCompat(service, sessionToken!!)
                transportControls = controller!!.transportControls
                if (started) {
                    controller!!.registerCallback(callback)
                }
            }
        }
    }

    private fun createContentIntent(description: MediaDescriptionCompat): PendingIntent {
        val openUI = Intent(service, FilesListActivity::class.java)
        openUI.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(service, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun createNotification(): Notification? {
        if (metadata == null || playbackState == null) {
            return null
        }

        val notificationBuilder = NotificationCompat.Builder(service)

        addPlayPauseAction(notificationBuilder)
        //addArt(notificationBuilder);

        val description = metadata!!.description

        val playPauseButtonPosition = 1
        notificationBuilder
                .setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(
                                *intArrayOf(playPauseButtonPosition))  // show only play/pause in compact view
                        .setMediaSession(sessionToken))
                .setColor(notificationColor)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.title)
                .setContentText(description.subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setSmallIcon(R.drawable.ic_info_black_24dp)//TODO: set proper icon
        } else {
            notificationBuilder.setSmallIcon(R.drawable.ic_main_icon)
        }

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

        return notificationBuilder.build()
    }

    private fun addArt(notificationBuilder: NotificationCompat.Builder) {
        val description = metadata!!.description
        var fetchArtUrl: String? = null
        val art: Bitmap? = null
        if (description.iconUri != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            val artUrl = description.iconUri!!.toString()
            //art = AlbumArtCache.getInstance().getBigImage(artUrl);
            if (art == null) {
                fetchArtUrl = artUrl
                // use a placeholder art while the remote art is being downloaded
                //                art = BitmapFactory.decodeResource(service.getResources(),
                //                        R.drawable.ic_default_art);
            }
        }
        //        notificationBuilder
        //                .setLargeIcon(art);
        if (fetchArtUrl != null) {
            fetchBitmapFromURLAsync(fetchArtUrl, notificationBuilder)
        }
    }

    private fun addPlayPauseAction(builder: NotificationCompat.Builder) {
        val label: String
        val icon: Int
        val intent: PendingIntent
        if (playbackState!!.state == PlaybackStateCompat.STATE_PLAYING) {
            label = service.getString(R.string.pause)
            icon = android.R.drawable.ic_media_pause
            intent = pauseIntent
        } else {
            label = service.getString(R.string.play)
            icon = android.R.drawable.ic_media_play
            intent = playIntent
        }
        builder.addAction(NotificationCompat.Action(icon, label, intent))
    }

    private fun fetchBitmapFromURLAsync(bitmapUrl: String,
                                        builder: NotificationCompat.Builder) {
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

    companion object {

        private val NOTIFICATION_ID = 412
        private val REQUEST_CODE = 100

        val ACTION_PAUSE = "com.sibvic.listernitonce.player.pause"
        val ACTION_PLAY = "com.sibvic.listernitonce.player.play"
    }
}
