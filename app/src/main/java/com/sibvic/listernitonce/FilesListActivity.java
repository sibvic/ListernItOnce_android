package com.sibvic.listernitonce;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.sibvic.listernitonce.Player.MediaPlaybackService;

import java.util.ArrayList;
import java.util.List;

public class FilesListActivity extends AppCompatActivity {
    FileListAdapter adapter;
    MediaBrowserCompat _player;
    FileInfoList _listItems = new FileInfoList();
    FileInfo _currentlyPlaying;
    final int GET_PERMISSIONS = 0;

    boolean isPermissionGranted(String name) {
        return ContextCompat.checkSelfPermission(this,name)
                != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_list);

        if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
                || !isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || !isPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {
            ActivityCompat.requestPermissions(this,
                    new String[]
                            {
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_PHONE_STATE
                            },
                    GET_PERMISSIONS);
        }
        else {
            InitPlayer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GET_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    InitPlayer();
                    _player.connect();
                }
                break;
            }
        }
    }

    private void InitPlayer() {
        _player = new MediaBrowserCompat(this,
                new ComponentName(this, MediaPlaybackService.class),
                connectionCallbacks, null);
        _player.subscribe("root", mSubscriptionCallback);
        handlePreferences();
    }

    SharedPreferences.OnSharedPreferenceChangeListener optionsChangeListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            if (key.equals("target_path")) {
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(FilesListActivity.this);
                String target_folder = settings.getString("target_path", "");
                MediaControllerCompat.getMediaController(FilesListActivity.this)
                        .getTransportControls()
                        .prepareFromMediaId(target_folder, null);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (_player != null) {
            _player.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(FilesListActivity.this) != null) {
            MediaControllerCompat.getMediaController(FilesListActivity.this).unregisterCallback(controllerCallback);
        }
        if (_player != null) {
            _player.disconnect();
        }
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    // Get the token for the MediaSession
                    MediaSessionCompat.Token token = _player.getSessionToken();

                    // Create a MediaControllerCompat
                    MediaControllerCompat mediaController;
                    try {
                        mediaController = new MediaControllerCompat(FilesListActivity.this, // Context
                                token);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        return;
                    }

                    // Save the controller
                    MediaControllerCompat.setMediaController(FilesListActivity.this, mediaController);
                    SharedPreferences settings = PreferenceManager
                            .getDefaultSharedPreferences(FilesListActivity.this);

                    String target_folder = settings.getString("target_path", "");
                    MediaControllerCompat.TransportControls transportControls = mediaController.getTransportControls();
                    if (transportControls != null) {
                        transportControls.prepareFromMediaId(target_folder, null);
                    }

                    // Finish building the UI
                    buildTransportControls();
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                }

                @Override
                public void onConnectionFailed() {
                    // The Service has refused our connection
                }
            };

    void buildTransportControls()
    {
        ImageButton playPauseButton = (ImageButton)findViewById(R.id.play_pause);
        // Attach a listener to the button
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Since this is a play/pause button, you'll need to test the current state
                // and choose the action accordingly

                int pbState = MediaControllerCompat.getMediaController(FilesListActivity.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(FilesListActivity.this).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(FilesListActivity.this).getTransportControls().play();
                }
            }});

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(FilesListActivity.this);

        // Display the initial state
        updateTitle(mediaController.getMetadata());
        updatePlayPauseButton(mediaController.getPlaybackState());

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback);
    }

    private void updatePlayPauseButton(PlaybackStateCompat pbState) {
        ImageButton playPauseButton = (ImageButton)findViewById(R.id.play_pause);
        int state = pbState.getState();
        switch (state) {
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED: {
                playPauseButton.setEnabled(false);
            }
            break;
            case PlaybackStateCompat.STATE_PAUSED: {
                playPauseButton.setEnabled(true);
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
            break;
            case PlaybackStateCompat.STATE_PLAYING: {
                playPauseButton.setEnabled(true);
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            }
            break;
            default: {
                Log.d("lio", "Unknown state " + Integer.toString(state));
            }
            break;
        }
    }

    private void updateTitle(MediaMetadataCompat metadata) {
        TextView currentFileName = (TextView)findViewById(R.id.current_file_name);
        String title = formatTitle(metadata);
        currentFileName.setText(title);
    }

    private String formatTitle(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return "---";
        }
        String title = metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE).toString();
        if (title.equals("")) {
            return "---";
        }
        else {
            return title;
        }
    }

    MediaControllerCompat.Callback controllerCallback =
        new MediaControllerCompat.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                updateTitle(metadata);
                String fileId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                _currentlyPlaying = _listItems.findFileById(fileId);
            }

            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                if (_currentlyPlaying == null) {
                    MediaMetadataCompat metadata = MediaControllerCompat
                            .getMediaController(FilesListActivity.this).getMetadata();
                    if (metadata != null) {
                        onMetadataChanged(metadata);
                    }
                }
                updatePlayPauseButton(state);
                updateProgress(state);
            }
        };

    private void updateProgress(PlaybackStateCompat state) {
        if (_currentlyPlaying == null) {
            return;
        }
        _currentlyPlaying.setCurrentPosition((int)state.getPosition());
        int indexOfFile = _listItems.indexOf(_currentlyPlaying);
        if (indexOfFile != -1) {
            ListView listView = (ListView) findViewById(android.R.id.list);
            if (state.getState() == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT
                    && _currentlyPlaying.getLength() >= _currentlyPlaying.getCurrentPosition()) {
                _listItems.remove(indexOfFile);
                adapter.remove(_currentlyPlaying);
                adapter.notifyDataSetChanged();
            }
            else {
                View v = listView.getChildAt(indexOfFile - listView.getFirstVisiblePosition());
                if (v != null) {
                    adapter.updateRow(indexOfFile, v);
                }
            }
        }
    }

    private void handlePreferences() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(optionsChangeListener);
        String target_folder = settings.getString("target_path", "");
        if (target_folder.equals("")) {
            _handler.postDelayed(_showOptionsTask, 100);
        }
    }

    private Handler _handler = new Handler();

    private Runnable _showOptionsTask = new Runnable() {
        public void run() {
            showOptions();
        }
    };

    private void showOptions() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    for (MediaBrowserCompat.MediaItem item : children) {
                        MediaDescriptionCompat description = item.getDescription();
                        Bundle extras = description.getExtras();
                        CharSequence title = description.getTitle();
                        long duration = extras != null ? extras.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) : 0;
                        long current_position = extras != null ? extras.getLong("current_position") : 0;
                        FileInfo fileInfo = new FileInfo(item.getMediaId(),
                                title == null ? "" : title.toString(),
                                (int) duration,
                                (int) current_position);
                        _listItems.add(fileInfo);
                    }

                    adapter = new FileListAdapter(FilesListActivity.this, _listItems.toArrayList());
                    adapter.notifyDataSetChanged();

                    ListView listView = (ListView)findViewById(android.R.id.list);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            FileInfo fileToPlay = adapter.getItem(position);
                            playFile(fileToPlay);
                        }
                    });
                }

                @Override
                public void onError(@NonNull String id) {
                }
            };

    private void playFile(FileInfo fileToPlay) {
        MediaControllerCompat.TransportControls transportControls =
                MediaControllerCompat.getMediaController(FilesListActivity.this).getTransportControls();
        transportControls.playFromMediaId(fileToPlay.getId(), null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_files_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                showOptions();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
