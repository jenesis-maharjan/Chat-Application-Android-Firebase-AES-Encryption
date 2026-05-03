# 💬 Chat Application (Android + Firebase + AES Encryption)

A secure real-time chat application built using **Java in Android Studio**, powered by **Firebase Realtime Database** and **Firebase Authentication**, with **AES encryption** for secure message transmission.

This project demonstrates real-time communication, authentication, and data security using symmetric encryption.

---

## 🚀 Key Highlights

- 🔐 Secure authentication using Firebase
- 💬 Real-time one-to-one messaging system
- 🔒 AES encryption for secure message transmission
- 👥 Dynamic user listing
- ⚡ Instant message synchronization using Firebase Realtime Database
- 📱 Simple, responsive, and user-friendly UI

---

## 🛠️ Tech Stack

- Java (Android Development)
- Firebase Authentication
- Firebase Realtime Database
- AES Symmetric Encryption
- XML (UI Design)
- RecyclerView
- CardView

---

## 🔐 Security Implementation (AES Encryption)

- Messages are encrypted using AES before being sent to Firebase
- Encrypted messages are stored in the database
- Only the intended receiver can decrypt messages
- Ensures confidentiality and data security in communication

---

## 📁 Project Structure

- `login.java` → Handles user authentication
- `registration.java` → Handles new user signup
- `chatwindo.java` → Chat interface and messaging logic
- `AESHelper.java` → Handles encryption and decryption
- `setting.java` → User settings screen
- `splash.java` → App launch screen
- Firebase → Backend services (Auth + Database)

---


## 🚀 How to Run the Project

### 1. Clone the repository
git clone https://github.com/jenesis-maharjan/Chat-Application-Android-Firebase-AES-Encryption.git

### 2. Open the project in Android Studio
- Click on **Open an Existing Project**
- Select the cloned folder

### 3. Setup Firebase
- Create a project in Firebase Console
- Enable Authentication and Realtime Database
- Download `google-services.json`
- Place it inside `/app` directory

### 4. Sync Gradle
- Allow Android Studio to download all dependencies

### 5. Run the Application
- Use an emulator or physical Android device
