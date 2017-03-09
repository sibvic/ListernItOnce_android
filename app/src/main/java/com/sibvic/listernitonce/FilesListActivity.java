package com.sibvic.listernitonce;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.sibvic.listernitonce.Media.FileFactory;
import com.sibvic.listernitonce.Media.MediaFile;
import com.sibvic.listernitonce.Player.Player;
import com.sibvic.listernitonce.Player.PlayerCallback;

import java.io.File;
import java.util.ArrayList;

public class FilesListActivity extends ListActivity implements PlayerCallback {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_list);
        refreshFiles();
        player = new Player(this);
        handler.postDelayed(updateTimeTask, 1000);

        handlePreferences();

        ImageButton playPauseButton = (ImageButton)findViewById(R.id.play_pause);
        playPauseButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.isPlaying()) {
                    player.pause();
                }
                else {
                    player.resume();
                }
            }
        });
    }

    private void handlePreferences() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String target_folder = settings.getString("target_path", "");
        if (target_folder.equals("")) {
            handler.postDelayed(showOptionsTask, 100);
        }

        settings.registerOnSharedPreferenceChangeListener(
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    if (key.equals("target_path")) {
                        refreshFiles();
                    }
                }
            });
    }

    FileListAdapter adapter;
    Player player;
    ArrayList<MediaFile> listItems;
    private Handler handler = new Handler();
    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            handler.postDelayed(this, 1000);
            MediaFile currentFile = player.getMediaFile();
            if (currentFile == null) {
                return;
            }
            int indexOfFile = listItems.indexOf(currentFile);
            if (indexOfFile != -1) {
                ListView listView = (ListView)findViewById(android.R.id.list);
                View v = listView.getChildAt(indexOfFile - listView.getFirstVisiblePosition());
                if(v != null) {
                    adapter.updateRow(indexOfFile, v);
                }
            }
        }
    };

    private Runnable showOptionsTask = new Runnable() {
        public void run() {
            showOptions();
        }
    };

    private void showOptions() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    private void refreshFiles() {
        listItems = new ArrayList<>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String target_folder = preferences.getString("target_path", "");
        if (!target_folder.equals("")) {
            File directory = new File(target_folder);
            FileFactory.addFilesFromFolder(listItems, directory);
        }
        adapter = new FileListAdapter(this, listItems);
        adapter.notifyDataSetChanged();
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        MediaFile fileToPlay = adapter.getItem(position);
        playFile(fileToPlay);
    }

    private void playFile(MediaFile fileToPlay) {
        if (player.isPlaying()) {
            player.stop();
        }
        player.play(fileToPlay);
    }

    @Override
    public void onStarted(MediaFile file) {
        ImageButton playPauseButton = (ImageButton)findViewById(R.id.play_pause);
        playPauseButton.setEnabled(true);
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);

        TextView currentFileName = (TextView)findViewById(R.id.current_file_name);
        currentFileName.setText(file.getTitle());
    }

    @Override
    public void onPaused(MediaFile file) {
        ImageButton playPauseButton = (ImageButton)findViewById(R.id.play_pause);
        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
    }

    @Override
    public void onResumed(MediaFile file) {
        ImageButton playPauseButton = (ImageButton)findViewById(R.id.play_pause);
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
    }

    @Override
    public void onStopped(MediaFile file) {
        ImageButton playPauseButton = (ImageButton)findViewById(R.id.play_pause);
        playPauseButton.setEnabled(false);
        if (file.getLength() > 0 && file.getCurrentPosition() >= file.getLength()) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (preferences.getBoolean("delete_after_play", false)) {
                deleteFile(file);
            }
            int indexOfFile = listItems.indexOf(file);
            if (indexOfFile != -1) {
                listItems.remove(indexOfFile);
                if (indexOfFile < listItems.size()) {
                    MediaFile fileToPlay = listItems.get(indexOfFile);
                    playFile(fileToPlay);
                }
            }
            adapter.notifyDataSetChanged();
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
}
