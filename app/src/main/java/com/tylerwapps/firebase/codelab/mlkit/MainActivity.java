// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.tylerwapps.firebase.codelab.mlkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private EditText editText;
    private Bitmap mSelectedImage;
    private GraphicOverlay mGraphicOverlay;
    private Camera camera;
    private Button takePicButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        takePicButton = findViewById(R.id.takePicButton);
        mGraphicOverlay = findViewById(R.id.graphic_overlay);

        buildCamera();

        takePicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    camera.takePicture();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });


        try {
            camera.takePicture();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void buildCamera(){
        camera = new Camera.Builder()
                .resetToCorrectOrientation(true)// it will rotate the camera bitmap to the correct orientation from meta data
                .setTakePhotoRequestCode(1)
                .setDirectory("pics")
                .setName("ali_" + System.currentTimeMillis())
                .setImageFormat(Camera.IMAGE_JPEG)
                .setCompression(75)
                .setImageHeight(1000)// it will try to achieve this height as close as possible maintaining the aspect ratio;
                .build(this);
    }

    // Get the bitmap and image path onActivityResult of an activity or fragment
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Camera.REQUEST_TAKE_PHOTO){
            Bitmap bitmap = camera.getCameraBitmap();
            if(bitmap != null) {
                setImage(bitmap);
                runTextRecognition();
            }else{
                Toast.makeText(this.getApplicationContext(),"Picture not taken!",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void runTextRecognition() {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mSelectedImage);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();

        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {

                                updateTextBox(sortTextBlocksByVerticalPosition(texts));
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
    }


    public List<FirebaseVisionText.Block> sortTextBlocksByVerticalPosition(FirebaseVisionText texts){

        List<FirebaseVisionText.Block> blocks = texts.getBlocks();

        TreeMap<Integer, FirebaseVisionText.Block> blockTreeMap = new TreeMap<>();

        for (int i = 0; i < blocks.size(); i++) {
            int verticalScore = 0;
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {

                    Point[] firstPoints = elements.get(0).getCornerPoints();
                    Point[] lastPoints = elements.get(elements.size() - 1).getCornerPoints();

                    verticalScore = firstPoints[0].y + lastPoints[0].y;
                }
            }
            blockTreeMap.put(verticalScore, blocks.get(i));
        }

        List<FirebaseVisionText.Block> blockList = new ArrayList<>();

        for (Integer key : blockTreeMap.keySet()) {
            blockList.add(blockTreeMap.get(key));
        }
        return blockList;
    }

    public void updateTextBox(List<FirebaseVisionText.Block> blocks){

        if (blocks.size() == 0) {
            showToast("No text found");
            return;
        }

        String foundText = "";

        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                if (j > 0){
                    foundText = foundText + "\n";
                }

                for (int k = 0; k < elements.size(); k++) {
                    foundText = foundText.concat(elements.get(k).getText() + " ");
                }
            }
        }

        editText.setText(foundText);

    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void setImage(Bitmap bitmap){
        mGraphicOverlay.clear();
        if (bitmap != null)
        mSelectedImage = bitmap;
    }

    // The bitmap is saved in the app's folder
    //  If the saved bitmap is not required use following code
    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.deleteImage();
    }
}
