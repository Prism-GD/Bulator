# Bulator

A lightweight Ubuntu userspace environment for Android, initially targeting ARM64 devices.

## Project status

**Version:** 0.1.0 — foundation stage  
**Current state:** Android application skeleton  
**Ubuntu runtime:** Not integrated yet

## Goals

- Run an Ubuntu ARM64 userspace on Android without requiring root.
- Keep the application lightweight and suitable for modest Android devices.
- Provide an integrated terminal.
- Use PRoot as the planned userspace execution layer.
- Build and distribute APKs through GitHub Actions and GitHub Releases.

## Development approach

Bulator is an Android application, not a replacement for the Android kernel and not a traditional virtual machine. The planned approach is to package the required Android-compatible runtime components and launch an Ubuntu root filesystem in userspace.

The project will be implemented independently. Studying other projects may inform architectural decisions, but their code will not be copied.

## Current build

The repository contains an initial native Android/Kotlin project and a GitHub Actions workflow that builds a debug APK.

The first screen is only a scaffold. The **Start Ubuntu** button is intentionally disabled until the PRoot runtime and Ubuntu root filesystem are implemented.

## Build

Run the **Build Bulator APK** workflow from the Actions tab. The generated debug APK is uploaded as an artifact.

## Roadmap

- [x] Create Android project foundation.
- [x] Add GitHub Actions APK build.
- [ ] Verify the first APK build.
- [ ] Add ARM64 PRoot runtime.
- [ ] Download and install a minimal Ubuntu ARM64 root filesystem.
- [ ] Implement terminal process and session management.
- [ ] Add persistent Ubuntu storage and lifecycle handling.

## Important

Bulator development uses GitHub and GitHub Actions. Termux is not part of the development workflow.
