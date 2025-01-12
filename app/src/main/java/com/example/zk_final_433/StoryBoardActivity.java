package com.example.zk_final_433;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class StoryBoardActivity extends AppCompatActivity {

    private SQLiteDatabase myDBphoto, myDBdraw;
    private EditText searchTag;
    private CheckBox filterPhotosOnly;
    private List<TaggedImage> taggedImages;
    private List<TaggedImage> selectedImages;
    private StoryBoardAdapter adapter;
    private TextView selectedTagsTextView, storyTextView;
    private static final int MAX_SELECTIONS = 3;
    private static final String url = "https://api.textcortex.com/v1/texts/social-media-posts";
    private static final String API_KEY = "gAAAAABnNV00XY5FlgYIG12mrd8w8B_EVfzZRn6lQhBx845s3JYBjm9e2IJWLVE4WmBOcSCVVdypbq43NQnUoT1cIZLHP6feGtqZJgJS2Ogv7PpST45hBvVRSQRpIgGyZMaN-ipQDN-u";
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        searchTag = findViewById(R.id.SearchTags);
        filterPhotosOnly = findViewById(R.id.filter_photos_only);
        ListView taggedImagesList = findViewById(R.id.tagged_story_list);
        selectedTagsTextView = findViewById(R.id.selected_tags_text);
        storyTextView = findViewById(R.id.story_text);

        taggedImages = new ArrayList<>();
        selectedImages = new ArrayList<>();
        adapter = new StoryBoardAdapter(this, taggedImages, selectedImages);
        taggedImagesList.setAdapter(adapter);

        myDBphoto = this.openOrCreateDatabase("Pictures", Context.MODE_PRIVATE, null);
        myDBdraw = this.openOrCreateDatabase("Drawings", Context.MODE_PRIVATE, null);

        taggedImagesList.setOnItemClickListener((parent, view, position, id) -> {
            TaggedImage clickedImage = taggedImages.get(position);
            if (selectedImages.contains(clickedImage)) {
                selectedImages.remove(clickedImage);
            } else if (selectedImages.size() < MAX_SELECTIONS) {
                selectedImages.add(clickedImage);
            } else {
                Toast.makeText(this, "You can only select up to 3 images.", Toast.LENGTH_SHORT).show();
            }
        });

        List<String> tags = new ArrayList<>();
        for (TaggedImage image : selectedImages) {
            tags.add(image.getTag());
        }
        updateSelectedTagsText(tags);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                } else {
                    Log.e("TTS", "TTS initialization failed.");
                }
            }
        });

        Button findButton = findViewById(R.id.find_button);
        findButton.setOnClickListener(v -> findImages());

        filterPhotosOnly.setOnCheckedChangeListener((buttonView, isChecked) -> findImages());

        Button storyButton = findViewById(R.id.story_button);
        storyButton.setOnClickListener(v -> generateStory());

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Load all images and sketches on activity start
        loadAllImagesAndSketches();
    }

    private void loadAllImagesAndSketches() {
        taggedImages.clear();

        // Fetch photos from Pictures table
        String photoQuery = "SELECT * FROM Pictures ORDER BY ID DESC";
        Cursor photoCursor = myDBphoto.rawQuery(photoQuery, null);
        if (photoCursor.moveToFirst()) {
            do {
                byte[] imageBlob = photoCursor.getBlob(photoCursor.getColumnIndex("Picture"));
                String tags = photoCursor.getString(photoCursor.getColumnIndex("Tags"));
                String date = photoCursor.getString(photoCursor.getColumnIndex("Date"));
                String time = photoCursor.getString(photoCursor.getColumnIndex("Time"));
                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                String timestamp = date + " " + time;

                TaggedImage taggedImage = new TaggedImage(imageBitmap, tags, timestamp);
                taggedImages.add(taggedImage);
            } while (photoCursor.moveToNext());
        }
        photoCursor.close();

        // Fetch sketches from Drawings table
        String sketchQuery = "SELECT * FROM Drawings ORDER BY ID DESC";
        Cursor sketchCursor = myDBdraw.rawQuery(sketchQuery, null);
        if (sketchCursor.moveToFirst()) {
            do {
                byte[] sketchBlob = sketchCursor.getBlob(sketchCursor.getColumnIndex("Drawing"));
                String tags = sketchCursor.getString(sketchCursor.getColumnIndex("Tags"));
                String date = sketchCursor.getString(sketchCursor.getColumnIndex("Date"));
                String time = sketchCursor.getString(sketchCursor.getColumnIndex("Time"));
                Bitmap sketchBitmap = BitmapFactory.decodeByteArray(sketchBlob, 0, sketchBlob.length);
                String timestamp = date + " " + time;

                TaggedImage taggedImage = new TaggedImage(sketchBitmap, tags, timestamp);
                taggedImages.add(taggedImage);
            } while (sketchCursor.moveToNext());
        }
        sketchCursor.close();

        adapter.notifyDataSetChanged();
    }

    private void findImages() {
        String searchTags = searchTag.getText().toString().trim();
        taggedImages.clear();

        // Check if the checkbox is checked (show everything) or unchecked (show only photos)
        if (filterPhotosOnly.isChecked()) {
            // Query both photos and sketches
            String photoQuery = "SELECT * FROM Pictures";
            if (!searchTags.isEmpty()) {
                String[] tags = searchTags.split(",");
                photoQuery += " WHERE ";
                for (int i = 0; i < tags.length; i++) {
                    photoQuery += "Tags LIKE '%" + tags[i].trim() + "%'";
                    if (i < tags.length - 1) {
                        photoQuery += " OR ";
                    }
                }
            }
            photoQuery += " ORDER BY ID DESC";

            Cursor photoCursor = myDBphoto.rawQuery(photoQuery, null);
            if (photoCursor.moveToFirst()) {
                do {
                    byte[] imageBlob = photoCursor.getBlob(photoCursor.getColumnIndex("Picture"));
                    String tags = photoCursor.getString(photoCursor.getColumnIndex("Tags"));
                    String date = photoCursor.getString(photoCursor.getColumnIndex("Date"));
                    String time = photoCursor.getString(photoCursor.getColumnIndex("Time"));
                    Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                    String timestamp = date + " " + time;

                    TaggedImage taggedImage = new TaggedImage(imageBitmap, tags, timestamp);
                    taggedImages.add(taggedImage);
                } while (photoCursor.moveToNext());
            }
            photoCursor.close();

            // Fetch sketches
            String sketchQuery = "SELECT * FROM Drawings";
            if (!searchTags.isEmpty()) {
                String[] tags = searchTags.split(",");
                sketchQuery += " WHERE ";
                for (int i = 0; i < tags.length; i++) {
                    sketchQuery += "Tags LIKE '%" + tags[i].trim() + "%'";
                    if (i < tags.length - 1) {
                        sketchQuery += " OR ";
                    }
                }
            }
            sketchQuery += " ORDER BY ID DESC";

            Cursor sketchCursor = myDBdraw.rawQuery(sketchQuery, null);
            if (sketchCursor.moveToFirst()) {
                do {
                    byte[] sketchBlob = sketchCursor.getBlob(sketchCursor.getColumnIndex("Drawing"));
                    String tags = sketchCursor.getString(sketchCursor.getColumnIndex("Tags"));
                    String date = sketchCursor.getString(sketchCursor.getColumnIndex("Date"));
                    String time = sketchCursor.getString(sketchCursor.getColumnIndex("Time"));
                    Bitmap sketchBitmap = BitmapFactory.decodeByteArray(sketchBlob, 0, sketchBlob.length);
                    String timestamp = date + " " + time;

                    TaggedImage taggedImage = new TaggedImage(sketchBitmap, tags, timestamp);
                    taggedImages.add(taggedImage);
                } while (sketchCursor.moveToNext());
            }
            sketchCursor.close();
        } else {
            // Only query photos
            String photoQuery = "SELECT * FROM Pictures";
            if (!searchTags.isEmpty()) {
                String[] tags = searchTags.split(",");
                photoQuery += " WHERE ";
                for (int i = 0; i < tags.length; i++) {
                    photoQuery += "Tags LIKE '%" + tags[i].trim() + "%'";
                    if (i < tags.length - 1) {
                        photoQuery += " OR ";
                    }
                }
            }
            photoQuery += " ORDER BY ID DESC";

            Cursor photoCursor = myDBphoto.rawQuery(photoQuery, null);
            if (photoCursor.moveToFirst()) {
                do {
                    byte[] imageBlob = photoCursor.getBlob(photoCursor.getColumnIndex("Picture"));
                    String tags = photoCursor.getString(photoCursor.getColumnIndex("Tags"));
                    String date = photoCursor.getString(photoCursor.getColumnIndex("Date"));
                    String time = photoCursor.getString(photoCursor.getColumnIndex("Time"));
                    Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                    String timestamp = date + " " + time;

                    TaggedImage taggedImage = new TaggedImage(imageBitmap, tags, timestamp);
                    taggedImages.add(taggedImage);
                } while (photoCursor.moveToNext());
            }
            photoCursor.close();
        }

        adapter.notifyDataSetChanged();
    }


    public void updateSelectedTagsText(List<String> tags) {
        selectedTagsTextView.setText("Selected Tags: " + String.join(", ", tags));
    }

    private void generateStory() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Please select at least one image to generate a story.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> tags = new ArrayList<>();
        for (TaggedImage image : selectedImages) {
            tags.add(image.getTag());
        }

        String context = "Only provide the story response text and nothing else. Create a creative and engaging short story(1-3 sentences) using the following tags: " + String.join(", ", tags);

        // Construct JSON body
        JSONObject data = new JSONObject();
        try {
            data.put("context", context);
            data.put("keywords", new JSONArray(tags));
            data.put("max_tokens", 30);
            data.put("mode", "twitter");
            data.put("model", "claude-3-haiku");
            data.put("n", 1);
            data.put("source_lang", "en");
            data.put("target_lang", "en");
        } catch (Exception e) {
            Toast.makeText(this, "Error building request JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("success", response.toString());
                try {
                    JSONObject dataObject = response.getJSONObject("data");

                    JSONArray outputsArray = dataObject.getJSONArray("outputs");

                    JSONObject firstOutput = outputsArray.getJSONObject(0);

                    String storyText = firstOutput.getString("text");

                    storyTextView.setText(storyText);

                    tts.speak(storyText, TextToSpeech.QUEUE_FLUSH, null, null);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(StoryBoardActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null) {
                    Log.e("error", "Status Code: " + error.networkResponse.statusCode);
                    Log.e("error", new String(error.networkResponse.data));
                } else {
                    Log.e("error", "Network error: " + error.getMessage());
                    Toast.makeText(StoryBoardActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " +API_KEY);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
