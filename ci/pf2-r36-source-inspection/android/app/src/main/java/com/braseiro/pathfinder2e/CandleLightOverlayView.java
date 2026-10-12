package com.braseiro.pathfinder2e;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Luz ambiente de vela global, não interativa. Complementa o pergaminho sem alterar conteúdo ou hit testing. */
public final class CandleLightOverlayView extends View {
    private final Paint light=new Paint(Paint.ANTI_ALIAS_FLAG),shade=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean flicker=true; private boolean narrationActive=false; private long narrationPulseUntil=0L; private float intensity=1f; private long started=System.nanoTime();
    public CandleLightOverlayView(Context c){this(c,null);}
    public CandleLightOverlayView(Context c,AttributeSet a){super(c,a);setFocusable(false);setLayerType(LAYER_TYPE_SOFTWARE,null);}
    public void setFlickerEnabled(boolean v){flicker=v;invalidate();}
    public void setIntensity(float v){intensity=Math.max(0f,Math.min(1f,v));invalidate();}
    public void onMasterNarrationStarted(){narrationActive=true;narrationPulseUntil=System.nanoTime()+650000000L;invalidate();}
    public void onNarrationChunk(){narrationPulseUntil=System.nanoTime()+650000000L;invalidate();}
    public void onMasterNarrationEnded(){narrationActive=false;narrationPulseUntil=System.nanoTime()+350000000L;invalidate();}
    @Override public boolean onTouchEvent(MotionEvent e){return false;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(!flicker||intensity<=0f||getWidth()<=0||getHeight()<=0)return;double t=(System.nanoTime()-started)/1e9;float pulse=(float)(.88+.07*Math.sin(t*6.7)+.035*Math.sin(t*13.1+1.9)+.02*Math.sin(t*3.1+.4));float narrativeGain=narrationActive?1f:.64f;if(System.nanoTime()<narrationPulseUntil)narrativeGain=Math.min(1.08f,narrativeGain+.10f);float i=Math.max(.15f,Math.min(1f,intensity))*pulse*narrativeGain;float w=getWidth(),h=getHeight();float cx=w*(.28f+.025f*(float)Math.sin(t*.53)),cy=h*(.06f+.018f*(float)Math.sin(t*.71));float radius=Math.max(w,h)*.95f;int warm=Color.argb((int)(48*i),255,178,77);light.setShader(new RadialGradient(cx,cy,radius,new int[]{warm,Color.argb((int)(18*i),255,131,35),Color.TRANSPARENT},new float[]{0f,.42f,1f},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,light);int edge=Color.argb((int)(42*i),18,7,2);shade.setShader(new RadialGradient(w*.5f,h*.42f,Math.max(w,h)*.78f,new int[]{Color.TRANSPARENT,Color.TRANSPARENT,edge},new float[]{0f,.62f,1f},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,shade);light.setShader(null);shade.setShader(null);postInvalidateDelayed(66);}
}
