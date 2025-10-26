# SocMedSS - Mini Social Media App

## Project Overview

**SocMedSS** is a simplified social media Android application built with Kotlin and XML layouts. Users can register, login, create posts with optional images, and view posts from all users in real-time.

---

## Tech Stack

- **Language**: Kotlin
- **UI**: XML Layouts with ViewBinding
- **Backend**: Firebase
  - Authentication (Email/Password)
  - Cloud Firestore (Database)
  - Firebase Storage (Image Storage)
- **Architecture Components**:
  - RecyclerView with ListAdapter
  - Navigation Component
  - Material Design 3
  - Glide (Image Loading)

---

## Project Structure

```
app/src/main/
├── java/com/example/socmedss/
│   ├── adapter/
│   │   └── PostsAdapter.kt          # RecyclerView adapter for posts
│   ├── auth/
│   │   ├── LoginActivity.kt         # Login screen
│   │   └── RegisterActivity.kt      # Registration screen
│   ├── model/
│   │   ├── Post.kt                  # Post data model
│   │   └── User.kt                  # User data model
│   ├── ui/
│   │   ├── HomeFeedFragment.kt      # Home feed with all posts
│   │   ├── CreatePostFragment.kt    # Create new post
│   │   └── ProfileFragment.kt       # User profile & their posts
│   ├── MainActivity.kt              # Main container with bottom nav
│   └── PostDetailsActivity.kt       # Detailed post view
│
└── res/
    ├── layout/
    │   ├── activity_main.xml                # Main activity layout
    │   ├── activity_login.xml               # Login UI
    │   ├── activity_register.xml            # Register UI
    │   ├── activity_post_details.xml        # Post details UI
    │   ├── fragment_home_feed.xml           # Home feed UI
    │   ├── fragment_create_post.xml         # Create post UI
    │   ├── fragment_profile.xml             # Profile UI
    │   └── item_post.xml                    # Post card layout
    ├── menu/
    │   └── bottom_navigation_menu.xml       # Bottom nav items
    ├── navigation/
    │   └── nav_graph.xml                    # Navigation graph
    └── values/
        └── themes.xml                       # App theme & styles
```

---

## Features

### 1️⃣ **Login Screen** (`LoginActivity`) & **Register Screen** (`RegisterActivity`)

**Login Screen:**

- Firebase Email/Password Authentication
- Email and password input fields
- Form validation (email format, password length)
- Navigation button to Register screen
- Automatic login state persistence

**Register Screen:**

- New user registration with Firebase
- Username, email, and password input fields
- Form validation:
  - Email format validation
  - Password length (min 6 characters)
  - Username length (min 3 characters)
- Creates user profile in Firestore upon registration
- Toolbar with back button to return to Login
- Navigation to Login screen after successful registration

### 2️⃣ **Home Feed Screen** (`HomeFeedFragment`)

- **RecyclerView** displaying all posts from all users
- **Real-time updates** using Firestore listeners
- Posts ordered by timestamp (newest first)
- Material Design cards for each post
- Click on post to view details
- Empty state when no posts exist
- Loading indicator during data fetch

### 3️⃣ **Create Post Screen** (`CreatePostFragment`)

- Text input with multi-line support
- Optional image attachment
- Image picker integration
- Image preview with remove option
- Upload image to Firebase Storage
- Save post metadata to Firestore
- Form validation (requires text content)
- Success feedback and form clearing

### 4️⃣ **Profile Screen** (`ProfileFragment`)

- Display current user information:
  - Username
  - Email
  - Profile picture (circular)
- Show user's posts only (filtered by userId)
- Real-time updates for user posts
- Logout functionality with confirmation dialog
- Empty state for users with no posts
- Material Design card for profile info

### 5️⃣ **Post Details Screen** (`PostDetailsActivity`)

- Larger view of selected post
- Full-size image display
- Formatted timestamp:
  - "Just now" for recent posts
  - Relative time (e.g., "5 minutes ago")
  - Full date for older posts
- Back navigation to previous screen
- Material Design card layout

---

## Navigation Structure

The app uses **Jetpack Navigation Component** with a **Bottom Navigation Bar**:

```
MainActivity (Bottom Navigation)
├── Home Feed Fragment
├── Create Post Fragment
└── Profile Fragment
```

**Separate Activities**:

- `LoginActivity` (Entry point if not authenticated)
- `RegisterActivity` (New user registration)
- `PostDetailsActivity` (Opened when clicking a post)

---

## Firebase Data Structure

### Firestore Collections

