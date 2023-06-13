package com.example.area62;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SavedVidActivity extends AppCompatActivity {
    private MustacheFragment arFragment;
    private VideoRecorder videoRecorder;
    private ModelRenderable modelRenderable;
    private boolean isAdded = false;
    private ListView textureListView;
    private ArrayAdapter<Integer> textureAdapter;
    private ArrayAdapter<String> itemAdapter;
    private List<String> itemNames = Arrays.asList("Bushy", "Brown", "Blue", "Specs", "Black", "French", "Shaolin", "Hitler");
    private List<Integer> textureList = Arrays.asList(R.drawable.bush, R.drawable.brown, R.drawable.blue, R.drawable.specs, R.drawable.googles_black, R.drawable.french, R.drawable.shaolin, R.drawable.hitler);

    private Texture currentTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        // Check if the RECORD_AUDIO permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission if it is not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        arFragment = (MustacheFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        ModelRenderable.builder().setSource(this, R.raw.model_triangulated).build().thenAccept(modelRenderable1 -> {
            modelRenderable = modelRenderable1;
            modelRenderable.setShadowCaster(false);
            modelRenderable.setShadowReceiver(false);
        });
        textureListView = findViewById(R.id.textureListView);

        itemAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemNames);
        textureAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textureList);
        textureListView.setAdapter(itemAdapter);

        textureListView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedTexture = textureAdapter.getItem(position);
            updateTexture(selectedTexture);
        });

        arFragment.getArSceneView().setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (modelRenderable == null || currentTexture == null)
                return;
            Frame frame = arFragment.getArSceneView().getArFrame();

            Collection<AugmentedFace> augmentedFaces = frame.getUpdatedTrackables(AugmentedFace.class);
            for (AugmentedFace augmentedFace: augmentedFaces) {
                if (isAdded){
                    return;
                }
                AugmentedFaceNode  augmentedFaceNode = new AugmentedFaceNode(augmentedFace);
                augmentedFaceNode.setParent(arFragment.getArSceneView().getScene());

                Vector3 scaleFactor = new Vector3(0.8f, 0.5f, 0.5f);
                Vector3 positionOffset = new Vector3(0.0f, 999f, 0.0f);
                augmentedFaceNode.setLocalScale(scaleFactor);
                augmentedFaceNode.setLocalPosition(positionOffset);
                augmentedFaceNode.setFaceMeshTexture(currentTexture);

                isAdded = true;
            }
        });


        Button button = findViewById(R.id.recordButton);
        button.setOnClickListener(view -> {
            if (videoRecorder == null) {
                videoRecorder = new VideoRecorder();
                videoRecorder.setSceneView(arFragment.getArSceneView());
                int orientation = getResources().getConfiguration().orientation;

                videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_HIGH, orientation);
            }

            boolean isRec = videoRecorder.onToggleRecord();
            if (isRec){
                Toast.makeText(this, "Started Recording", Toast.LENGTH_LONG).show();
                button.setText("Stop Recording");
            }else {
                Toast.makeText(this, "Stopped Recording", Toast.LENGTH_LONG).show();
                button.setText("Start Recording");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Save Video");
                final EditText tagEditText = new EditText(this);
                tagEditText.setHint("Enter a File Name");
                builder.setView(tagEditText);
                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tag = tagEditText.getText().toString();
                        videoRecorder.setFilename(tag);
                        videoRecorder.changeFileName();
                        Toast.makeText(SavedVidActivity.this, "Video Saved", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();

            }
        });

        FloatingActionButton roundButton = findViewById(R.id.roundButton);
        roundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the new activity
               Intent intent = new Intent(SavedVidActivity.this, MainActivity.class);
               startActivity(intent);
            }
        });
    }

    private void updateTexture(int textureResource) {
        Texture.builder().setSource(this, textureResource).build().thenAccept(texture -> {
            currentTexture = texture;

            // Update the augmented faces with the new texture
            if (arFragment != null && arFragment.getArSceneView() != null) {
                for (Node child : arFragment.getArSceneView().getScene().getChildren()) {
                    if (child instanceof AugmentedFaceNode) {
                        AugmentedFaceNode faceNode = (AugmentedFaceNode) child;
                        faceNode.setFaceMeshTexture(currentTexture);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
}