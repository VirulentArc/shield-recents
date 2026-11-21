package local.shield.recents;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

/** Minimal Android 15 IOverviewProxy endpoint; IDs match the checked-in AOSP AIDL. */
public final class OverviewService extends Service {
    private static final String DESCRIPTOR = "com.android.systemui.shared.recents.IOverviewProxy";
    private final Binder binder = new Binder() {
        { attachInterface(null, DESCRIPTOR); }
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) { if (reply != null) reply.writeString(DESCRIPTOR); return true; }
            if (code >= FIRST_CALL_TRANSACTION && code <= LAST_CALL_TRANSACTION) data.enforceInterface(DESCRIPTOR);
            int method = code - FIRST_CALL_TRANSACTION;
            if (method == 6 || method == 7) {
                Log.i("ShieldRecents","overview request=" + method);
                Intent intent = new Intent(OverviewService.this, RecentsTvActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("toggle",method == 6);
                startActivity(intent);
            } else if (method == 8) {
                // Home itself owns dismissal. Alt-Tab release should launch the focused task.
                boolean altTab = data.readInt() != 0;
                boolean home = data.readInt() != 0;
                RecentsTvActivity.hide(altTab && !home);
            }
            return true;
        }
    };
    @Override public IBinder onBind(Intent intent) { Log.i("ShieldRecents","Overview bound"); return binder; }
}
