# Hotspot Pulse

A colourful Android companion for your phone's personal hotspot (Kotlin + Jetpack Compose).

## Features

| | |
|---|---|
| **Internet check before sharing** | Tapping the power orb first checks that mobile data **or** Wi-Fi is available. If both are off, the start is refused with an error dialog (`NoUpstreamException`) and a one-tap shortcut to turn the internet on. |
| **One-tap hotspot** | After the check passes, the app opens your phone's hotspot switch directly. |
| **Live session meter** | Data shared, live speed, session length, animated gauge. |
| **Data budget + forecast** | Set a session cap (250 MB – 10 GB). The app predicts *when* you'll hit it at the current speed, warns at a percentage you choose, and alerts at 100%. |
| **Cost meter + fair split** | Enter your plan's price per GB to see what a session costs, then split the data and cost between friends. |
| **Battery guardian** | Alerts you when the battery drops below your threshold while sharing unplugged. |
| **Forget-me-not reminder** | Repeating "your hotspot is still on" reminders. |
| **Instant join QR** | Friends scan a Wi-Fi QR code with their camera to join. No typing needed. |
| **Session history** | Bar chart and list of past sessions with totals. |
| **Quick Settings tile + live notification** | Usage at a glance from anywhere. |

## Platform limits (honest notes)

- **Android only.** iOS doesn't let any third-party app control or monitor Personal Hotspot.
- **Turning tethering on/off:** Android (8+) reserves this for the system, so the app opens the system hotspot switch for the final tap.
- **Per-device data limits / blocking:** not possible for regular apps. Many phones (Samsung, Xiaomi, OnePlus, Pixel…) offer a connected-devices list, data limit or blocklist in their own hotspot settings. The Guard tab links straight to that screen.
- **Usage counting:** measured from the phone's mobile-data counters while the hotspot is on, so it also includes this phone's own traffic during the session.
- **Detection:** the hotspot is detected from its network interface. If a phone hides this, use *"Hotspot already on? Track this session"*.

## Build

- **Android Studio:** open the `hotspot-pulse` folder and run it.
- **Command line:** run `./gradlew assembleDebug`. The APK lands in `app/build/outputs/apk/debug/`.
- **GitHub Actions:** every push runs the unit tests and uploads `hotspot-pulse-apk` as a downloadable artifact.

Requires Android 8.0+ (API 26).
