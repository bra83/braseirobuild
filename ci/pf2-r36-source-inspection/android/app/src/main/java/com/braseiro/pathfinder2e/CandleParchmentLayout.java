package com.braseiro.pathfinder2e;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Pergaminho vivo para a fala do Mestre. Não usa WebGL e não cria objetos por frame:
 * apenas shaders de Canvas atualizados em ~30 fps enquanto a View está anexada.
 */
public final class CandleParchmentLayout extends LinearLayout {
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean flickerEnabled = true;
    private float intensity = 1f;
    private boolean attached;
    private long epoch = System.nanoTime();

    public CandleParchmentLayout(Context context) { this(context, null); }
    public CandleParchmentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setOrientation(VERTICAL);
        int p = dp(15);
        setPadding(p, dp(13), p, dp(16));
        base.setColor(Color.rgb(205, 174, 119));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1.4f));
        border.setColor(Color.rgb(92, 58, 31));
    }

    public void setFlickerEnabled(boolean enabled) { flickerEnabled = enabled; invalidate(); }
    public void setIntensity(float value) { intensity = Math.max(0f, Math.min(1.6f, value)); invalidate(); }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        epoch = System.nanoTime();
        if (flickerEnabled) postInvalidateOnAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w=getWidth(), h=getHeight();
        if(w<=0||h<=0)return;
        base.setShader(new LinearGradient(0,0,w,h,
                new int[]{Color.rgb(224,198,150),Color.rgb(194,157,103),Color.rgb(222,193,143)},
                new float[]{0f,.57f,1f},Shader.TileMode.CLAMP));
        canvas.drawRoundRect(dp(2),dp(2),w-dp(2),h-dp(2),dp(8),dp(8),base);
        base.setShader(null);
        canvas.drawRoundRect(dp(3),dp(3),w-dp(3),h-dp(3),dp(7),dp(7),border);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int w=getWidth(), h=getHeight();
        if(w<=0||h<=0)return;
        double t=(System.nanoTime()-epoch)/1_000_000_000d;
        float flicker=flickerEnabled?(float)(.86 + .075*Math.sin(t*5.31) + .04*Math.sin(t*11.73+1.7) + .025*Math.sin(t*19.9+.4)):1f;
        flicker=Math.max(.68f,Math.min(1.08f,flicker))*intensity;
        float wander=flickerEnabled?(float)(Math.sin(t*1.37)*.045 + Math.sin(t*.61+2.2)*.025):0f;
        float cx=w*(.50f+wander), cy=-h*.04f;
        float radius=Math.max(w,h)*.92f;
        int warmA=Math.max(0,Math.min(105,Math.round(78*flicker)));
        light.setShader(new RadialGradient(cx,cy,radius,
                new int[]{Color.argb(warmA,255,205,116),Color.argb(Math.max(0,warmA/3),255,174,74),Color.TRANSPARENT},
                new float[]{0f,.42f,1f},Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,light);
        light.setShader(null);

        int edgeA=Math.max(38,Math.min(115,Math.round(74 + (1f-flicker)*72f)));
        shade.setShader(new LinearGradient(0,0,0,h,
                new int[]{Color.TRANSPARENT,Color.argb(edgeA/3,42,20,8),Color.argb(edgeA,28,12,5)},
                new float[]{0f,.58f,1f},Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,shade);
        shade.setShader(null);
        if(attached&&flickerEnabled)postInvalidateDelayed(33L);
    }

    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
