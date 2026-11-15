# Bulator

A lightweight Lubuntu-oriented Linux container manager for Android, initially targeting ARM64 devices.

## Version 0.2.0 — first functional manager prototype

This version adds locally persisted container profiles, per-container settings, a keyboard mapping editor, and saved keyboard presets.

### Working in this version
- Create, select, configure, and delete container profiles.
- Save container name, desktop preference, and display resolution.
- Edit a keyboard mapping and save named presets.
- Load saved presets on the device.
- Open Android keyboard settings / request the soft keyboard.

### Not yet integrated
- PRoot ARM64 runtime.
- Lubuntu ARM64 root filesystem download and extraction.
- Real Ubuntu shell and command execution.
- LXQt graphical desktop rendering.
- Sending mapped keys into a running Linux session.

The app labels these features as pending rather than pretending that Linux is already running.

## Architecture

Bulator will run a Lubuntu ARM64 userspace in Android using PRoot, without root access or replacing the Android kernel. It is a userspace container environment, not a hardware-virtualizing virtual machine.

The initial Lubuntu target is approximately 3.9 GB for the installed base system; this is a planning target, not a storage quota. Apps and user downloads installed later use additional device storage.

## Development

Development and APK builds use GitHub and GitHub Actions. Termux is not part of the workflow.

Run **Build Bulator APK** in the Actions tab. The debug APK is uploaded as the `Bulator-debug-apk` artifact.
