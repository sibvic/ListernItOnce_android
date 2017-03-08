package com.sibvic.listernitonce;

import android.app.ListActivity;
import android.media.AudioManager;
import android.os.Environment;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

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
    }
    FileListAdapter adapter;
    Player player;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    private void refreshFiles() {
        File directory = new File(Environment.getExternalStorageDirectory().toString() + "/Music");
        ArrayList<MediaFile> listItems = new ArrayList<>();
        addFilesFromFolder(listItems, directory);

        MediaFile[] mediaFiles = listItems.toArray(new MediaFile[listItems.size()]);
        adapter = new FileListAdapter(this, mediaFiles);
        adapter.notifyDataSetChanged();
        setListAdapter(adapter);
    }

    private void addFilesFromFolder(ArrayList<MediaFile> files, File folder) {
        File[] filesInFolder = folder.listFiles();
        if (filesInFolder == null) {
            return;
        }
        for (File file : filesInFolder) {
            if (file.isDirectory()) {
                addFilesFromFolder(files, file);
                continue;
            }
            if (FileFactory.isMediaFile(file)) {
                files.add(FileFactory.getMediaFile(file));
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        player.play(adapter.getItem(position));
    }

    @Override
    public void onStarted(MediaFile file) {

    }

    @Override
    public void onPaused(MediaFile file) {
        //TODO: save progress
    }

    @Override
    public void onResumed(MediaFile file) {

    }

    @Override
    public void onStopped(MediaFile file) {

        if (file.getLength() > 0 && file.getCurrentPosition() >= file.getLength()) {
            //TODO: delete finished media file
        }
        else {
            //TODO: save progress
        }
    }
}
