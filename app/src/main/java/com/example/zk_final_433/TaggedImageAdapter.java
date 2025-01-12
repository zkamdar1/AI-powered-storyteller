package com.example.zk_final_433;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class TaggedImageAdapter extends ArrayAdapter<TaggedImage> {

    private Context context;
    private List<TaggedImage> images;

    public TaggedImageAdapter(Context context, List<TaggedImage> images) {
        super(context, 0, images);
        this.context = context;
        this.images = images;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_tagged_image, parent, false);
        }

        TaggedImage taggedImage = images.get(position);

        ImageView imageView = convertView.findViewById(R.id.tagged_image);
        TextView tagTextView = convertView.findViewById(R.id.tag_text);
        TextView timestampTextView = convertView.findViewById(R.id.timestamp_text);

        imageView.setImageBitmap(taggedImage.getImage());
        tagTextView.setText(taggedImage.getTag());
        timestampTextView.setText(taggedImage.getTimestamp());

        return convertView;
    }
}
