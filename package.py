from pathlib import Path
import zipfile
import hashlib
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parent
ET.parse(root/'privapp-permissions-shield-recents.xml')
for mode in ('install','remove'):
    name='ShieldRecents-Android15-experimental.zip' if mode=='install' else 'ShieldRecents-REMOVE.zip'
    with zipfile.ZipFile(root/'dist'/name,'w',zipfile.ZIP_DEFLATED) as out:
        out.write(root/'recovery-update-binary.sh','META-INF/com/google/android/update-binary')
        out.writestr('META-INF/com/google/android/updater-script','# Shield Recents shell installer\n')
        out.writestr('mode',mode+'\n')
        if mode=='install':
            for file in ('dist/ShieldRecents.apk','dist/ShieldRecentsOverlay.apk','privapp-permissions-shield-recents.xml','71-shield-recents.sh'):
                out.write(root/file,'files/'+Path(file).name)
checks=[]
for p in sorted((root/'dist').iterdir()):
    if p.suffix in ('.apk','.zip'): checks.append(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name)
(root/'dist/SHA256SUMS').write_text('\n'.join(checks)+'\n')
