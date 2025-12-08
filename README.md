# Expense Tracker Android

Welcome to the Expense Tracker Android app! This project is designed to help users keep track of their daily expenses with ease. The app allows users to add, view, and analyze their expenses using intuitive stats and charts. It is developed using modern Android development practices, including Jetpack Compose, Room Database, Dagger Hilt for dependency injection, and the MVVM architecture.

## Features

- **Add Expense:** Easily add your daily expenses with a few taps.
- **Track Expenses:** View a list of all your expenses, organized by date and category.
- **Analyze with Stats:** Get insights into your spending habits with detailed stats and charts.
  
## Technologies Used

- **Jetpack Compose:** A modern toolkit for building native Android UI.
- **Room Database:** A robust database layer on top of SQLite for managing local data.
- **Dagger Hilt:** A dependency injection library for Android that reduces the boilerplate of manual dependency injection.
- **MVVM Architecture:** Model-View-ViewModel architecture for separating the UI, business logic, and data handling in the app.

## YouTube Tutorial Series

This project is part of a series of tutorials available on my YouTube channel. Follow along with the videos to build this app from scratch!

1. **[Part 1: Project Setup and Basics](https://youtu.be/LfHkAUzup5E)**
2. **[Part 2: Implementing Room Database](https://youtu.be/dPeSoNWVu-Y)**
3. **[Part 3: Adding and Displaying Expenses](https://youtu.be/mq8lekRbF4I)**
4. **[Part 4: Tracking Expenses with Stats](https://youtu.be/xolI_2svC6w)**

Be sure to check out the videos for a detailed guide on how to implement each feature.

## Screenshots

Here are some screenshots of the Expense Tracker app in action:

| Home Screen | Add Expense | Stats |
|-------------|-------------|-------------|
| ![Home Screen](screenshots/Screenshot_1724273822.png) | ![Add Expense](screenshots/Screenshot_1724273829.png) | ![Stats](screenshots/Screenshot_1724273956.png) |

## Getting Started

### Prerequisites

- Android Studio Bumblebee or later
- Java 11 or later
- Android SDK 21 or later

### Installation

1. Clone the repository:

    ```bash
    git clone https://github.com/yourusername/ExpenseTrackerAndroid.git
    ```

2. Open the project in Android Studio.

3. Sync the project with Gradle files.

4. Run the app on an emulator or physical device.

### Usage

- **Adding an Expense:**
  - Tap on the "Add Expense" button.
  - Enter the amount, select a category, and add any notes if necessary.
  - Save the expense to track it.

- **Viewing Expenses:**
  - Navigate to the "Expense List" screen to view all your recorded expenses.
  - Tap on any expense to edit or delete it.

- **Tracking with Stats:**
  - Go to the "Stats" section to view charts and summaries of your spending habits over time.

## Plaid Integration (optional)

This project includes a demo Plaid Link integration that is opt-in. To enable the Plaid SDK and build the app with Plaid Link, do the following:

1. In the project root `gradle.properties`, add:

```
plaidSdkVersion=3.16.0
```

(Replace `3.16.0` with the exact stable version recommended by Plaid docs if needed.)

2. Configure a demo API key and backend:

- Local testing: copy `filesForServer/.env.example` to `filesForServer/.env` and set `PLAID_CLIENT_ID`, `PLAID_SECRET`, `PLAID_ENV=sandbox`, and `DEMO_API_KEY`.
- Replit: set those same variables in Replit Secrets.

3. Re-sync Gradle in Android Studio and rebuild the project.

When `plaidSdkVersion` is set, the app will add the Plaid Link SDK dependency and `PlaidLinkActivity` can open Plaid Link using your backend.

## Contributions

Contributions are welcome! Feel free to open issues or submit pull requests to help improve this project.

## Contact

For any queries or suggestions, feel free to contact me at [furqanullah717@gmail.com](mailto:furqanullah717@gmail.com).

---

**YouTube Channel:** [CodeWithFK](https://www.youtube.com/@codewithfk)
