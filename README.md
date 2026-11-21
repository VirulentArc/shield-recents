# Shield Recents

An experimental system task switcher for **NVIDIA Shield TV Pro (2019)** running
**LineageOS 22.2 / Android 15**. Double-press Home to switch between apps or dismiss
their tasks.

The TV card layout, graphics and adapted rendering methods come from stock Shield
SystemUI and AOSP TV Recents. Task management and Home shortcut handling are
adapted for Android 15. The existing LineageOS SystemUI APK stays unchanged.

## Compatibility and status

Tested on a 2019 Shield Pro (`mdarcy`) with
`22.2-20261116-NIGHTLY-foster`, Android 15. The 2017 Shield and other builds are
unverified. This is an unofficial project, unaffiliated with NVIDIA or LineageOS.

Live installation through rooted ADB has been tested. App switching, individual
task dismissal and Close all were verified against Android's real task list.
The physical remote's single and double Home presses were also tested, and the
shortcut service started automatically after a full reboot.

**The recovery installer and removal ZIPs have not been flash-tested in recovery.**
OTA preservation and long-press assistant behavior are also unverified.

## Downloads

The v0.2 payload below is the version installed and tested on the Shield:

- [Experimental recovery installer](packages/v0.2/ShieldRecents-Android15-experimental.zip?raw=true)
- [Recovery removal package](packages/v0.2/ShieldRecents-REMOVE.zip?raw=true)
- [Privileged component APK](packages/v0.2/ShieldRecents.apk?raw=true)
- [Framework overlay APK](packages/v0.2/ShieldRecentsOverlay.apk?raw=true)
- [SHA-256 checksums](packages/v0.2/SHA256SUMS)

Installation requires system integration through recovery or rooted ADB. Ordinary
APK installation alone does not supply the required privileges and framework
configuration. ZIPs are not signed with LineageOS's private recovery key.

## Controls

| Input | Action |
| --- | --- |
| Single Home | Return to the launcher after the double-press interval |
| Double Home | Open or close Shield Recents |
| Hold Home | Launch the installed assistant through `ACTION_ASSIST` |
| Left / Right | Select a task |
| OK / Enter | Switch to the selected task |
| Down, then OK | Dismiss the selected task |
| Down again | Move to Close all; press OK to activate |
| Back | Return to the previous screen |

The Home service filters only HOME key events. OK, Enter, Back, Esc and pointer
events pass through. It does not request accessibility window-content access.

## How it works

The component queries Android's task manager on each refresh. After dismissal it
queries again to confirm that the task disappeared. Home, SystemUI, the switcher
itself and nonstandard task types are excluded. The list can be empty after boot
until apps are opened.

Task removal follows Android's task semantics. Cached processes and background
services, such as a VPN service, remain under Android's management. If task
screenshots are unavailable, stock-style app banners or icons are used.

The package installs four files:

```text
/system/product/priv-app/ShieldRecents/ShieldRecents.apk
/system/product/overlay/ShieldRecentsOverlay.apk
/system/product/etc/permissions/privapp-permissions-shield-recents.xml
/system/addon.d/71-shield-recents.sh
```

The overlay enables `config_hasRecents` for default and television configurations
and registers `local.shield.recents/.RecentsTvActivity` as the Recents provider.
Android supplies the corresponding task-management permissions. A privileged
boot receiver enables the package's Home shortcut AccessibilityService while
preserving other enabled services. The application has no INTERNET permission.

The tested TV SystemUI has its old TV Recents activity absent and its Recents
button callbacks optimized out. Home shortcut handling therefore lives inside
this package. An Android 15 IOverviewProxy endpoint is included for compatible
SystemUI clients.

## Rooted ADB installation

This method was tested on the 2019 Shield Pro. Enable rooted debugging in
LineageOS developer options and select the intended device with `ANDROID_SERIAL`
if more than one device is connected. The script checks for `mdarcy` and
LineageOS 22.2 foster, modifies the four files listed above, and restarts Android.
It does not format user data. Keep a recovery route available when modifying
system software.

From the repository directory:

```sh
adb root
adb shell mkdir -p /data/local/tmp/shield-recents
adb push packages/v0.2/ShieldRecents.apk packages/v0.2/ShieldRecentsOverlay.apk \
  privapp-permissions-shield-recents.xml 71-shield-recents.sh \
  install-live.sh remove-live.sh /data/local/tmp/shield-recents/
adb shell sh /data/local/tmp/shield-recents/install-live.sh
```

Android's interface restarts during installation. Once ready, open an app and
double-press Home.

For removal through rooted ADB:

```sh
adb root
adb push remove-live.sh /data/local/tmp/shield-recents-remove.sh
adb shell sh /data/local/tmp/shield-recents-remove.sh
```

The removal script preserves other enabled accessibility services and restarts
Android. The recovery removal ZIP deletes the four system files; it does not
format user data.

The addon.d v3 script is intended to preserve the component across compatible
LineageOS 22.2 / Android 15 updates. It skips payload restoration onto an
incompatible Android/LineageOS version. OTA preservation has not been tested.

## Build

Requirements: JDK 17 or newer, Python 3, Android SDK platform `android-35`, and SDK
build-tools `35.0.0`. The build uses the SDK tools directly, without Gradle or
third-party application libraries.

```sh
export JAVA_HOME=/path/to/jdk
export ANDROID_SDK_ROOT=/path/to/android-sdk
bash build.sh
python3 package.py
```

For a custom SDK layout, set `ANDROID_BUILD_TOOLS_DIR` and `ANDROID_JAR` instead
of `ANDROID_SDK_ROOT`. `ANDROID_BUILD_TOOLS_VERSION` defaults to `35.0.0`.

A local signing key and random password file are created under `build/` on the
first build. Keep those files for future updates; they are ignored by Git.
Custom paths can be supplied through `SHIELD_RECENTS_KEYSTORE` and
`SHIELD_RECENTS_PASSWORD_FILE`. Independently generated keys cannot replace an
installed build signed by another key through an ordinary APK update.

Output goes to `dist/`. The pinned, tested v0.2 payload lives under
`packages/v0.2/`; rebuilding does not overwrite that directory.

The overlay build uses `--no-resource-deduping` and `--no-resource-removal` and
checks that the compiled television value survives. These flags follow
[Android's RRO build rules](https://source.android.com/docs/core/runtime/rros).

`bash build-fixture.sh` builds a disposable two-task test app under
`build/fixture/`. It is excluded from installation packages.

## License and provenance

See [LICENSE](LICENSE) and [NOTICE](NOTICE). The reused TV interface is derived
from AOSP TV Recents and locally extracted Shield 9.0.0 resources. The two PNG
icons were verified byte-for-byte against their AOSP versions. Full firmware,
decompiled SystemUI, SDK binaries, personal backups, device logs and signing keys
are excluded from this repository.
