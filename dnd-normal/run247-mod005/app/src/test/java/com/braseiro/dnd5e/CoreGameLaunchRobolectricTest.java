package com.braseiro.dnd5e;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Looper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** The D&D base table must open into usable core-game navigation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class CoreGameLaunchRobolectricTest {
    @Test public void baseCampaignAndCorePlayScreensOpenFromTheMainActivity() {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try {
            MainActivity activity=controller.get();
            View root=activity.findViewById(android.R.id.content);
            assertNotNull(root);
            assertTrue("Aventura navigation missing",hasContentDescription(root,"Abrir Aventura"));
            assertTrue("Combate navigation missing",hasContentDescription(root,"Abrir Combate"));
            assertTrue("Heróis navigation missing",hasContentDescription(root,"Abrir Heróis"));
            assertTrue("Dados navigation missing",hasContentDescription(root,"Abrir Dados"));
            assertTrue("Mapa navigation missing",hasContentDescription(root,"Abrir Mapa"));
            assertTrue("campaign screen did not render",hasText(root,"ESTADO DA MESA"));
            assertTrue("campaign start action missing",hasText(root,"INICIAR AVENTURA"));
            assertTrue("hero screen did not open",openCoreScreen(root,"Abrir Heróis","Heróis"));
            assertTrue("combat screen did not open",openCoreScreen(root,"Abrir Combate","Combate"));
            assertTrue("dice screen did not open",openCoreScreen(root,"Abrir Dados","Dados"));
            assertTrue("map screen did not open",openCoreScreen(root,"Abrir Mapa","MUNDO DO MAPA"));
        } finally {
            controller.pause().stop().destroy();
        }
    }

    private static boolean hasContentDescription(View view,String expected) {
        if(view.getContentDescription()!=null&&expected.contentEquals(view.getContentDescription()))return true;
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)if(hasContentDescription(group.getChildAt(i),expected))return true;
        }
        return false;
    }
    private static boolean openCoreScreen(View root,String buttonDescription,String expectedHeading) {
        View button=findContentDescription(root,buttonDescription);
        if(button==null||!button.performClick())return false;
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        return hasText(root,expectedHeading);
    }
    private static View findContentDescription(View view,String expected) {
        if(view.getContentDescription()!=null&&expected.contentEquals(view.getContentDescription()))return view;
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++){
                View found=findContentDescription(group.getChildAt(i),expected);
                if(found!=null)return found;
            }
        }
        return null;
    }
    private static boolean hasText(View view,String expected) {
        if(view instanceof TextView&&expected.contentEquals(((TextView)view).getText()))return true;
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),expected))return true;
        }
        return false;
    }
}
