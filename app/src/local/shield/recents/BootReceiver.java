package local.shield.recents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BootReceiver extends BroadcastReceiver {
    static final String COMPONENT="local.shield.recents/.HomeKeyService";
    @Override public void onReceive(Context context, Intent intent) {
        // Only the recovery-installed privileged build can initialize its shortcut automatically.
        if(context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")!=PackageManager.PERMISSION_GRANTED)return;
        if(Settings.Secure.getInt(context.getContentResolver(),"shield_recents_home_enabled",1)==0)return;
        String existing=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        List<String> services=new ArrayList<>();
        if(existing!=null&&!existing.isEmpty())services.addAll(Arrays.asList(existing.split(":")));
        if(!services.contains(COMPONENT)&&!services.contains("local.shield.recents/local.shield.recents.HomeKeyService")) {
            services.add(COMPONENT);
            Settings.Secure.putString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,String.join(":",services));
        }
        Settings.Secure.putInt(context.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,1);
        Log.i("ShieldRecents","Home shortcut initialized after "+intent.getAction());
    }
}
