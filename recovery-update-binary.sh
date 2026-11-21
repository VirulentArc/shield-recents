#!/sbin/sh
# Recovery installer for the non-A/B foster Shield family, Android 15 / LineageOS 22.2.
set -eu
OUTFD="/proc/self/fd/$2"
ZIP="$3"
TMP=/tmp/shield-recents-installer
MOUNTED_HERE=0
say() { echo "ui_print $*" > "$OUTFD"; echo ui_print > "$OUTFD"; }
cleanup() {
  rm -rf "$TMP"
  if [ "$MOUNTED_HERE" = 1 ]; then umount /mnt/shield-recents-system || true; fi
}
trap cleanup EXIT
fail() { say "$*"; exit 1; }
mkdir -p "$TMP"
unzip -oq "$ZIP" -d "$TMP" || fail "Cannot extract package"
MODE=$(cat "$TMP/mode")
SYSTEM_OUT=
for candidate in /system /system/system /system_root/system /mnt/system/system; do
  if [ -f "$candidate/build.prop" ] && [ -d "$candidate/framework" ]; then SYSTEM_OUT="$candidate"; break; fi
done
if [ -z "$SYSTEM_OUT" ]; then
  BLOCK=
  if [ -f /etc/recovery.fstab ]; then
    BLOCK=$(awk '$2=="/system" || $2=="/system_root" || $2=="/" {print $1;exit}' /etc/recovery.fstab)
  fi
  if [ ! -b "$BLOCK" ]; then BLOCK=/dev/block/bootdevice/by-name/APP; fi
  [ -b "$BLOCK" ] || fail "Cannot locate Shield system partition"
  mkdir -p /mnt/shield-recents-system
  mount -t ext4 -o rw "$BLOCK" /mnt/shield-recents-system || fail "Cannot mount system"
  MOUNTED_HERE=1
  SYSTEM_OUT=/mnt/shield-recents-system/system
fi
[ -f "$SYSTEM_OUT/build.prop" ] || fail "System build properties missing"
if [ "$MODE" != remove ]; then
  grep -q '^ro.build.version.sdk=35$' "$SYSTEM_OUT/build.prop" || fail "Android 15 is required"
  grep -q '^ro.lineage.version=22.2-.*foster' "$SYSTEM_OUT/build.prop" || fail "LineageOS 22.2 foster is required"
fi
if [ "$MOUNTED_HERE" = 0 ]; then
  mount -o remount,rw "$SYSTEM_OUT" 2>/dev/null || mount -o remount,rw "$(dirname "$SYSTEM_OUT")" 2>/dev/null || fail "Cannot remount system writable"
fi
PRODUCT="$SYSTEM_OUT/product"
if [ -L "$PRODUCT" ]; then fail "Separate product partitions are not supported by this package"; fi
[ -d "$PRODUCT" ] || fail "Product directory missing"
if [ "$MODE" = remove ]; then
  say "Removing Shield Recents"
  rm -f "$PRODUCT/priv-app/ShieldRecents/ShieldRecents.apk" "$PRODUCT/overlay/ShieldRecentsOverlay.apk" "$PRODUCT/etc/permissions/privapp-permissions-shield-recents.xml" "$SYSTEM_OUT/addon.d/71-shield-recents.sh"
  rmdir "$PRODUCT/priv-app/ShieldRecents" 2>/dev/null || true
else
  say "Installing experimental Shield Recents for Android 15"
  mkdir -p "$PRODUCT/priv-app/ShieldRecents" "$PRODUCT/overlay" "$PRODUCT/etc/permissions" "$SYSTEM_OUT/addon.d"
  cp "$TMP/files/ShieldRecents.apk" "$PRODUCT/priv-app/ShieldRecents/ShieldRecents.apk"
  cp "$TMP/files/ShieldRecentsOverlay.apk" "$PRODUCT/overlay/ShieldRecentsOverlay.apk"
  cp "$TMP/files/privapp-permissions-shield-recents.xml" "$PRODUCT/etc/permissions/privapp-permissions-shield-recents.xml"
  cp "$TMP/files/71-shield-recents.sh" "$SYSTEM_OUT/addon.d/71-shield-recents.sh"
  chmod 755 "$PRODUCT/priv-app/ShieldRecents" "$SYSTEM_OUT/addon.d/71-shield-recents.sh"
  for FILE in "$PRODUCT/priv-app/ShieldRecents/ShieldRecents.apk" "$PRODUCT/overlay/ShieldRecentsOverlay.apk" "$PRODUCT/etc/permissions/privapp-permissions-shield-recents.xml" "$SYSTEM_OUT/addon.d/71-shield-recents.sh"; do
    chown 0:0 "$FILE"
    chcon u:object_r:system_file:s0 "$FILE" || fail "Cannot label package files"
  done
  chmod 644 "$PRODUCT/priv-app/ShieldRecents/ShieldRecents.apk" "$PRODUCT/overlay/ShieldRecentsOverlay.apk" "$PRODUCT/etc/permissions/privapp-permissions-shield-recents.xml"
fi
sync
say "Done. Reboot to load the change."
