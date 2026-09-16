# Erika Nail Art

An offline Android appointment manager designed for a nail salon. It keeps clients, services, appointments, tips, income, reminders, and backups in one private on-device app.

## Highlights

- Monthly calendar with Spain time zone support and a clear daily appointment count.
- Appointment creation and editing with AM/PM time selection, service-specific prices, tips, notes, and statuses.
- A 15-minute minimum gap between appointments while allowing flexible service timing.
- Active and inactive client lists, so client history is retained without deleting it.
- Service catalogue with custom icons and a shareable price list for prospective clients.
- Income dashboard for today, a selected month, or a custom date range; service revenue and tips are displayed separately.
- Daily next-day reminder. Tapping its notification opens the Agenda screen directly.
- Configurable reminder time and a button to send a test notification.
- JSON backup export and restore for safely moving the salon data to another device.
- Erika Nail Art branding, custom launcher icon, and a welcome screen credited to Michael Moncada.

## Data and privacy

The app works offline. Salon data is stored locally on the device. Use **Settings → Save backup** regularly and keep the generated JSON file in a safe place, such as your cloud storage, before changing or losing a phone.

Restoring a backup replaces the current clients, services, and appointments after confirmation.

## Run locally

1. Open this folder in Android Studio.
2. Use JDK 17 and Android SDK 35.
3. Let Gradle sync, then run the `app` configuration on an Android 8 (API 26) or newer device/emulator.

Alternatively, on Windows:

```powershell
.\gradlew.bat installDebug
```

## Technology

- Kotlin and Jetpack Compose
- Room for local persistence and database migrations
- Hilt for dependency injection
- WorkManager for daily reminders
- Material 3 UI
