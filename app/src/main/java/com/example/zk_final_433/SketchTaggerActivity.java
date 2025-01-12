package com.example.zk_final_433;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.os.Handler;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import java.io.IOException;

public class SketchTaggerActivity extends AppCompatActivity {
    private final String API_KEY = "AIzaSyCqkCrHsxgMk1GWpq-IWt6uq4iQ5cifXcQ";
    private EditText inputTags, searchTag;
    private SQLiteDatabase myDB;
    private Bitmap placeHolder;
    ListView taggedImagesList;
    List<TaggedImage> taggedImages;
    TaggedImageAdapter adapter;
    private Handler uiHandler;
    private MyDrawingArea drawingArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingArea = findViewById(R.id.cusview);

        drawingArea.setOnDrawingCompleteListener(new MyDrawingArea.OnDrawingCompleteListener() {
            @Override
            public void onDrawingComplete(Bitmap drawing) {
                generateTagsFromImage(drawing);
            }
        });

        taggedImagesList = findViewById(R.id.tagged_sketch_list);
        taggedImages = new ArrayList<>();
        adapter = new TaggedImageAdapter(this, taggedImages);
        taggedImagesList.setAdapter(adapter);
        inputTags = findViewById(R.id.Tags);
        searchTag = findViewById(R.id.SearchTag);
        uiHandler = new Handler(Looper.getMainLooper());

        placeHolder = Bitmap.createBitmap(110, 110, Bitmap.Config.ARGB_8888);
        placeHolder.eraseColor(Color.GRAY);

        myDB = this.openOrCreateDatabase("Drawings", Context.MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS Drawings(" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Drawing BLOB," +
                "Date TEXT," +
                "Time TEXT," +
                "Tags TEXT)");

        displayRecentImages();

        findViewById(R.id.backbtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SketchTaggerActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }


    public void clearCanvas(View view) {
        MyDrawingArea drawingArea = findViewById(R.id.cusview);
        drawingArea.clearDraw();
    }

    public void saveDrawing(View view) {
        MyDrawingArea drawingArea = findViewById(R.id.cusview);
        Bitmap drawing = drawingArea.getBitmap();
        String tags = inputTags.getText().toString();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        drawing.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] drawingByte = stream.toByteArray();

        SimpleDateFormat date = new SimpleDateFormat("MMM d, yyyy");
        SimpleDateFormat time = new SimpleDateFormat(" - h a");
        String currentDate = date.format(new Date());
        String currentTime = time.format(new Date());

        ContentValues cv = new ContentValues();

        cv.put("Drawing", drawingByte);
        cv.put("Date", currentDate);
        cv.put("Time", currentTime);
        cv.put("Tags", tags);
        myDB.insert("Drawings", null, cv);

        drawingArea.clearDraw();

        inputTags.setText("");

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        displayRecentImages();
    }

    private void generateTagsFromImage(Bitmap bitmap) {
        new Thread(() -> {
            try {
                List<String> tags = classifyImage(bitmap);
                String tagText = String.join(", ", tags);

                uiHandler.post(() -> inputTags.setText(tagText));

            } catch (IOException e) {
                Log.e("VisionAPI", "Error classifying image", e);
            }
        }).start();
    }

    private List<String> classifyImage(Bitmap bitmap) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, bout);
        Image myImage = new Image();
        myImage.encodeContent(bout.toByteArray());

        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myImage);
        Feature feature = new Feature();
        feature.setType("LABEL_DETECTION");
        feature.setMaxResults(10);
        annotateImageRequest.setFeatures(List.of(feature));

        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer(API_KEY));
        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
        batchRequest.setRequests(List.of(annotateImageRequest));
        Vision.Images.Annotate annotate = vision.images().annotate(batchRequest);

        BatchAnnotateImagesResponse response = annotate.execute();
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();

        List<String> tags = new ArrayList<>();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                if (label.getScore() >= 0.85) {
                    tags.add(label.getDescription());
                }
            }
            if (tags.isEmpty() && !labels.isEmpty()) {
                tags.add(labels.get(0).getDescription());
            }
        }
        return tags;
    }

    private void displayRecentImages() {
        taggedImages.clear();
        Cursor cursor = myDB.rawQuery("SELECT * FROM Drawings ORDER BY ID DESC", null);

        if (cursor.moveToFirst()) {
            do {
                byte[] imageBlob = cursor.getBlob(cursor.getColumnIndex("Drawing"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                String time = cursor.getString(cursor.getColumnIndex("Time"));
                String tags = cursor.getString(cursor.getColumnIndex("Tags"));

                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                String timestamp = date + " " + time;

                TaggedImage taggedImage = new TaggedImage(imageBitmap, tags, timestamp);
                taggedImages.add(taggedImage);
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    public void findImages(View view) {
        String searchTags = searchTag.getText().toString().trim();
        taggedImages.clear();

        String query = "SELECT * FROM Drawings";
        if (!searchTags.isEmpty()) {
            query += " WHERE Tags LIKE '%" + searchTags + "%'";
        }
        query += " ORDER BY ID DESC";

        Cursor cursor = myDB.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            int i = 0;
            do {
                byte[] imageBlob = cursor.getBlob(cursor.getColumnIndex("Drawing"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                String time = cursor.getString(cursor.getColumnIndex("Time"));
                String tags = cursor.getString(cursor.getColumnIndex("Tags"));

                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                String timestamp = date + " " + time;

                TaggedImage taggedImage = new TaggedImage(imageBitmap, tags, timestamp);
                taggedImages.add(taggedImage);
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
        searchTag.setText("");

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
