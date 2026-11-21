package local.shield.recents;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.graphics.ColorSpace;
import android.os.Bundle;
import android.os.Process;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/** Android 15 adapter replacing the stock Android 11 SystemUI task wrappers. */
public final class Tasks {
    private final Object service;
    private final Class<?> api;
    private final Context context;
    public Tasks(Context context) throws Exception {
        this.context = context;
        api = Class.forName("android.app.IActivityTaskManager");
        service = Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
    }
    private Object call(String name, Class<?>[] types, Object... args) throws Exception {
        try { return api.getMethod(name, types).invoke(service, args); }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw e;
        }
    }
    @SuppressWarnings("unchecked")
    public List<ActivityManager.RecentTaskInfo> list() throws Exception {
        int userId = Process.myUid() / 100000;
        Object slice = call("getRecentTasks", new Class<?>[]{int.class,int.class,int.class}, 100, 1, userId);
        List<ActivityManager.RecentTaskInfo> all = (List<ActivityManager.RecentTaskInfo>) slice.getClass().getMethod("getList").invoke(slice);
        List<ActivityManager.RecentTaskInfo> result = new ArrayList<>();
        ComponentName home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).resolveActivity(context.getPackageManager());
        for (ActivityManager.RecentTaskInfo task : all) {
            ComponentName base = task.baseActivity != null ? task.baseActivity : task.baseIntent.getComponent();
            if (base == null || base.getPackageName().equals(context.getPackageName()) || base.getPackageName().equals("com.android.systemui")) continue;
            if (home != null && base.equals(home)) continue;
            int type = (int) Class.forName("android.app.TaskInfo").getMethod("getActivityType").invoke(task);
            if (type != 1) continue;
            result.add(task);
        }
        return result;
    }
    public void open(int id) throws Exception {
        int result=(Integer)call("startActivityFromRecents", new Class<?>[]{int.class,Bundle.class}, id, null);
        if(result<0)throw new IllegalStateException("Android refused to launch task "+id+": "+result);
    }
    public boolean remove(int id) throws Exception {
        return (Boolean) call("removeTask", new Class<?>[]{int.class}, id);
    }
    public Bitmap snapshot(int id) throws Exception {
        Object snapshot = call("getTaskSnapshot", new Class<?>[]{int.class,boolean.class}, id, true);
        if (snapshot == null) return null;
        HardwareBuffer buffer = (HardwareBuffer) snapshot.getClass().getMethod("getHardwareBuffer").invoke(snapshot);
        if (buffer == null) return null;
        try {
            ColorSpace color = (ColorSpace) snapshot.getClass().getMethod("getColorSpace").invoke(snapshot);
            Bitmap hardware = Bitmap.wrapHardwareBuffer(buffer,color);
            return hardware == null ? null : hardware.copy(Bitmap.Config.ARGB_8888,false);
        } finally { buffer.close(); }
    }
    public static void main(String[] args) throws Exception {
        // A shell-identity probe validates the device's hidden API signatures before installation.
        Object service = Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
        Class<?> api = Class.forName("android.app.IActivityTaskManager");
        Object slice = api.getMethod("getRecentTasks",int.class,int.class,int.class).invoke(service,100,1,0);
        List<?> tasks = (List<?>) slice.getClass().getMethod("getList").invoke(slice);
        System.out.println("recentTasks=" + tasks.size());
        for (Object item : tasks) {
            ActivityManager.RecentTaskInfo task = (ActivityManager.RecentTaskInfo)item;
            System.out.println("task="+task.taskId+" running="+task.isRunning+" base="+task.baseActivity);
        }
        for (String method : new String[]{"removeTask","startActivityFromRecents","getTaskSnapshot"}) {
            for (java.lang.reflect.Method m : api.getMethods()) if (m.getName().equals(method)) System.out.println(m.toString());
        }
    }
}
