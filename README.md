Based on the project files, here is a README for **NammaRaliuBuddy**.

---

# NammaRaliuBuddy

NammaRaliuBuddy is an Android application designed to provide live assistance and real-time updates for rail commuters. Built with Kotlin and Jetpack Compose, it integrates Firebase for live data and Google Maps for train tracking.

## Features

* **Live Station Info**: Real-time updates on platform numbers and automated coach positions.
* **Live Train Status**: Track train locations in an "IRCTC-style" interface using Google Maps and location services.
* **Live Pings & Alerts**: A real-time feed of platform notifications and automated status updates.
* **Smart Notifications**: Proximity-based alerts that notify you when your destination is within 5km, including the specific platform you need to stand on.
* **User Authentication**: Secure login and registration powered by Firebase Auth.

## Tech Stack

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose
* **Backend**: Firebase Realtime Database & Firebase Authentication
* **Maps & Location**: Google Maps Compose & Play Services Location
* **Navigation**: AndroidX Navigation Compose
* **Build System**: Gradle with Version Catalogs (`libs.versions.toml`)

## Project Structure

* `app/src/main/java/com/example/nammaraliubuddy/ui/`: Contains the UI screens including `HomeScreen`, `MapScreen`, `StationScreen`, and `PingScreen`.
* `app/src/main/java/com/example/nammaraliubuddy/ui/auth/`: Handles `LoginScreen` and `RegisterScreen`.
* `app/src/main/java/com/example/nammaraliubuddy/ui/theme/`: Defines the app's green and white color scheme and typography.

## Setup Requirements

* **Android Studio**: Ladybug or newer recommended.
* **JDK**: Version 21.
* **Google Services**: A valid `google-services.json` file must be placed in the `app/` directory to enable Firebase features.
* **API Key**: A Google Maps API key is required in the `AndroidManifest.xml` or via the Secrets Gradle Plugin.

## Permissions

The app requires the following permissions to function correctly:

* `INTERNET`: For Firebase and Map data.
* `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: For real-time train tracking.
* `ACCESS_BACKGROUND_LOCATION`: For destination arrival alerts while the app is in the background.
* `POST_NOTIFICATIONS`: To send live status pings.

## Getting Started

1. Clone the repository.
2. Add your `google-services.json` to the `app/` folder.
3. Sync the project with Gradle files.
4. Run the application on an emulator or physical device.
