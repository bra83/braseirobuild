from pathlib import Path
import json, re, sys

root=Path(sys.argv[1])

main=root/'android/app/src/main/java/com/braseiro/forbiddenlands/MainActivity.java'
s=main.read_text()

s=s.replace('import android.os.Bundle;', '''import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import android.webkit.WebViewRenderProcess;
import android.webkit.WebViewRenderProcessClient;''')

s=s.replace('import com.getcapacitor.BridgeActivity;', '''import com.getcapacitor.BridgeActivity;
import com.getcapacitor.WebViewListener;''')

s=s.replace('public class MainActivity extends BridgeActivity {', '''public class MainActivity extends BridgeActivity {
    private static final String TAG = "BraseiroMainActivity";
    private static final String RECOVERY_PREFS = "webview_renderer_recovery";
    private static final long UNRESPONSIVE_GRACE_MS = 3_000L;

    private final Handler rendererHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingUnresponsiveRecovery = null;
    private boolean rendererRecoveryScheduled = false;
    private boolean rendererListenerInstalled = false;

    private void clearBrokenR13WatchdogState() {
        SharedPreferences prefs = getSharedPreferences(RECOVERY_PREFS, MODE_PRIVATE);
        prefs.edit()
            .remove("force_software_renderer")
            .remove("recent_failure_count")
            .remove("last_failure_at")
            .remove("recent_crash_count")
            .remove("last_crash_at")
            .apply();
    }

    private void cancelPendingUnresponsiveRecovery() {
        if (pendingUnresponsiveRecovery != null) {
            rendererHandler.removeCallbacks(pendingUnresponsiveRecovery);
            pendingUnresponsiveRecovery = null;
        }
    }

    private void recoverRenderer(WebView webView, String reason, boolean didCrash, int priorityAtExit) {
        if (rendererRecoveryScheduled) return;
        rendererRecoveryScheduled = true;
        cancelPendingUnresponsiveRecovery();

        Log.e(TAG,
            "Recovering WebView renderer. reason=" + reason +
            ", didCrash=" + didCrash +
            ", priorityAtExit=" + priorityAtExit);

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
                Log.w(TAG, "Recreating Capacitor activity after confirmed WebView renderer failure.");
                recreate();
            }
        }, 250L);
    }

    private void installRendererRecovery() {
        if (getBridge() == null || getBridge().getWebView() == null) return;

        final WebView webView = getBridge().getWebView();

        if (!rendererListenerInstalled) {
            rendererListenerInstalled = true;
            getBridge().addWebViewListener(new WebViewListener() {
                @Override
                public boolean onRenderProcessGone(WebView deadWebView, RenderProcessGoneDetail detail) {
                    recoverRenderer(
                        deadWebView,
                        "process-gone",
                        detail.didCrash(),
                        detail.rendererPriorityAtExit()
                    );
                    return true;
                }
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.setWebViewRenderProcessClient(new WebViewRenderProcessClient() {
                @Override
                public void onRenderProcessUnresponsive(WebView view, WebViewRenderProcess renderer) {
                    if (rendererRecoveryScheduled || pendingUnresponsiveRecovery != null) return;

                    Log.e(TAG, "Android reported WebView renderer unresponsive; waiting grace period.");

                    pendingUnresponsiveRecovery = () -> {
                        pendingUnresponsiveRecovery = null;
                        if (rendererRecoveryScheduled) return;

                        boolean terminated = false;
                        try {
                            if (renderer != null) {
                                terminated = renderer.terminate();
                            }
                        } catch (Throwable terminateError) {
                            Log.e(TAG, "Renderer terminate failed", terminateError);
                        }

                        Log.e(TAG, "Confirmed renderer unresponsive after grace period. terminated=" + terminated);

                        if (!terminated) {
                            recoverRenderer(view, "confirmed-unresponsive", false, -1);
                        }
                    };

                    rendererHandler.postDelayed(pendingUnresponsiveRecovery, UNRESPONSIVE_GRACE_MS);
                }

                @Override
                public void onRenderProcessResponsive(WebView view, WebViewRenderProcess renderer) {
                    if (pendingUnresponsiveRecovery != null) {
                        Log.i(TAG, "WebView renderer became responsive during grace period; recovery cancelled.");
                        cancelPendingUnresponsiveRecovery();
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        rendererRecoveryScheduled = false;
        getWindow().getDecorView().postDelayed(this::installRendererRecovery, 1_000L);
    }

    @Override
    public void onPause() {
        cancelPendingUnresponsiveRecovery();
        super.onPause();
    }''')

needle='''        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);'''
replacement=needle + '''

        clearBrokenR13WatchdogState();
        installRendererRecovery();'''
if needle not in s:
    raise SystemExit('MainActivity system bars anchor missing')
s=s.replace(needle,replacement,1)
main.write_text(s)

manifest=root/'android/app/src/main/AndroidManifest.xml'
m=manifest.read_text()
if 'android:hardwareAccelerated="true"' not in m:
    m=m.replace('<application', '<application\n        android:hardwareAccelerated="true"', 1)
manifest.write_text(m)

pkg_path=root/'package.json'
pkg=json.loads(pkg_path.read_text())
pkg['version']='3.3.10'
pkg_path.write_text(json.dumps(pkg, ensure_ascii=False, indent=2)+'\n')

lock_path=root/'package-lock.json'
if lock_path.exists():
    lock=json.loads(lock_path.read_text())
    lock['version']='3.3.10'
    if isinstance(lock.get('packages'),dict) and '' in lock['packages']:
        lock['packages']['']['version']='3.3.10'
    lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2)+'\n')

for file in (root/'scripts').iterdir():
    if not file.is_file():
        continue
    t=file.read_text(errors='ignore')
    t=t.replace('3\\.3\\.6','3\\.3\\.10').replace('3.3.6','3.3.10').replace('303600','3031000')
    file.write_text(t)

version=root/'src/version.ts'
v=version.read_text().replace('3.3.6','3.3.10').replace('303600','3031000').replace('Runtime 3.3.10 R10','Runtime 3.3.10 R14')
version.write_text(v)

gradle=root/'android/app/build.gradle'
g=gradle.read_text()
g=re.sub(r'versionCode\s+\d+', 'versionCode 3031000', g)
g=re.sub(r'versionName\s+"[^"]+"', 'versionName "3.3.10"', g)
g=g.replace('3.3.6','3.3.10').replace('303600','3031000')
gradle.write_text(g)

print('R14 native unresponsive-renderer callback patch applied; R13 heartbeat removed.')
