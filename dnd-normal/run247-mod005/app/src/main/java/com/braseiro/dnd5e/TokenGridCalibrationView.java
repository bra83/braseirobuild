package com.braseiro.dnd5e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Visual calibrator: drag the four corner handles so the rectangle encloses the token grid. */
public class TokenGridCalibrationView extends View {
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint linePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap bitmap;
    private final RectF imageRect=new RectF();
    private float left=.065f, top=.055f, right=.935f, bottom=.945f;
    private int columns=5, rows=9, active=-1;

    public TokenGridCalibrationView(Context context){super(context);linePaint.setColor(Color.argb(230,255,193,64));linePaint.setStyle(Paint.Style.STROKE);linePaint.setStrokeWidth(dp(2));handlePaint.setColor(Color.rgb(230,72,58));handlePaint.setStyle(Paint.Style.FILL);setBackgroundColor(Color.rgb(12,12,12));}
    public void setBitmap(Bitmap value){bitmap=value;invalidate();}
    public void setGrid(int c,int r){columns=Math.max(1,c);rows=Math.max(1,r);invalidate();}
    public void setBounds(double l,double t,double r,double b){left=clamp((float)l);top=clamp((float)t);right=clamp((float)r);bottom=clamp((float)b);normalize();invalidate();}
    public float getGridLeft(){return left;} public float getGridTop(){return top;} public float getGridRight(){return right;} public float getGridBottom(){return bottom;}
    protected void onDraw(Canvas canvas){super.onDraw(canvas);if(bitmap==null)return;float vw=getWidth(),vh=getHeight(),scale=Math.min(vw/bitmap.getWidth(),vh/bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;imageRect.set((vw-w)/2f,(vh-h)/2f,(vw+w)/2f,(vh+h)/2f);canvas.drawBitmap(bitmap,null,imageRect,imagePaint);float l=px(left,true),r=px(right,true),t=px(top,false),b=px(bottom,false);RectF box=new RectF(l,t,r,b);canvas.drawRect(box,linePaint);float cw=(r-l)/columns,ch=(b-t)/rows;float radius=Math.min(cw,ch)*.43f;for(int y=0;y<rows;y++)for(int x=0;x<columns;x++){float cx=l+(x+.5f)*cw,cy=t+(y+.5f)*ch;canvas.drawCircle(cx,cy,radius,linePaint);}drawHandle(canvas,l,t);drawHandle(canvas,r,t);drawHandle(canvas,l,b);drawHandle(canvas,r,b);}
    private void drawHandle(Canvas c,float x,float y){c.drawCircle(x,y,dp(11),handlePaint);c.drawCircle(x,y,dp(15),linePaint);}
    public boolean onTouchEvent(MotionEvent e){if(bitmap==null)return true;float x=e.getX(),y=e.getY();if(e.getAction()==MotionEvent.ACTION_DOWN){active=nearest(x,y);return true;}if(e.getAction()==MotionEvent.ACTION_MOVE&&active>=0){float nx=clamp((x-imageRect.left)/Math.max(1,imageRect.width())),ny=clamp((y-imageRect.top)/Math.max(1,imageRect.height()));if(active==0){left=nx;top=ny;}else if(active==1){right=nx;top=ny;}else if(active==2){left=nx;bottom=ny;}else{right=nx;bottom=ny;}normalize();invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){active=-1;return true;}return true;}
    private int nearest(float x,float y){float[][] p={{px(left,true),px(top,false)},{px(right,true),px(top,false)},{px(left,true),px(bottom,false)},{px(right,true),px(bottom,false)}};int best=0;float bd=Float.MAX_VALUE;for(int i=0;i<4;i++){float dx=x-p[i][0],dy=y-p[i][1],d=dx*dx+dy*dy;if(d<bd){bd=d;best=i;}}return best;}
    private float px(float n,boolean horizontal){return horizontal?imageRect.left+n*imageRect.width():imageRect.top+n*imageRect.height();}
    private void normalize(){if(right-left<.08f){if(active==0||active==2)left=right-.08f;else right=left+.08f;}if(bottom-top<.08f){if(active==0||active==1)top=bottom-.08f;else bottom=top+.08f;}left=clamp(left);right=clamp(right);top=clamp(top);bottom=clamp(bottom);}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
}
