# Threads Clone - Android App

A feature-rich social media application built for Android, inspired by Meta's Threads platform. This app implements core social networking features including threaded conversations, privacy controls, and real-time interactions.

## 🎯 Features

### Phase 1: Core Social Features
- **Clickable Hashtags** - Tap hashtags to view all posts with that tag
- **Clickable Mentions** - Tap @mentions to view user profiles
- **Repost Functionality** - Repost threads with toggle support
- **External Sharing** - Share threads to other apps (WhatsApp, Email, etc.)

### Phase 2: Performance Enhancements
- **Pagination** - Efficient loading with 20 items per page
- **Infinite Scroll** - Automatic loading as you scroll
- **Pull-to-Refresh** - Swipe down to refresh your feed

### Phase 3: Advanced Social Features
- **Follow Requests** - Privacy control for private accounts
- **Request Management** - Accept/reject follow requests
- **Nested Replies** - Threaded conversations up to 3 levels deep
- **Visual Hierarchy** - Indented replies for clear conversation flow

## 🏗️ Architecture

- **Language**: Java
- **Backend**: Firebase Realtime Database
- **Authentication**: Firebase Auth with Google Sign-in
- **UI**: Material Design Components
- **Image Loading**: Picasso

## 📦 Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Firebase project configured
- Android device/emulator (API 21+)

### Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add your Android app to the project
3. Download `google-services.json` and place in `app/` directory
4. Enable Google Sign-In in Firebase Authentication
5. Create a Realtime Database

### Firebase Security Rules

**IMPORTANT**: Add these rules to your Realtime Database:

1. Go to Firebase Console → Realtime Database → Rules
2. Replace with the following:

```json
{
  "rules": {
    "Users": {
      ".read": true,
      ".write": "auth != null"
    },
    "Threads": {
      ".read": true,
      ".write": "auth != null"
    },
    "FollowRequests": {
      "$targetUserId": {
        ".read": "$targetUserId === auth.uid",
        ".write": "$targetUserId === auth.uid || auth != null",
        "$requestId": {
          ".validate": "newData.hasChildren(['requesterId', 'requesterUsername', 'targetUserId', 'timestamp', 'status'])"
        }
      }
    }
  }
}
```

3. Click **Publish**

### Build and Run

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build → Make Project
5. Run on device/emulator

## 🧪 Testing Checklist

### Pre-Test Setup
- [ ] Build succeeds without errors
- [ ] App installs on device/emulator
- [ ] Firebase rules published
- [ ] Can sign in/create account

### Phase 1: Hashtags & Mentions

**Hashtags**
- [ ] Create thread with hashtag: "Love #android development"
- [ ] Hashtag is blue/clickable
- [ ] Click hashtag opens HashtagActivity
- [ ] HashtagActivity displays posts with that tag
- [ ] Multiple hashtags work: "#coding #android #fun"

**Mentions**
- [ ] Create thread with mention: "Hey @username check this"
- [ ] Mention is blue/clickable
- [ ] Click mention opens ProfileActivity
- [ ] Multiple mentions work: "@user1 and @user2"

### Phase 1: Repost

- [ ] Click repost button → Dialog appears
- [ ] Dialog shows "Repost" and "Quote" options
- [ ] Click "Repost" → Count increases
- [ ] Repost again → Toggles off (count decreases)
- [ ] Repost state persists after refresh

### Phase 1: Share

- [ ] Click share button → Android share sheet opens
- [ ] Share to WhatsApp/Email/etc works
- [ ] Shared text is formatted correctly
- [ ] Can cancel share dialog

### Phase 2: Pagination

- [ ] Scroll to bottom → New threads load automatically
- [ ] Loading indicator appears while loading
- [ ] Each page loads 20 items
- [ ] No duplicate threads
- [ ] Smooth scrolling without lag

### Phase 2: Pull-to-Refresh

- [ ] Pull down on feed → SwipeRefreshLayout activates
- [ ] Loading spinner appears
- [ ] Feed refreshes with new content
- [ ] Pagination resets correctly

### Phase 3: Follow Requests

**Setup**: Have 2 test accounts (one private, one public)

**Send Request**
- [ ] Search for private user
- [ ] Click "Follow" → Button changes to "Requested"
- [ ] Request saved in Firebase under FollowRequests/{userId}
- [ ] Button stays "Requested" after leaving and returning

**View Requests**
- [ ] Navigate to Notifications tab
- [ ] "Requests" chip shows count badge: "Requests (2)"
- [ ] Click "Requests" → Opens FollowRequestsActivity
- [ ] List displays requester info (profile, name, username)

**Accept Request**
- [ ] Click "Accept" → Request removed from list
- [ ] User added to followers list
- [ ] Requester's following list updated
- [ ] Toast shows: "Follow request accepted"

**Reject Request**
- [ ] Get another request
- [ ] Click "Reject" → Request removed immediately
- [ ] User NOT added to followers
- [ ] Toast shows: "Follow request rejected"

**Public Accounts**
- [ ] Follow public account → Direct follow (no request)
- [ ] Button immediately shows "Following"
- [ ] No request created in Firebase

### Phase 3: Nested Replies

**Basic Reply**
- [ ] Open thread → See "Reply" button on comments
- [ ] Click "Reply" → Dialog opens
- [ ] Parent comment preview shows
- [ ] Type and post reply
- [ ] Reply appears under parent with 32dp indentation
- [ ] Toast: "Reply posted!"

