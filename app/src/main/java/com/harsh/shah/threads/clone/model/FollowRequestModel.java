package com.harsh.shah.threads.clone.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * Model for follow requests sent to private accounts
 */
public class FollowRequestModel implements Parcelable {
    
    private String requestId;
    private String requesterId;
    private String requesterUsername;
    private String requesterName;
    private String requesterProfileImage;
    private String targetUserId;
    private String timestamp;
    private String status; // "pending", "accepted", "rejected"
    
    public FollowRequestModel() {
    }
    
    public FollowRequestModel(String requestId, String requesterId, String requesterUsername, 
                             String requesterName, String requesterProfileImage, 
                             String targetUserId, String timestamp, String status) {
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.requesterUsername = requesterUsername;
        this.requesterName = requesterName;
        this.requesterProfileImage = requesterProfileImage;
        this.targetUserId = targetUserId;
        this.timestamp = timestamp;
        this.status = status;
    }
    
    protected FollowRequestModel(Parcel in) {
        requestId = in.readString();
        requesterId = in.readString();
        requesterUsername = in.readString();
        requesterName = in.readString();
        requesterProfileImage = in.readString();
        targetUserId = in.readString();
        timestamp = in.readString();
        status = in.readString();
    }
    
    public static final Creator<FollowRequestModel> CREATOR = new Creator<FollowRequestModel>() {
        @Override
        public FollowRequestModel createFromParcel(Parcel in) {
            return new FollowRequestModel(in);
        }
        
        @Override
        public FollowRequestModel[] newArray(int size) {
            return new FollowRequestModel[size];
        }
    };
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getRequesterId() {
        return requesterId;
    }
    
    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }
    
    public String getRequesterUsername() {
        return requesterUsername;
    }
    
    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }
    
    public String getRequesterName() {
        return requesterName;
    }
    
    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }
    
    public String getRequesterProfileImage() {
        return requesterProfileImage;
    }
    
    public void setRequesterProfileImage(String requesterProfileImage) {
        this.requesterProfileImage = requesterProfileImage;
    }
    
    public String getTargetUserId() {
        return targetUserId;
    }
    
    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(requestId);
        dest.writeString(requesterId);
        dest.writeString(requesterUsername);
        dest.writeString(requesterName);
        dest.writeString(requesterProfileImage);
        dest.writeString(targetUserId);
        dest.writeString(timestamp);
        dest.writeString(status);
    }
    
    @NonNull
    @Override
    public String toString() {
        return "FollowRequestModel{" +
                "requestId='" + requestId + '\'' +
                ", requesterId='" + requesterId + '\'' +
                ", requesterUsername='" + requesterUsername + '\'' +
                ", targetUserId='" + targetUserId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
