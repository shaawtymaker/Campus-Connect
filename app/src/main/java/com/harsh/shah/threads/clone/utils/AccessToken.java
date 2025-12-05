package com.harsh.shah.threads.clone.utils;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AccessToken {

    private static final String SCOPES = "https://fcm.googleapis.com/fcm/send";

    private static String getAccessToken() throws IOException {

        final InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(inputStream)
                .createScoped(Arrays.asList(SCOPES));
        googleCredentials.refresh();
        return googleCredentials.getAccessToken().getTokenValue();
    }
}
