#!/sbin/sh
# ADDOND_VERSION=3
. /tmp/backuptool.functions
# ADDOND_VERSION=2
. /tmp/backuptool.functions
list_files() {
cat <<EOF
product/priv-app/ShieldRecents/ShieldRecents.apk
product/overlay/ShieldRecentsOverlay.apk
product/etc/permissions/privapp-permissions-shield-recents.xml
EOF
}
case "$1" in
  backup) list_files | while read FILE; do backup_file "$S/$FILE"; done ;;
  restore)
    if grep -q '^ro.build.version.sdk=35$' "$S/build.prop" && grep -q '^ro.lineage.version=22.2-.*foster' "$S/build.prop"; then
      list_files | while read FILE; do restore_file "$S/$FILE"; done
    fi
    ;;
  post-restore)
    list_files | while read FILE; do
      DEST=$(get_output_path "$S/$FILE")
      if [ -f "$DEST" ]; then chown 0:0 "$DEST"; chmod 644 "$DEST"; chmod 755 "$(dirname "$DEST")"; fi
    done
    ;;
esac
