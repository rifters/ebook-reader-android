# Google Services Configuration

This directory contains `google-services.json` which is required for Firebase integration.

## For Building/Testing

A placeholder `google-services.json` file is included in the repository to allow the app to build without requiring Firebase setup. This file contains dummy values and will not enable actual sync functionality.

## For Production/Development with Real Sync

To enable actual cloud sync functionality:

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app with package name: `com.rifters.ebookreader`
3. Download your actual `google-services.json` file
4. Replace the placeholder file in this directory
5. Enable Firestore Database and Authentication (Anonymous) in your Firebase Console

**Important**: Never commit your real `google-services.json` file with actual credentials. The `.gitignore` file is configured to prevent this, but be careful when making changes.

## Security Notes

- The placeholder file contains only dummy/invalid credentials
- Real credentials should be kept secure and not committed to version control
- Each developer/deployment should use their own Firebase project
- Use Firebase security rules to protect your data in production
