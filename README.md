# 🛠️ Mane-Kelsa
### Hyper-Local Work Directory for Self-Employment

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-0288D1?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-In%20Development-FFA000?style=for-the-badge)

> **MindMatrix VTU Internship Program — Android Development Project**  
> Connecting local workers and residents through a digital self-employment platform.

---

# 📖 About the Project

**Mane-Kelsa** is an Android application designed to help domestic workers, gardeners, cleaners, and local laborers find nearby work opportunities in small towns and rural areas.

In many local communities, workers depend only on “word of mouth” to get jobs. If their regular employer is unavailable, they often lose income because there is no centralized local job directory.

Mane-Kelsa solves this problem by creating a **Hyper-Local Work Directory** where workers can:

- Create professional profiles
- Set daily availability status
- Display skills and rates
- Receive direct calls from nearby residents
- Build trust through ratings and reviews

The application focuses on simplicity, accessibility, and localized usability for semi-literate users.

---

# ✨ Features

| Feature | Description |
|---|---|
| 👤 **Worker Profile Management** | Create worker profiles with photo, skills, and daily rates |
| 📍 **Nearby Worker Listing** | Residents can find available workers nearby |
| 📞 **Direct Calling Feature** | One-tap calling using Android Dial Intent |
| 🟢 **Availability Toggle** | Workers can set “Available Today” status instantly |
| ⭐ **Worker Ratings** | Residents can give ratings and feedback |
| 🌐 **Real-Time Availability Sync** | Availability updates instantly for all users |
| 📴 **Simple User Interface** | Large buttons and easy navigation for accessibility |
| 🇮🇳 **Kannada Localization** | User interface designed for regional language support |

---

# 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **Architecture** | MVVM (ViewModel + LiveData + Repository) |
| **UI Design** | XML Layouts, ConstraintLayout, Material Design |
| **Backend** | Firebase Realtime Database |
| **Authentication** | Firebase Authentication |
| **Real-Time Updates** | Firebase Realtime Sync |
| **Communication** | Intent API (`ACTION_DIAL`) |
| **Version Control** | Git & GitHub |
| **Development Tool** | Android Studio |

---

# 🗂️ Project Structure

```text
mane-kelsa/
├── app/
│   └── src/main/
│       ├── java/com/manekelsa/
│       │   ├── ui/
│       │   │   ├── auth/             # Login & registration screens
│       │   │   ├── worker/           # Worker profile screens
│       │   │   ├── home/             # Dashboard and worker listing
│       │   │   ├── availability/     # Availability toggle module
│       │   │   └── rating/           # Ratings and reviews
│       │   │
│       │   ├── viewmodel/            # MVVM ViewModels
│       │   ├── repository/           # Repository layer
│       │   ├── firebase/             # Firebase configuration
│       │   ├── models/               # Data models/entities
│       │   └── utils/                # Helper utilities
│       │
│       └── res/
│           ├── layout/               # XML layout files
│           ├── drawable/             # Icons and assets
│           └── values/               # Colors, strings, themes
│
├── screenshots/
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

# 📱 Application Screens

- **Login & Registration** — Worker authentication system
- **Home Dashboard** — View nearby available workers
- **Worker Profile Screen** — Display worker details and skills
- **Availability Toggle Screen** — Update “Available Today” status
- **Worker Listing Screen** — Browse workers by category
- **Call Worker Screen** — One-tap direct calling functionality
- **Ratings & Reviews** — View and submit worker ratings

---

# 📸 Screenshots

## Home Dashboard

<img src="screenshots/customer homepage.jpeg" width="300"/>

---

## Worker Profile

<img src="screenshots/worker homepage.jpeg" width="300"/>

---

## Availability Toggle

<img src="screenshots/worker profilepage.jpeg" width="300"/>

---

## Worker Listing

<img src="screenshots/payment page.jpeg" width="300"/>

---

# 🚀 Getting Started

## Prerequisites

- Android Studio Hedgehog or later
- Minimum SDK: API 26+
- Kotlin 1.9+
- Firebase Project Setup

---

## Installation

```bash
# Clone the repository
git clone https://github.com/your-username/mane-kelsa.git
```

---

## Open Project

1. Open Android Studio
2. Select **Open Existing Project**
3. Choose the `mane-kelsa` folder

---

## Firebase Configuration

1. Create Firebase project
2. Add Android application package
3. Download `google-services.json`
4. Place it inside:

```text
app/google-services.json
```

---

## Build & Run

1. Wait for Gradle sync to complete
2. Connect Android device or start emulator
3. Click **Run ▶️**

---

# 🧠 Core Functionalities

## Worker Profile Management

Workers can:

- Upload profile photos
- Add work skills
- Set daily service rates
- Update personal information

---

## Real-Time Availability

Workers can toggle:

```text
Available Today
```

status, which updates instantly using Firebase Realtime Database.

---

## Nearby Worker Discovery

Residents can browse available workers nearby based on categories and local area.

---

## One-Tap Calling

The application uses:

```kotlin
Intent.ACTION_DIAL
```

to directly call workers from the app.

---

## Worker Ratings

Residents can provide:

- Ratings
- Feedback
- Trust indicators

to improve service reliability.

---

# 🏗️ Architecture Overview

```text
UI Layer (Activities / Fragments)
        ↕
ViewModel (LiveData)
        ↕
Repository Layer
        ↕
Firebase Realtime Database
```

---

# 🎯 Internship Weekly Progress

| Week | Work Done |
|---|---|
| Week 1 | Android Studio setup, Kotlin basics, XML layouts |
| Week 2 | Firebase Authentication and Realtime Database setup |
| Week 3 | Worker profile module and UI implementation |
| Week 4 | Availability toggle and real-time synchronization |
| Week 5 | Calling feature and ratings module |
| Week 6 | UI improvements, testing, and documentation |

---

# 🌱 Impact Goals

- **Unorganized Sector Growth** — Digitizing local labor opportunities
- **Economic Security** — Helping workers receive steady local work
- **Urban-Rural Employment Connectivity** — Connecting residents with nearby workers
- **Digital Inclusion** — Supporting semi-literate users with simple UI design

---

# 📋 Success Criteria

- [x] Worker availability updates instantly
- [x] Nearby worker listing functions correctly
- [x] One-tap calling works successfully
- [x] Firebase sync works in real time
- [x] UI remains simple and accessible
- [x] Application supports Kannada localization

---

# 👨‍💻 Developer

**Akshan Kulal**  
B.E. Computer Science & Engineering  
Sir M Visvesvaraya Institute of Technology, Bengaluru  
MindMatrix VTU Internship Program — 2026

---

# 📄 License

This project is developed for academic and internship evaluation purposes.

---

# ⭐ GitHub Repository Checklist

- Complete source code uploaded
- Proper README documentation added
- Clean folder structure maintained
- Meaningful Git commit history created
- Screenshots included
- Firebase configuration documented
- Build-ready Android project uploaded

---

# 📬 Contact

For suggestions or collaboration:

- GitHub: your-github-profile
- Email: your-email@example.com

---

<p align="center">Made with ❤️ for local workers and self-employment opportunities</p>