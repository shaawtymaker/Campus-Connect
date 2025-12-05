package com.harsh.shah.threads.clone.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harsh.shah.threads.clone.R;

public class MDialogUtil extends MaterialAlertDialogBuilder {
    View view;

    public MDialogUtil(@NonNull Context context) {
        super(context);
        view = View.inflate(context, R.layout.material_dialog_view,null);
        setView(view);
    }

    public MDialogUtil setTitle(String title){
        ((TextView)view.findViewById(R.id.title)).setText(title);
        return this;
    }

    public MDialogUtil setMessage(String message, boolean isVisible){
        if(!isVisible) {
            view.findViewById(R.id.subtitle).setVisibility(View.GONE);
            return this;
        }
        ((TextView)view.findViewById(R.id.subtitle)).setText(message);
        return this;
    }

    public MDialogUtil setB1(String text, View.OnClickListener listener){
        view.findViewById(R.id.b1).setOnClickListener(listener);
        ((TextView)view.findViewById(R.id.b1)).setText(text);
        return this;
    }

    public MDialogUtil setB2(String text, View.OnClickListener listener){
        view.findViewById(R.id.b2).setOnClickListener(listener);
        ((TextView)view.findViewById(R.id.b2)).setText(text);
        return this;
    }

    public TextView getTitleView(){
        return (TextView)view.findViewById(R.id.title);
    }

    public TextView getSubTitleView(){
        return (TextView)view.findViewById(R.id.subtitle);
    }

    public TextView getB1View(){
        return (TextView)view.findViewById(R.id.b1);
    }

    public TextView getB2View(){
        return (TextView)view.findViewById(R.id.b2);
    }
}
