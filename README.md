# MedTrack 

An elderly-friendly medication reminder app designed to help older adults manage their medications with simplicity and reliability.

## Overview

MedTrack is an Android application built with a focus on accessibility and ease of use for elderly individuals who need help remembering to take their medications on time. The app features a clean, minimal interface with large text, high contrast, and intuitive interactions.

## Features

### Core Functionality

- **Simple Medication Management**: Add, edit, and delete medications with an easy-to-use interface
- **Flexible Scheduling**: Set up medications to be taken:
  - Daily at specific times
  - Every X hours
  - On selected days of the week
- **Meal Timing Options**: Specify if medicine should be taken before, after, or with food
- **Reliable Reminders**: 
  - Pre-alarm notification 10 minutes before scheduled time
  - Main alarm at exact time
  - Persistent notifications until marked as taken
- **Quick Confirmation**: Large "Taken" button for easy medication logging
- **Daily Overview**: Clear home screen showing today's medications with color-coded statuses:
  - 🟢 Green: Taken
  - 🟡 Yellow: Due soon
  - 🔴 Red: Missed
  - 🔵 Blue: Scheduled for later
- **Medication History**: Complete log of all medication intake for doctor visits
- **Offline-First**: Works completely offline with local SQLite database

### Accessibility Features

- Large, readable typography (18-20sp minimum)
- High-contrast color scheme
- Touch targets ≥48dp for easy tapping
- Haptic feedback on button presses
- Optional voice announcements
- Icons always accompanied by text labels

## Technical Stack

- **Platform**: Android
- **Language**: Kotlin
- **IDE**: Android Studio
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: SQLite with Room
- **Background Tasks**: WorkManager
- **Alarms**: AlarmManager with exact-time delivery

## Database Schema

### Tables

**medications**
- Medicine details, dosage, frequency, and scheduling information

**medication_times**
- Multiple time slots for each medication

**intake_logs**
- Complete history of medication intake (taken, missed, or skipped)

## Installation

1. Clone the repository
2. Open the project in Android Studio
3. Build and run on an Android device or emulator (API level [specify minimum SDK])

```bash
git clone [repository-url]
cd medtrack
```

## Architecture

The app follows clean architecture principles:

- **MVVM Pattern**: Separation of UI, business logic, and data layers
- **Repository Pattern**: Single source of truth for data access
- **Room Database**: Type-safe database access with compile-time verification
- **AlarmManager**: Ensures reliable alarm delivery even when app is closed or device restarts

## Key Design Principles

1. **Ultra-Minimal UI**: Only essential actions visible, no clutter
2. **Predictable Interactions**: One primary action per screen
3. **Elder-Friendly**: Large buttons, clear labels, plenty of whitespace
4. **Accessibility-First**: Built with accessibility as a core requirement
5. **Reliability**: Alarms work offline and survive device restarts

## User Flows

### Adding a Medication
1. Tap "Add Medicine"
2. Enter medicine name and dosage
3. Select meal timing (before/after/with food)
4. Choose schedule type and times
5. Save - alarms are automatically scheduled

### Taking Medication
1. Receive notification/alarm at scheduled time
2. Open app or tap notification
3. Press large "Taken" button
4. Medication is logged and alarm stops

### Viewing History
- Access medication history to see complete intake logs
- Useful for doctor appointments and caregiver monitoring

## Edge Cases Handled

- ✅ Device restarts - alarms automatically reload
- ✅ Timezone changes - schedules recalculate
- ✅ End dates - medications automatically archive
- ✅ Disabled notifications - warning displayed to user
- ✅ Low battery - reminder shown when battery < 20%

## Future Enhancements

- **Cloud Sync**: Optional backup and sync with Supabase
- **Caregiver Portal**: Web dashboard for family members to monitor medication adherence
- **Smart Features**: 
  - AI-based adherence insights
  - Pill recognition via camera
  - Automatic refill reminders

## Target Users

- **Primary**: Elderly individuals (60-90+) with limited smartphone literacy
- **Secondary** (future): Caregivers, family members, healthcare providers

## Success Metrics

- 95% successful alarm deliveries
- 80%+ medication confirmations recorded
- < 1 minute to complete "Add Medicine" flow
- 100% offline functionality


## Support

For issues or questions, please contact [khansayeem03@gmail.com]

---

**Dedicated to my mother**
