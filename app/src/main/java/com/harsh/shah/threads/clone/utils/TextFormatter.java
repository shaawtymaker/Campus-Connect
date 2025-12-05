package com.harsh.shah.threads.clone.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.harsh.shah.threads.clone.activities.HashtagActivity;
import com.harsh.shah.threads.clone.activities.ProfileActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting thread text with clickable hashtags and mentions
 */
public class TextFormatter {

    private static final String HASHTAG_PATTERN = "#[\\w]+";
    private static final String MENTION_PATTERN = "@[\\w]+";
    private static final int HIGHLIGHT_COLOR = Color.parseColor("#0095F6"); // Instagram blue

    /**
     * Parse hashtags from text
     */
    public static List<String> parseHashtags(String text) {
        List<String> hashtags = new ArrayList<>();
        if (text == null || text.isEmpty()) return hashtags;

        Pattern pattern = Pattern.compile(HASHTAG_PATTERN);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String hashtag = matcher.group().substring(1); // Remove # symbol
            if (!hashtags.contains(hashtag)) {
                hashtags.add(hashtag);
            }
        }

        return hashtags;
    }

    /**
     * Parse mentions from text
     */
    public static List<String> parseMentions(String text) {
        List<String> mentions = new ArrayList<>();
        if (text == null || text.isEmpty()) return mentions;

        Pattern pattern = Pattern.compile(MENTION_PATTERN);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String mention = matcher.group().substring(1); // Remove @ symbol
            if (!mentions.contains(mention)) {
                mentions.add(mention);
            }
        }

        return mentions;
    }

    /**
     * Format text with clickable hashtags and mentions
     */
    public static SpannableString formatThreadText(String text, Context context) {
        if (text == null || text.isEmpty()) {
            return new SpannableString("");
        }

        SpannableString spannableString = new SpannableString(text);

        // Add hashtag spans
        addHashtagSpans(spannableString, text, context);

        // Add mention spans
        addMentionSpans(spannableString, text, context);

        return spannableString;
    }

    /**
     * Apply formatted text to TextView and enable link clicks
     */
    public static void applyFormattedText(TextView textView, String text, Context context) {
        SpannableString formatted = formatThreadText(text, context);
        textView.setText(formatted);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT); // Remove highlight on click
    }

    /**
     * Add clickable spans for hashtags
     */
    private static void addHashtagSpans(SpannableString spannable, String text, Context context) {
        Pattern pattern = Pattern.compile(HASHTAG_PATTERN);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            final String hashtag = matcher.group().substring(1); // Remove # symbol
            final int start = matcher.start();
            final int end = matcher.end();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(context, HashtagActivity.class);
                    intent.putExtra("hashtag", hashtag);
                    context.startActivity(intent);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(HIGHLIGHT_COLOR);
                    ds.setUnderlineText(false);
                }
            };

            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Add clickable spans for mentions
     */
    private static void addMentionSpans(SpannableString spannable, String text, Context context) {
        Pattern pattern = Pattern.compile(MENTION_PATTERN);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            final String username = matcher.group().substring(1); // Remove @ symbol
            final int start = matcher.start();
            final int end = matcher.end();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("username", username);
                    context.startActivity(intent);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(HIGHLIGHT_COLOR);
                    ds.setUnderlineText(false);
                }
            };

            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
