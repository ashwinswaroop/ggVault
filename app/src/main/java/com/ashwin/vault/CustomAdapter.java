package com.ashwin.vault;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final ArrayList<String>  files;

    public CustomAdapter(Activity context,
                         ArrayList<String> files) {
        super(context, R.layout.list_row, files);
        this.context = context;
        this.files = files;

    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.list_row, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.textView1);

        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView1);
        txtTitle.setText(files.get(position));

        imageView.setImageResource(R.drawable.lock);
        return rowView;
    }
}