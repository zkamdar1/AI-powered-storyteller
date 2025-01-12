package com.example.zk_final_433;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class StoryBoardAdapter extends ArrayAdapter<TaggedImage> {

    private Context context;
    private List<TaggedImage> taggedImages;
    private List<TaggedImage> selectedImages;
    private static final int MAX_SELECTIONS = 3;


    public StoryBoardAdapter(Context context, List<TaggedImage> taggedImages, List<TaggedImage> selectedImages) {
        super(context, R.layout.list_item_checkbox, taggedImages);
        this.context = context;
        this.taggedImages = taggedImages;
        this.selectedImages = selectedImages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_checkbox, parent, false);
        }

        TaggedImage taggedImage = taggedImages.get(position);

        ImageView imageThumbnail = convertView.findViewById(R.id.tagged_image);
        TextView imageTags = convertView.findViewById(R.id.tag_text);
        CheckBox selectCheckbox = convertView.findViewById(R.id.select_checkbox);
        TextView timestampTextView = convertView.findViewById(R.id.timestamp_text);

        imageThumbnail.setImageBitmap(taggedImage.getImage());
        imageTags.setText(taggedImage.getTag());
        selectCheckbox.setChecked(selectedImages.contains(taggedImage));
        timestampTextView.setText(taggedImage.getTimestamp());

        selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (selectedImages.size() < MAX_SELECTIONS) {
                    selectedImages.add(taggedImage);
                } else {
                    // Prevent additional selection and uncheck the box
                    selectCheckbox.setChecked(false);
                    Toast.makeText(context, "You can only select up to 3 images.", Toast.LENGTH_SHORT).show();
                }
            } else {
                selectedImages.remove(taggedImage);
            }
            updateSelectedTagsText();
        });

        return convertView;
    }

    private void updateSelectedTagsText() {
        List<String> tags = new ArrayList<>();
        for (TaggedImage image : selectedImages) {
            tags.add(image.getTag());
        }
        ((StoryBoardActivity) context).updateSelectedTagsText(tags);
    }
}
