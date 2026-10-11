package com.braseiro.dnd5e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;

/**
 * Mesa tática local. O token usa a imagem persistente do herói, NPC ou criatura;
 * iniciais só aparecem quando nenhuma imagem legível está associada.
 */
public final class TacticalBoardView extends View {
    public interface InfoListener { void onInfoRequested(long combatantId,String name); }
    /** G08: a tap only REQUESTS a destination; movement is committed by AppDb
     * after validating initiative, path, opportunity reactions and movement budget. */
    public interface MovementListener { void onGridDestinationRequested(long combatantId,int cellX,int cellY); }

    private final Context context;
    private final AppDb db;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final HashMap<String,Bitmap> tokenCache=new HashMap<String,Bitmap>();
    private long selectedId=-1;
    private float density;
    private InfoListener infoListener;
    private MovementListener movementListener;

    public TacticalBoardView(Context value,AppDb database){
        super(value);context=value.getApplicationContext();db=database;
        density=getResources().getDisplayMetrics().density;
        setMinimumHeight((int)(360*density));
        setBackgroundColor(Color.rgb(25,24,24));
        setFocusable(true);
    }

    public void setInfoListener(InfoListener listener){infoListener=listener;}
    public void setMovementListener(MovementListener listener){movementListener=listener;}

    protected void onDraw(Canvas c){
        super.onDraw(c);
        int w=getWidth(),h=getHeight();
        drawGeneratedScene(c,w,h);
        drawGrid(c,w,h);
        List<JSONObject> tokens=db.listTacticalTokens();
        for(JSONObject token:tokens)drawToken(c,token,w,h);
    }


