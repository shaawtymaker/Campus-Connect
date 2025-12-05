package com.harsh.shah.threads.clone.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static int getRandomNumber(int Min, int Max) {
        return Min + (int) (Math.random() * ((Max - Min) + 1));
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static long getNowInMillis() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public static String calculateTimeDiff(long createdAt) {
        long now = Calendar.getInstance().getTimeInMillis();
        long diffInMillis = now - createdAt;

        // Convert milliseconds to seconds, minutes, hours, days, and weeks
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long weeks = TimeUnit.MILLISECONDS.toDays(diffInMillis) / 7;

        // Determine the appropriate time unit based on the difference
        if (weeks > 0) {
            return weeks + "w";
        } else if (days > 0) {
            return days + "d";
        } else if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }

    public static String calculateLikes(int likes){
        if(likes>=1000)
            return String.valueOf(likes/1000)+"k";
        else
            return String.valueOf(likes);
    }

    public static Bitmap getBitmapFromURL(String s) {
        try {
            URL url = new URL(s);
            InputStream inputStream = url.openStream();
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            return null;
        }
    }
}