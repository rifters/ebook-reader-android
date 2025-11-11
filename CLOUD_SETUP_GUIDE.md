# Cloud Integration Setup Guide

This guide covers the setup required for all cloud storage providers in the EBook Reader app.

## Overview

The app now supports:
- **OAuth Flow**: Google Drive, Dropbox, OneDrive
- **Multiple Accounts**: Per provider account management
- **Offline Cache**: File listings cached locally
- **Background Downloads**: With progress notifications
- **Conflict Resolution**: Automatic file rename on collision
- **Token Encryption**: Android KeyStore for secure token storage
- **Additional Providers**: MEGA, pCloud, Yandex Disk, Box

---

## 1. Google Drive Setup

### Requirements
- Google Cloud Console project
- OAuth 2.0 Client ID for Android

### Steps
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Google Drive API**
4. Create **OAuth 2.0 Client ID** credentials
   - Type: Android
   - Package name: `com.rifters.ebookreader`
   - SHA-1 certificate fingerprint (get with `keytool -list -v -keystore ~/.android/debug.keystore`)
5. Download `google-services.json` (if using Firebase)
6. No code changes needed - uses existing `google-services.json`

### Testing
```kotlin
startActivityForResult(Intent(this, GoogleSignInActivity::class.java), REQUEST_CODE)
```

---

## 2. Dropbox Setup

### Requirements
- Dropbox App Console account
- App key

