from pathlib import Path
import json, re, sys

root=Path(sys.argv[1])

main=root/'android/app/src/main/java/com/braseiro/forbiddenlands/MainActivity.java'
s=main.read_text()

s=s.replace('import android.os.Bundle;', '''import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;''')

s=s.replace('import com.getcapacitor.BridgeActivity;', '''import com.getcapacitor.BridgeActivity;
import com.getcapacitor.WebViewListener;''')

s=s.replace('public class MainActivity extends BridgeActivity {', '''public class MainActivity extends BridgeActivity {
    private static final String TAG = "BraseiroMainActivity";
    private static final String RECOVERY_PREFS = "webview_renderer_recovery";
    private static final long RENDERER_RECOVERY_WINDOW_MS = 60_000L;
    private static final long RENDERER_HEARTBEAT_INTERVAL_MS = 2_000L;
    private static final long RENDERER_HEARTBEAT_TIMEOUT_MS = 8_000L;

    private final Handler rendererWatchdogHandler = new Handler(Looper.getMainLooper());
    private Runnable rendererWatchdogRunnable;
    private long heartbeatOutstandingAt = 0L;
    private boolean rendererRecoveryScheduled = false;
    private boolean rendererListenerInstalled = false;

    private void destroyRendererAndRecreate(WebView webView, String reason, boolean didCrash, int priorityAtExit) {
        if (rendererRecoveryScheduled) return;
        rendererRecoveryScheduled = true;
        stopRendererWatchdog();

        final SharedPreferences prefs = getSharedPreferences(RECOVERY_PREFS, MODE_PRIVATE);
        final long now = System.currentTimeMillis();
        final long previous = prefs.getLong("last_failure_at", 0L);
        final int count = now - previous <= RENDERER_RECOVERY_WINDOW_MS
            ? prefs.getInt("recent_failure_count", 0) + 1
            : 1;

        SharedPreferences.Editor editor = prefs.edit()
            .putLong("last_failure_at", now)
            .putInt("recent_failure_count", count);

        if ("hang".equals(reason) || count >= 2) {
            editor.putBoolean("force_software_renderer", true);
        }
        editor.apply();

        Log.e(TAG,
            "Recovering WebView renderer. reason=" + reason +
            ", didCrash=" + didCrash +
            ", priorityAtExit=" + priorityAtExit +
            ", recoveryCount=" + count);

        try {
            ViewParent parent = webView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webView);
            }
            webView.stopLoading();
            webView.removeAllViews();
            webView.destroy();
        } catch (Throwable cleanupError) {
            Log.e(TAG, "Renderer cleanup failed", cleanupError);
        }

        getWindow().getDecorView().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                Log.w(TAG, "Recreating Capacitor activity after WebView renderer " + reason + ".");
                recreate();
            }
        }, 250L);
    }

    private void stopRendererWatchdog() {
        if (rendererWatchdogRunnable != null) {
            rendererWatchdogHandler.removeCallbacks(rendererWatchdogRunnable);
        }
        heartbeatOutstandingAt = 0L;
    }

    private void startRendererWatchdog(final WebView webView) {
        stopRendererWatchdog();

        rendererWatchdogRunnable = new Runnable() {
            @Override
            public void run() {
                if (rendererRecoveryScheduled || isFinishing() || isDestroyed()) return;
                if (getBridge() == null || getBridge().getWebView() != webView) return;

                if (!hasWindowFocus()) {
                    heartbeatOutstandingAt = 0L;
                    rendererWatchdogHandler.postDelayed(this, RENDERER_HEARTBEAT_INTERVAL_MS);
                    return;
                }

                final long now = SystemClock.elapsedRealtime();

                if (heartbeatOutstandingAt > 0L) {
                    if (now - heartbeatOutstandingAt >= RENDERER_HEARTBEAT_TIMEOUT_MS) {
                        Log.e(TAG,
                            "WebView renderer heartbeat timed out after " +
                            (now - heartbeatOutstandingAt) + "ms.");
                        destroyRendererAndRecreate(webView, "hang", false, -1);
                        return;
                    }

                    rendererWatchdogHandler.postDelayed(this, RENDERER_HEARTBEAT_INTERVAL_MS);
                    return;
                }

                heartbeatOutstandingAt = now;
                try {
                    webView.evaluateJavascript("void 0", ignored -> {
                        heartbeatOutstandingAt = 0L;
                    });
                } catch (Throwable heartbeatError) {
                    Log.e(TAG, "WebView heartbeat call failed", heartbeatError);
                    destroyRendererAndRecreate(webView, "heartbeat-error", false, -1);
                    return;
                }

                rendererWatchdogHandler.postDelayed(this, RENDERER_HEARTBEAT_INTERVAL_MS);
            }
        };

        rendererWatchdogHandler.postDelayed(rendererWatchdogRunnable, 5_000L);
    }

    private void installRendererRecovery() {
        if (getBridge() == null || getBridge().getWebView() == null) return;

        final WebView currentWebView = getBridge().getWebView();
        final SharedPreferences prefs = getSharedPreferences(RECOVERY_PREFS, MODE_PRIVATE);
        final long now = System.currentTimeMillis();
        final long lastFailureAt = prefs.getLong("last_failure_at", 0L);
        final int recentFailureCount = now - lastFailureAt <= RENDERER_RECOVERY_WINDOW_MS
            ? prefs.getInt("recent_failure_count", 0)
            : 0;

        if (prefs.getBoolean("force_software_renderer", false) || recentFailureCount >= 2) {
            currentWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            Log.w(TAG, "WebView safe renderer mode enabled.");
        }

        if (!rendererListenerInstalled) {
            rendererListenerInstalled = true;
            getBridge().addWebViewListener(new WebViewListener() {
                @Override
                public boolean onRenderProcessGone(WebView webView, RenderProcessGoneDetail detail) {
                    destroyRendererAndRecreate(
                        webView,
                        "crash",
                        detail.didCrash(),
                        detail.rendererPriorityAtExit()
                    );
                    return true;
                }
            });
        }

        startRendererWatchdog(currentWebView);
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().getDecorView().postDelayed(() -> {
            rendererRecoveryScheduled = false;
            if (getBridge() != null && getBridge().getWebView() != null) {
                startRendererWatchdog(getBridge().getWebView());
            }
        }, 1_000L);
    }

    @Override
    public void onPause() {
        stopRendererWatchdog();
        super.onPause();
    }''')

