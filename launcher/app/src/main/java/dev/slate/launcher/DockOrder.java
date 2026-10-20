package dev.slate.launcher;

import java.util.ArrayList;
import java.util.List;

/** The dock as an ordered list of package names, stored as one comma-separated preference. */
final class DockOrder {
    static final int MAX = 6;

    private DockOrder() {
    }

    static List<String> parse(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (!p.isEmpty() && !out.contains(p)) {
                out.add(p);
            }
        }
        return out;
    }

    static String join(List<String> dock) {
        return String.join(",", dock);
    }

    /** Appends pkg unless it is already docked or the dock is full. Returns whether it changed. */
    static boolean add(List<String> dock, String pkg) {
        if (dock.contains(pkg) || dock.size() >= MAX) {
            return false;
        }
        dock.add(pkg);
        return true;
    }

    /** Moves the app at index by delta (-1 left, +1 right). Returns whether it moved. */
    static boolean move(List<String> dock, int index, int delta) {
        int target = index + delta;
        if (index < 0 || index >= dock.size() || target < 0 || target >= dock.size()) {
            return false;
        }
        dock.add(target, dock.remove(index));
        return true;
    }
}