**Nested Replies (Level 2)**
- [ ] Reply to a reply
- [ ] Post 2nd level reply
- [ ] Reply shows 64dp indentation (2 × 32dp)
- [ ] Visual hierarchy is clear

**Nested Replies (Level 3)**
- [ ] Reply to level-2 reply
- [ ] Post 3rd level reply
- [ ] Reply shows 96dp indentation (3 × 32dp)
- [ ] Maximum depth reached

**Max Depth Limit**
- [ ] Try replying to level-3 comment
- [ ] Toast: "Maximum nesting level reached"
- [ ] Dialog doesn't open

**Data Verification**
- [ ] Check Firebase: comments/{commentId}
- [ ] parentCommentId is set correctly
- [ ] depth field is correct (0, 1, 2, or 3)
- [ ] Parent's replyIds array updated

### Edge Cases

**App Stability**
- [ ] Rotate device → No crashes
- [ ] Background/foreground → Data persists
- [ ] Poor network → Graceful handling
- [ ] No internet → Appropriate message

**Data Integrity**
- [ ] Refresh preserves likes, reposts, follows
- [ ] Multiple users can interact simultaneously
- [ ] Large threads with many comments load correctly

## 🐛 Bug Report Template

If you find any issues during testing:

```
Feature: [e.g., Nested Replies]
Issue: [e.g., Reply button not showing]
Steps to Reproduce:
  1. [First step]
  2. [Second step]
  3. [etc.]
Expected: [What should happen]
Actual: [What actually happens]
Device: [e.g., Pixel 5, Android 12]
```

## 📁 Project Structure

```
app/
├── src/main/java/com/harsh/shah/threads/clone/
│   ├── activities/
│   │   ├── FollowRequestsActivity.java      # Manage follow requests
│   │   ├── HashtagActivity.java             # View hashtag posts
│   │   ├── ThreadViewActivity.java          # Thread detail with nested replies
│   │   └── ...
│   ├── fragments/
│   │   ├── ActivityNotificationFragment.java # Notifications with request badge
│   │   ├── HomeFragment.java                 # Feed with pagination
│   │   ├── SearchFragment.java               # Search with follow request logic
│   │   └── ...
│   ├── model/
│   │   ├── CommentsModel.java                # Comment with parent/child fields
│   │   ├── FollowRequestModel.java           # Follow request data
│   │   ├── ThreadModel.java                  # Thread with hashtags/mentions
│   │   └── ...
│   └── utils/
│       ├── TextFormatter.java                # Hashtag/mention parser
│       └── Utils.java
├── res/
│   └── layout/
│       ├── activity_follow_requests.xml      # Follow requests screen
│       ├── dialog_reply_comment.xml          # Reply dialog
│       ├── dialog_repost.xml                 # Repost dialog
│       └── ...
└── ...
```

## 🔑 Key Components

### TextFormatter
Parses and formats text with clickable hashtags and mentions using regular expressions and `ClickableSpan`.

### FollowRequestsActivity
Displays pending follow requests with accept/reject functionality and real-time Firebase updates.

### ThreadViewActivity
Shows thread details with nested comments, visual indentation, and reply functionality up to 3 levels deep.

### BaseActivity
Manages Firebase references including `mFollowRequestsDatabaseReference` for the follow request system.

## 🚀 Performance

- **Pagination**: Loads 20 threads per page using Firebase cursor-based queries
- **Efficient Queries**: Uses `orderByKey()`, `limitToLast()`, and `endBefore()` for optimal performance
- **Image Loading**: Picasso library with placeholder images and caching
- **Real-time Updates**: Firebase ValueEventListeners for live data synchronization

## 📊 Database Structure

### FollowRequests
```json
{
  "FollowRequests": {
    "targetUserId": {
      "requestId": {
        "requesterId": "xyz",
        "requesterUsername": "john_doe",
        "requesterName": "John Doe",
        "requesterProfileImage": "url",
        "targetUserId": "abc",
        "timestamp": "1234567890",
        "status": "pending"
      }
    }
  }
}
```

### Threads with Nested Comments
```json
{
  "Threads": {
    "threadId": {
      "comments": {
        "comment1": {
          "text": "Great post!",
          "parentCommentId": "",
          "depth": 0,
          "replyIds": ["comment2"]
        },
        "comment2": {
          "text": "Thanks!",
          "parentCommentId": "comment1",
          "depth": 1,
          "replyIds": []
        }
      }
    }
  }
}
```

## 🎨 UI/UX Features

- Material Design components
- Dark mode support
- Smooth animations
- Responsive layouts
- Intuitive navigation
- Clear visual hierarchy for nested replies

## 📝 Implementation Status

✅ **Phase 1**: Hashtags, Mentions, Reposts, Sharing - COMPLETE  
✅ **Phase 2**: Pagination, Pull-to-Refresh - COMPLETE  
✅ **Phase 3**: Follow Requests, Nested Replies - COMPLETE  

**Total**: ~1,500 lines of code added across 22 files

## 🔮 Future Enhancements

- Direct Messages
- Video support
- Advanced search
- Trending hashtags
- Stories/Reels
- Push notifications for follow requests
- Request expiry (auto-delete after 30 days)
- Bulk accept/reject requests

## 📄 License

[Add your license here]

## 👨‍💻 Author

[Add your name/info here]

## 🙏 Acknowledgments

- Firebase for backend infrastructure
- Material Design for UI components
- Picasso for image loading
- Android development community

---

**Ready for Testing!** Follow the checklist above to verify all features are working correctly. 🚀
