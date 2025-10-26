# ImgBB API Setup Instructions

This app uses **ImgBB** for free image hosting instead of Firebase Storage (no credit card required!).

## 📋 Setup Steps

### 1. Get Your ImgBB API Key

1. Go to **https://api.imgbb.com/**
2. Click **"Get API Key"**
3. Sign up with your email or Google account (FREE!)
4. Copy your API key from the dashboard

### 2. Add API Key to Your Project

1. Open `local.properties` file in your project root
2. Add this line at the bottom:
   ```properties
   IMGBB_API_KEY=your_actual_api_key_here
   ```
3. Replace `your_actual_api_key_here` with your actual ImgBB API key
4. Save the file

### 3. Build and Run

```bash
./gradlew assembleDebug
```

That's it! Your app will now upload images to ImgBB.

---

## 🔒 Security Notes

- ✅ `local.properties` is **automatically ignored by Git**
- ✅ Your API key is **never committed** to version control
- ✅ The API key is loaded at build time into `BuildConfig`
- ✅ **ImgBB is completely FREE** (no credit card needed)

## 📊 ImgBB Free Tier Limits

- ✅ Unlimited uploads
- ✅ Up to 32 MB per image
- ✅ API rate limit: ~100 requests per hour

## 🐛 Troubleshooting

### "API_KEY is empty" Error

- Make sure you added `IMGBB_API_KEY=your_key` to `local.properties`
- Make sure there are no spaces around the `=` sign
- Rebuild the project: `./gradlew clean assembleDebug`

### "Upload failed" Error

- Check your internet connection
- Verify your API key is correct at https://api.imgbb.com/
- Make sure the image file size is under 32 MB

---

## 🔄 Sharing Your Project

When sharing your project with teammates:

1. **Don't commit** `local.properties` (it's already .gitignore'd)
2. Share these instructions with your team
3. Each person needs to get their own ImgBB API key
4. Each person adds their key to their own `local.properties`

---

## ✨ Why ImgBB?

- ✅ **100% FREE** (no credit card)
- ✅ Fast and reliable CDN
- ✅ Permanent image hosting
- ✅ No Firebase billing worries
- ✅ Simple REST API

---

**Need help?** Check https://api.imgbb.com/
