package com.harsh.shah.threads.clone.model;

import androidx.annotation.NonNull;

import java.util.List;

public class UserModel {
    private List<String> likedPosts;
    private String bio;
    private List<ThreadModel> threadsPosted;
    private boolean notificationsEnabled;
    private String profileImage;
    private String uid;
    private String password;
    private List<String> followers;
    private List<String> following;
    private String name;
    private boolean publicAccount;
    private List<String> blockedUsers;
    private String email;
    private List<NotificationItemModel> notifications;
    private String username;
    private String infoLink;
    private List<String> savedThreads;
    private String fcmToken;

    public UserModel() {
    }

    public UserModel(List<String> savedThreads, List<String> likedPosts, String bio, List<ThreadModel> threadsPosted, boolean notificationsEnabled, String profileImage, String uid, String password, List<String> followers, List<String> following, String name, boolean publicAccount, List<String> blockedUsers, String email, List<NotificationItemModel> notifications, String username, String infoLink) {
        this.savedThreads = savedThreads;
        this.likedPosts = likedPosts;
        this.bio = bio;
        this.threadsPosted = threadsPosted;
        this.notificationsEnabled = notificationsEnabled;
        this.profileImage = profileImage;
        this.uid = uid;
        this.password = password;
        this.followers = followers;
        this.following = following;
        this.name = name;
        this.publicAccount = publicAccount;
        this.blockedUsers = blockedUsers;
        this.email = email;
        this.notifications = notifications;
        this.username = username;
        this.infoLink = infoLink;
    }

    public List<String> getLikedPosts() {
        return likedPosts;
    }

    public void setLikedPosts(List<String> likedPosts) {
        this.likedPosts = likedPosts;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<ThreadModel> getThreadsPosted() {
        return threadsPosted;
    }

    public void setThreadsPosted(List<ThreadModel> threadsPosted) {
        this.threadsPosted = threadsPosted;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getFollowers() {
        return followers;
    }

    public void setFollowers(List<String> followers) {
        this.followers = followers;
    }

    public List<String> getFollowing() {
        return following;
    }

    public void setFollowing(List<String> following) {
        this.following = following;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPublicAccount() {
        return publicAccount;
    }

    public void setPublicAccount(boolean publicAccount) {
        this.publicAccount = publicAccount;
    }

    public List<String> getBlockedUsers() {
        return blockedUsers;
    }

    public void setBlockedUsers(List<String> blockedUsers) {
        this.blockedUsers = blockedUsers;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<NotificationItemModel> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationItemModel> notifications) {
        this.notifications = notifications;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInfoLink() {
        return infoLink;
    }

    public void setInfoLink(String infoLink) {
        this.infoLink = infoLink;
    }

    public List<String> getSavedThreads() {
        return savedThreads;
    }

    public void setSavedThreads(List<String> savedThreads) {
        this.savedThreads = savedThreads;
    }

    @NonNull
    @Override
    public String toString() {
        return
                "TestUsers{" +
                        "likedPosts = '" + likedPosts + '\'' +
                        ",bio = '" + bio + '\'' +
                        ",threadsPosted = '" + threadsPosted + '\'' +
                        ",notificationsEnabled = '" + notificationsEnabled + '\'' +
                        ",profileImage = '" + profileImage + '\'' +
                        ",uid = '" + uid + '\'' +
                        ",password = '" + password + '\'' +
                        ",followers = '" + followers + '\'' +
                        ",following = '" + following + '\'' +
                        ",name = '" + name + '\'' +
                        ",publicAccount = '" + publicAccount + '\'' +
                        ",blockedUsers = '" + blockedUsers + '\'' +
                        ",email = '" + email + '\'' +
                        ",notifications = '" + notifications + '\'' +
                        ",username = '" + username + '\'' +
                        ",infoLink = '" + infoLink + '\'' +
                        ",savedThreads = '" + savedThreads + '\'' +
                        ",fcmToken = '" + fcmToken + '\'' +
                        "}";
    }

    public void setFcmToken(String token) {
        this.fcmToken = token;
    }
    public String getFcmToken() {
        return fcmToken;
    }

}