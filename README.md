# EdgeBackCustomizer

LSPosed (libxposed API 102) module that customizes the edge back gesture in
AOSP / Pixel SystemUI (Android 12-17).

* App UI: Jetpack Compose + Material 3 (Monet), 3 pages: Home, Customize, Settings. Turkish and English.
* Module: still in discovery. It changes no behaviour yet; it dumps the back-gesture
  classes to `/data/user/0/com.android.systemui/files/ebc_dump.txt` (logcat tag `EBC`).

Build: push to `main`, GitHub Actions produces the debug APK artifact.

Note: `app/src/main/res/drawable-nodpi/header_sky.jpg` is a placeholder header image.
Replace it with an image you have the rights to before publishing.

## Fonts

The app UI can use rounded variable fonts (Nunito, Quicksand, Fredoka, Outfit, Manrope, Baloo 2,
Comfortaa, DM Sans). They are SIL Open Font License 1.1 fonts from https://github.com/google/fonts and
are downloaded by the GitHub Actions workflow into `app/src/main/assets/fonts/` (not committed).
You can also import your own .ttf/.otf from Settings -> Font.