### Steps
1. Go to [Dropbox App Console](https://www.dropbox.com/developers/apps)
2. Create app
   - Choose: Scoped access
   - Access: Full Dropbox
   - Name your app
3. Copy the **App key**
4. Under **Permissions**, enable:
   - `files.metadata.read`
   - `files.content.read`
   - `files.content.write`
5. Update `DropboxOAuthActivity.kt`:
   ```kotlin
   private val APP_KEY = "YOUR_DROPBOX_APP_KEY"
   ```
6. Add to `AndroidManifest.xml` inside `<application>`:
   ```xml
   <activity
       android:name="com.dropbox.core.android.AuthActivity"
       android:configChanges="orientation|keyboard"
       android:launchMode="singleTask">
       <intent-filter>
           <data android:scheme="db-YOUR_APP_KEY" />
           <action android:name="android.intent.action.VIEW" />
           <category android:name="android.intent.category.BROWSABLE" />
           <category android:name="android.intent.category.DEFAULT" />
       </intent-filter>
   </activity>
   ```

---

## 3. OneDrive Setup

### Requirements
- Azure AD app registration
- MSAL configuration

### Steps
1. Go to [Azure Portal](https://portal.azure.com/)
2. Navigate to **Azure Active Directory** > **App registrations**
3. Click **New registration**
   - Name: EBook Reader
   - Supported account types: Personal Microsoft accounts and organizational
   - Redirect URI: Leave blank for now
4. Note the **Application (client) ID**
5. Go to **Authentication**
   - Add platform: Android
   - Package name: `com.rifters.ebookreader`
   - Signature hash: Run this command and paste result:
     ```bash
     keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
     ```
   - Default password: `android`
6. Copy the generated redirect URI (format: `msauth://com.rifters.ebookreader/HASH`)
7. Update `/app/src/main/res/raw/msal_config.json`:
   ```json
   {
     "client_id": "YOUR_CLIENT_ID_HERE",
     "redirect_uri": "msauth://com.rifters.ebookreader/YOUR_HASH"
   }
   ```
8. Under **API permissions**, add:
   - Microsoft Graph > Delegated > `Files.Read`
   - Microsoft Graph > Delegated > `Files.Read.All`

---

## 4. Box Setup

### Requirements
- Box Developer account
- Box Android SDK

### Steps
1. Go to [Box Developer Console](https://app.box.com/developers/console)
2. Create New App
   - Select: Custom App
   - Authentication: OAuth 2.0 with JWT or User Authentication
3. Note **Client ID** and **Client Secret**
4. Set **Redirect URI**: `https://app.box.com/`
5. Update `BoxProvider.kt` with your credentials:
   ```kotlin
   BoxConfig.CLIENT_ID = "your_client_id"
   BoxConfig.CLIENT_SECRET = "your_client_secret"
   ```
6. Implementation note: Current BoxProvider has commented-out SDK calls. Uncomment after adding credentials.

---

## 5. Yandex Disk Setup

### Requirements
- Yandex OAuth app

### Steps
1. Go to [Yandex OAuth](https://oauth.yandex.com/)
2. Register new application
3. Under **Platforms**, select **Mobile app**
4. Enable permission: **Access to Yandex.Disk**
5. Get OAuth token via browser flow or use WebView
6. Pass token to `YandexDiskProvider`

**Note**: Implementation is complete and functional.

---

## 6. MEGA Setup

### Requirements
- MEGA account
- REST API implementation (SDK requires native build)

### Steps
1. MEGA API uses username/password or session ID
2. Current implementation uses REST API approach
3. For production:
   - Implement login via MEGA API: `https://g.api.mega.co.nz/cs`
   - Store session ID as token
4. Alternative: Use official MEGA SDK (requires CMake and NDK setup)

**Note**: Skeleton implementation provided. Full REST API integration needed.

---

## 7. pCloud Setup

### Requirements
- pCloud account
- REST API

### Steps
1. pCloud uses OAuth 2.0
2. Register app at [pCloud Developers](https://docs.pcloud.com/)
3. Implement OAuth flow similar to Dropbox
4. Use REST API for file operations

**Note**: Skeleton implementation provided. REST API calls needed.

---

## Additional Configuration

### String Resources

Add to `app/src/main/res/values/strings.xml` (already added):
```xml
<string name="google_sign_in">Google Sign-In</string>
<string name="dropbox_sign_in">Dropbox Sign-In</string>
<string name="onedrive_sign_in">OneDrive Sign-In</string>
```

### Permissions

Already configured in `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Notification Channel

The `DownloadNotificationHelper` automatically creates a notification channel for download progress.

---

## Testing

### Test Multi-Account Flow
1. Open **CloudBrowserActivity**
2. Click "Manage" button
3. Add multiple accounts (e.g., 2 Google Drive accounts)
4. Switch between accounts using the spinner
5. Browse files from each account

### Test Downloads
1. Navigate to a file in cloud storage
2. Tap to download
3. Check notification shade for progress
4. Downloaded file appears in library automatically

### Test Token Encryption
```kotlin
val encrypted = TokenEncryption.encrypt("my-secret-token")
val decrypted = TokenEncryption.decrypt(encrypted)
assert(decrypted == "my-secret-token")
```

---

## Security Notes

1. **Token Encryption**: All tokens stored using Android KeyStore (AES-256-GCM)
2. **API Keys**: Never commit API keys to version control
3. **ProGuard**: Ensure crypto classes are not obfuscated
4. **KeyStore Fallback**: On API < 23, tokens stored unencrypted (with warning log)

---

## Troubleshooting

### Google Sign-In Not Working
- Verify SHA-1 fingerprint matches in Google Console
- Check `google-services.json` is in `app/` folder
- Ensure Google Play Services is installed on device

### Dropbox Auth Fails
- Verify App Key in `DropboxOAuthActivity.kt`
- Check `db-YOUR_APP_KEY` scheme in manifest matches app key
- Ensure Dropbox app is installed (or browser flow works)

### OneDrive Auth Fails
- Verify `msal_config.json` client ID is correct
- Check redirect URI hash matches your keystore
- Ensure signature hash is generated correctly

### Downloads Not Showing
- Check WorkManager is initialized
- Verify notification permissions (Android 13+)
- Check `DownloadNotificationHelper` channel creation

---

## Architecture Summary

```
CloudBrowserActivity
  └── CloudStorageViewModel
        └── CloudStorageManager
              ├── GoogleDriveProvider
              ├── DropboxProvider
              ├── OneDriveProvider
              ├── BoxProvider
              ├── YandexDiskProvider
              ├── MegaProvider
              └── PCloudProvider

CloudAccountManager (encrypted storage)
  └── TokenEncryption (Android KeyStore)

DownloadWorker (background downloads)
  ├── DownloadNotificationHelper
  ├── ConflictResolver
  └── AutoImportService
```

---

## Next Steps

1. **Complete MEGA/pCloud providers**: Implement REST API calls
2. **Add upload UI**: Allow uploading books to cloud
3. **Sync settings**: Add ability to auto-sync on WiFi
4. **Folder creation**: UI for creating folders in cloud
5. **Share functionality**: Share books between cloud accounts
