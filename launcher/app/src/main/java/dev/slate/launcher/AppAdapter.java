package dev.slate.launcher;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    interface Listener {
        void onTap(View view, AppEntry app);

        void onLongPress(View view, AppEntry app);
    }

    private final List<AppEntry> apps = new ArrayList<>();
    private final Listener listener;
    private final Typeface labelFont;

    AppAdapter(Listener listener, Typeface labelFont) {
        this.listener = listener;
        this.labelFont = labelFont;
        setHasStableIds(true);
    }

    void submit(List<AppEntry> next) {
        apps.clear();
        apps.addAll(next);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return apps.get(position).component.flattenToShortString().hashCode();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        AppEntry app = apps.get(position);
        h.icon.setImageDrawable(app.icon);
        h.label.setText(app.label);
        h.label.setTypeface(labelFont);
        h.itemView.setContentDescription(app.label);
        Press.attach(h.icon);
        h.itemView.setOnClickListener(v -> listener.onTap(h.icon, app));
        h.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(v, app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            label = v.findViewById(R.id.label);
        }
    }
}
