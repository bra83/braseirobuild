package com.braseiro.dnd5e;

import android.app.Application;

/** Loads only APK-signer-verified local D&D 2024 data; other editions/worlds stay untouched. */
public final class BraseiroDndApplication extends Application {
    private static volatile BraseiroDndApplication instance;

    /** Asset access for read-only feature packs; never holds an Activity. */
    public static Application currentContext(){ return instance; }

    @Override public void onCreate(){
        DndStartupTrace.begin();
        super.onCreate();
        instance=this;
        DndStartupTrace.mark("ANDROID_APPLICATION_READY");
        new Dnd2024RulePackManager(this).restore();
        DndStartupTrace.mark("RULEPACK_RESTORED");
    }
}
