package com.braseiro.dnd5e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class MapBoardView extends View {
    public interface Listener {
        void onStateChanged(float tokenX, float tokenY, float scale, float offsetX, float offsetY, String currentHexId, String revealsJson);
        void onHexSelected(String hexId);
    }

    private Bitmap bitmap;
    private Bitmap overlayBitmap;
    private Bitmap gmMasterBitmap;
    private int sourceImageWidth;
    private int sourceImageHeight;
    private int overlaySourceWidth;
    private int overlaySourceHeight;
    private int previewSample = 1;
    private int overlayPreviewSample = 1;
    private final LargeImageTileSource mapTiles;
    private final LargeImageTileSource overlayTiles;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fogPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint destinationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routeUnderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hexIdOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint hexIdFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint annotationOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint annotationFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint selectionRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint locationLabelOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint locationLabelFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint locationMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mapMatrix = new Matrix();
    private final Matrix inverseMapMatrix = new Matrix();
    private final RectF logicalMapBounds = new RectF();
    private float logicalWidth = 1f;
    private float logicalHeight = 1f;
    private float bitmapScaleX = 1f;
    private float bitmapScaleY = 1f;
    private float scale = 1f;
    private float offsetX;
    private float offsetY;
    private float lastX;
    private float lastY;
    private float downX;
    private float downY;
    private float tokenX = .5f;
    private float tokenY = .5f;
    private float gridSize = 64f;
    private boolean mapLocked;
    private String gridType = "Quadrada";
    private boolean fog;
    private boolean masterView;
    private boolean gmKnowledgeVisible;
    private boolean revealMode;
    private boolean dragToken;
    private boolean moved;
    private boolean overlayVisible = true;
    private String overlayMode = "dynamic";
    private float minLabelZoom = 1.6f;
    // O mapa de Faerûn já possui o contorno dos hexes impresso no próprio map.png.
    // Esta opção controla apenas o contorno vetorial adicional; cálculo, snap, névoa
    // e IDs continuam usando a geometria mesmo quando o contorno está oculto.
    private boolean vectorGridVisible = true;
    private int fogAlpha = 253;
    private final LinkedHashSet<String> revealedHexIds = new LinkedHashSet<>();
    private final ArrayList<float[]> manualReveals = new ArrayList<>();
    private final ScaleGestureDetector scaleDetector;
    private Listener listener;
    private MapManifestGeometry geometry;
    private String currentHexId = "";
    // Seleção de UI é deliberadamente separada da posição canônica do grupo.
    private String selectedHexId = "";
    private String destinationHexId = "";
    private boolean positionLocked;
    private final ArrayList<String> routeHexIds = new ArrayList<>();
    private final HashMap<String,String> annotationIcons = new HashMap<String,String>();
    private final ArrayList<JSONObject> locationLabels = new ArrayList<JSONObject>();
    private boolean locationNamesVisible;
    private String highlightedLocationName = "";
    private String highlightedLocationHexId = "";
    private float highlightedLocationU = Float.NaN;
    private float highlightedLocationV = Float.NaN;

    public MapBoardView(Context context) { this(context, null); }

    public MapBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.rgb(18, 15, 13));
        // A prévia mantém o mapa inteiro leve; os mosaicos recuperam a resolução
        // original sob demanda quando o usuário amplia.
        mapTiles = new LargeImageTileSource(context, 48);
        overlayTiles = new LargeImageTileSource(context, 24);
        gridPaint.setColor(Color.argb(185, 244, 219, 153));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1.5f);
        fogPaint.setColor(Color.argb(fogAlpha, 0, 0, 0));
        selectedPaint.setColor(Color.argb(190, 255, 215, 70));
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(3f);
        destinationPaint.setColor(Color.rgb(255, 215, 0));
        destinationPaint.setStyle(Paint.Style.STROKE);
        routeUnderPaint.setColor(Color.rgb(215, 35, 35));
        routeUnderPaint.setStyle(Paint.Style.STROKE);
        routeUnderPaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setColor(Color.BLACK);
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        hexIdOutlinePaint.setColor(Color.argb(235,255,248,222));
        hexIdOutlinePaint.setStyle(Paint.Style.STROKE);
        hexIdOutlinePaint.setTextAlign(Paint.Align.CENTER);
        hexIdOutlinePaint.setFakeBoldText(true);
        hexIdFillPaint.setColor(Color.argb(255,25,18,12));
        hexIdFillPaint.setStyle(Paint.Style.FILL);
        hexIdFillPaint.setTextAlign(Paint.Align.CENTER);
        hexIdFillPaint.setFakeBoldText(true);
        selectionRingPaint.setStyle(Paint.Style.STROKE);
        selectionRingPaint.setColor(Color.rgb(255,215,70));
        selectionLabelPaint.setColor(Color.WHITE);
        selectionLabelPaint.setTextAlign(Paint.Align.CENTER);
        selectionLabelPaint.setFakeBoldText(true);
        locationLabelOutlinePaint.setColor(Color.argb(245,20,16,12));
        locationLabelOutlinePaint.setStyle(Paint.Style.STROKE);
        locationLabelOutlinePaint.setTextAlign(Paint.Align.CENTER);
        locationLabelOutlinePaint.setFakeBoldText(true);
        locationLabelFillPaint.setColor(Color.argb(255,255,245,215));
        locationLabelFillPaint.setStyle(Paint.Style.FILL);
        locationLabelFillPaint.setTextAlign(Paint.Align.CENTER);
        locationLabelFillPaint.setFakeBoldText(true);
        locationMarkerPaint.setColor(Color.rgb(255,205,75));
        locationMarkerPaint.setStyle(Paint.Style.FILL);
        annotationOutlinePaint.setColor(Color.BLACK);
        annotationOutlinePaint.setStyle(Paint.Style.STROKE);
        annotationOutlinePaint.setTextAlign(Paint.Align.CENTER);
        annotationOutlinePaint.setFakeBoldText(true);
        annotationFillPaint.setColor(Color.rgb(255,214,82));
        annotationFillPaint.setStyle(Paint.Style.FILL);
        annotationFillPaint.setTextAlign(Paint.Align.CENTER);
        annotationFillPaint.setFakeBoldText(true);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float old = scale;
                scale *= detector.getScaleFactor();
                if (scale < .001f) scale = .001f;
                if (scale > 25f) scale = 25f;
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                offsetX = focusX - (focusX - offsetX) * (scale / old);
                offsetY = focusY - (focusY - offsetY) * (scale / old);
                updateMapMatrices();
                invalidate();
                notifyState();
                return true;
            }
        });
    }

    public void setListener(Listener listener) { this.listener = listener; }

    public void setHexAnnotations(List<JSONObject> notes){/*BR_JSON_WRAP*/try{
        annotationIcons.clear();
        if(notes!=null)for(JSONObject note:notes){if(note==null)continue;String id=note.optString("hex_id","").trim().toUpperCase(Locale.US);if(id.length()==0)continue;String icon=note.optString("icon","✎").trim();annotationIcons.put(id,icon.length()==0?"✎":icon);}
        invalidate();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Nomes do Atlas são uma camada de apresentação; não alteram posição nem catálogo. */
    public void setLocationLabels(List<JSONObject> labels){
        locationLabels.clear();
        if(labels!=null)for(JSONObject label:labels)if(label!=null)locationLabels.add(label);
        invalidate();
    }
    public void setLocationNamesVisible(boolean visible){locationNamesVisible=visible;invalidate();}
    /** Referência cartográfica privada do Mestre. Nunca altera posição, catálogo ou contexto público. */
    public boolean loadGmMasterReference(Uri uri){
        recycleBitmap(gmMasterBitmap);gmMasterBitmap=null;
        if(uri==null)return false;
        try{BitmapInfo info=decode(uri);gmMasterBitmap=info.bitmap;invalidate();return gmMasterBitmap!=null;}catch(Exception e){AppLog.w("MapBoardView","Referência GM não pôde ser carregada.",e);return false;}
    }
    public void setGmKnowledgeVisible(boolean visible){gmKnowledgeVisible=visible&&masterView;invalidate();}
    public boolean isGmKnowledgeVisible(){return gmKnowledgeVisible&&masterView;}
    public boolean isLocationNamesVisible(){return locationNamesVisible;}
    public void setSelectedHexId(String id){
        if(id==null||id.trim().length()==0){selectedHexId="";invalidate();return;}
        if(geometry==null)return;MapManifestGeometry.ManifestHex h=geometry.byId(id.trim());if(h==null){selectedHexId="";invalidate();return;}selectedHexId=h.id;invalidate();
    }
    public boolean focusNormalizedPoint(double u,double v,float targetScale){
        if(Double.isNaN(u)||Double.isNaN(v)||u<0d||u>1d||v<0d||v>1d)return false;
        return focusMapPoint((float)(u*logicalWidth),(float)(v*logicalHeight),targetScale);
    }
    public void highlightLocation(String name,String hexId,Double u,Double v){
        highlightedLocationName=name==null?"":name.trim();highlightedLocationHexId=hexId==null?"":hexId.trim();
        highlightedLocationU=u==null?Float.NaN:u.floatValue();highlightedLocationV=v==null?Float.NaN:v.floatValue();
        if(highlightedLocationHexId.length()>0)setSelectedHexId(highlightedLocationHexId);else invalidate();
    }

    public boolean load(Uri uri) {
        recycleBitmap(bitmap);
        bitmap = null;
        try {
            BitmapInfo info = decode(uri);
            bitmap = info.bitmap;
            sourceImageWidth = info.sourceWidth;
            sourceImageHeight = info.sourceHeight;
            previewSample = info.sample;
            mapTiles.open(uri, sourceImageWidth, sourceImageHeight, previewSample);
            updateCoordinateSpace();
            if (geometry != null) validateManifestDimensions();
            fit();
            invalidate();
            return bitmap != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadOverlay(Uri uri) throws Exception {
        recycleBitmap(overlayBitmap);
        overlayBitmap = null;
        if (uri == null || uri.toString().trim().length() == 0) {
            overlayTiles.close();
            invalidate();
            return true;
        }
        BitmapInfo info = decode(uri);
        overlaySourceWidth = info.sourceWidth;
        overlaySourceHeight = info.sourceHeight;
        overlayPreviewSample = info.sample;
        if (sourceImageWidth > 0 && sourceImageHeight > 0 && (overlaySourceWidth != sourceImageWidth || overlaySourceHeight != sourceImageHeight)) {
            recycleBitmap(info.bitmap);
            throw new Exception("Overlay desalinhado: " + overlaySourceWidth + "×" + overlaySourceHeight + "; mapa " + sourceImageWidth + "×" + sourceImageHeight + ".");
        }
        overlayBitmap = info.bitmap;
        overlayTiles.open(uri, overlaySourceWidth, overlaySourceHeight, overlayPreviewSample);
        invalidate();
        return true;
    }

    private BitmapInfo decode(Uri uri) throws Exception {
        InputStream in = getContext().getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("Arquivo não pôde ser aberto.");
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, bounds);
        in.close();
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new Exception("Imagem inválida.");
        int sample = 1;
        while (bounds.outWidth / sample > 4096 || bounds.outHeight / sample > 4096) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        in = getContext().getContentResolver().openInputStream(uri);
        Bitmap decoded = BitmapFactory.decodeStream(in, null, options);
        in.close();
        if (decoded == null) throw new Exception("A imagem não pôde ser decodificada.");
        return new BitmapInfo(decoded, bounds.outWidth, bounds.outHeight, sample);
    }

    public void fit() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        updateCoordinateSpace();
        // scale é sempre "pixels de tela por pixel lógico". O tamanho da
        // prévia decodificada jamais participa da geometria do atlas.
        scale = Math.min((float) getWidth() / logicalWidth, (float) getHeight() / logicalHeight);
        scale = Math.max(.001f, scale);
        offsetX = (getWidth() - logicalWidth * scale) / 2f;
        offsetY = (getHeight() - logicalHeight * scale) / 2f;
        updateMapMatrices();
        invalidate();
        notifyState();
    }

    public void setGrid(String type, float size) {
        if (hasManifest()) {
            gridType = getManifestGridType();
            gridSize = getManifestGridSize();
            invalidate();
            return;
        }
        gridType = type == null ? "Quadrada" : type;
        gridSize = Math.max(8, size);
        invalidate();
    }

    public void setMapLocked(boolean locked) { mapLocked = locked; }
    public void setPositionLocked(boolean locked) { positionLocked = locked; }
    public boolean isPositionLocked() { return positionLocked; }
    public boolean isMapLocked() { return mapLocked; }
    public void setFog(boolean enabled) { fog = enabled; invalidate(); }
    public void setMasterView(boolean enabled) { masterView=enabled;if(!enabled)gmKnowledgeVisible=false;invalidate(); }
    public boolean isMasterView() { return masterView; }
    public void setFogAlpha(int alpha) { fogAlpha = Math.max(190, Math.min(255, alpha)); fogPaint.setColor(Color.argb(fogAlpha, 0, 0, 0)); invalidate(); }
    public void setRevealMode(boolean enabled) { revealMode = enabled; }
    public void setOverlayVisible(boolean visible) { overlayVisible = visible; invalidate(); }
    public boolean isOverlayVisible() { return overlayVisible; }
    public boolean hasOverlay() { return geometry != null || overlayBitmap != null; }
    public void setOverlayMode(String mode) {
        String value=mode==null?"dynamic":mode.trim().toLowerCase(Locale.US);
        if (!"dynamic".equals(value) && !"raster".equals(value) && !"vector".equals(value)) value="dynamic";
        // O modo vetorial usa o mesmo desenho nativo sob demanda: preserva a nitidez
        // sem transformar o SVG em bitmap. O SVG/SVGZ permanece no pacote como fonte alternativa.
        overlayMode=value;
        invalidate();
    }
    public String getOverlayMode() { return overlayMode; }
    public void setMinLabelZoom(float value) { minLabelZoom=Math.max(.1f,value);invalidate(); }
    public float getMinLabelZoom() { return minLabelZoom; }
    public float getSourceZoom() { return Math.min(logicalScreenScaleX(), logicalScreenScaleY()); }
    public void setVectorGridVisible(boolean visible) { vectorGridVisible = visible; invalidate(); }
    public boolean isVectorGridVisible() { return vectorGridVisible; }

    public void clearFog() {
        revealedHexIds.clear();
        manualReveals.clear();
        invalidate();
        notifyState();
    }

    public void setToken(float x, float y) {
        tokenX = clamp(x, 0, 1);
        tokenY = clamp(y, 0, 1);
        updateCurrentHexFromToken(false);
        if (currentHexId.length() > 0) revealedHexIds.add(currentHexId);
        invalidate();
        notifyState();
    }

    public void setCurrentHexId(String id) {
        if (geometry == null || id == null || id.trim().length() == 0) return;
        MapManifestGeometry.ManifestHex hex = geometry.byId(id);
        if (hex == null || bitmap == null) return;
        currentHexId = hex.id;
        tokenX = clamp(hex.centerX / logicalWidth, 0, 1);
        tokenY = clamp(hex.centerY / logicalHeight, 0, 1);
        revealedHexIds.add(hex.id);
        invalidate();
        notifyState();
    }

    public void setTransform(float valueScale, float x, float y) {
        if (valueScale > 0) scale = valueScale;
        offsetX = x;
        offsetY = y;
        updateMapMatrices();
        invalidate();
    }

    public boolean focusHexId(String id,float targetScale) {
        if(geometry==null)return false;MapManifestGeometry.ManifestHex h=geometry.byId(id);
        return h!=null&&focusMapPoint(h.centerX,h.centerY,targetScale);
    }
    public boolean focusCurrentPosition(float targetScale) {
        if(currentHexId!=null&&currentHexId.length()>0&&focusHexId(currentHexId,targetScale))return true;
        if(bitmap==null||logicalWidth<=0||logicalHeight<=0)return false;
        return focusMapPoint(tokenX*logicalWidth,tokenY*logicalHeight,targetScale);
    }
    public boolean focusMapPoint(float mapX,float mapY,float targetScale) {
        if(bitmap==null||getWidth()<=0||getHeight()<=0)return false;
        if(targetScale>0)scale=Math.max(.001f,Math.min(25f,targetScale));
        offsetX=getWidth()/2f-mapX*scale;offsetY=getHeight()/2f-mapY*scale;
        updateMapMatrices();invalidate();notifyState();return true;
    }

    public float getScaleValue() { return scale; }
    public float getOffsetXValue() { return offsetX; }
    public float getOffsetYValue() { return offsetY; }
    public float getTokenXValue() { return tokenX; }
    public float getTokenYValue() { return tokenY; }
    public String getCurrentHexId() { return currentHexId; }
    public String getSelectedHexId() { return selectedHexId; }
    public String getDestinationHexId() { return destinationHexId; }
    public boolean isHexRevealed(String id) { return id != null && (id.equalsIgnoreCase(currentHexId) || revealedHexIds.contains(id)); }
    public boolean isNeighborOfCurrent(String id) { return geometry != null && id != null && geometry.neighbors(currentHexId).contains(id); }
    public List<String> getRouteHexIds() { return new ArrayList<>(routeHexIds); }
    public void setTravelState(boolean locked,String destination,String routeJson) {
        positionLocked=locked;destinationHexId=destination==null?"":destination;routeHexIds.clear();
        try { JSONArray a=new JSONArray(routeJson==null?"[]":routeJson);for(int i=0;i<a.length();i++){String id=a.optString(i,"");if(geometry!=null&&geometry.byId(id)!=null)routeHexIds.add(id);} } catch(Exception ignored){AppLog.ignored("MapBoardView","Falha tolerada; a execução continuou.",ignored);}
        invalidate();
    }
    public void setDestination(String id) { destinationHexId=id==null?"":id;invalidate(); }
    public void clearPosition() { currentHexId="";selectedHexId="";positionLocked=false;invalidate(); }
    public void setRoute(List<String> ids) { routeHexIds.clear();if(ids!=null)routeHexIds.addAll(ids);invalidate(); }
    public String serializeRoute() {/*BR_JSON_WRAP*/try{ JSONArray a=new JSONArray();for(String id:routeHexIds)a.put(id);return a.toString(); }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public boolean hasManifest() { return geometry != null && geometry.size() > 0; }
    public String getManifestGridType() { return geometry != null && "pointy-top".equals(geometry.getOrientation()) ? "Hexagonal pointy-top" : "Hexagonal flat-top"; }
    public float getManifestGridSize() { return geometry != null && geometry.getHexWidth() > 0 ? geometry.getHexWidth() : gridSize; }
    public float getManifestHexScaleKm() { return geometry == null ? 0f : geometry.getHexScaleKm(); }
    public String getManifestSummary() {
        if (geometry == null) return "Sem manifesto";
        String scaleInfo = "";
        if (geometry.getHexScaleKm() > 0) scaleInfo = " • " + formatScale(geometry.getHexScaleKm()) + " km entre centros horizontais • distância contínua";
        return geometry.size() + " hexes • " + geometry.getOrientation() + scaleInfo + (geometry.getLabel().length() > 0 ? " • " + geometry.getLabel() : "");
    }

    private static String formatScale(float value) {
        if (Math.abs(value - Math.round(value)) < .01f) return String.valueOf(Math.round(value));
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    public String applyManifest(String json) throws Exception {
        return applyGeometry(MapManifestGeometry.parse(json));
    }

    /** Aplica geometria já resolvida pelo contrato do Atlas, sem reinterpretar o map_manifest. */
    public String applyGeometry(MapManifestGeometry parsed) throws Exception {
        if (parsed == null || parsed.size() <= 0) throw new Exception("Geometria do Atlas ausente.");
        geometry = parsed;
        minLabelZoom = parsed.getMinLabelZoom();
        setOverlayMode(parsed.getPreferredOverlayMode());
        validateManifestDimensions();
        gridType = getManifestGridType();
        gridSize = getManifestGridSize();
        updateCoordinateSpace();
        updateCurrentHexFromToken(false);
        if (bitmap != null && getWidth() > 0 && getHeight() > 0) fit();
        else invalidate();
        return getManifestSummary();
    }

    private void validateManifestDimensions() throws Exception {
        if (geometry == null || sourceImageWidth <= 0 || sourceImageHeight <= 0 || geometry.getImageWidth() <= 0 || geometry.getImageHeight() <= 0) return;
        int dw = Math.abs(sourceImageWidth - Math.round(geometry.getImageWidth()));
        int dh = Math.abs(sourceImageHeight - Math.round(geometry.getImageHeight()));
        if (dw > 2 || dh > 2) throw new Exception("Dimensões divergentes: mapa " + sourceImageWidth + "×" + sourceImageHeight + ", manifesto " + Math.round(geometry.getImageWidth()) + "×" + Math.round(geometry.getImageHeight()) + ".");
    }

    public void clearManifest() {
        geometry = null;
        currentHexId = "";
        revealedHexIds.clear();
        updateCoordinateSpace();
        invalidate();
    }

    public List<String> getCurrentAndNeighborHexIds() {
        ArrayList<String> result = new ArrayList<>();
        if (currentHexId.length() == 0 || geometry == null) return result;
        result.add(currentHexId);
        result.addAll(geometry.neighbors(currentHexId));
        return result;
    }

    public String serializeReveals() {/*BR_JSON_WRAP*/try{
        try {
            JSONObject root = new JSONObject();
            JSONArray ids = new JSONArray();
            for (String id : revealedHexIds) ids.put(id);
            JSONArray points = new JSONArray();
            for (float[] point : manualReveals) points.put(new JSONArray().put(point[0]).put(point[1]));
            root.put("hexIds", ids);
            root.put("points", points);
            return root.toString();
        } catch (Exception e) {
            return "{}";
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public void setRevealState(String json) {
        revealedHexIds.clear();
        manualReveals.clear();
        if (json == null || json.trim().length() == 0) return;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray ids = root.optJSONArray("hexIds");
            if (ids != null) for (int i = 0; i < ids.length(); i++) {
                String id = ids.optString(i, "");
                if (id.length() > 0) revealedHexIds.add(id);
            }
            JSONArray points = root.optJSONArray("points");
            if (points != null) for (int i = 0; i < points.length(); i++) {
                JSONArray point = points.optJSONArray(i);
                if (point != null && point.length() >= 2) manualReveals.add(new float[]{(float) point.optDouble(0), (float) point.optDouble(1)});
            }
        } catch(Exception ignored){AppLog.ignored("MapBoardView","Falha tolerada; a execução continuou.",ignored);}
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (bitmap != null && oldw == 0) fit();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) {
            paint.setColor(Color.LTGRAY);
            paint.setTextSize(34);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Escolha uma imagem de mapa", getWidth() / 2f, getHeight() / 2f, paint);
            return;
        }
        updateCoordinateSpace();
        updateMapMatrices();
        canvas.save();
        canvas.concat(mapMatrix);
        Rect baseSource = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bitmap, baseSource, logicalMapBounds, paint);
        mapTiles.draw(canvas, logicalPixelWidth(), logicalPixelHeight(), getSourceZoom(), paint, new Runnable() {
            @Override public void run() { postInvalidateOnAnimation(); }
        });
        // V4 GM Knowledge: o Master rotulado é uma camada privada de referência.
        // A geometria e os nomes canônicos continuam vindo dos JSONs estruturados.
        if(masterView&&gmKnowledgeVisible&&gmMasterBitmap!=null){
            Rect gmSrc=new Rect(0,0,gmMasterBitmap.getWidth(),gmMasterBitmap.getHeight());
            canvas.drawBitmap(gmMasterBitmap,gmSrc,logicalMapBounds,paint);
        }
        if (overlayVisible && "raster".equals(overlayMode) && overlayBitmap != null) {
            Rect src = new Rect(0, 0, overlayBitmap.getWidth(), overlayBitmap.getHeight());
            canvas.drawBitmap(overlayBitmap, src, logicalMapBounds, paint);
            overlayTiles.draw(canvas, logicalPixelWidth(), logicalPixelHeight(), getSourceZoom(), paint, new Runnable() {
                @Override public void run() { postInvalidateOnAnimation(); }
            });
        }
        drawGrid(canvas);
        if (overlayVisible && geometry != null && !"raster".equals(overlayMode)) drawDynamicHexIds(canvas);
        if (locationNamesVisible) drawLocationLabels(canvas);
        if (fog) drawFog(canvas);
        drawTravelPlan(canvas);
        drawSelectionFeedback(canvas);
        drawAnnotations(canvas);
        drawToken(canvas);
        canvas.restore();
    }

    private void drawDynamicHexIds(Canvas canvas) {
        if (geometry == null || bitmap == null || getSourceZoom() < minLabelZoom) return;
        Rect clip = canvas.getClipBounds();
        float marginX = geometry.getHexWidth();
        float marginY = geometry.getHexHeight();
        float left = clip.left - marginX;
        float top = clip.top - marginY;
        float right = clip.right + marginX;
        float bottom = clip.bottom + marginY;
        List<MapManifestGeometry.ManifestHex> visible = geometry.visible(left, top, right, bottom);
        if (visible.isEmpty()) return;
        float density=getResources().getDisplayMetrics().scaledDensity;
        float screenText=Math.max(10f*density, Math.min(16f*density, 12f*density*getSourceZoom()/Math.max(minLabelZoom,.1f)));
        float localText=screenText/Math.max(.0001f,getSourceZoom());
        hexIdOutlinePaint.setTextSize(localText);
        hexIdFillPaint.setTextSize(localText);
        hexIdOutlinePaint.setStrokeWidth(Math.max(.5f,2.2f/Math.max(.0001f,getSourceZoom())));
        Paint.FontMetrics fm=hexIdFillPaint.getFontMetrics();
        float baselineOffset=-(fm.ascent+fm.descent)/2f;
        for (MapManifestGeometry.ManifestHex hex:visible) {
            float x=hex.centerX;
            float y=hex.centerY+baselineOffset;
            canvas.drawText(hex.id,x,y,hexIdOutlinePaint);
            canvas.drawText(hex.id,x,y,hexIdFillPaint);
        }
    }

    private boolean isHexGrid() { return gridType != null && gridType.toLowerCase(Locale.US).contains("hex"); }
    private boolean pointy() { return gridType != null && gridType.toLowerCase(Locale.US).contains("pointy"); }

    private void drawGrid(Canvas canvas) {
        if ("Sem grade".equals(gridType)) return;
        if (geometry != null && !vectorGridVisible) return;
        // Com o continente inteiro enquadrado um hex oficial tem poucos pixels.
        // Desenhar ~98 mil polígonos nessa escala só cria ruído e GC; o hit-test
        // continua ativo e usa os mesmos centros. O contorno surge ao ampliar.
        if (geometry != null && Math.max(geometry.getHexWidth(),geometry.getHexHeight()) * getSourceZoom() < 6f) return;
        float width = logicalWidth;
        float height = logicalHeight;
        gridPaint.setStrokeWidth(Math.max(.35f, 1.7f / Math.max(.0001f, getSourceZoom())));
        if (isHexGrid() && geometry != null) {
            Rect clip = canvas.getClipBounds();
            float left = clip.left;
            float top = clip.top;
            float right = clip.right;
            float bottom = clip.bottom;
            for (MapManifestGeometry.ManifestHex hex : geometry.visible(left, top, right, bottom)) {
                drawHex(canvas, hex.centerX, hex.centerY, hex.width, hex.height, "pointy-top".equals(geometry.getOrientation()), gridPaint);
            }
        } else if (isHexGrid()) {
            if (pointy()) drawPointyGrid(canvas, width, height); else drawFlatGrid(canvas, width, height);
        } else {
            for (float x = 0; x < width; x += gridSize) canvas.drawLine(x, 0, x, height, gridPaint);
            for (float y = 0; y < height; y += gridSize) canvas.drawLine(0, y, width, y, gridPaint);
        }
    }

    private void drawFlatGrid(Canvas canvas, float width, float height) {
        float radius = gridSize / 2f;
        float hexHeight = (float) (Math.sqrt(3) * radius);
        float stepX = 1.5f * radius;
        for (float x = radius; x < width + radius; x += stepX) {
            int column = Math.round((x - radius) / stepX);
            float yStart = column % 2 == 0 ? hexHeight / 2f : hexHeight;
            for (float y = yStart; y < height + hexHeight; y += hexHeight) drawRegularHex(canvas, x, y, radius, false, gridPaint);
        }
    }

    private void drawPointyGrid(Canvas canvas, float width, float height) {
        float radius = gridSize / 2f;
        float hexWidth = (float) (Math.sqrt(3) * radius);
        float stepY = 1.5f * radius;
        for (float y = radius; y < height + radius; y += stepY) {
            int row = Math.round((y - radius) / stepY);
            float xStart = row % 2 == 0 ? hexWidth / 2f : hexWidth;
            for (float x = xStart; x < width + hexWidth; x += hexWidth) drawRegularHex(canvas, x, y, radius, true, gridPaint);
        }
    }

    private void drawHex(Canvas canvas, float centerX, float centerY, float width, float height, boolean pointyTop, Paint usePaint) {
        canvas.drawPath(hexBoundsPath(centerX, centerY, width, height, pointyTop), usePaint);
    }

    private void drawRegularHex(Canvas canvas, float centerX, float centerY, float radius, boolean pointyTop, Paint usePaint) {
        float width = pointyTop ? (float)(Math.sqrt(3) * radius) : radius * 2f;
        float height = pointyTop ? radius * 2f : (float)(Math.sqrt(3) * radius);
        drawHex(canvas, centerX, centerY, width, height, pointyTop, usePaint);
    }

    private Path hexBoundsPath(float centerX, float centerY, float width, float height, boolean pointyTop) {
        Path path = new Path();
        float halfW = Math.max(.5f, width / 2f);
        float halfH = Math.max(.5f, height / 2f);
        if (pointyTop) {
            path.moveTo(centerX, centerY - halfH);
            path.lineTo(centerX + halfW, centerY - halfH / 2f);
            path.lineTo(centerX + halfW, centerY + halfH / 2f);
            path.lineTo(centerX, centerY + halfH);
            path.lineTo(centerX - halfW, centerY + halfH / 2f);
            path.lineTo(centerX - halfW, centerY - halfH / 2f);
        } else {
            path.moveTo(centerX - halfW, centerY);
            path.lineTo(centerX - halfW / 2f, centerY - halfH);
            path.lineTo(centerX + halfW / 2f, centerY - halfH);
            path.lineTo(centerX + halfW, centerY);
            path.lineTo(centerX + halfW / 2f, centerY + halfH);
            path.lineTo(centerX - halfW / 2f, centerY + halfH);
        }
        path.close();
        return path;
    }

    private void drawFog(Canvas canvas) {
        fogPaint.setColor(Color.argb(fogAlpha, 0, 0, 0));
        // Android moderno: exclui os hexes revelados do clip e desenha a névoa
        // diretamente no viewport. Isso evita saveLayer de 21600×15390 e o
        // comportamento inconsistente de PorterDuff.CLEAR em GPUs diferentes.
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            int save=canvas.save();
            if(geometry!=null){
                for(String id:revealedHexIds){MapManifestGeometry.ManifestHex h=geometry.byId(id);if(h!=null)canvas.clipOutPath(hexBoundsPath(h.centerX,h.centerY,h.width*1.02f,h.height*1.02f,"pointy-top".equals(geometry.getOrientation())));}
            }
            for(float[] point:manualReveals){Path hole=new Path();hole.addCircle(point[0]*logicalWidth,point[1]*logicalHeight,getRevealRadius()*1.05f,Path.Direction.CW);canvas.clipOutPath(hole);}
            MapManifestGeometry.ManifestHex current=geometry==null?null:geometry.byId(currentHexId);
            if(current!=null)canvas.clipOutPath(hexBoundsPath(current.centerX,current.centerY,current.width*1.02f,current.height*1.02f,"pointy-top".equals(geometry.getOrientation())));
            else if(currentHexId.length()>0){Path hole=new Path();hole.addCircle(tokenX*logicalWidth,tokenY*logicalHeight,getRevealRadius()*1.05f,Path.Direction.CW);canvas.clipOutPath(hole);}
            Rect clip=canvas.getClipBounds();canvas.drawRect(clip.left-2,clip.top-2,clip.right+2,clip.bottom+2,fogPaint);canvas.restoreToCount(save);return;
        }
        // Compatibilidade API 24/25: limita a camada ao clip visível, nunca à
        // imagem inteira. O caminho CLEAR fica restrito a poucos hexes.
        Rect clip=canvas.getClipBounds();
        int save=canvas.saveLayer(clip.left,clip.top,clip.right,clip.bottom,null,Canvas.ALL_SAVE_FLAG);
        canvas.drawRect(clip.left,clip.top,clip.right,clip.bottom,fogPaint);
        Paint cut=new Paint(Paint.ANTI_ALIAS_FLAG);cut.setColor(Color.TRANSPARENT);cut.setStyle(Paint.Style.FILL);cut.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
        if(geometry!=null)for(String id:revealedHexIds){MapManifestGeometry.ManifestHex h=geometry.byId(id);if(h!=null)clearFogAt(canvas,h.centerX,h.centerY,cut,h);}
        for(float[] point:manualReveals)clearFogAt(canvas,point[0]*logicalWidth,point[1]*logicalHeight,cut,null);
        MapManifestGeometry.ManifestHex current=geometry==null?null:geometry.byId(currentHexId);if(current!=null)clearFogAt(canvas,current.centerX,current.centerY,cut,current);
        else if(currentHexId.length()>0)clearFogAt(canvas,tokenX*logicalWidth,tokenY*logicalHeight,cut,null);
        cut.setXfermode(null);canvas.restoreToCount(save);
    }

    private void clearFogAt(Canvas canvas, float x, float y, Paint cut, MapManifestGeometry.ManifestHex hex) {
        if (isHexGrid()) {
            if (hex != null && geometry != null) drawHex(canvas, x, y, hex.width * 1.12f, hex.height * 1.12f, "pointy-top".equals(geometry.getOrientation()), cut);
            else drawRegularHex(canvas, x, y, getRevealRadius(), pointy(), cut);
        } else canvas.drawCircle(x, y, getRevealRadius() * 1.2f, cut);
    }

    private float getRevealRadius() {
        if (geometry != null) {
            if ("pointy-top".equals(geometry.getOrientation())) return Math.max(8, geometry.getHexHeight() * .58f);
            return Math.max(8, geometry.getHexWidth() * .58f);
        }
        return Math.max(gridSize * .78f, 55);
    }

    private void drawLocationLabels(Canvas canvas) {
        if(geometry==null||locationLabels.isEmpty())return;float z=Math.max(.0001f,getSourceZoom());Rect clip=canvas.getClipBounds();
        float density=getResources().getDisplayMetrics().scaledDensity;float localText=Math.max(11f*density,Math.min(15f*density,12.5f*density*Math.max(1f,z/.12f)))/z;
        locationLabelOutlinePaint.setTextSize(localText);locationLabelFillPaint.setTextSize(localText);locationLabelOutlinePaint.setStrokeWidth(Math.max(.7f,2.8f/z));
        ArrayList<RectF> occupied=new ArrayList<RectF>();
        for(JSONObject loc:locationLabels){
            String name=loc.optString("name","").trim(),hexId=loc.optString("hex_id","").trim();if(name.length()==0||hexId.length()==0)continue;
            boolean hidden=loc.optInt("hidden",0)==1,discovered=loc.optInt("discovered",1)==1;if(!masterView&&(hidden||!discovered||!isHexRevealed(hexId)))continue;
            String type=loc.optString("type","").toLowerCase(Locale.US);boolean major=type.contains("cidade")||type.contains("porto")||type.contains("fortaleza")||type.contains("castelo")||type.contains("metropole")||type.contains("metrópole");if(z<.055f&&!major)continue;
            float x=Float.NaN,y=Float.NaN;if(loc.has("position_u")&&!loc.isNull("position_u")&&loc.has("position_v")&&!loc.isNull("position_v")){x=(float)(loc.optDouble("position_u")*logicalWidth);y=(float)(loc.optDouble("position_v")*logicalHeight);}
            if(Float.isNaN(x)||Float.isNaN(y)){MapManifestGeometry.ManifestHex h=geometry.byId(hexId);if(h==null)continue;x=h.centerX;y=h.centerY;}
            if(x<clip.left-300f/z||x>clip.right+300f/z||y<clip.top-100f/z||y>clip.bottom+100f/z)continue;
            float width=locationLabelFillPaint.measureText(name)+12f/z;RectF box=new RectF(x-width/2f,y-localText*1.65f,x+width/2f,y+localText*.25f);boolean collision=false;for(RectF used:occupied)if(RectF.intersects(used,box)){collision=true;break;}if(collision)continue;occupied.add(box);
            float marker=Math.max(2.2f/z,4f);canvas.drawCircle(x,y,marker,locationMarkerPaint);float baseline=y-(8f/z);canvas.drawText(name,x,baseline,locationLabelOutlinePaint);canvas.drawText(name,x,baseline,locationLabelFillPaint);
        }
    }

    private void drawTravelPlan(Canvas canvas) {
        if (geometry==null) return;
        if (destinationHexId.length()>0) {
            MapManifestGeometry.ManifestHex d=geometry.byId(destinationHexId);
            if(d!=null){destinationPaint.setStrokeWidth(Math.max(2f,6f/Math.max(.0001f,getSourceZoom())));drawHex(canvas,d.centerX,d.centerY,d.width*1.08f,d.height*1.08f,"pointy-top".equals(geometry.getOrientation()),destinationPaint);}
        }
        if(routeHexIds.isEmpty()||currentHexId.length()==0)return;
        MapManifestGeometry.ManifestHex start=geometry.byId(currentHexId);if(start==null)return;
        float viewScale=Math.max(.0001f,getSourceZoom());
        MapManifestGeometry.ManifestHex previous=start;routePaint.setStrokeWidth(Math.max(2f,5f/viewScale));routePaint.setPathEffect(new DashPathEffect(new float[]{Math.max(5f,12f/viewScale),Math.max(4f,9f/viewScale)},0));
        for(String id:routeHexIds){MapManifestGeometry.ManifestHex h=geometry.byId(id);if(h==null)continue;Path segment=new Path();segment.moveTo(previous.centerX,previous.centerY);segment.lineTo(h.centerX,h.centerY);routePaint.setColor(isHexRevealed(id)?Color.BLACK:Color.rgb(230,35,35));canvas.drawPath(segment,routePaint);previous=h;}
        routePaint.setPathEffect(null);
    }

    /** Feedback de toque em tamanho de tela. O hex não é redimensionado; apenas
     * um anel/label visual deixa claro qual célula oficial foi resolvida. */
    private void drawSelectionFeedback(Canvas canvas) {
        if(geometry==null||selectedHexId.length()==0)return;
        MapManifestGeometry.ManifestHex h=geometry.byId(selectedHexId);if(h==null)return;
        float z=Math.max(.0001f,getSourceZoom());
        selectionRingPaint.setStrokeWidth(Math.max(1.5f,3.5f/z));
        float screenRadius=22f*getResources().getDisplayMetrics().density;
        float radius=Math.max(Math.max(h.width,h.height)*.72f,screenRadius/z);
        canvas.drawCircle(h.centerX,h.centerY,radius,selectionRingPaint);
        selectionLabelPaint.setTextSize(Math.max(9f/z,13f*getResources().getDisplayMetrics().scaledDensity/z));
        canvas.drawText(h.id,h.centerX,h.centerY-radius-(6f/z),selectionLabelPaint);
    }

    private void drawAnnotations(Canvas canvas) {
        if(geometry==null||annotationIcons.isEmpty())return;
        float viewScale=Math.max(.0001f,getSourceZoom());
        for(Map.Entry<String,String> entry:annotationIcons.entrySet()){
            MapManifestGeometry.ManifestHex hex=geometry.byId(entry.getKey());if(hex==null||(!masterView&&!isHexRevealed(hex.id)))continue;
            float x=hex.centerX+hex.width*.22f;
            float y=hex.centerY-hex.height*.20f;
            float textSize=Math.max(8f/viewScale,Math.min(hex.height*.36f,18f/viewScale));
            annotationOutlinePaint.setTextSize(textSize);annotationFillPaint.setTextSize(textSize);
            annotationOutlinePaint.setStrokeWidth(Math.max(.8f,2.2f/viewScale));
            String icon=entry.getValue()==null||entry.getValue().trim().length()==0?"✎":entry.getValue().trim();
            Paint.FontMetrics fm=annotationFillPaint.getFontMetrics();float baseline=y-(fm.ascent+fm.descent)/2f;
            canvas.drawText(icon,x,baseline,annotationOutlinePaint);canvas.drawText(icon,x,baseline,annotationFillPaint);
        }
    }

    private void drawToken(Canvas canvas) {
        if(currentHexId.length()==0)return;
        MapManifestGeometry.ManifestHex selected = geometry == null ? null : geometry.byId(currentHexId);
        float x = selected == null ? tokenX * logicalWidth : selected.centerX;
        float y = selected == null ? tokenY * logicalHeight : selected.centerY;
        if (selected != null) {
            // O hexId é a fonte de verdade. Corrige silenciosamente coordenadas
            // normalizadas herdadas de versões antigas antes de persistir o estado.
            tokenX=clamp(selected.centerX/logicalWidth,0,1);
            tokenY=clamp(selected.centerY/logicalHeight,0,1);
            selectedPaint.setStrokeWidth(Math.max(1.5f, 4f / Math.max(.0001f,getSourceZoom())));
            drawHex(canvas, x, y, selected.width * .84f, selected.height * .84f, "pointy-top".equals(geometry.getOrientation()), selectedPaint);
        }
        float radius = Math.max(7, getRevealRadius() * .24f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(180, 35, 35));
        canvas.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, 4f / Math.max(.0001f,getSourceZoom())));
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(radius);
        canvas.drawText("H", x, y + radius * .35f, paint);
        if (currentHexId.length() > 0 && getSourceZoom() > .6f) {
            paint.setTextSize(Math.max(8, 13f / Math.max(.0001f,getSourceZoom())));
            paint.setColor(Color.WHITE);
            canvas.drawText(currentHexId, x, y - radius - Math.max(3, 4f / Math.max(.0001f,getSourceZoom())), paint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        if (bitmap == null) return true;
        float[] mapPoint = screenToLogical(event.getX(), event.getY());
        float bx = mapPoint[0];
        float by = mapPoint[1];
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // O mapa é um canvas 2D dentro de uma página vertical rolável.
                // A partir do primeiro toque dentro do canvas, o gesto pertence ao mapa
                // até UP/CANCEL; a página continua rolável quando o gesto começa fora dele.
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                lastX = event.getX();
                lastY = event.getY();
                downX = lastX;
                downY = lastY;
                moved = false;
                float tx = tokenX * logicalWidth;
                float ty = tokenY * logicalHeight;
                dragToken = !positionLocked && currentHexId.length()>0 && Math.hypot(bx - tx, by - ty) < Math.max(22, getRevealRadius() * 1.4f);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.hypot(event.getX() - downX, event.getY() - downY) > 8f * getResources().getDisplayMetrics().density) moved = true;
                if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                    if (dragToken) {
                        tokenX = clamp(bx / logicalWidth, 0, 1);
                        tokenY = clamp(by / logicalHeight, 0, 1);
                    } else if (!mapLocked && !revealMode) {
                        offsetX += event.getX() - lastX;
                        offsetY += event.getY() - lastY;
                    }
                    lastX = event.getX();
                    lastY = event.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (dragToken) {
                    snapToken(bx, by, true);
                } else if (!moved && !scaleDetector.isInProgress()) {
                    if (revealMode) revealAt(bx, by);
                    else selectHex(bx, by);
                }
                dragToken = false;
                notifyState();
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private void selectHex(float mapX,float mapY){if(geometry==null)return;MapManifestGeometry.ManifestHex hex=geometry.hitTest(mapX,mapY);if(hex==null)return;selectedHexId=hex.id;invalidate();if(listener!=null)listener.onHexSelected(hex.id);}

    private void revealAt(float mapX, float mapY) {
        if (geometry != null) {
            MapManifestGeometry.ManifestHex hex = geometry.hitTest(mapX, mapY);
            if (hex != null) {
                revealedHexIds.add(hex.id);
                return;
            }
        }
        manualReveals.add(new float[]{clamp(mapX / logicalWidth, 0, 1), clamp(mapY / logicalHeight, 0, 1)});
    }

    private void snapToken(float mapX, float mapY, boolean announce) {
        if (geometry != null) {
            MapManifestGeometry.ManifestHex hex = geometry.hitTest(mapX, mapY);
            if (hex != null) {
                tokenX = clamp(hex.centerX / logicalWidth, 0, 1);
                tokenY = clamp(hex.centerY / logicalHeight, 0, 1);
                currentHexId = hex.id;
                revealedHexIds.add(hex.id);
                if (announce && listener != null) listener.onHexSelected(hex.id);
                return;
            }
        }
        tokenX = clamp(mapX / logicalWidth, 0, 1);
        tokenY = clamp(mapY / logicalHeight, 0, 1);
        currentHexId = "";
    }

    private void updateCurrentHexFromToken(boolean announce) {
        if (bitmap == null || geometry == null) return;
        MapManifestGeometry.ManifestHex hex = nearestManifestHex(tokenX * logicalWidth, tokenY * logicalHeight);
        if (hex == null) return;
        currentHexId = hex.id;
        tokenX = clamp(hex.centerX / logicalWidth, 0, 1);
        tokenY = clamp(hex.centerY / logicalHeight, 0, 1);
        if (announce && listener != null) listener.onHexSelected(hex.id);
    }

    private MapManifestGeometry.ManifestHex nearestManifestHex(float mapX, float mapY) {
        return geometry == null ? null : geometry.nearest(mapX, mapY);
    }

    private void updateCoordinateSpace() {
        if (bitmap == null) {
            logicalWidth = 1f;
            logicalHeight = 1f;
            bitmapScaleX = 1f;
            bitmapScaleY = 1f;
            logicalMapBounds.set(0, 0, logicalWidth, logicalHeight);
            updateMapMatrices();
            return;
        }
        // Com manifesto, todas as camadas permanecem no espaço lógico original.
        // Sem manifesto, preservamos a grade manual no espaço da prévia.
        logicalWidth = geometry != null && geometry.getImageWidth() > 0 ? geometry.getImageWidth() : bitmap.getWidth();
        logicalHeight = geometry != null && geometry.getImageHeight() > 0 ? geometry.getImageHeight() : bitmap.getHeight();
        logicalWidth = Math.max(1f, logicalWidth);
        logicalHeight = Math.max(1f, logicalHeight);
        bitmapScaleX = bitmap.getWidth() / logicalWidth;
        bitmapScaleY = bitmap.getHeight() / logicalHeight;
        logicalMapBounds.set(0, 0, logicalWidth, logicalHeight);
        updateMapMatrices();
    }

    private void updateMapMatrices() {
        // Uma única matriz converte o espaço lógico do manifesto para a tela.
        // bitmapScaleX/Y são apenas diagnóstico da prévia e nunca entram aqui.
        mapMatrix.setValues(new float[]{scale,0f,offsetX,0f,scale,offsetY,0f,0f,1f});
        if (!mapMatrix.invert(inverseMapMatrix)) inverseMapMatrix.reset();
    }

    private float[] screenToLogical(float screenX, float screenY) {
        updateMapMatrices();
        float[] point = new float[]{screenX, screenY};
        inverseMapMatrix.mapPoints(point);
        return point;
    }

    private float logicalScreenScaleX() { return Math.max(.0001f, scale); }
    private float logicalScreenScaleY() { return Math.max(.0001f, scale); }
    private int logicalPixelWidth() { return Math.max(1, Math.round(logicalWidth)); }
    private int logicalPixelHeight() { return Math.max(1, Math.round(logicalHeight)); }

    /** Diagnóstico reproduzível: ida e volta deve preservar o ponto lógico. */
    public boolean validateCoordinateRoundTrip(float logicalX, float logicalY, float tolerance) {
        updateMapMatrices();
        float[] point = new float[]{logicalX, logicalY};
        mapMatrix.mapPoints(point);
        inverseMapMatrix.mapPoints(point);
        return Math.abs(point[0] - logicalX) <= Math.max(.001f, tolerance)
                && Math.abs(point[1] - logicalY) <= Math.max(.001f, tolerance);
    }

    /** Valida a mesma transformação em um zoom arbitrário sem alterar a tela. */
    public boolean validateCoordinateRoundTripAtScale(float logicalX,float logicalY,float testScale,float tolerance){
        float use=Math.max(.0001f,testScale);Matrix forward=new Matrix(),inverse=new Matrix();
        forward.setValues(new float[]{use,0f,37f,0f,use,-19f,0f,0f,1f});
        if(!forward.invert(inverse))return false;
        float[] point=new float[]{logicalX,logicalY};forward.mapPoints(point);inverse.mapPoints(point);
        return Math.abs(point[0]-logicalX)<=Math.max(.001f,tolerance)&&Math.abs(point[1]-logicalY)<=Math.max(.001f,tolerance);
    }

    public float getLogicalWidth() { return logicalWidth; }
    public float getLogicalHeight() { return logicalHeight; }
    public float getDecodedBitmapScaleX() { return bitmapScaleX; }
    public float getDecodedBitmapScaleY() { return bitmapScaleY; }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private void notifyState() {
        if (listener != null) listener.onStateChanged(tokenX, tokenY, scale, offsetX, offsetY, currentHexId, serializeReveals());
    }

    private void recycleBitmap(Bitmap value) {
        if (value != null && !value.isRecycled()) value.recycle();
    }

    @Override protected void onDetachedFromWindow() {
        // A aba Mapa é mantida em cache. Fechar os mosaicos aqui deixava o atlas
        // borrado ou vazio ao voltar de outra aba.
        super.onDetachedFromWindow();
    }

    public void trimTileCache(){mapTiles.trimMemory();overlayTiles.trimMemory();}

    public void releaseResources(){
        mapTiles.close();overlayTiles.close();recycleBitmap(bitmap);recycleBitmap(overlayBitmap);recycleBitmap(gmMasterBitmap);bitmap=null;overlayBitmap=null;gmMasterBitmap=null;
    }

    private static final class BitmapInfo {
        final Bitmap bitmap;
        final int sourceWidth;
        final int sourceHeight;
        final int sample;
        BitmapInfo(Bitmap bitmap, int sourceWidth, int sourceHeight, int sample) {
            this.bitmap = bitmap;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.sample = sample;
        }
    }
}
