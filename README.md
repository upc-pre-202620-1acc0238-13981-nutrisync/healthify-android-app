# healthify-android-app
healthify android app

## CI/CD

| Workflow | When | What it does |
|---|---|---|
| `ci.yml` | Pull requests to `develop` or `main`, pushes to `develop` | `lintDebug`, `testDebugUnitTest` and `assembleDebug` |
| `distribute.yml` | Pushes to `main`, or by hand from the Actions tab | Builds the signed release APK and uploads it to Firebase App Distribution (group `qa`) |

Each distributed build takes the workflow run number as its `versionCode`.

### Repository secrets

| Secret | Value |
|---|---|
| `FIREBASE_APP_ID` | Firebase App ID of `pe.edu.upc.healthify` |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | JSON key of a service account with the *Firebase App Distribution Admin* role |
| `KEYSTORE_BASE64` | Upload keystore (`.jks`) encoded in base64 |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

### Signing a release build locally

Create `keystore.properties` at the project root (it is ignored by git):

```properties
KEYSTORE_FILE=C:/path/to/healthify-upload.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=healthify
KEY_PASSWORD=...
```

Without it, `assembleRelease` still builds, unsigned.
