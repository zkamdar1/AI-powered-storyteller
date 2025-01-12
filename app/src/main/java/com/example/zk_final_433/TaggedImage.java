package com.example.zk_final_433;

import android.graphics.Bitmap;

public class TaggedImage {
    private Bitmap image;
    private String tag;
    private String timestamp;

    public TaggedImage(Bitmap image, String tag, String timestamp) {
        this.image = image;
        this.tag = tag;
        this.timestamp = timestamp;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getTag() {
        return tag;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
