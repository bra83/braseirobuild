package com.braseiro.dnd5e;

import android.content.Context;
import android.view.MotionEvent;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** G08 contract: UI touch must not bypass the SQLite movement executor. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run242TacticalBoardRobolectricTest {
    private static void tap(TacticalBoardView board,float x,float y){
        MotionEvent event=MotionEvent.obtain(0,1,MotionEvent.ACTION_UP,x,y,0);
        try{board.onTouchEvent(event);}finally{event.recycle();}
    }
    @Test public void touchEmitsIntentWithoutChangingPersistedToken()throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String dbName="g08_touch_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long actor=db.addCombatant("G08 actor",20,24,24,14,"",true);
            db.setCombatRunning(true);
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();
            JSONObject first=db.tacticalGridPosition(actor);
            assertFalse(first.toString(),first.has("grid_blocked"));
            // Direct placement remains forbidden even if AI/GM tried to use it.
            db.setTacticalTokenPosition(actor,.9f,.9f);
            JSONObject still=db.tacticalGridPosition(actor);
            assertEquals(first.optInt("cell_x"),still.optInt("cell_x"));
            assertEquals(first.optInt("cell_y"),still.optInt("cell_y"));
            TacticalBoardView board=new TacticalBoardView(context,db);
            board.layout(0,0,480,320);
            final long[] calls={0,0};final int[] destination={-1,-1};
            board.setMovementListener(new TacticalBoardView.MovementListener(){
                public void onGridDestinationRequested(long id,int x,int y){
                    calls[0]++;calls[1]=id;destination[0]=x;destination[1]=y;
                }
            });
            JSONObject actorToken=null;
            for(JSONObject token:db.listTacticalTokens())if(token.optLong("combatant_id")==actor)actorToken=token;
            assertNotNull(actorToken);
            tap(board,(float)(actorToken.optDouble("x")*480d),
                (float)(actorToken.optDouble("y")*320d));
            tap(board,420f,275f);
            assertEquals(1,calls[0]);assertEquals(actor,calls[1]);
            assertEquals(10,destination[0]);assertEquals(6,destination[1]);
            JSONObject after=db.tacticalGridPosition(actor);
            assertEquals(first.optInt("cell_x"),after.optInt("cell_x"));
            assertEquals(first.optInt("cell_y"),after.optInt("cell_y"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
