package dev.slate.launcher;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements AppAdapter.Listener {

    private static final String PREFS = "slate";
    private static final String KEY_DOCK = "dock";
    private static final String[] WALLPAPER_NAMES = {"wallpaper.jpg", "wallpaper.png"};

    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, AppEntry> byPackage = new HashMap<>();

    private LauncherApps launcherApps;
    private SharedPreferences prefs;
    private AppAdapter adapter;
    private RecyclerView grid;
    private GridLayoutManager layoutManager;
    private LinearLayout dock;
    private TextView greeting;
    private TextView wifiPill;
    private ImageView wallpaper;
    private long wallpaperStamp = -1;
    private Typeface regular;

    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            loadApps();
        }
    };

    private final BroadcastReceiver timeTick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            updateGreeting();
        }
    };

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onCapabilitiesChanged(Network n, NetworkCapabilities caps) {
            main.post(MainActivity.this::updateWifi);
        }

        @Override
        public void onLost(Network n) {
            main.post(MainActivity.this::updateWifi);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        launcherApps = getSystemService(LauncherApps.class);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        regular = font(400);
        Typeface thin = font(200);
        Typeface light = font(300);

        View root = findViewById(R.id.root);
        View content = findViewById(R.id.content);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            content.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });

        greeting = findViewById(R.id.greeting);
        greeting.setTypeface(light);
        ((TextView) findViewById(R.id.clock)).setTypeface(thin);
        ((TextView) findViewById(R.id.date)).setTypeface(light);

        wifiPill = findViewById(R.id.pill_wifi);
        wifiPill.setTypeface(regular);
        wifiPill.setOnClickListener(v -> startSafely(new Intent(Settings.Panel.ACTION_WIFI)));
        TextView settingsPill = findViewById(R.id.pill_settings);
        settingsPill.setTypeface(regular);
        settingsPill.setOnClickListener(v -> startSafely(new Intent(Settings.ACTION_SETTINGS)));

        wallpaper = findViewById(R.id.wallpaper);
        // Dim custom wallpapers so the white clock, labels and glass stay readable on bright images.
        wallpaper.setColorFilter(Color.argb(110, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        dock = findViewById(R.id.dock);

        grid = findViewById(R.id.grid);
        layoutManager = new GridLayoutManager(this, spanCount());
        grid.setLayoutManager(layoutManager);
        grid.setItemAnimator(null);
        adapter = new AppAdapter(this, regular);
        grid.setAdapter(adapter);

        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pkgFilter.addDataScheme("package");
        registerReceiver(packagesChanged, pkgFilter);

        IntentFilter timeFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeTick, timeFilter);

        getSystemService(ConnectivityManager.class).registerDefaultNetworkCallback(networkCallback);

        updateGreeting();
        updateWifi();
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
        loadWallpaper();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home: back to the top of the grid.
        grid.smoothScrollToPosition(0);
    }

    @Override
    public void onBackPressed() {
        grid.smoothScrollToPosition(0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        layoutManager.setSpanCount(spanCount());
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        unregisterReceiver(timeTick);
        getSystemService(ConnectivityManager.class).unregisterNetworkCallback(networkCallback);
        loader.shutdownNow();
        super.onDestroy();
    }

    // ---- apps ----

    private void loadApps() {
        loader.execute(() -> {
            List<AppEntry> list = new ArrayList<>();
            for (LauncherActivityInfo info : launcherApps.getActivityList(null, Process.myUserHandle())) {
                if (info.getComponentName().getPackageName().equals(getPackageName())) {
                    continue;
                }
                boolean system = (info.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                list.add(new AppEntry(info.getComponentName(), info.getUser(),
                        info.getLabel().toString(), info.getBadgedIcon(0), system));
            }
            list.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
            main.post(() -> {
                byPackage.clear();
                for (AppEntry e : list) {
                    byPackage.putIfAbsent(e.pkg(), e);
                }
                adapter.submit(list);
                renderDock();
            });
        });
    }

    @Override
    public void onTap(View icon, AppEntry app) {
        Rect bounds = new Rect();
        icon.getGlobalVisibleRect(bounds);
        Bundle opts = ActivityOptions.makeScaleUpAnimation(icon, 0, 0, icon.getWidth(), icon.getHeight()).toBundle();
        try {
            launcherApps.startMainActivity(app.component, app.user, bounds, opts);
        } catch (RuntimeException e) {
            loadApps(); // app vanished or got disabled; refresh
        }
    }

    @Override
    public void onLongPress(View anchor, AppEntry app) {
        List<String> docked = dockPackages();
        int index = docked.indexOf(app.pkg());
        PopupMenu menu = new PopupMenu(this, anchor);
        if (index < 0) {
            menu.getMenu().add(0, 1, 0, "Add to dock").setEnabled(docked.size() < DockOrder.MAX);
        } else {
            menu.getMenu().add(0, 2, 0, "Remove from dock");
            if (index > 0) {
                menu.getMenu().add(0, 3, 1, "Move left");
            }
            if (index < docked.size() - 1) {
                menu.getMenu().add(0, 4, 2, "Move right");
            }
        }
        menu.getMenu().add(0, 5, 3, "App info");
        if (!app.system) {
            menu.getMenu().add(0, 6, 4, "Uninstall");
        }
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    DockOrder.add(docked, app.pkg());
                    break;
                case 2:
                    docked.remove(index);
                    break;
                case 3:
                    DockOrder.move(docked, index, -1);
                    break;
                case 4:
                    DockOrder.move(docked, index, 1);
                    break;
                case 5:
                    startSafely(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", app.pkg(), null)));
                    return true;
                case 6:
                    startSafely(new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.pkg(), null)));
                    return true;
                default:
                    return false;
            }
            prefs.edit().putString(KEY_DOCK, DockOrder.join(docked)).apply();
            renderDock();
            return true;
        });
        menu.show();
    }

    // ---- dock ----

    private List<String> dockPackages() {
        return DockOrder.parse(prefs.getString(KEY_DOCK, getString(R.string.default_dock)));
    }

    private void renderDock() {
        dock.removeAllViews();
        int size = dp(72);
        int gap = dp(10);
        for (String pkg : dockPackages()) {
            AppEntry app = byPackage.get(pkg);
            if (app == null) {
                continue; // not installed or disabled; keep it in prefs so it returns if re-enabled
            }
            FrameLayoutCell cell = new FrameLayoutCell(this);
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            icon.setContentDescription(app.label);
            cell.addView(icon, new ViewGroup.LayoutParams(size, size));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginStart(gap);
            lp.setMarginEnd(gap);
            Press.attach(icon);
            cell.setOnClickListener(v -> onTap(icon, app));
            cell.setOnLongClickListener(v -> {
                onLongPress(v, app);
                return true;
            });
            dock.addView(cell, lp);
        }
        dock.setVisibility(dock.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    /** Plain container so Press can scale the icon while the cell receives touches. */
    private static final class FrameLayoutCell extends android.widget.FrameLayout {
        FrameLayoutCell(Context c) {
            super(c);
            setClickable(true);
            setLongClickable(true);
        }
    }

    // ---- top bar ----

    private void updateGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String text;
        if (h >= 5 && h < 12) {
            text = "Good morning";
        } else if (h >= 12 && h < 17) {
            text = "Good afternoon";
        } else if (h >= 17 && h < 22) {
            text = "Good evening";
        } else {
            text = "Good night";
        }
        greeting.setText(text);
    }

    private void updateWifi() {
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            wifiPill.setText(caps == null ? "Offline" : "Mobile data");
            return;
        }
        WifiInfo info = getSystemService(WifiManager.class).getConnectionInfo();
        String ssid = info == null ? null : info.getSSID();
        if (ssid == null || ssid.equals(WifiManager.UNKNOWN_SSID) || ssid.isEmpty()) {
            wifiPill.setText("Wi-Fi");
        } else {
            wifiPill.setText(String.format(Locale.ROOT, "Wi-Fi  ·  %s", ssid.replace("\"", "")));
        }
    }

    // ---- wallpaper ----

    private void loadWallpaper() {
        File dir = getExternalFilesDir(null);
        File found = null;
        if (dir != null) {
            for (String name : WALLPAPER_NAMES) {
                File f = new File(dir, name);
                if (f.isFile()) {
                    found = f;
                    break;
                }
            }
        }
        long stamp = found == null ? 0 : found.lastModified();
        if (stamp == wallpaperStamp) {
            return;
        }
        wallpaperStamp = stamp;
        if (found == null) {
            wallpaper.setImageDrawable(null);
            wallpaper.setVisibility(View.GONE);
            return;
        }
        File file = found;
        int maxSide = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        loader.execute(() -> {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (opts.inSampleSize * 2) >= maxSide) {
                opts.inSampleSize *= 2;
            }
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bmp = BitmapFactory.decodeFile(file.getPath(), opts);
            main.post(() -> {
                wallpaper.setImageBitmap(bmp);
                wallpaper.setVisibility(bmp == null ? View.GONE : View.VISIBLE);
            });
        });
    }

    // ---- helpers ----

    private Typeface font(int weight) {
        try {
            return new Typeface.Builder(getAssets(), "fonts/Outfit.ttf")
                    .setFontVariationSettings("'wght' " + weight)
                    .build();
        } catch (RuntimeException e) {
            return Typeface.DEFAULT;
        }
    }

    private int spanCount() {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        return Math.max(4, widthDp / 140);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void startSafely(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            if (!Settings.ACTION_SETTINGS.equals(i.getAction())) {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }
    }
}
