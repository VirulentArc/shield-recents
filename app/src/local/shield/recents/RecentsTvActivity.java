package local.shield.recents;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Stock Shield card resources with an independently initialized Android 15 controller. */
public final class RecentsTvActivity extends Activity {
    private static WeakReference<RecentsTvActivity> current = new WeakReference<>(null);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Tasks tasks;
    private LinearLayout row;
    private TextView status;
    private Button closeAll;
    private int generation;
    private boolean busy;
    public static void hide(boolean launchFocused) {
        RecentsTvActivity activity = current.get();
        if(activity!=null) activity.runOnUiThread(() -> {
            View focused=activity.getCurrentFocus();
            if(launchFocused && focused instanceof TaskCardView) focused.performClick();
            else activity.finish();
        });
    }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        current=new WeakReference<>(this);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.88f);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setBackgroundResource(R.drawable.recents_tv_background_gradient);
        status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(18);
        status.setGravity(Gravity.CENTER);screen.addView(status,new LinearLayout.LayoutParams(-1,dp(40)));
        HorizontalScrollView scroll=new HorizontalScrollView(this);
        scroll.setFillViewport(true); scroll.setClipChildren(false);scroll.setClipToPadding(false);
        scroll.setHorizontalScrollBarEnabled(false);
        row=new LinearLayout(this);row.setGravity(Gravity.CENTER);row.setPadding(dp(35),dp(25),dp(35),dp(12));
        row.setClipChildren(false);row.setClipToPadding(false);
        scroll.addView(row,new HorizontalScrollView.LayoutParams(-2,-2));
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,dp(300)));
        closeAll=new Button(this);closeAll.setText("Close all recent apps");
        closeAll.setOnClickListener(v -> closeAll());screen.addView(closeAll,new LinearLayout.LayoutParams(-2,-2));
        TextView hint=new TextView(this);hint.setText("Left / Right: choose    OK: switch    Down, then OK: close    Back: return");
        hint.setTextColor(0xffeeeeee);hint.setGravity(Gravity.CENTER);hint.setPadding(0,dp(12),0,0);
        screen.addView(hint,new LinearLayout.LayoutParams(-1,-2));
        setContentView(screen);
        try { tasks=new Tasks(this); }
        catch(Exception e) { showError(e); }
    }
    @Override protected void onResume() {super.onResume();if(tasks!=null)refresh(-1);}
    @Override protected void onStop() {super.onStop();if(!isChangingConfigurations())finish();}
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if(intent.getBooleanExtra("toggle",false)) finish();
    }
    private void refresh(int focusIndex) {
        if(isFinishing())return;
        busy=true;closeAll.setEnabled(false);status.setText("Loading recent apps…");
        int token=++generation;
        worker.execute(() -> {
            try {
                List<ActivityManager.RecentTaskInfo> list=tasks.list();
                runOnUiThread(() -> {
                    if(token!=generation||isFinishing()||isDestroyed())return;
                    row.removeAllViews();
                    for(ActivityManager.RecentTaskInfo info:list) addCard(info,token);
                    status.setText(list.isEmpty()?"No recent apps": "Recent apps");
                    closeAll.setEnabled(!list.isEmpty());busy=false;
                    if(!list.isEmpty())row.getChildAt(Math.max(0,Math.min(focusIndex<0?0:focusIndex,list.size()-1))).requestFocus();
                    Log.i("ShieldRecents","Loaded task IDs="+list.stream().map(t->Integer.toString(t.taskId)).collect(java.util.stream.Collectors.joining(",")));
                });
            } catch(Exception e){runOnUiThread(()->showError(e));}
        });
    }
    private void addCard(ActivityManager.RecentTaskInfo task,int token) {
        TaskCardView card=(TaskCardView)getLayoutInflater().inflate(R.layout.recents_tv_task_card_view,row,false);
        ComponentName component=task.baseActivity!=null?task.baseActivity:task.baseIntent.getComponent();
        String name=component.getPackageName();Drawable banner=null;boolean hasBanner=false;
        try {
            ApplicationInfo app=getPackageManager().getApplicationInfo(component.getPackageName(),0);
            name=getPackageManager().getActivityInfo(component,0).loadLabel(getPackageManager()).toString();
            if(task.taskDescription!=null&&task.taskDescription.getLabel()!=null)name=task.taskDescription.getLabel();
            banner=getPackageManager().getActivityBanner(component);
            if(banner==null)banner=getPackageManager().getApplicationBanner(app);
            hasBanner=banner!=null;
            if(banner==null)banner=getPackageManager().getApplicationIcon(app);
        }catch(Exception ignored){}
        ((TextView)card.findViewById(R.id.card_title_text)).setText(name);
        card.findViewById(R.id.card_extra_badge).setVisibility(View.GONE);
        if(hasBanner)card.setAsBannerView(banner);else card.setAsIconView(banner);
        card.setContentDescription(name+", recent app");
        card.setTag(task.taskId);
        card.bind(()->open(task.taskId),()->remove(task.taskId,row.indexOfChild(card)));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.setMargins(dp(14),0,dp(14),0);
        row.addView(card,params);
        worker.execute(()->{
            try {
                Bitmap bitmap=tasks.snapshot(task.taskId);
                if(bitmap!=null)runOnUiThread(()->{
                    if(token==generation&&!isDestroyed()){card.setAsScreenShotView(bitmap);}
                    else bitmap.recycle();
                });
            }catch(Exception e){Log.w("ShieldRecents","Snapshot unavailable for "+task.taskId+": "+e);}
        });
    }
    private void open(int id) {
        if(busy)return;
        try {tasks.open(id);finish();}catch(Exception e){showError(e);}
    }
    private void remove(int id,int index) {
        if(busy)return;busy=true;closeAll.setEnabled(false);
        worker.execute(()->{
            try {
                if(!tasks.remove(id))throw new IllegalStateException("Android did not remove task "+id);
                for(ActivityManager.RecentTaskInfo remaining:tasks.list())if(remaining.taskId==id)throw new IllegalStateException("Task "+id+" is still present");
                Log.i("ShieldRecents","Confirmed removed task="+id);
                runOnUiThread(()->refresh(index));
            }catch(Exception e){runOnUiThread(()->showError(e));}
        });
    }
    private void closeAll() {
        if(busy)return;busy=true;closeAll.setEnabled(false);
        worker.execute(()->{
            try {
                List<ActivityManager.RecentTaskInfo> before=tasks.list();
                for(ActivityManager.RecentTaskInfo task:before)if(!tasks.remove(task.taskId))throw new IllegalStateException("Android did not remove task "+task.taskId);
                List<ActivityManager.RecentTaskInfo> after=tasks.list();
                for(ActivityManager.RecentTaskInfo remaining:after)for(ActivityManager.RecentTaskInfo old:before)if(remaining.taskId==old.taskId)throw new IllegalStateException("Task "+old.taskId+" is still present");
                Log.i("ShieldRecents","Close all verified: before="+before.size()+" after="+after.size());
                runOnUiThread(()->refresh(0));
            }catch(Exception e){runOnUiThread(()->showError(e));}
        });
    }
    private void showError(Exception e){
        Log.e("ShieldRecents","Task operation failed",e);busy=false;
        if(status!=null)status.setText("Task access failed: "+e.getClass().getSimpleName()+". System integration needs checking.");
        if(closeAll!=null)closeAll.setEnabled(false);
    }
    @Override protected void onDestroy(){generation++;worker.shutdownNow();if(current.get()==this)current.clear();super.onDestroy();}
}
