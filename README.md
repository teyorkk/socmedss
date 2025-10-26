# 📱 SocMedss - Mini Social Media App

A modern Android social media application built with Kotlin, XML layouts, and Firebase services.

## ✨ Features

- **User Authentication** - Register and login with email/password using Firebase Auth
- **Create Posts** - Share text updates with optional images
- **Image Uploads** - Upload images using ImgBB (free, no credit card required!)
- **Comments** - Comment on any post and engage with others
- **Edit & Delete** - Full control over your posts and comments
- **Profile Management** - Customizable profile picture that syncs across all posts
- **Real-time Updates** - See new posts and comments instantly
- **Smart Navigation** - Auto-redirect to home feed after posting
- **Optimized Loading** - Fast image loading with disk caching

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: XML Layouts with Material Design 3
- **Architecture**: MVVM pattern with ViewBinding
- **Navigation**: Jetpack Navigation Component
- **Backend**: Firebase Firestore (database), Firebase Auth (authentication)
- **Image Storage**: ImgBB API (free tier)
- **Image Loading**: Glide with disk caching
- **Async Operations**: Kotlin Coroutines

## 📋 Prerequisites

- Android Studio Hedgehog or later
- Android SDK API 24+ (Android 7.0+)
- Firebase project setup
- ImgBB API key (free from [api.imgbb.com](https://api.imgbb.com/))

## 🚀 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/socmedss.git
cd socmedss
```

### 2. Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
3. Download `google-services.json` and place it in the `app/` directory
4. Enable **Email/Password Authentication** in Firebase Console
5. Create a **Firestore Database** in test mode

#### Firestore Security Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 3. ImgBB API Setup

1. Get your free API key from [api.imgbb.com](https://api.imgbb.com/)
2. Open `local.properties` (create if it doesn't exist)
3. Add your API key:

```properties
IMGBB_API_KEY=your_api_key_here
```

⚠️ **Note**: `local.properties` is ignored by git for security. Each developer needs their own API key.

### 4. Build and Run

```bash
./gradlew assembleDebug
```

Or click **Run** ▶️ in Android Studio

## 📱 App Structure

### Screens

1. **Login/Register** - User authentication
2. **Home Feed** - View all posts with real-time updates
3. **Create Post** - Share new posts with optional images
4. **Profile** - View profile, posts, and manage account
5. **Post Details** - View post with comments

### Firebase Data Structure

```
users/
  └── {userId}/
      ├── username: string
      ├── email: string
      └── profileImage: string (ImgBB URL)

posts/
  └── {postId}/
      ├── userId: string
      ├── username: string
      ├── profileImageUrl: string
      ├── text: string
      ├── imageUrl: string (ImgBB URL)
      ├── timestamp: timestamp
      └── comments/ (subcollection)
          └── {commentId}/
              ├── userId: string
              ├── username: string
              ├── profileImageUrl: string
              ├── text: string
              └── timestamp: timestamp
```

## 🎨 Key Features Explained

### Profile Picture Sync
When you update your profile picture, it automatically updates across:
- All your existing posts
- All your existing comments
- Uses Firestore batch operations for efficiency

### Smart Deletion
Deleting a post automatically:
- Removes all associated comments
- Uses batch operations for performance
- Provides clear user feedback

### Image Optimization
- Images compressed to 800x800px max
- JPEG quality: 85%
- Disk caching with Glide
- Faster subsequent loads

### Real-time Updates
- Posts appear instantly without refresh
- Comments update in real-time
- Firestore snapshot listeners

## 📦 Dependencies

```kotlin
// Firebase
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")

// UI
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.navigation:navigation-fragment-ktx")
implementation("androidx.navigation:navigation-ui-ktx")

// Image Loading
implementation("com.github.bumptech.glide:glide:4.16.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
```

## 📸 Screenshots

<!-- Add screenshots here -->

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 👨‍💻 Author

**Your Name**
- GitHub: [@YOUR_USERNAME](https://github.com/YOUR_USERNAME)

## 🙏 Acknowledgments

- Firebase for backend services
- ImgBB for free image hosting
- Material Design for UI components
- Glide for efficient image loading

## 📝 Project Status

✅ **Active Development**

### Recent Updates:
- ✅ Auto-navigation to home feed after posting
- ✅ Optimized loading indicators
- ✅ Cascade delete for posts and comments
- ✅ Profile picture sync across app
- ✅ Improved image caching

## 🐛 Known Issues

None at the moment! 🎉

## 📞 Support

If you have any questions or issues, please open an issue on GitHub.

---

**Built with ❤️ using Kotlin and Firebase**