#### `users` Collection

Stores user profile information.

```javascript
users/{userId}
├── userId: string       // Firebase Auth UID
├── username: string     // Display name
├── email: string        // User email
└── profileImage: string // Optional profile image URL
```

**Example Document**:

```json
{
  "userId": "abc123xyz",
  "username": "john_doe",
  "email": "john@example.com",
  "profileImage": null
}
```

#### `posts` Collection

Stores all user posts.

```javascript
posts/{postId}
├── userId: string       // Author's user ID
├── username: string     // Author's username
├── text: string         // Post content
├── imageUrl: string     // Optional image URL
└── timestamp: timestamp // Auto-generated by Firestore
```

**Example Document**:

```json
{
  "userId": "abc123xyz",
  "username": "john_doe",
  "text": "Beautiful sunset today! 🌅",
  "imageUrl": "https://storage.googleapis.com/...",
  "timestamp": "2024-01-15T18:30:00Z"
}
```

### Firebase Storage Structure

Images are stored in Firebase Storage:

```
posts/
└── {userId}/
    ├── {uuid1}.jpg
    ├── {uuid2}.jpg
    └── {uuid3}.jpg
```

---

## Key Components Explained

### PostsAdapter (RecyclerView Adapter)

**File**: `adapter/PostsAdapter.kt`

- Uses `ListAdapter` with `DiffUtil` for efficient updates
- ViewBinding for type-safe view access
- Glide for image loading with placeholder and error handling
- Timestamp formatting (relative and absolute)
- Click listener for navigating to post details

**Key Methods**:

- `bind(post: Post)`: Binds post data to views
- `formatTimestamp(timestamp: Date?)`: Converts Date to readable format

### ViewBinding

ViewBinding is enabled in all Activities and Fragments for type-safe view access:

```kotlin
// Example from CreatePostFragment
private var _binding: FragmentCreatePostBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
    _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
    return binding.root
}
```

### Material Design 3 Components Used

1. **MaterialToolbar** - App bars with elevation
2. **MaterialButton** - Primary and outlined buttons
3. **MaterialCardView** - Post cards and profile card
4. **TextInputLayout** - Form inputs with error states
5. **BottomNavigationView** - Bottom navigation bar
6. **ShapeableImageView** - Circular profile images
7. **FloatingActionButton** - Remove image button

---

## Gradle Dependencies

### Firebase

```kotlin
implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
implementation("com.google.firebase:firebase-analytics")
```

### AndroidX Libraries

```kotlin
implementation("androidx.core:core-ktx:1.13.1")
implementation("androidx.appcompat:appcompat:1.7.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("androidx.fragment:fragment-ktx:1.7.0")
implementation("androidx.cardview:cardview:1.0.0")
```

### Navigation

```kotlin
implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
```

### Material Design & Image Loading

```kotlin
implementation("com.google.android.material:material:1.12.0")
implementation("com.github.bumptech.glide:glide:4.16.0")
```

---

## Permissions Required

The app requires the following permissions (declared in `AndroidManifest.xml`):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

---

## Authentication Flow

```
1. App Launch
   ├── User logged in? → MainActivity (Home Feed)
   └── Not logged in? → LoginActivity

2. LoginActivity
   ├── User clicks "Login"
   │   ├── Validate email & password
   │   ├── Firebase signInWithEmailAndPassword()
   │   └── Success → MainActivity
   │
   └── User clicks "Create New Account"
       └── Navigate to RegisterActivity

3. RegisterActivity
   ├── User fills username, email, password
   ├── Validate all fields
   ├── Firebase createUserWithEmailAndPassword()
   ├── Create user document in Firestore
   ├── Success → MainActivity
   └── "Back to Login" button → LoginActivity

4. Logout
   ├── User clicks Logout in Profile
   ├── Confirmation dialog
   ├── Firebase signOut()
   └── Navigate to LoginActivity
```

---

## Image Upload Flow

```
1. User selects image in CreatePostFragment
   ├── Image picker opens
   ├── User selects image
   └── Preview shown in UI

2. User clicks "Post" button
   ├── Validate text content
   ├── Show loading indicator
   │
   ├── If image selected:
   │   ├── Generate unique filename: posts/{userId}/{uuid}.jpg
   │   ├── Upload to Firebase Storage
   │   ├── Get download URL
   │   └── Create post with imageUrl
   │
   └── If no image:
       └── Create post with null imageUrl

3. Post saved to Firestore
   ├── Real-time listeners trigger update
   ├── Post appears in Home Feed
   ├── Post appears in user's Profile
   └── Success message shown
```

