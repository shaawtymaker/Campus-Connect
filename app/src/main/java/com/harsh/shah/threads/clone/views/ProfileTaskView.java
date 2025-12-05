package com.harsh.shah.threads.clone.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.harsh.shah.threads.clone.R;


public class ProfileTaskView extends LinearLayout {

    public ProfileTaskView(Context context) {
        super(context);
        init(null, 0);
    }

    public ProfileTaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ProfileTaskView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes

        inflate(getContext(), R.layout.profile_setup_task_view, this);

        final ShapeableImageView imageView = findViewById(R.id.imageView);
        final TextView title = findViewById(R.id.title);
        final TextView description = findViewById(R.id.description);
        final TextView button = findViewById(R.id.button);


        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ProfileTaskView, defStyle, 0);

        imageView.setImageDrawable(a.getDrawable(R.styleable.ProfileTaskView_imageSrc));
        title.setText(a.getString(R.styleable.ProfileTaskView_title));
        description.setText(a.getString(R.styleable.ProfileTaskView_description));
        button.setText(a.getString(R.styleable.ProfileTaskView_buttonTitle));

        a.recycle();
    }

}