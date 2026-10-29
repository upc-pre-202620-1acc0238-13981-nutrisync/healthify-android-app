# healthify-android-app
healthify android app

## CI/CD

| Workflow | When | What it does |
|---|---|---|
| `ci.yml` | Pull requests to `develop` or `main`, pushes to `develop` | `lintDebug`, `testDebugUnitTest` and `assembleDebug` |
| `distribute.yml` | Pushes to `main`, or by hand from the Actions tab | Builds the signed release APK and uploads it to Firebase App Distribution (group `qa`) |

Each distributed build takes the workflow run number as its `versionCode`.
