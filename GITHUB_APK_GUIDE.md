# GitHub APK Generation Guidelines — Smart Dialer Pro

This guide explains how to automatically build and download **Smart Dialer Pro** APK files using GitHub Actions.

---

## 1. Prerequisites Included in This Repository

- **Gradle Version:** Configured via `gradle/wrapper/gradle-wrapper.properties` (**Gradle 9.3.1**). Because `gradlew` is not committed in the template export, the included GitHub Actions workflow installs Gradle `9.3.1` directly via `gradle/actions/setup-gradle@v4`.
- **Android SDK & Java:** Targets `compileSdk 36` / `targetSdk 36` / `minSdk 26` and builds cleanly on **JDK 17** (`temurin`).
- **Debug Keystore:** `debug.keystore.base64` is tracked in the repository root and automatically decoded during CI builds so debug APKs match your Firebase-registered SHA-1/SHA-256 fingerprint.
- **Release Signing Variables (`app/build.gradle.kts`):**
  - `KEYSTORE_PATH` (defaults to `${rootDir}/my-upload-key.jks`)
  - `STORE_PASSWORD`
  - `KEY_PASSWORD`
  - Fixed `keyAlias`: `"upload"`

---

## 2. Generate a Debug APK on GitHub (No Secrets Required)

1. Push this repository to GitHub (using **Export → Push to GitHub** in AI Studio or `git push`).
2. Open your repository on GitHub and click the **Actions** tab.
3. Select **Build Smart Dialer Pro APK** in the left sidebar.
4. Click **Run workflow** (or wait for any push to `main` / `master` to finish).
5. Once **Build Debug APK** turns green, scroll down to **Artifacts** and download **Smart-Dialer-Pro-Debug-APK** (contains `Smart Dialer Pro-debug.apk`).

---

## 3. Generate a Signed Release APK on GitHub

### Step A: Create an Upload Keystore (Once, Locally)
```bash
keytool -genkeypair -v -keystore my-upload-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```
> Keep the alias exact (`upload`) because `app/build.gradle.kts` expects `keyAlias = "upload"`.

Convert the keystore to a single-line base64 string:
- **Linux / macOS:**
  ```bash
  base64 -w 0 my-upload-key.jks   # macOS: base64 -i my-upload-key.jks | tr -d '\n'
  ```
- **Windows (PowerShell):**
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks"))
  ```

### Step B: Add GitHub Repository Secrets
Open **GitHub Repository → Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret Name | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Single-line base64 string of `my-upload-key.jks` |
| `STORE_PASSWORD` | Your keystore store password |
| `KEY_PASSWORD` | Your `upload` key password |

### Step C: Register Release Keystore Fingerprints for Firebase Google Sign-In
To use **Cloud Sync & Backup** (Google Sign-In) on a Release APK:
1. Extract your release keystore's fingerprints:
   ```bash
   keytool -list -v -keystore my-upload-key.jks -alias upload
   ```
2. Open **Firebase Console → Project Settings → Your Android App (`com.aistudio.smartdialer.kxmzpq`)**.
3. Add both the **SHA-1** and **SHA-256** fingerprints from `my-upload-key.jks`, then update `app/google-services.json` in your repository.

### Step D: Download or Auto-Publish Release APKs
- **Download from Actions Artifacts:** Run the workflow manually or push to `main`/`master`, then download **Smart-Dialer-Pro-Release-APK**.
- **Auto-Publish to GitHub Releases:** Push a version tag:
  ```bash
  git tag v1.0.0
  git push origin v1.0.0
  ```
  The workflow attaches `Smart Dialer Pro-release.apk` directly to the GitHub Release page.

---

## 4. Security Checklist Before Pushing Publicly

- Never commit `my-upload-key.jks`, `.env`, or raw passwords to Git.
- Only store signing credentials in **GitHub Actions Repository Secrets**.
- Keep `debug.keystore` untracked (already listed in `.gitignore`).
