package dev.slate.launcher;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/** iPad-style press feedback: icon sinks while touched, springs back on release. */
final class Press {
    private Press() {
    }

    @SuppressLint("ClickableViewAccessibility")
    static void attach(View icon) {
        View target = icon;
        View touchArea = icon.getParent() instanceof View ? (View) icon.getParent() : icon;
        touchArea.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    target.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).setInterpolator(null).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    target.animate().scaleX(1f).scaleY(1f).setDuration(260)
                            .setInterpolator(new OvershootInterpolator(2.5f)).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }
}