needle='''        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);'''
if needle not in s:
    needle='''        controller.setAppearanceLightStatusBars(false);
                  controller.setAppearanceLightNavigationBars(false);'''
replacement=needle + '''

        installRendererRecovery();'''
s=s.replace(needle,replacement,1)
main.write_text(s)

manifest=root/'android/app/src/main/AndroidManifest.xml'
m=manifest.read_text()
if 'android:hardwareAccelerated="true"' not in m:
    m=m.replace('<application', '<application\n        android:hardwareAccelerated="true"', 1)
manifest.write_text(m)

pkg_path=root/'package.json'
pkg=json.loads(pkg_path.read_text())
pkg['version']='3.3.9'
pkg_path.write_text(json.dumps(pkg, ensure_ascii=False, indent=2)+'\n')

lock_path=root/'package-lock.json'
if lock_path.exists():
    lock=json.loads(lock_path.read_text())
    lock['version']='3.3.9'
    if isinstance(lock.get('packages'),dict) and '' in lock['packages']:
        lock['packages']['']['version']='3.3.9'
    lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2)+'\n')

for file in (root/'scripts').iterdir():
    if not file.is_file():
        continue
    t=file.read_text(errors='ignore')
    t=t.replace('3\\.3\\.6','3\\.3\\.9').replace('3.3.6','3.3.9').replace('303600','303900')
    file.write_text(t)

version=root/'src/version.ts'
v=version.read_text().replace('3.3.6','3.3.9').replace('303600','303900').replace('Runtime 3.3.9 R10','Runtime 3.3.9 R13')
version.write_text(v)

gradle=root/'android/app/build.gradle'
g=gradle.read_text()
g=re.sub(r'versionCode\s+\d+', 'versionCode 303900', g)
g=re.sub(r'versionName\s+"[^"]+"', 'versionName "3.3.9"', g)
g=g.replace('3.3.6','3.3.9').replace('303600','303900')
gradle.write_text(g)

print('R13 hung-renderer watchdog patch applied.')
