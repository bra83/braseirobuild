package com.braseiro.dnd5e;

import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.concurrent.TimeUnit;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test-only wait for the production Heroes view's asynchronous SQLite snapshot
 * and incremental card render. Never bypasses rendering or fakes state.
 */
final class HeroUiAsyncTestAwait {
    private HeroUiAsyncTestAwait() {}

    static void visible(View root, String exactText) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
        while (System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(48, TimeUnit.MILLISECONDS);
            if (contains(root, exactText)) return;
            try { Thread.sleep(25); }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted waiting for Heroes: " + exactText, error);
            }
        }
        throw new AssertionError("Heroes screen did not render production control: " + exactText);
    }

    private static boolean contains(View root, String label) {
        if (root == null) return false;
        if (root instanceof TextView
                && label.contentEquals(((TextView) root).getText())) return true;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i=0; i<group.getChildCount(); i++)
                if (contains(group.getChildAt(i), label)) return true;
        }
        return false;
    }
}
