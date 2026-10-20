package dev.slate.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class DockOrderTest {

    @Test
    public void parseTrimsSkipsBlanksAndDuplicates() {
        assertEquals(Arrays.asList("a.b", "c.d"), DockOrder.parse(" a.b, ,c.d,a.b,"));
        assertTrue(DockOrder.parse("").isEmpty());
        assertTrue(DockOrder.parse(null).isEmpty());
    }

    @Test
    public void joinRoundTrips() {
        List<String> dock = Arrays.asList("a.b", "c.d");
        assertEquals(dock, DockOrder.parse(DockOrder.join(dock)));
    }

    @Test
    public void addRefusesDuplicatesAndFullDock() {
        List<String> dock = new ArrayList<>(Arrays.asList("a.b"));
        assertFalse(DockOrder.add(dock, "a.b"));
        for (int i = 0; dock.size() < DockOrder.MAX; i++) {
            assertTrue(DockOrder.add(dock, "p.n" + i));
        }
        assertFalse(DockOrder.add(dock, "x.y"));
        assertEquals(DockOrder.MAX, dock.size());
    }

    @Test
    public void moveStaysInBounds() {
        List<String> dock = new ArrayList<>(Arrays.asList("a.a", "b.b", "c.c"));
        assertTrue(DockOrder.move(dock, 0, 1));
        assertEquals(Arrays.asList("b.b", "a.a", "c.c"), dock);
        assertTrue(DockOrder.move(dock, 2, -1));
        assertEquals(Arrays.asList("b.b", "c.c", "a.a"), dock);
        assertFalse(DockOrder.move(dock, 0, -1));
        assertFalse(DockOrder.move(dock, 2, 1));
        assertFalse(DockOrder.move(dock, 5, -1));
    }
}
