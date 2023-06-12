package com.example.area62;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class SavedVidActivity extends AppCompatActivity {
    private ListView videoListView;
    private ArrayAdapter<String> videoAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_videos);

        videoListView = findViewById(R.id.saved_video_list);
        videoAdapter = new ArrayAdapter<>(this, R.layout.list_item_video, R.id.video_name);
        videoListView.setAdapter(videoAdapter);

        // Get the list of video files from the /Videos folder
        File videosFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/Videos");
        //File[] videoFiles = videosFolder.listFiles();

        File[] videoFiles = videosFolder.listFiles();
        if (videoFiles != null) {
            Log.d("SavedVidActivity", "Number of video files: " + videoFiles.length);
            for (File file : videoFiles) {
                Log.d("SavedVidActivity", "Video file name: " + file.getName());
                videoAdapter.add(file.getName());
            }
        }

        // Set item click listener for the ListView
        videoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected video file
                File selectedVideoFile = videoFiles[position];

                // Generate a content:// URI using FileProvider
                Uri videoUri = FileProvider.getUriForFile(SavedVidActivity.this, getApplicationContext().getPackageName() + ".fileprovider", selectedVideoFile);

                // Grant URI permissions to the intent
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(videoUri, "video/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Verify that the intent resolves to a video player activity
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(SavedVidActivity.this, "No video player app found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
