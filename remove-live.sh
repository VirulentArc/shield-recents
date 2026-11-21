#!/system/bin/sh
set -eu
[ "$(id -u)" = 0 ] || { echo 'Rooted debugging is required'; exit 1; }
# Preserve any other enabled accessibility services.
CURRENT=$(settings get secure enabled_accessibility_services)
REMAINING=
OLDIFS=$IFS
IFS=:
for ENTRY in $CURRENT; do
  case "$ENTRY" in null|local.shield.recents/.HomeKeyService|local.shield.recents/local.shield.recents.HomeKeyService|'') continue;; esac
  if [ -z "$REMAINING" ]; then REMAINING="$ENTRY"; else REMAINING="$REMAINING:$ENTRY"; fi
done
IFS=$OLDIFS
settings put secure shield_recents_home_enabled 0
settings put secure enabled_accessibility_services "$REMAINING"
if [ -z "$REMAINING" ]; then settings put secure accessibility_enabled 0; fi
stop
trap 'start' EXIT
sleep 2
mount -o remount,rw /
rm -f /system/product/priv-app/ShieldRecents/ShieldRecents.apk /system/product/overlay/ShieldRecentsOverlay.apk /system/product/etc/permissions/privapp-permissions-shield-recents.xml /system/addon.d/71-shield-recents.sh
rmdir /system/product/priv-app/ShieldRecents 2>/dev/null || true
sync
mount -o remount,ro /
start
trap - EXIT
