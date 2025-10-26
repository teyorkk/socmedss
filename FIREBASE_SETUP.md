# Firebase Setup Instructions for SocMedSS

This document provides step-by-step instructions to configure Firebase for the SocMedSS Android application.

## Prerequisites

- Android Studio (latest version)
- Google account
- Active internet connection

---

## Step 1: Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or **"Create a project"**
3. Enter your project name: **"SocMedSS"** (or any name you prefer)
4. (Optional) Enable Google Analytics if you want usage tracking
5. Click **"Create project"** and wait for setup to complete

---

## Step 2: Register Your Android App

1. In the Firebase Console, click on the **Android icon** to add an Android app
2. Enter the following details:

   - **Android package name**: `com.example.socmedss`
   - **App nickname** (optional): SocMedSS
   - **Debug signing certificate SHA-1** (optional but recommended for future features)

3. Click **"Register app"**

---

## Step 3: Download google-services.json

1. After registering, Firebase will provide a `google-services.json` file
2. Download this file
3. Place it in your project's **`app/`** directory:
   ```
   socmedss/
   └── app/
       ├── google-services.json  ← Place here
       ├── build.gradle.kts
       └── src/
   ```

**Note**: The project already has a placeholder `google-services.json`. Replace it with your own downloaded file.

---

## Step 4: Enable Firebase Authentication

1. In the Firebase Console, navigate to **Build** → **Authentication**
2. Click **"Get started"**
3. Go to the **"Sign-in method"** tab
4. Enable **"Email/Password"** provider:
   - Click on "Email/Password"
   - Toggle the **Enable** switch
   - Click **"Save"**

---

## Step 5: Set Up Cloud Firestore

1. In the Firebase Console, navigate to **Build** → **Firestore Database**
2. Click **"Create database"**
3. Select **"Start in test mode"** for development (you can add security rules later)
4. Choose a Firestore location (select the region closest to your users)
5. Click **"Enable"**

### Firestore Security Rules (Optional - for Production)

For production, update your Firestore rules to:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can read any user but only write their own
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Posts collection - authenticated users can read all, but only write their own
    match /posts/{postId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null &&
                              request.auth.uid == resource.data.userId;
    }
  }
}
```

---

## Step 6: Set Up Firebase Storage

1. In the Firebase Console, navigate to **Build** → **Storage**
2. Click **"Get started"**
3. Select **"Start in test mode"** for development
4. Click **"Next"** and then **"Done"**

### Storage Security Rules (Optional - for Production)

For production, update your Storage rules to:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /posts/{userId}/{allPaths=**} {
      // Allow read to all authenticated users
      allow read: if request.auth != null;
      // Allow write only if the user is uploading to their own folder
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## Step 7: Verify Setup in Android Studio

1. Open the project in Android Studio
2. Sync Gradle files (File → Sync Project with Gradle Files)
3. Verify that no errors appear in the build output
4. Check that the following are properly configured:
   - `google-services.json` is in the `app/` directory
   - Firebase dependencies are in `app/build.gradle.kts`
   - Google Services plugin is applied

---

## Step 8: Test the Application

1. Run the app on an emulator or physical device
2. Try registering a new account
3. Verify in Firebase Console that:
   - User appears in **Authentication** → **Users**
   - User document created in **Firestore** → **users** collection
4. Try creating a post with an image
5. Verify in Firebase Console that:
   - Post document created in **Firestore** → **posts** collection
   - Image uploaded to **Storage** → **posts/{userId}/**

---

## Firestore Data Structure

The app uses the following Firestore structure:

### Users Collection

```
users/
  └── {userId}
       ├── userId: string
       ├── username: string
       ├── email: string
       └── profileImage: string (optional)
```

### Posts Collection

```
posts/
  └── {postId}
       ├── userId: string
       ├── username: string
       ├── text: string
       ├── imageUrl: string (optional)
       └── timestamp: timestamp
```

---

## Troubleshooting

### Build Errors

**Error**: "File google-services.json is missing"

- **Solution**: Ensure `google-services.json` is placed in the `app/` directory

**Error**: "Plugin with id 'com.google.gms.google-services' not found"

- **Solution**: Verify the plugin is added in the root `build.gradle.kts`

### Authentication Errors

**Error**: "We have blocked all requests from this device due to unusual activity"

- **Solution**: This is a Firebase safety feature. Try again later or enable app verification.

**Error**: "The email address is already in use by another account"

- **Solution**: Use a different email or delete the existing user from Firebase Console

### Firestore Errors

**Error**: "PERMISSION_DENIED: Missing or insufficient permissions"

- **Solution**: Ensure Firestore is in test mode or check your security rules

### Storage Errors

**Error**: "User does not have permission to access this object"

- **Solution**: Ensure Storage is in test mode or check your security rules

---

## Additional Resources

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Authentication](https://firebase.google.com/docs/auth/android/start)
- [Cloud Firestore](https://firebase.google.com/docs/firestore/quickstart)
- [Firebase Storage](https://firebase.google.com/docs/storage/android/start)

---

## Security Best Practices

1. **Never commit** `google-services.json` to public repositories
2. Add `google-services.json` to `.gitignore` if needed
3. Use **Firebase Security Rules** for production
4. Enable **App Check** for additional security
5. Regularly review **Authentication** and **Firestore** usage in Firebase Console
6. Set up **budget alerts** in Google Cloud Console to avoid unexpected charges

---

## Support

If you encounter issues:

1. Check the [Firebase Documentation](https://firebase.google.com/docs)
2. Review Logcat output in Android Studio
3. Check Firebase Console for error messages
4. Verify all Firebase services are enabled

---

**Setup Complete!** 🎉

Your SocMedSS app is now configured with Firebase and ready to use.