---

## Real-Time Updates

The app uses Firestore **snapshot listeners** for real-time updates:

### Home Feed

```kotlin
firestore.collection("posts")
    .orderBy("timestamp", Query.Direction.DESCENDING)
    .addSnapshotListener { snapshot, error ->
        // Updates automatically when posts change
    }
```

### Profile Posts

```kotlin
firestore.collection("posts")
    .whereEqualTo("userId", userId)
    .orderBy("timestamp", Query.Direction.DESCENDING)
    .addSnapshotListener { snapshot, error ->
        // Updates automatically when user's posts change
    }
```

---

## Design Guidelines

### Material Design 3

- Uses Material You theme components
- Consistent color scheme with primary/secondary colors
- Proper elevation and shadows
- Rounded corners on cards (12dp radius)
- Icon buttons with proper sizing

### Layout Strategy

- **ConstraintLayout** for flexible layouts
- **ScrollView** for scrollable content
- **RecyclerView** for lists
- Proper padding and margins (8dp, 12dp, 16dp, 24dp)
- Responsive layouts that work on different screen sizes

### Typography

- Title: 36sp bold
- Username: 18sp-24sp bold
- Body text: 14sp-16sp
- Timestamps: 12sp secondary color

---

## Code Documentation

All code files include comprehensive documentation:

- **Class-level** KDoc comments explaining purpose
- **Method-level** comments for public/important methods
- **Parameter documentation** using `@param`
- **Return value documentation** using `@return`
- **Inline comments** for complex logic

---

## Testing the App

### Manual Testing Checklist

1. **Registration**

   - [ ] Create new account with valid credentials
   - [ ] Try invalid email format
   - [ ] Try short password (< 6 characters)
   - [ ] Try short username (< 3 characters)
   - [ ] Verify user created in Firebase Console

2. **Login**

   - [ ] Login with registered account
   - [ ] Try wrong password
   - [ ] Try non-existent email
   - [ ] Verify auto-login on app restart

3. **Create Post**

   - [ ] Create text-only post
   - [ ] Create post with image
   - [ ] Try posting without text (should fail)
   - [ ] Remove selected image
   - [ ] Verify post appears in feed

4. **Home Feed**

   - [ ] View all posts
   - [ ] Click on post to view details
   - [ ] Verify real-time updates (create post from another device)
   - [ ] Check empty state when no posts

5. **Profile**

   - [ ] View profile information
   - [ ] View user's posts only
   - [ ] Logout successfully
   - [ ] Verify logout confirmation dialog

6. **Post Details**
   - [ ] View post with image
   - [ ] View post without image
   - [ ] Check timestamp formatting
   - [ ] Back navigation works

---

## Future Enhancements

Potential features to add:

- Like/Comment functionality
- User profiles with editable information
- Follow/Unfollow users
- Image upload from camera
- Post deletion
- Search functionality
- Hashtags and mentions
- Push notifications
- Dark mode support
- Profile picture upload

---

## Troubleshooting

### Common Issues

**Issue**: App crashes on startup

- Check if `google-services.json` is properly configured
- Verify Firebase project is set up correctly
- Check Logcat for error messages

**Issue**: Images not loading

- Verify internet permission in manifest
- Check Firebase Storage rules
- Ensure Glide is properly configured

**Issue**: Posts not appearing

- Check Firestore rules
- Verify user is authenticated
- Check Logcat for Firestore errors

**Issue**: Navigation not working

- Verify navigation graph is properly set up
- Check fragment names in nav_graph.xml match actual class names

---

## Performance Considerations

1. **Glide Caching**: Images are cached automatically by Glide
2. **DiffUtil**: Efficient RecyclerView updates with ListAdapter
3. **ViewBinding**: Faster than findViewById
4. **Real-time Listeners**: Minimize listener scope to reduce bandwidth
5. **Image Compression**: Consider compressing images before upload

---

## Security Considerations

1. **Authentication**: Always check if user is logged in before showing content
2. **Firestore Rules**: Implement proper security rules in production
3. **Storage Rules**: Restrict file uploads to authenticated users only
4. **Input Validation**: Always validate user input before sending to Firebase
5. **API Keys**: Keep `google-services.json` private

---

## License

This is an educational project for learning Android development with Firebase.

---

## Contact & Support

For issues or questions:

- Check Firebase Console for backend errors
- Review Logcat for Android errors
- Consult Firebase documentation
- Check project comments and documentation

---

**Happy Coding!** 🚀
