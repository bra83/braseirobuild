package com.braseiro.dnd5e;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Native interactive illustrated map surface for Strixhaven. */
public final class StrixhavenCampusView extends View {
    public interface Listener { void onHotspotTapped(StrixhavenLocationCatalog.Hotspot hotspot); void onFreePointTapped(float x,float y); }

    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint routePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tokenPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelTextPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix imageMatrix=new Matrix();
    private final Matrix inverseMatrix=new Matrix();
    private final ScaleGestureDetector scaleDetector;
    private Bitmap bitmap;
    private String pendingMapAsset = "";
    private List<StrixhavenLocationCatalog.Hotspot> hotspots=new ArrayList<StrixhavenLocationCatalog.Hotspot>();
    private Listener listener;
    private float tokenX=.5f,tokenY=.6f;
    private float selectedX=-1f,selectedY=-1f;
    private float userScale=1f,panX=0f,panY=0f;
    private float lastX,lastY;
    private boolean panning=false;
    private int gold=Color.rgb(221,176,74);
    private int dark=Color.rgb(31,24,17);

    public StrixhavenCampusView(Context context){
        super(context);setLayerType(View.LAYER_TYPE_HARDWARE,null);setFocusable(true);
        routePaint.setColor(Color.argb(220,245,194,69));routePaint.setStyle(Paint.Style.STROKE);routePaint.setStrokeWidth(dp(3));routePaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setColor(gold);ringPaint.setStyle(Paint.Style.STROKE);ringPaint.setStrokeWidth(dp(2));
        tokenPaint.setColor(gold);tokenPaint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(Color.argb(220,24,18,13));labelPaint.setStyle(Paint.Style.FILL);
        labelTextPaint.setColor(Color.rgb(250,238,204));labelTextPaint.setTextSize(dp(11));labelTextPaint.setFakeBoldText(true);labelTextPaint.setTextAlign(Paint.Align.CENTER);
        scaleDetector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector detector){userScale*=detector.getScaleFactor();userScale=Math.max(minUserScale(),Math.min(4f,userScale));invalidate();return true;}
        });
    }

    public void setListener(Listener listener){this.listener=listener;}
    public void setMapAsset(String assetPath,List<StrixhavenLocationCatalog.Hotspot> hotspots,float tokenX,float tokenY){
        // Old code decoded and recycled full-resolution bitmaps on the main thread
        // for EVERY redraw/recreated view, causing recurrent Android ANRs.
        pendingMapAsset=assetPath==null?"":assetPath;
        bitmap=null;
        this.hotspots=hotspots==null?new ArrayList<StrixhavenLocationCatalog.Hotspot>():new ArrayList<StrixhavenLocationCatalog.Hotspot>(hotspots);
        this.tokenX=clamp01(tokenX);this.tokenY=clamp01(tokenY);selectedX=-1f;selectedY=-1f;userScale=1f;panX=panY=0f;
        invalidate();
        StrixhavenMapBitmapCache.request(getContext(),pendingMapAsset,this);
    }

    // Called only on the UI handler, after an asynchronous decode or a cache hit.
    void acceptDecodedMap(String assetPath,Bitmap ready){
        if(!pendingMapAsset.equals(assetPath))return;
        if(ready!=null&&!ready.isRecycled())bitmap=ready;
        invalidate();
    }
    public void setTokenPosition(float x,float y){tokenX=clamp01(x);tokenY=clamp01(y);invalidate();}
    public void select(float x,float y){selectedX=clamp01(x);selectedY=clamp01(y);invalidate();}
    public void clearSelection(){selectedX=selectedY=-1f;invalidate();}
    public void resetCamera(){userScale=1f;panX=panY=0f;invalidate();}
    public void animateTokenTo(final float x,final float y){
        final float sx=tokenX,sy=tokenY,tx=clamp01(x),ty=clamp01(y);ValueAnimator a=ValueAnimator.ofFloat(0f,1f);a.setDuration(520L);a.addUpdateListener(animation->{float t=(Float)animation.getAnimatedValue();tokenX=sx+(tx-sx)*t;tokenY=sy+(ty-sy)*t;invalidate();});a.start();
    }

    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawColor(Color.rgb(18,14,11));if(bitmap==null){int prevColor=labelTextPaint.getColor();labelTextPaint.setColor(Color.rgb(230,207,160));canvas.drawText("Preparando mapa de Strixhaven…",getWidth()/2f,getHeight()/2f,labelTextPaint);labelTextPaint.setColor(prevColor);return;}computeMatrix();canvas.drawBitmap(bitmap,imageMatrix,imagePaint);
        if(selectedX>=0f){float[] a=mapPoint(tokenX,tokenY),b=mapPoint(selectedX,selectedY);canvas.drawLine(a[0],a[1],b[0],b[1],routePaint);}
        for(StrixhavenLocationCatalog.Hotspot h:hotspots)drawHotspot(canvas,h);
        drawMeeple(canvas,tokenX,tokenY);
    }

    private void computeMatrix(){
        if(bitmap==null||getWidth()==0||getHeight()==0)return;float sx=getWidth()/(float)bitmap.getWidth(),sy=getHeight()/(float)bitmap.getHeight();float fit=Math.min(sx,sy),cover=Math.max(sx,sy);boolean landscape=bitmap.getWidth()>bitmap.getHeight();float base=landscape?cover:fit;userScale=Math.max(landscape?fit/base:1f,userScale);float scale=base*userScale;float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;clampPan(w,h);float dx=(getWidth()-w)/2f+panX,dy=(getHeight()-h)/2f+panY;imageMatrix.reset();imageMatrix.postScale(scale,scale);imageMatrix.postTranslate(dx,dy);imageMatrix.invert(inverseMatrix);
    }
    private float[] mapPoint(float nx,float ny){float[] pts={nx*bitmap.getWidth(),ny*bitmap.getHeight()};imageMatrix.mapPoints(pts);return pts;}
    private void drawHotspot(Canvas c,StrixhavenLocationCatalog.Hotspot h){
        float[] p=mapPoint(h.x,h.y);float r=dp(h.college?8:6);tokenPaint.setColor(h.college?Color.rgb(255,203,78):Color.rgb(240,219,159));c.drawCircle(p[0],p[1],r,tokenPaint);ringPaint.setColor(Color.argb(240,70,45,18));ringPaint.setStrokeWidth(dp(2));c.drawCircle(p[0],p[1],r+dp(3),ringPaint);
        String label=h.label;float tw=labelTextPaint.measureText(label);float pad=dp(7);float top=p[1]+r+dp(5);RectF box=new RectF(p[0]-tw/2-pad,top,p[0]+tw/2+pad,top+dp(22));c.drawRoundRect(box,dp(9),dp(9),labelPaint);ringPaint.setColor(Color.argb(210,221,176,74));ringPaint.setStrokeWidth(dp(1));c.drawRoundRect(box,dp(9),dp(9),ringPaint);c.drawText(label,p[0],top+dp(15),labelTextPaint);
    }
    private void drawMeeple(Canvas c,float nx,float ny){
        float[] p=mapPoint(nx,ny);float glow=dp(18);tokenPaint.setColor(Color.argb(90,255,194,55));c.drawCircle(p[0],p[1],glow,tokenPaint);tokenPaint.setColor(dark);c.drawCircle(p[0],p[1],dp(12),tokenPaint);ringPaint.setColor(gold);ringPaint.setStrokeWidth(dp(3));c.drawCircle(p[0],p[1],dp(12),ringPaint);tokenPaint.setColor(gold);c.drawCircle(p[0],p[1]-dp(4),dp(3.4f),tokenPaint);RectF body=new RectF(p[0]-dp(5),p[1],p[0]+dp(5),p[1]+dp(7));c.drawRoundRect(body,dp(4),dp(4),tokenPaint);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        scaleDetector.onTouchEvent(e);switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:lastX=e.getX();lastY=e.getY();panning=false;return true;
            case MotionEvent.ACTION_MOVE:if(!scaleDetector.isInProgress()){float dx=e.getX()-lastX,dy=e.getY()-lastY;if(Math.abs(dx)+Math.abs(dy)>dp(3))panning=true;if(canPan()){panX+=dx;panY+=dy;invalidate();}lastX=e.getX();lastY=e.getY();}return true;
            case MotionEvent.ACTION_UP:if(!panning&&!scaleDetector.isInProgress())handleTap(e.getX(),e.getY());return true;
        }return true;
    }
    private void handleTap(float vx,float vy){
        if(bitmap==null)return;computeMatrix();float[] pt={vx,vy};inverseMatrix.mapPoints(pt);float nx=pt[0]/bitmap.getWidth(),ny=pt[1]/bitmap.getHeight();if(nx<0f||nx>1f||ny<0f||ny>1f)return;StrixhavenLocationCatalog.Hotspot best=null;double bestD=Double.MAX_VALUE;for(StrixhavenLocationCatalog.Hotspot h:hotspots){double dx=h.x-nx,dy=h.y-ny,d=dx*dx+dy*dy;if(d<bestD){bestD=d;best=h;}}float tolerance=.085f/userScale;if(best!=null&&bestD<=tolerance*tolerance){selectedX=best.x;selectedY=best.y;invalidate();if(listener!=null)listener.onHotspotTapped(best);return;}selectedX=clamp01(nx);selectedY=clamp01(ny);invalidate();if(listener!=null)listener.onFreePointTapped(selectedX,selectedY);
    }
    @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();bitmap=null;/* Cache owns shared decoded maps; never recycle a bitmap held by another view. */}

    private float minUserScale(){
        if(bitmap==null||getWidth()==0||getHeight()==0)return 1f;
        float sx=getWidth()/(float)bitmap.getWidth(),sy=getHeight()/(float)bitmap.getHeight();float fit=Math.min(sx,sy),cover=Math.max(sx,sy);return bitmap.getWidth()>bitmap.getHeight()?Math.max(.20f,fit/cover):1f;
    }
    private boolean canPan(){
        if(bitmap==null||getWidth()==0||getHeight()==0)return false;float sx=getWidth()/(float)bitmap.getWidth(),sy=getHeight()/(float)bitmap.getHeight();float fit=Math.min(sx,sy),cover=Math.max(sx,sy);float base=bitmap.getWidth()>bitmap.getHeight()?cover:fit;float scale=base*userScale;return bitmap.getWidth()*scale>getWidth()+1f||bitmap.getHeight()*scale>getHeight()+1f;
    }
    private void clampPan(float renderedW,float renderedH){
        if(renderedW<=getWidth())panX=0f;else{float max=(renderedW-getWidth())/2f;panX=Math.max(-max,Math.min(max,panX));}
        if(renderedH<=getHeight())panY=0f;else{float max=(renderedH-getHeight())/2f;panY=Math.max(-max,Math.min(max,panY));}
    }
    private float clamp01(float v){return Math.max(0f,Math.min(1f,v));}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
}
