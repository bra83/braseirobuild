package com.braseiro.pathfinder2e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Recorte leve da posição atual no atlas. Exibe o hex atual e os seis centros
 * mais próximos; os hexes ainda não revelados permanecem cobertos pela névoa.
 * A geometria vem exclusivamente do manifesto, nunca de OCR.
 */
public final class SceneHexMiniMapView extends View {
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fogPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint revealedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hiddenOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Uri imageUri;
    private MapManifestGeometry geometry;
    private MapManifestGeometry.ManifestHex current;
    private final ArrayList<MapManifestGeometry.ManifestHex> visibleHexes = new ArrayList<>();
    private final Set<String> revealed = new HashSet<>();
    private Bitmap regionBitmap;
    private final RectF logicalRegion = new RectF();
    private Rect decodedRegion = new Rect();
    private final RectF destination = new RectF();
    private int sourceWidth;
    private int sourceHeight;
    private String message = "Sem posição no atlas";

    public SceneHexMiniMapView(Context context) { this(context, null); }
    public SceneHexMiniMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(Color.rgb(11, 8, 7));
        fogPaint.setColor(Color.argb(244, 0, 0, 0));
        fogPaint.setStyle(Paint.Style.FILL);
        currentPaint.setColor(Color.rgb(238, 192, 82));
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeWidth(dp(3));
        revealedPaint.setColor(Color.argb(190, 245, 224, 164));
        revealedPaint.setStyle(Paint.Style.STROKE);
        revealedPaint.setStrokeWidth(dp(1));
        hiddenOutlinePaint.setColor(Color.argb(220, 188, 48, 42));
        hiddenOutlinePaint.setStyle(Paint.Style.STROKE);
        hiddenOutlinePaint.setStrokeWidth(dp(1.4f));
        hiddenOutlinePaint.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextSize(dp(12));
        labelPaint.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);
        emptyPaint.setColor(Color.rgb(191, 164, 116));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setTextSize(dp(12));
    }

    public void bind(JSONObject mapState) {
        recycle();
        geometry = null;
        current = null;
        visibleHexes.clear();
        revealed.clear();
        message = "Sem posição no atlas";
        if (mapState == null) { invalidate(); return; }
        try {
            String manifest = mapState.optString("manifest_json", "");
            String hexId = mapState.optString("current_hex_id", "").trim();
            String uriText = mapState.optString("image_uri", "").trim();
            if (manifest.length() == 0 || hexId.length() == 0 || uriText.length() == 0) {
                message = hexId.length() == 0 ? "Posição ainda não definida" : "Mapa não carregado";
                invalidate();
                return;
            }
            geometry = MapManifestGeometry.parse(manifest);
            current = geometry.byId(hexId);
            if (current == null) {
                message = "Hex " + hexId + " não encontrado";
                invalidate();
                return;
            }
            imageUri = Uri.parse(uriText);
            parseReveals(mapState.optString("reveals_json", "{}"));
            revealed.add(current.id.toUpperCase(Locale.US));
            collectNearestSeven();
            prepareRegionBounds();
            decodeRegion();
            message = "";
        } catch (Throwable e) {
            recycle();
            geometry = null;
            current = null;
            visibleHexes.clear();
            message = "Prévia do hex indisponível";
            AppLog.e("SceneHexMiniMap", "Falha isolada ao montar a prévia do mapa.", e);
        }
        invalidate();
    }

    private void parseReveals(String raw) {
        if (raw == null || raw.trim().length() == 0) return;
        try {
            JSONObject root = new JSONObject(raw);
            JSONArray ids = root.optJSONArray("hexIds");
            if (ids == null) ids = root.optJSONArray("hex_ids");
            if (ids != null) for (int i = 0; i < ids.length(); i++) {
                String id = ids.optString(i, "").trim();
                if (id.length() > 0) revealed.add(id.toUpperCase(Locale.US));
            }
        } catch (Exception ignored) {
            try {
                JSONArray ids = new JSONArray(raw);
                for (int i = 0; i < ids.length(); i++) {
                    String id = ids.optString(i, "").trim();
                    if (id.length() > 0) revealed.add(id.toUpperCase(Locale.US));
                }
            } catch(Exception ignoredAgain){AppLog.ignored("SceneHexMiniMapView","Falha tolerada; a execução continuou.",ignoredAgain);}
        }
    }

    private void collectNearestSeven() {
        if (geometry == null || current == null) return;
        visibleHexes.add(current);
        for (String id : geometry.neighbors(current.id)) {
            MapManifestGeometry.ManifestHex h = geometry.byId(id);
            if (h != null && !visibleHexes.contains(h)) visibleHexes.add(h);
            if (visibleHexes.size() >= 7) return;
        }
        ArrayList<MapManifestGeometry.ManifestHex> candidates = new ArrayList<>(geometry.all());
        Collections.sort(candidates, new Comparator<MapManifestGeometry.ManifestHex>() {
            @Override public int compare(MapManifestGeometry.ManifestHex a, MapManifestGeometry.ManifestHex b) {
                return Double.compare(distance2(a, current), distance2(b, current));
            }
        });
        for (MapManifestGeometry.ManifestHex h : candidates) {
            if (visibleHexes.size() >= 7) break;
            if (!visibleHexes.contains(h)) visibleHexes.add(h);
        }
    }

    private static double distance2(MapManifestGeometry.ManifestHex a, MapManifestGeometry.ManifestHex b) {
        double dx = a.centerX - b.centerX, dy = a.centerY - b.centerY;
        return dx * dx + dy * dy;
    }

    private void prepareRegionBounds() {
        float minX = current.centerX, maxX = current.centerX, minY = current.centerY, maxY = current.centerY;
        for (MapManifestGeometry.ManifestHex h : visibleHexes) {
            minX = Math.min(minX, h.centerX - h.width * .58f);
            maxX = Math.max(maxX, h.centerX + h.width * .58f);
            minY = Math.min(minY, h.centerY - h.height * .58f);
            maxY = Math.max(maxY, h.centerY + h.height * .58f);
        }
        float padX = Math.max(4f, geometry.getHexWidth() * .20f);
        float padY = Math.max(4f, geometry.getHexHeight() * .20f);
        int declaredW = Math.round(geometry.getImageWidth());
        int declaredH = Math.round(geometry.getImageHeight());
        int left = Math.max(0, (int)Math.floor(minX - padX));
        int top = Math.max(0, (int)Math.floor(minY - padY));
        int right = declaredW > 0 ? Math.min(declaredW, (int)Math.ceil(maxX + padX)) : (int)Math.ceil(maxX + padX);
        int bottom = declaredH > 0 ? Math.min(declaredH, (int)Math.ceil(maxY + padY)) : (int)Math.ceil(maxY + padY);
        logicalRegion.set(left, top, Math.max(left + 2, right), Math.max(top + 2, bottom));
    }

    private void decodeRegion() throws Exception {
        InputStream in = getContext().getContentResolver().openInputStream(imageUri);
        if (in == null) throw new Exception("Imagem não encontrada");
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(in, false);
            if (decoder == null) throw new Exception("Mapa inválido");
            sourceWidth = decoder.getWidth();
            sourceHeight = decoder.getHeight();
            float logicalW=Math.max(1f,geometry.getImageWidth()),logicalH=Math.max(1f,geometry.getImageHeight());
            float bitmapScaleX=sourceWidth/logicalW,bitmapScaleY=sourceHeight/logicalH;
            int left=Math.round(logicalRegion.left*bitmapScaleX),top=Math.round(logicalRegion.top*bitmapScaleY);
            int right=Math.round(logicalRegion.right*bitmapScaleX),bottom=Math.round(logicalRegion.bottom*bitmapScaleY);
            left=Math.max(0,Math.min(sourceWidth-2,left));top=Math.max(0,Math.min(sourceHeight-2,top));
            right=Math.max(left+2,Math.min(sourceWidth,right));bottom=Math.max(top+2,Math.min(sourceHeight,bottom));
            decodedRegion=new Rect(left,top,right,bottom);
            int target = Math.max(360, Math.max(getWidth(), getHeight()) * 2);
            int sample = 1;
            while (decodedRegion.width() / sample > target || decodedRegion.height() / sample > target) sample *= 2;
            for(int attempt=0;attempt<3&&regionBitmap==null;attempt++){
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = Math.max(1, sample << attempt);
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                try{regionBitmap = decoder.decodeRegion(decodedRegion, options);}catch(OutOfMemoryError memoryError){AppLog.w("SceneHexMiniMap","Memória insuficiente; reduzindo a prévia do mapa.",memoryError);System.gc();}
            }
            if (regionBitmap == null) throw new Exception("Recorte não pôde ser lido");
        } finally {
            try{in.close();}catch(Exception ignored){AppLog.ignored("SceneHexMiniMap","Falha ao fechar o mapa.",ignored);}
            if(decoder!=null)decoder.recycle();
        }
    }

    private void updateDestination(){
        float rw=Math.max(1f,logicalRegion.width()),rh=Math.max(1f,logicalRegion.height());
        float fit=Math.min(getWidth()/rw,getHeight()/rh);float w=rw*fit,h=rh*fit;
        destination.set((getWidth()-w)/2f,(getHeight()-h)/2f,(getWidth()+w)/2f,(getHeight()+h)/2f);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (regionBitmap == null || geometry == null || current == null || visibleHexes.isEmpty()) {
            canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }
        updateDestination();
        // O recorte retangular nunca deve revelar mapa fora dos hexes explorados.
        // Primeiro cobrimos tudo de preto e só então desenhamos a imagem dentro
        // de cada hex efetivamente revelado. Assim, quando apenas o hex atual
        // foi explorado, toda a área ao redor permanece completamente escura.
        canvas.drawColor(Color.BLACK);
        for (MapManifestGeometry.ManifestHex h : visibleHexes) {
            Path path = hexPath(h);
            boolean isCurrent = h.id.equalsIgnoreCase(current.id);
            boolean isRevealed = isCurrent || revealed.contains(h.id.toUpperCase(Locale.US));
            if (isRevealed) {
                int checkpoint = canvas.save();
                canvas.clipPath(path);
                canvas.drawBitmap(regionBitmap, null, destination, imagePaint);
                canvas.restoreToCount(checkpoint);
                canvas.drawPath(path, isCurrent ? currentPaint : revealedPaint);
            } else {
                canvas.drawPath(path, fogPaint);
                canvas.drawPath(path, hiddenOutlinePaint);
            }
        }
        float cx = mapToViewX(current.centerX), cy = mapToViewY(current.centerY);
        canvas.drawText(current.id, cx, cy + labelPaint.getTextSize() * .35f, labelPaint);
    }

    private Path hexPath(MapManifestGeometry.ManifestHex h) {
        float cx = mapToViewX(h.centerX), cy = mapToViewY(h.centerY);
        float w = h.width / Math.max(1f, logicalRegion.width()) * destination.width();
        float hh = h.height / Math.max(1f, logicalRegion.height()) * destination.height();
        Path p = new Path();
        if (geometry.getOrientation().contains("point")) {
            p.moveTo(cx, cy - hh / 2f);
            p.lineTo(cx + w / 2f, cy - hh / 4f);
            p.lineTo(cx + w / 2f, cy + hh / 4f);
            p.lineTo(cx, cy + hh / 2f);
            p.lineTo(cx - w / 2f, cy + hh / 4f);
            p.lineTo(cx - w / 2f, cy - hh / 4f);
        } else {
            p.moveTo(cx - w / 2f, cy);
            p.lineTo(cx - w / 4f, cy - hh / 2f);
            p.lineTo(cx + w / 4f, cy - hh / 2f);
            p.lineTo(cx + w / 2f, cy);
            p.lineTo(cx + w / 4f, cy + hh / 2f);
            p.lineTo(cx - w / 4f, cy + hh / 2f);
        }
        p.close();
        return p;
    }

    private float mapToViewX(float x) { return destination.left+(x-logicalRegion.left)/Math.max(1f,logicalRegion.width())*destination.width(); }
    private float mapToViewY(float y) { return destination.top+(y-logicalRegion.top)/Math.max(1f,logicalRegion.height())*destination.height(); }

    @Override protected void onDetachedFromWindow() {
        // A tela de aventura permanece em cache; o recorte é liberado apenas
        // quando houver novo bind ou quando a Activity for destruída.
        super.onDetachedFromWindow();
    }

    public void releaseResources(){recycle();}

    private void recycle() {
        if (regionBitmap != null && !regionBitmap.isRecycled()) regionBitmap.recycle();
        regionBitmap = null;
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
