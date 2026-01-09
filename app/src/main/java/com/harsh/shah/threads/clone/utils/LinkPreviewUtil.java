package com.harsh.shah.threads.clone.utils;

import android.os.Handler;
import android.os.Looper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinkPreviewUtil {

    public interface PreviewCallback {
        void onPreviewLoaded(LinkPreview preview);
        void onError(Exception e);
    }

    public static class LinkPreview {
        public String url;
        public String title;
        public String description;
        public String imageUrl;

        public LinkPreview(String url, String title, String description, String imageUrl) {
            this.url = url;
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
        }
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void fetchPreview(String url, PreviewCallback callback) {
        executor.execute(() -> {
            try {
                // Ensure protocol is present
                String fetchUrl = url;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    fetchUrl = "https://" + url;
                }

                Document doc = Jsoup.connect(fetchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(10000)
                        .get();

                String title = getMetaTag(doc, "og:title");
                if (title.isEmpty()) title = doc.title();

                String description = getMetaTag(doc, "og:description");
                if (description.isEmpty()) description = getMetaTag(doc, "description");

                String image = getMetaTag(doc, "og:image");

                LinkPreview preview = new LinkPreview(fetchUrl, title, description, image);

                mainHandler.post(() -> callback.onPreviewLoaded(preview));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private static String getMetaTag(Document doc, String property) {
        Element el = doc.select("meta[property=" + property + "]").first();
        if (el == null) {
            el = doc.select("meta[name=" + property + "]").first();
        }
        return el != null ? el.attr("content") : "";
    }
}
