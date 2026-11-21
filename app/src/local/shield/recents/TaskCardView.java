/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright 2026 VirulentArc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package local.shield.recents;

import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Hosts the extracted stock Shield card XML with a current-Android task adapter. */
public final class TaskCardView extends LinearLayout {
    private boolean dismiss;
    private Runnable openAction, removeAction;
    public TaskCardView(Context c, AttributeSet a) { super(c,a); }
    // Adapted from the corresponding stock Shield TaskCardView methods. Keep the
    // extracted banner/icon dimensions rather than stretching an app icon into a screenshot.
    public void setAsBannerView(Drawable drawable) {
        ImageView image=findViewById(R.id.card_view_banner_icon);
        LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)image.getLayoutParams();
        params.width=getResources().getDimensionPixelSize(R.dimen.recents_tv_banner_width);
        params.height=getResources().getDimensionPixelSize(R.dimen.recents_tv_banner_height);
        image.setLayoutParams(params);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImageDrawable(drawable);
    }
    public void setAsIconView(Drawable drawable) {
        ImageView image=findViewById(R.id.card_view_banner_icon);
        LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)image.getLayoutParams();
        params.width=getResources().getDimensionPixelSize(R.dimen.recents_tv_fallback_icon_width);
        params.height=getResources().getDimensionPixelSize(R.dimen.recents_tv_fallback_icon_height);
        image.setLayoutParams(params);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImageDrawable(drawable);
    }
    public void setAsScreenShotView(Bitmap bitmap) {
        ImageView image=findViewById(R.id.card_view_banner_icon);
        LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)image.getLayoutParams();
        params.width=-1;params.height=-1;
        image.setLayoutParams(params);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImageBitmap(bitmap);
    }
    public void bind(Runnable open, Runnable remove) {
        openAction=open; removeAction=remove;
        setOnClickListener(v -> { if(dismiss) removeAction.run(); else openAction.run(); });
        setOnFocusChangeListener((v,focused) -> {
            if(!focused) setDismiss(false);
            animate().scaleX(focused?1.08f:1f).scaleY(focused?1.08f:1f).setDuration(150).start();
            setElevation(focused?20:6);
            findViewById(R.id.dismiss_icon).setAlpha(focused?0.5f:0f);
        });
    }
    private void setDismiss(boolean value) {
        dismiss=value;
        findViewById(R.id.dismiss_icon).setAlpha(value?1f:0.5f);
        findViewById(R.id.card_dismiss_text).setAlpha(value?1f:0f);
        findViewById(R.id.card_view_thumbnail).setAlpha(value?0.65f:1f);
    }
    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if(key==KeyEvent.KEYCODE_DPAD_DOWN && !dismiss) {setDismiss(true);return true;}
        if(key==KeyEvent.KEYCODE_DPAD_UP && dismiss) {setDismiss(false);return true;}
        if(key==KeyEvent.KEYCODE_DEL || key==KeyEvent.KEYCODE_FORWARD_DEL) {
            if(event.getRepeatCount()==0) removeAction.run(); return true;
        }
        return super.onKeyDown(key,event);
    }
}
