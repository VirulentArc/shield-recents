package local.shield.recents.test;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
public class FixtureActivity extends Activity {
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        TextView text=new TextView(this);text.setText(getComponentName().getShortClassName());
        text.setTextSize(32);text.setGravity(Gravity.CENTER);setContentView(text);
    }
}
