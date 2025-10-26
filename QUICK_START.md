# Quick Start Guide - SocMedSS

Get your SocMedSS app running in 5 minutes! ⚡

---

## 🚀 Fast Setup (5 Steps)

### Step 1: Firebase Setup (2 minutes)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" → Name it "SocMedSS"
3. Add Android app:
   - Package name: `com.example.socmedss`
   - Download `google-services.json`
   - Place it in `app/` folder (replace existing one)

### Step 2: Enable Firebase Services (1 minute)

1. **Authentication**: Build → Authentication → Enable "Email/Password"
2. **Firestore**: Build → Firestore → "Create database" → Test mode
3. **Storage**: Build → Storage → "Get started" → Test mode

### Step 3: Sync Project (1 minute)

1. Open project in Android Studio
2. Click "Sync Project with Gradle Files"
3. Wait for sync to complete

### Step 4: Run App (1 minute)

1. Connect device or start emulator
2. Click Run button (▶️)
3. Wait for app to install

### Step 5: Test (30 seconds)

1. Register a new account
2. Create a post
3. Done! ✅

---

## 📱 First Time Using the App

### Create Your First Account

1. App opens to Login screen
2. Click "Create Account"
3. Enter username (min 3 characters)
4. Enter email (valid format)
5. Enter password (min 6 characters)
6. Click "Register"

### Create Your First Post

1. Tap "Create" in bottom navigation
2. Write something (required)
3. (Optional) Tap "Add Image" to attach a photo
4. Tap "Post"
5. Your post appears in Home feed!

### View Your Profile

1. Tap "Profile" in bottom navigation
2. See your info and posts
3. Tap "Logout" when done

---

## 🐛 Quick Troubleshooting

### App won't build?

- Make sure `google-services.json` is in `app/` folder
- Try "File → Invalidate Caches → Invalidate and Restart"

### Can't login/register?

- Check Firebase Authentication is enabled
- Check internet connection
- Look at Logcat for error messages

### Posts not showing?

- Check Firestore is created and in test mode
- Try creating a new post
- Check Logcat for errors

### Images not uploading?

- Check Storage is enabled in test mode
- Check internet connection
- Verify permissions in AndroidManifest.xml

---

## 📚 Need More Help?

- **Firebase Setup**: See [FIREBASE_SETUP.md](FIREBASE_SETUP.md)
- **Technical Details**: See [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md)
- **Full Overview**: See [README.md](README.md)

---

## 🎯 What's Next?

After basic setup works, you can:

- Customize colors in `res/values/colors.xml`
- Change app name in `res/values/strings.xml`
- Update app icon in `res/mipmap-*/`
- Add more features!

---

## 📞 Still Stuck?

1. Check **Logcat** in Android Studio (View → Tool Windows → Logcat)
2. Check **Firebase Console** for backend errors
3. Review the detailed guides in documentation files

---

**That's it! You're ready to go! 🎉**

