package com.harsh.shah.threads.clone.model;

import androidx.annotation.NonNull;

public class NotificationItemModel {
    public String senderUid;
    public boolean isFollowRequest;
    public String title;
    public String description;
    public String postId;
    public String time;

    public NotificationItemModel(String senderUid, boolean isFollowRequest, String title, String description, String postId, String time) {
        this.senderUid = senderUid;
        this.isFollowRequest = isFollowRequest;
        this.title = title;
        this.description = description;
        this.postId = postId;
        this.time = time;
    }

    public NotificationItemModel() {
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public boolean isFollowRequest() {
        return isFollowRequest;
    }

    public void setFollowRequest(boolean followRequest) {
        isFollowRequest = followRequest;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @NonNull
    @Override
    public String toString() {
        return "NotificationItemModel{" +
                "senderUid='" + senderUid + '\'' +
                ", isFollowRequest=" + isFollowRequest +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", postId='" + postId + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
