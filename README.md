## Build Configuration

This project requires the Android SDK to be configured.

### Fixing "SDK location not found"

You need to tell Gradle where your Android SDK is located. You can do this in one of two ways:

1.  **Create `local.properties` file:**
    *   Rename `local.properties.example` to `local.properties`.
    *   Open `local.properties` and verify the `sdk.dir` path matches your Android SDK location.
    *   For Windows users, the default path is usually `C:\Users\<YourUsername>\AppData\Local\Android\Sdk`.

2.  **Set Environment Variable:**
    *   Set the `ANDROID_HOME` environment variable to your Android SDK location.
