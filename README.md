# 🧵 Campus Connect - An Android Application

A full-featured native Android social media application inspired by Meta's Threads. This project demonstrates a robust implementation of modern social networking features, real-time interactions, and a clean, responsive UI.

## ✨ Key Features

### 🗳️ Interactive Polls
- Create polls with up to 4 options.
- Real-time voting updates (powered by Firebase).
- Visual percentage indicators and single-vote enforcement.

### 🔗 Smart Link Previews
- Automatically detects URLs in posts (e.g., YouTube, Articles).
- Fetches and displays rich preview cards (Title, Description, Image).
- Optimized to hide previews if a user uploads a custom image.

### 💾 Saved Threads (Bookmarks)
- Bookmark threads for later reading.
- Dedicate "Saved" screen to manage shortlisted content.

### 📝 Core Social Features
- **Posting:** Create threads with Text, Images, and Links.
- **Feed:** Infinite scrolling home feed with pagination.
- **Interactions:** Like, Comment, and Repost threads.
- **Nested Replies:** Threaded comments system (Reddit-style) up to 3 levels deep.
- **Search:** Find users and explore content.
- **Profile:** Customizable user profiles with "Following" and "Followers" management.

### 🛠️ Performance & Optimization
- **Optimized Size:** Cleaned up unused video processing libraries (saved ~100MB).
- **Efficient Loading:** Uses `Picasso` for image caching and `Jsoup` for background metadata fetching.

---

## 📱 Tech Stack

**Language:** Java (Native Android)  
**Architecture:** MVC / MVVM Pattern  
**Minimum SDK:** API 24 (Android 7.0)  
**Compile SDK:** API 34 (Android 14)

### Backend (Firebase)
- **Firebase Authentication:** Secure Email/Password & Google Sign-In.
- **Realtime Database:** Storing users, threads, comments, and notifications.
- **Cloud Storage:** storing profile pictures and thread images.
- **FCM:** Infrastructure for push notifications.

### Libraries Used
- **Jsoup:** For parsing HTML to generate Link Previews.
- **Picasso / Glide:** For efficient image loading and caching.
- **SwipeRefreshLayout:** For "pull-to-refresh" functionality.
- **CircleImageView:** For rounded profile pictures.

---

## 📸 Screenshots

| Home Feed | Create Poll | Link Preview |
|:---:|:---:|:---:|
| ![Home](assets/home.png) | ![Poll](assets/poll.png) | ![Link](assets/link.png) |

*(Note: Add your screenshots to an `assets` folder in your repo)*

---

## 🚀 Setup & Installation

1.  **Clone the Repo**
    ```bash
    git clone https://github.com/shaawtymaker/Campus-Connect.git
    ```

2.  **Firebase Setup**
    *   Create a project on [Firebase Console](https://console.firebase.google.com/).
    *   Enable **Authentication** (Email/Password).
    *   Enable **Realtime Database** and set rules to `true` (for testing) or authorized users.
    *   Download `google-services.json` and place it in the `app/` folder.

3.  **Build**
    *   Open the project in **Android Studio**.
    *   Sync Gradle files.
    *   Run the app (`Shift + F10`).

---

## 🤝 Contribution

Contributions are welcome!
1.  Fork the project.
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the Branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

**Developed with ❤️ by Akanksh Adi Chandra**
