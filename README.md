# EdgeBackCustomizer

LSPosed module for customizing the edge back gesture on AOSP / Pixel System UI (Android 12 and up).
Built on the libxposed API (102). Needs LSPosed (or Vector) with Zygisk, and root for the
status and restart helpers.

## Features

- Back pill look: colors (theme or custom HSL), gradient and angle, opacity, glow, roundness,
  size (uniform, width, height), arrow size and thickness, outline, vertical offset
- Active zone (bottom percentage), left/right edge switches, edge trigger width
- Haptic feedback on/off
- App UI: Material 3 with Monet, accent color, AMOLED mode, floating dock, English and Turkish,
  custom fonts, settings backup and restore

## Setup

1. Install the APK and enable the module in LSPosed.
2. In the module's scope tick **System UI** and **EdgeBackCustomizer**.
3. Restart System UI (button in the app) or reboot.

Reinstalling the app with a different signature clears the scope; builds from this repo use a fixed
debug key so updates install over each other.

## Build

Push to `main` and GitHub Actions produces a shrunk release APK (one universal package for all
ABIs, the app has no native code of its own). Locally: `gradle :app:assembleRelease` (JDK 17).

## Fonts

The UI can use rounded variable fonts (Nunito, Quicksand, Fredoka). They are licensed under the SIL
Open Font License 1.1 and are fetched from https://github.com/google/fonts by the workflow into
`app/src/main/assets/fonts/` (not committed). Your own .ttf/.otf can be imported from
Settings -> Font.

The header image in `res/drawable-nodpi/header_sky.jpg` is a placeholder; replace it with one you
have the rights to.
