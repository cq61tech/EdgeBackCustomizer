# EdgeBackCustomizer

LSPosed (libxposed API 102) module that customizes the edge back gesture in
AOSP / Pixel SystemUI (Android 12-17).

Status: **Phase 0 - discovery build.** It changes no behaviour. It dumps the
members of the back-gesture classes to
`/data/user/0/com.android.systemui/files/ebc_dump.txt` (and logcat tag `EBC`)
so the real class/method names of each Android version can be verified.

Build: push to `main`, GitHub Actions produces the debug APK artifact.
