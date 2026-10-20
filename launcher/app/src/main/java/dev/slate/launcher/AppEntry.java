package dev.slate.launcher;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

/** One launchable activity shown in the grid or the dock. */
final class AppEntry {
    final ComponentName component;
    final UserHandle user;
    final String label;
    final Drawable icon;
    final boolean system;

    AppEntry(ComponentName component, UserHandle user, String label, Drawable icon, boolean system) {
        this.component = component;
        this.user = user;
        this.label = label;
        this.icon = icon;
        this.system = system;
    }

    String pkg() {
        return component.getPackageName();
    }
}
