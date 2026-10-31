package com.braseiro.dnd5e;

import android.app.Application;

/** Loads only APK-signer-verified local D&D 2024 data; other editions/worlds stay untouched. */
public final class BraseiroDndApplication extends Application {
    @Override public void onCreate(){
        DndStartupTrace.begin();
        super.onCreate();
        DndStartupTrace.mark("ANDROID_APPLICATION_READY");
        DndUiStallMonitor.start(this);
        new Dnd2024RulePackManager(this).restore();
        DndStartupTrace.mark("RULEPACK_RESTORED");
    }
}
