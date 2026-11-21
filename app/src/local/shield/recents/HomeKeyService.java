package local.shield.recents;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

/** Filters only HOME key events; pointer, OK, Enter, Esc and Back pass through untouched. */
public final class HomeKeyService extends AccessibilityService {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean down,secondPress,longPress;
    private boolean waitingForSecond;
    private final Runnable single=()->{
        waitingForSecond=false;
        performGlobalAction(GLOBAL_ACTION_HOME);
    };
    private final Runnable hold=()->{
        if(!down)return;
        longPress=true;waitingForSecond=false;handler.removeCallbacks(single);
        try {
            startActivity(new Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }catch(Exception e){
            Log.w("ShieldRecents","Voice assistant unavailable",e);
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    };
    @Override protected void onServiceConnected(){Log.i("ShieldRecents","Home shortcut connected");}
    @Override protected boolean onKeyEvent(KeyEvent event) {
        if(event.getKeyCode()!=KeyEvent.KEYCODE_HOME)return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN) {
            if(event.getRepeatCount()==0) {
                secondPress=waitingForSecond;waitingForSecond=false;
                handler.removeCallbacks(single);down=true;longPress=false;
                handler.postDelayed(hold,ViewConfiguration.getLongPressTimeout());
            }
            return true;
        }
        if(event.getAction()==KeyEvent.ACTION_UP) {
            down=false;handler.removeCallbacks(hold);
            if(event.isCanceled()||longPress){secondPress=false;return true;}
            if(secondPress){
                secondPress=false;waitingForSecond=false;
                startActivity(new Intent(this,RecentsTvActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("toggle",true));
                Log.i("ShieldRecents","Home double press");
            }else{
                waitingForSecond=true;
                handler.postDelayed(single,ViewConfiguration.getDoubleTapTimeout());
            }
            return true;
        }
        return true;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
