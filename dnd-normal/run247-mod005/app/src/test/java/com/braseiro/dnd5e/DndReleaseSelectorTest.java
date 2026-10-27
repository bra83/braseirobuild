package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class DndReleaseSelectorTest {
    private static JSONObject release(String tag, boolean draft, boolean prerelease,
                                      boolean hasApk, boolean hasDigest) throws Exception {
        JSONArray assets=new JSONArray();
        if(hasApk)assets.put(new JSONObject().put("name","BRASEIRO_DND_6.6.17.apk"));
        if(hasDigest)assets.put(new JSONObject().put("name","BRASEIRO_DND_6.6.17.apk.sha256"));
        return new JSONObject().put("tag_name",tag).put("draft",draft)
            .put("prerelease",prerelease).put("assets",assets);
    }
    @Test public void searchesPastThirtyUnrelatedVtts() throws Exception {
        JSONArray releases=new JSONArray();
        for(int i=0;i<55;i++)
            releases.put(release("star-wars-r"+i,false,false,true,true));
        releases.put(release("dnd-6.6.17",false,false,true,true));
        assertEquals("dnd-6.6.17",
            DndReleaseSelector.firstInstallable(releases).getString("tag_name"));
    }
    @Test public void skipsDraftsPrereleasesAndMissingIntegrity() throws Exception {
        JSONArray releases=new JSONArray();
        releases.put(release("dnd-draft",true,false,true,true));
        releases.put(release("dnd-prerelease",false,true,true,true));
        releases.put(release("dnd-no-hash",false,false,true,false));
        releases.put(release("dnd-valid",false,false,true,true));
        assertEquals("dnd-valid",
            DndReleaseSelector.firstInstallable(releases).getString("tag_name"));
    }
    @Test public void rejectsWrongOrEmptyReleases() throws Exception {
        assertNull(DndReleaseSelector.firstInstallable(new JSONArray()));
        JSONArray invalid=new JSONArray().put(release("forbidden-lands-1",false,false,true,true))
            .put(release("dnd-no-apk",false,false,false,true));
        assertNull(DndReleaseSelector.firstInstallable(invalid));
    }
}
