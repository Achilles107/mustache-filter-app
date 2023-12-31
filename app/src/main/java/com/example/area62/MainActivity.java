package com.example.area62;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ListView videoListView;
    private VideoAdapter videoAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_videos);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        // Check if the RECORD_AUDIO permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission if it is not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }




        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        videoListView = findViewById(R.id.saved_video_list);
        videoAdapter = new VideoAdapter(this, R.layout.list_item_video, new ArrayList<>());
        videoListView.setAdapter(videoAdapter);

        // Get the list of video files from the /Videos folder
        File videosFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/Videos");

        File[] videoFiles = videosFolder.listFiles();
        if (videoFiles != null) {
            Log.d("MainActivity", "Number of video files: " + videoFiles.length);
            for (File file : videoFiles) {
                Log.d("MainActivity", "Video file name: " + file.getName());

                String fName = file.getName().replace(".mp4", "");
                VideoItem videoItem = new VideoItem(fName, getVideoDuration(file));

                videoAdapter.add(videoItem);
            }
        }

        // Set item click listener for the ListView
        videoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected video file
                VideoItem selectedVideoItem = videoAdapter.getItem(position);
                if (selectedVideoItem != null) {

                    // Get the selected video file
                    File selectedVideoFile = selectedVideoItem.getFile();
                    // Generate a content:// URI using FileProvider
                    Uri videoUri = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".fileprovider", selectedVideoFile);

                    // Grant URI permissions to the intent
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(videoUri, "video/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    // Verify that the intent resolves to a video player activity
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "No video player app found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


        videoListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final VideoItem selectedVideoItem = videoAdapter.getItem(position);
                final File videoFile = new File(videosFolder, selectedVideoItem.getName()+".mp4");

                // Create an AlertDialog to prompt for the new file name
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Rename Video");
                builder.setMessage("Enter the new file name");

                final EditText input = new EditText(MainActivity.this);
                input.setText(selectedVideoItem.getName().replaceFirst("[.][^.]+$", "")); // Pre-fill the current file name
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newFileName = input.getText().toString().trim();
                        if (!newFileName.isEmpty()) {
                            File newFile = new File(videosFolder, newFileName + ".mp4");
                            Log.i("Main", "File name: "+selectedVideoItem.getName());
                            boolean renamed = videoFile.renameTo(newFile);

                            if (renamed) {
                                // Update the video item in the adapter
                                selectedVideoItem.setName(newFileName);
                                videoAdapter.notifyDataSetChanged();
                                recreate();
                                Toast.makeText(MainActivity.this, "Video renamed successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to rename video", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a valid file name", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();

                return true;
            }
        });

        FloatingActionButton roundButton = findViewById(R.id.recordScreen);
        roundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the new activity
                Intent intent = new Intent(MainActivity.this, SavedVidActivity.class);
                startActivity(intent);
            }
        });
    }


    private String getVideoDuration(File videoFile) {
        String duration = "";
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFile.getPath());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMillis = Long.parseLong(durationStr);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;
            duration = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            retriever.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return duration;
    }
}

class VideoItem {
    private String name;
    private String duration;
    private File file;

    public VideoItem(String name, String duration) {
        this.name = name;
        this.duration = duration;
        this.file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/Videos/" + name + ".mp4");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDuration() {
        return duration;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return name;
    }
}

class VideoAdapter extends ArrayAdapter<VideoItem> {
    private int resource;

    public VideoAdapter(Context context, int resource, List<VideoItem> items) {
        super(context, resource, items);
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resource, parent, false);
        }

        VideoItem videoItem = getItem(position);

        TextView videoNameTextView = convertView.findViewById(R.id.video_name);
        TextView videoDurationTextView = convertView.findViewById(R.id.video_duration);

        videoNameTextView.setText(videoItem.getName());
        videoDurationTextView.setText(videoItem.getDuration());

        return convertView;
    }
}

