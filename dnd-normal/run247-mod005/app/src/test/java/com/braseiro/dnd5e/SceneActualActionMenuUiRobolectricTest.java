package com.braseiro.dnd5e;

import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** The actual MainActivity menu must expose 4 when replaying the A57's one-card save. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class SceneActualActionMenuUiRobolectricTest {
    @Test public void onDeviceOldOneChoiceSaveDrawsFourItemNativeTrigger() throws Exception {
        String narration=SceneScreenshotFourActionsRegressionTest.LIVE_SCENE;
        JSONArray saved=new JSONArray().put(StoryMomentumAction.option(narration));
        JSONArray visible=StoryMomentumAction.ensureVisible(saved,narration);
        assertEquals("Restored story must expose 4",4,visible.length());

        MainActivity activity=Robolectric.buildActivity(MainActivity.class).get();
        LinearLayout drawer=new LinearLayout(activity);
        drawer.setOrientation(LinearLayout.VERTICAL);
        activity.setContentView(drawer);
        try{
            Method add=MainActivity.class.getDeclaredMethod("addSceneActionDropdown",
                    LinearLayout.class,JSONArray.class,boolean.class,boolean.class);
            add.setAccessible(true);
            add.invoke(activity,drawer,visible,false,false);
            assertEquals("Exactly one action launcher in drawer",1,drawer.getChildCount());
            View child=drawer.getChildAt(0);
            assertTrue("A clickable Android Button must be rendered",child instanceof Button);
            Button trigger=(Button)child;
            assertTrue("Must be enabled while no roll is pending",trigger.isEnabled());
            assertEquals("✦ AÇÕES SUGERIDAS (4) ▾",trigger.getText().toString());
            assertTrue(trigger.getContentDescription().toString().contains("4 escolhas"));

            // All four native vector icons, used inside the actual dropdown.
            for(int icon:new int[]{R.drawable.br_scene_eye,R.drawable.br_scene_talk,
                    R.drawable.br_scene_people,R.drawable.br_scene_clock})
                assertNotNull("Missing native vector resource "+icon,activity.getDrawable(icon));

            // The menu cannot bypass scene ticket checks with obsolete options.
            JSONObject old=visible.getJSONObject(1);
            assertTrue(SceneActionEvidence.ticketMatches(old,narration));
            assertFalse(SceneActionEvidence.ticketMatches(old,
                    "Urzmaktok já deixou o Jardim. Kvothe encontra a estufa vazia."));
        } finally {
            activity.finish();
        }
    }
}