    private void drawGeneratedScene(Canvas c,int w,int h){
        try{
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            String env=scene.optString("environment","open");
            paint.setStyle(Paint.Style.FILL);
            int bg="forest".equals(env)?Color.rgb(31,54,35):"interior".equals(env)?Color.rgb(54,47,39):"ruins".equals(env)?Color.rgb(60,58,52):"urban".equals(env)?Color.rgb(58,51,43):"wetland".equals(env)?Color.rgb(34,55,54):Color.rgb(47,51,42);
            paint.setColor(bg);c.drawRect(0,0,w,h,paint);
            int cols=Math.max(1,scene.optInt("width_cells",12)),rows=Math.max(1,scene.optInt("height_cells",8));float cw=w/(float)cols,ch=h/(float)rows;
            JSONArray obstacles=new JSONArray(scene.optString("obstacles_json","[]"));
            for(int i=0;i<obstacles.length();i++){
                JSONObject o=obstacles.optJSONObject(i);if(o==null)continue;int x=o.optInt("x"),y=o.optInt("y");String type=o.optString("type");float cx=(x+.5f)*cw,cy=(y+.5f)*ch,rx=Math.max(6,cw*.28f),ry=Math.max(6,ch*.28f);
                paint.setStyle(Paint.Style.FILL);
                if(type.matches("tree|bush|reed"))paint.setColor(Color.rgb(48,88,50));else if(type.matches("water|mud"))paint.setColor(Color.rgb(50,86,96));else if(type.matches("wall|pillar|rock|rubble"))paint.setColor(Color.rgb(105,101,88));else paint.setColor(Color.rgb(112,78,48));
                if(type.matches("wall|crate|table|stall|cart|fallen_log"))c.drawRoundRect(new RectF(cx-rx,cy-ry,cx+rx,cy+ry),4*density,4*density,paint);else c.drawOval(new RectF(cx-rx,cy-ry,cx+rx,cy+ry),paint);
                // G08: visualize only explicitly declared mechanical terrain.
                if(o.optBoolean("difficult_terrain",false)&&!o.optBoolean("blocks_movement",false)){
                    paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(2f,density*2));
                    paint.setColor(Color.rgb(240,182,76));
                    c.drawRect(x*cw+2*density,y*ch+2*density,
                        (x+1)*cw-2*density,(y+1)*ch-2*density,paint);
                    paint.setStyle(Paint.Style.FILL);paint.setTextSize(Math.max(9f,11*density));
                    paint.setTextAlign(Paint.Align.RIGHT);
                    c.drawText("×2",(x+1)*cw-4*density,(y+1)*ch-4*density,paint);
                }
            }
        }catch(Exception ignored){AppLog.ignored("TacticalBoard","Cena procedural indisponível; usando tabuleiro base.",ignored);}
    }

    private void drawGrid(Canvas c,int w,int h){
        // RUN200: same persistent scene grid used by the spell geometry executor.
        JSONObject scene=new ProceduralTacticalSceneGenerator(db).current();
        int cols=Math.max(1,Math.min(100,scene.optInt("width_cells",12)));
        int rows=Math.max(1,Math.min(100,scene.optInt("height_cells",8)));
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,density));
        paint.setColor(Color.rgb(76,72,66));
        for(int i=0;i<=cols;i++){float x=w*i/(float)cols;c.drawLine(x,0,x,h,paint);}
        for(int i=0;i<=rows;i++){float y=h*i/(float)rows;c.drawLine(0,y,w,y,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(220,205,160));
        paint.setTextSize(Math.max(8f,9*density));paint.setTextAlign(Paint.Align.LEFT);
        c.drawText("5 pés / célula • "+cols+"×"+rows,5*density,12*density,paint);
    }

    private void drawToken(Canvas c,JSONObject t,int w,int h){
        float x=(float)t.optDouble("x",.5)*w,y=(float)t.optDouble("y",.5)*h,r=25*density;
        boolean player=t.optInt("is_player",0)==1;
        boolean selected=t.optLong("combatant_id")==selectedId;
        boolean active=t.optInt("active_turn",0)==1;
        boolean neutral=t.optString("resources_json","").contains("\"noncombatant\":true");
        int base=player?Color.rgb(35,90,65):(neutral?Color.rgb(124,92,38):Color.rgb(125,45,38));

        paint.setStyle(Paint.Style.FILL);paint.setColor(0x66000000);c.drawCircle(x+2*density,y+3*density,r+3*density,paint);
        paint.setColor(base);c.drawCircle(x,y,r,paint);

        String tokenUri=t.optString("token_uri","").trim();
        Bitmap bitmap=tokenBitmap(tokenUri);
        if(bitmap!=null){
            int save=c.save();Path clip=new Path();clip.addCircle(x,y,r-2*density,Path.Direction.CW);c.clipPath(clip);
            drawCenterCrop(c,bitmap,new RectF(x-r,y-r,x+r,y+r));
            c.restoreToCount(save);
        }else{
            drawInitials(c,t.optString("name","?"),x,y);
        }

        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((selected||active)?4*density:2*density);
        paint.setColor(active?Color.rgb(245,196,81):(selected?Color.rgb(255,225,130):Color.WHITE));c.drawCircle(x,y,r,paint);

        drawInitiativeBadge(c,t,x,y,r);
        drawInfoBadge(c,x,y);
        drawHpBar(c,t,x,y,r);
        drawStatusMarker(c,t,x,y,r);
        drawNamePlate(c,t.optString("name","?"),x,y,r);
    }

    private void drawCenterCrop(Canvas canvas,Bitmap bitmap,RectF destination){
        int bw=Math.max(1,bitmap.getWidth()),bh=Math.max(1,bitmap.getHeight());
        float dstRatio=destination.width()/Math.max(1f,destination.height());
        float srcRatio=bw/(float)bh;
        Rect source;
        if(srcRatio>dstRatio){
            int sourceWidth=Math.max(1,Math.round(bh*dstRatio));int left=Math.max(0,(bw-sourceWidth)/2);
            source=new Rect(left,0,Math.min(bw,left+sourceWidth),bh);
        }else{
            int sourceHeight=Math.max(1,Math.round(bw/dstRatio));int top=Math.max(0,(bh-sourceHeight)/2);
            source=new Rect(0,top,bw,Math.min(bh,top+sourceHeight));
        }
        paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);canvas.drawBitmap(bitmap,source,destination,paint);
    }

    private void drawInitials(Canvas c,String name,float x,float y){
        String clean=name==null?"?":name.trim();if(clean.length()==0)clean="?";String[] parts=clean.split("\\s+");String label;
        if(parts.length>1)label=(parts[0].substring(0,1)+parts[parts.length-1].substring(0,1)).toUpperCase();
        else label=clean.length()>2?clean.substring(0,2).toUpperCase():clean.toUpperCase();
        paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(14*density);paint.setColor(Color.WHITE);paint.setFakeBoldText(true);c.drawText(label,x,y+5*density,paint);paint.setFakeBoldText(false);
    }

    private void drawInitiativeBadge(Canvas c,JSONObject t,float x,float y,float r){
        float bx=x-r*.78f,by=y-r*.86f,br=10*density;
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(33,35,33));c.drawCircle(bx,by,br,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*density);paint.setColor(Color.rgb(224,190,93));c.drawCircle(bx,by,br,paint);
        paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(10*density);paint.setColor(Color.WHITE);paint.setFakeBoldText(true);c.drawText(String.valueOf(t.optInt("initiative",0)),bx,by+3.5f*density,paint);paint.setFakeBoldText(false);
    }

    private void drawInfoBadge(Canvas c,float x,float y){
        float bx=infoX(x),by=infoY(y),br=9.5f*density;
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(224,190,93));c.drawCircle(bx,by,br,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*density);paint.setColor(Color.rgb(24,36,28));c.drawCircle(bx,by,br,paint);
        paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(12*density);paint.setFakeBoldText(true);paint.setColor(Color.rgb(24,36,28));c.drawText("i",bx,by+4*density,paint);paint.setFakeBoldText(false);
    }

    private void drawHpBar(Canvas c,JSONObject t,float x,float y,float r){
        int max=Math.max(1,t.optInt("max_hp",1)),hp=Math.max(0,Math.min(max,t.optInt("hp",0)));float pct=hp/(float)max;
        RectF bg=new RectF(x-r,y+r+4*density,x+r,y+r+9*density);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(55,55,55));c.drawRoundRect(bg,2*density,2*density,paint);
        RectF fg=new RectF(bg.left,bg.top,bg.left+bg.width()*pct,bg.bottom);paint.setColor(pct>.5f?Color.rgb(65,170,94):(pct>.2f?Color.rgb(214,157,55):Color.rgb(190,55,45)));c.drawRoundRect(fg,2*density,2*density,paint);
    }

    private void drawStatusMarker(Canvas c,JSONObject t,float x,float y,float r){
        String conditions=t.optString("conditions","").trim();if(conditions.isEmpty())return;
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(160,91,190));c.drawCircle(x+r*.78f,y+r*.76f,6*density,paint);
    }

    private void drawNamePlate(Canvas c,String name,float x,float y,float r){
        String clean=name==null?"?":name.trim();if(clean.length()>16)clean=clean.substring(0,15)+"…";
        paint.setTextSize(9.5f*density);paint.setTextAlign(Paint.Align.CENTER);paint.setFakeBoldText(true);
        float width=paint.measureText(clean)+10*density;RectF plate=new RectF(x-width/2,y+r+11*density,x+width/2,y+r+25*density);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xCC161616);c.drawRoundRect(plate,4*density,4*density,paint);
        paint.setColor(Color.WHITE);c.drawText(clean,x,plate.centerY()+3.5f*density,paint);paint.setFakeBoldText(false);
    }

    private Bitmap tokenBitmap(String uri){/*BR_JSON_WRAP*/try{
        if(uri==null||uri.trim().isEmpty())return null;Bitmap cached=tokenCache.get(uri);if(cached!=null&&!cached.isRecycled())return cached;
        try{Bitmap decoded=ImageStorageManager.decodeSampled(context,Uri.parse(uri),320,320);if(decoded!=null){tokenCache.put(uri,decoded);return decoded;}}
        catch(Throwable error){AppLog.w("TacticalBoard","A imagem associada ao token não pôde ser aberta: "+uri,error);}
        return null;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        float px=e.getX(),py=e.getY();List<JSONObject> tokens=db.listTacticalTokens();
        for(JSONObject t:tokens){float x=(float)t.optDouble("x",.5)*getWidth(),y=(float)t.optDouble("y",.5)*getHeight();float dx=px-infoX(x),dy=py-infoY(y),hit=15*density;if(dx*dx+dy*dy<=hit*hit){if(infoListener!=null)infoListener.onInfoRequested(t.optLong("combatant_id"),t.optString("name"));return true;}}
        float nx=px/Math.max(1f,getWidth()),ny=py/Math.max(1f,getHeight());
        if(selectedId>0){
            long mover=selectedId;selectedId=-1;invalidate();
            if(movementListener!=null){
                JSONObject scene=new ProceduralTacticalSceneGenerator(db).current();
                TacticalGridTapRules.Cell target=TacticalGridTapRules.fromTap(getWidth(),getHeight(),
                    scene.optInt("width_cells",0),scene.optInt("height_cells",0),px,py);
                if(target.valid())movementListener.onGridDestinationRequested(mover,target.x,target.y);
            }
            // A touch NEVER performs a direct token-coordinate database write.
            return true;
        }
        double best=.12;long id=-1;for(JSONObject t:tokens){double dx=t.optDouble("x",.5)-nx,dy=t.optDouble("y",.5)-ny,d=Math.sqrt(dx*dx+dy*dy);if(d<best){best=d;id=t.optLong("combatant_id");}}
        selectedId=id;invalidate();return true;
    }

    public void refresh(){tokenCache.clear();db.ensureTacticalTokensForCombat();invalidate();}
    protected void onDetachedFromWindow(){super.onDetachedFromWindow();tokenCache.clear();}
    private float infoX(float tokenX){return tokenX+20*density;}
    private float infoY(float tokenY){return tokenY-20*density;}
}
