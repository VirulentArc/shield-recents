#!/system/bin/sh
set -eu
case "$(getprop ro.lineage.version)" in 22.2-*foster) ;; *) echo 'Unsupported LineageOS build'; exit 1;; esac
[ "$(getprop ro.product.device)" = mdarcy ] || { echo 'This live test targets the 2019 Shield'; exit 1; }
[ "$(id -u)" = 0 ] || exit 1
SRC=/data/local/tmp/shield-recents
P=/system/product
stop
sleep 2
trap 'sync; mount -o remount,ro / 2>/dev/null || true; start' EXIT
mount -o remount,rw /
mkdir -p "$P/priv-app/ShieldRecents" "$P/overlay" "$P/etc/permissions" /system/addon.d
cp "$SRC/ShieldRecents.apk" "$P/priv-app/ShieldRecents/ShieldRecents.apk"
cp "$SRC/ShieldRecentsOverlay.apk" "$P/overlay/ShieldRecentsOverlay.apk"
cp "$SRC/privapp-permissions-shield-recents.xml" "$P/etc/permissions/privapp-permissions-shield-recents.xml"
cp "$SRC/71-shield-recents.sh" /system/addon.d/71-shield-recents.sh
chown 0:0 "$P/priv-app/ShieldRecents" "$P/priv-app/ShieldRecents/ShieldRecents.apk" "$P/overlay/ShieldRecentsOverlay.apk" "$P/etc/permissions/privapp-permissions-shield-recents.xml" /system/addon.d/71-shield-recents.sh
chmod 755 "$P/priv-app/ShieldRecents" /system/addon.d/71-shield-recents.sh
chmod 644 "$P/priv-app/ShieldRecents/ShieldRecents.apk" "$P/overlay/ShieldRecentsOverlay.apk" "$P/etc/permissions/privapp-permissions-shield-recents.xml"
restorecon -RF "$P/priv-app/ShieldRecents" "$P/overlay/ShieldRecentsOverlay.apk" "$P/etc/permissions/privapp-permissions-shield-recents.xml" /system/addon.d/71-shield-recents.sh
sha256sum "$P/priv-app/ShieldRecents/ShieldRecents.apk" "$P/overlay/ShieldRecentsOverlay.apk"
