# Erika Nail Art

An offline Android appointment manager designed for a nail salon. It keeps clients, services, appointments, tips, income, reminders, and backups in one private on-device app.

## Features

### Agenda

- Monthly calendar using the Spain time zone, with accurate month lengths and a compact `1 cita` / `N citas` count for each day.
- The current date is selected automatically when the app opens.
- Create and edit appointments with a client, date, AM/PM time, one or more services, per-appointment prices, tips, and service notes.
- Keep the appointment editor focused by choosing services from a dedicated selector; only the selected services and their per-appointment prices remain visible in the form.
- Appointment cards show the service total and tip separately, retain notes, and support pending, paid, and cancelled statuses.
- Tap an appointment to edit it; its card also offers WhatsApp reminder, payment, pending, cancellation, and deletion actions.
- The WhatsApp action opens an editable, client-specific reminder with the appointment date, time, services, and salon address already filled in.
- The app prevents appointments less than 15 minutes apart, without imposing artificial service durations.
- Search the appointments of the selected day by client name or service name.

### Clients and services

- Create, edit, search, deactivate, and reactivate clients while preserving their appointment history.
- Client search works by name or telephone number; the responsive two-column grid supports any number of client cards without truncating records.
- Create, edit, reorder, and remove services with a price and custom icon.
- Filter services by their icon and use the saved manual order across the app.
- Generate an editable service catalogue in that same order, then share it through the Android share sheet.

### Income

- Review paid appointments for today, a selected month, or a custom date range.
- Date-range validation prevents a start date after the end date and vice versa.
- Revenue from services and tips is always presented separately, along with the combined total.
- The detailed list preserves the client, date, services, amount, and optional tip for every paid appointment.

### Daily reminders

- A WorkManager reminder is scheduled daily at a configurable AM/PM hour.
- It reports the appointments for the following day, with correct singular/plural wording, or confirms that the next day is free.
- A test button sends a notification immediately and its temporary sending message clears automatically.
- Tapping a notification opens the Agenda directly on the current date.

### WhatsApp client reminders

- Review and edit every prefilled reminder before WhatsApp opens, then send it normally from the conversation.
- Local nine-digit Spanish phone numbers are prepared with the `+34` country code; international numbers keep their existing prefix.

### Backups and privacy

- Export all clients, services, appointments, statuses, prices, tips, and notes to a portable JSON backup.
- Importing a backup first displays its creation date and the number of clients, services, and appointments it contains.
- Restoration requires explicit confirmation because it replaces the local salon data.
- Settings display the date and time of the latest backup saved on the device.
- Backups preserve the manual service order and are compatible with legacy formats 1 and 2; service durations are intentionally not stored.

### Brand and accessibility

- Full-bleed adaptive Erika Nail Art launcher icon that fills Android's circular mask without a white inner border.
- Floral welcome screen aligned with the Erika Nail Art identity.
- Welcome screen includes the developer credit: “Developed by / Michael Moncada”.
- High-contrast, bold cursive typography and enlarged type scale across the interface.

## Data and privacy

The app works offline. Salon data is stored locally on the device. Use **Settings → Save backup** regularly and keep the generated JSON file in a safe place, such as your cloud storage, before changing or losing a phone.

Restoring a backup replaces the current clients, services, and appointments after a preview and explicit confirmation.

## Quality checks

The project includes JVM unit tests for appointment-gap validation, money parsing/formatting, and appointment total calculations. The app has also been manually verified on an Android emulator for client/service/appointment CRUD, income updates, backup export and restore, reminders, the share sheet, launcher icon, and welcome experience.

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
