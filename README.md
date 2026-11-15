# Bulator

A persistent ARM64 Linux userspace for Android, without root access. The target desktop is **Lubuntu with LXQt**, including its file manager, application menu, terminal, settings and APT.

## Version 0.3.0-alpha — Linux runtime integration

The current source adds the first runtime-integration path:

- GitHub Actions cross-compiles upstream PRoot-RS for `aarch64-linux-android`.
- The PRoot-RS executable and loader shim are packaged into the ARM64 APK.
- The app can download the official Ubuntu Base 24.04.5 ARM64 root filesystem.
- The downloaded archive is checked against Ubuntu's published SHA256SUMS before extraction.
- Each container receives a stable ID and its own persistent root filesystem directory.
- The app includes a smoke test that attempts to execute Linux commands through PRoot-RS.

**Important:** this alpha is not yet the complete Lubuntu desktop. Ubuntu Base is the minimal foundation. The LXQt desktop, graphical display server, interactive terminal/PTY, keyboard injection and device-level PRoot compatibility still require implementation and real-device testing. A successful Android APK build alone does not prove that ptrace works on every phone.

## Data persistence

Each container keeps its own rootfs, downloads, cache and configuration under the app's persistent private storage. Stopping the app does not intentionally remove those files. Uninstalling Bulator can remove private app data, so backup/export is required before uninstalling.

## Architecture

Bulator runs a Linux userspace through PRoot on the Android kernel. It is not a hardware-virtualizing virtual machine and does not replace Android.

The initial rootfs download is a minimal Ubuntu Base archive (about 29 MB compressed), not a 3.9 GB desktop installation. The approximately 3.9 GB figure is a planning estimate for the later installed desktop system, not a quota.

## Development and builds

Development and APK builds use GitHub and GitHub Actions. **Termux is not part of the workflow.**

Open the Actions tab and select **Build Bulator APK**. The workflow uploads the debug APK as the `Bulator-debug-apk` artifact after a successful build.
