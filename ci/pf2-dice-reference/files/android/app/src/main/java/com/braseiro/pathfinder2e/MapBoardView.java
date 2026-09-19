package com.braseiro.pathfinder2e;

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
    private Bitmap regionsOverlayBitmap;
    private Bitmap politicalOverlayBitmap;
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
    private final Paint campaignPoiPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint campaignPoiOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint kingmakerTerritoryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint canonicalPoiPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint canonicalPoiOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
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
    private boolean revealMode;
    private boolean dragToken;
    private boolean moved;
    private boolean overlayVisible = true;
    private boolean regionsOverlayVisible = true;
    private boolean politicalOverlayVisible = false;
    private String overlayMode = "dynamic";
    private float minLabelZoom = 1.6f;
    // O contorno visual é independente da geometria canônica do manifesto.
    // Cálculo, snap, névoa e IDs continuam usando a geometria mesmo com o contorno oculto.
    private boolean vectorGridVisible = true;
    private int fogAlpha = 253;
    private final LinkedHashSet<String> revealedHexIds = new LinkedHashSet<>();
    private final ArrayList<float[]> manualReveals = new ArrayList<>();
    private final ScaleGestureDetector scaleDetector;
    private Listener listener;
    private MapManifestGeometry geometry;
    private String currentHexId = "";
    private String destinationHexId = "";
    private boolean positionLocked;
    private final ArrayList<String> routeHexIds = new ArrayList<>();
    private final HashMap<String,String> annotationIcons = new HashMap<String,String>();
    private final HashMap<String,ArrayList<JSONObject>> campaignPoisByHex = new HashMap<String,ArrayList<JSONObject>>();
    private final HashMap<String,ArrayList<JSONObject>> canonicalPoisByHex = new HashMap<String,ArrayList<JSONObject>>();
    private final HashMap<String,JSONObject> kingmakerHexesById = new HashMap<String,JSONObject>();

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
        annotationOutlinePaint.setColor(Color.BLACK);
        annotationOutlinePaint.setStyle(Paint.Style.STROKE);
        annotationOutlinePaint.setTextAlign(Paint.Align.CENTER);
        annotationOutlinePaint.setFakeBoldText(true);
        annotationFillPaint.setColor(Color.rgb(255,214,82));
        annotationFillPaint.setStyle(Paint.Style.FILL);
        annotationFillPaint.setTextAlign(Paint.Align.CENTER);
        annotationFillPaint.setFakeBoldText(true);
        campaignPoiPaint.setStyle(Paint.Style.FILL);
        campaignPoiPaint.setTextAlign(Paint.Align.CENTER);
        campaignPoiPaint.setFakeBoldText(true);
        campaignPoiOutlinePaint.setColor(Color.argb(235,20,12,8));
        campaignPoiOutlinePaint.setStyle(Paint.Style.STROKE);
        campaignPoiOutlinePaint.setTextAlign(Paint.Align.CENTER);
        campaignPoiOutlinePaint.setFakeBoldText(true);
        kingmakerTerritoryPaint.setStrokeJoin(Paint.Join.ROUND);
        canonicalPoiPaint.setStyle(Paint.Style.FILL);
        canonicalPoiPaint.setTextAlign(Paint.Align.CENTER);
        canonicalPoiPaint.setFakeBoldText(true);
        canonicalPoiOutlinePaint.setStyle(Paint.Style.STROKE);
        canonicalPoiOutlinePaint.setTextAlign(Paint.Align.CENTER);
        canonicalPoiOutlinePaint.setFakeBoldText(true);
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

    public void setHexAnnotations(List<JSONObject> notes){
        annotationIcons.clear();
        if(notes!=null)for(JSONObject note:notes){if(note==null)continue;String id=note.optString("hex_id","").trim().toUpperCase(Locale.US);if(id.length()==0)continue;String icon=note.optString("icon","✎").trim();annotationIcons.put(id,icon.length()==0?"✎":icon);}
        invalidate();
    }

    /** Official read-only Atlas locations with a valid referenced hex. */
    public void setCanonicalPois(List<JSONObject> pois){
        canonicalPoisByHex.clear();
        if(pois!=null)for(JSONObject poi:pois){
            if(poi==null)continue;
            String id=poi.optString("hex_id","").trim().toUpperCase(Locale.US);
            if(id.length()==0)continue;
            ArrayList<JSONObject> rows=canonicalPoisByHex.get(id);
            if(rows==null){rows=new ArrayList<JSONObject>();canonicalPoisByHex.put(id,rows);}
            rows.add(poi);
        }
        invalidate();
    }

    /** Dynamic campaign/procedural POIs. Canonical map data remains in the manifest/database. */
    public void setCampaignPois(List<JSONObject> pois){
        campaignPoisByHex.clear();
        if(pois!=null)for(JSONObject poi:pois){if(poi==null)continue;String id=poi.optString("hex_id","").trim().toUpperCase(Locale.US);if(id.length()==0)continue;ArrayList<JSONObject> rows=campaignPoisByHex.get(id);if(rows==null){rows=new ArrayList<JSONObject>();campaignPoisByHex.put(id,rows);}rows.add(poi);}
        invalidate();
    }

    /** Camada de campanha Kingmaker; nunca modifica a geometria ou os locais canônicos. */
    public void setKingmakerHexes(List<JSONObject> hexes){
        kingmakerHexesById.clear();
        if(hexes!=null)for(JSONObject row:hexes){if(row==null)continue;String id=row.optString("hex_id","").trim().toUpperCase(Locale.US);if(id.length()>0)kingmakerHexesById.put(id,row);}
        invalidate();
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

    private BitmapInfo decode(Uri uri) throws Exception { return decode(uri,4096); }

    private BitmapInfo decode(Uri uri,int maxDimension) throws Exception {
        InputStream in = getContext().getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("Arquivo não pôde ser aberto.");
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, bounds);
        in.close();
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new Exception("Imagem inválida.");
        int sample = 1;
        while (bounds.outWidth / sample > Math.max(512,maxDimension) || bounds.outHeight / sample > Math.max(512,maxDimension)) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        in = getContext().getContentResolver().openInputStream(uri);
        Bitmap decoded = BitmapFactory.decodeStream(in, null, options);
        in.close();
        if (decoded == null) throw new Exception("A imagem não pôde ser decodificada.");
        return new BitmapInfo(decoded, bounds.outWidth, bounds.outHeight, sample);
    }

    public boolean loadRegionsOverlay(Uri uri) throws Exception {
        recycleBitmap(regionsOverlayBitmap);regionsOverlayBitmap=null;
        if(uri==null||uri.toString().trim().isEmpty()){invalidate();return true;}
        BitmapInfo info=decode(uri,2048);validateAuxOverlay(info,"Regiões");regionsOverlayBitmap=info.bitmap;invalidate();return true;
    }

    public boolean loadPoliticalOverlay(Uri uri) throws Exception {
        recycleBitmap(politicalOverlayBitmap);politicalOverlayBitmap=null;
        if(uri==null||uri.toString().trim().isEmpty()){invalidate();return true;}
        BitmapInfo info=decode(uri,2048);validateAuxOverlay(info,"Política");politicalOverlayBitmap=info.bitmap;invalidate();return true;
    }

    private void validateAuxOverlay(BitmapInfo info,String label) throws Exception {
        if(sourceImageWidth>0&&sourceImageHeight>0&&(info.sourceWidth!=sourceImageWidth||info.sourceHeight!=sourceImageHeight)){recycleBitmap(info.bitmap);throw new Exception(label+" desalinhada: "+info.sourceWidth+"×"+info.sourceHeight+"; mapa "+sourceImageWidth+"×"+sourceImageHeight+".");}
    }

    public void setRegionsOverlayVisible(boolean visible){regionsOverlayVisible=visible;invalidate();}
    public boolean isRegionsOverlayVisible(){return regionsOverlayVisible;}
    public void setPoliticalOverlayVisible(boolean visible){politicalOverlayVisible=visible;invalidate();}
    public boolean isPoliticalOverlayVisible(){return politicalOverlayVisible;}

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
    public void setMasterView(boolean enabled) { masterView=enabled;invalidate(); }
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
    public void clearPosition() { currentHexId="";positionLocked=false;invalidate(); }
    public void setRoute(List<String> ids) { routeHexIds.clear();if(ids!=null)routeHexIds.addAll(ids);invalidate(); }
    public String serializeRoute() { JSONArray a=new JSONArray();for(String id:routeHexIds)a.put(id);return a.toString(); }
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
        MapManifestGeometry parsed = MapManifestGeometry.parse(json);
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

    public String serializeReveals() {
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
    }

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
        if(regionsOverlayVisible&&regionsOverlayBitmap!=null){Rect src=new Rect(0,0,regionsOverlayBitmap.getWidth(),regionsOverlayBitmap.getHeight());canvas.drawBitmap(regionsOverlayBitmap,src,logicalMapBounds,paint);}
        if(politicalOverlayVisible&&politicalOverlayBitmap!=null){Rect src=new Rect(0,0,politicalOverlayBitmap.getWidth(),politicalOverlayBitmap.getHeight());canvas.drawBitmap(politicalOverlayBitmap,src,logicalMapBounds,paint);}
        if (overlayVisible && "raster".equals(overlayMode) && overlayBitmap != null) {
            Rect src = new Rect(0, 0, overlayBitmap.getWidth(), overlayBitmap.getHeight());
            canvas.drawBitmap(overlayBitmap, src, logicalMapBounds, paint);
            overlayTiles.draw(canvas, logicalPixelWidth(), logicalPixelHeight(), getSourceZoom(), paint, new Runnable() {
                @Override public void run() { postInvalidateOnAnimation(); }
            });
        }
        drawKingmakerTerritory(canvas);
        drawGrid(canvas);
        if (overlayVisible && geometry != null && !"raster".equals(overlayMode)) drawDynamicHexIds(canvas);
        if (fog && !masterView) drawFog(canvas);
        drawTravelPlan(canvas);
        drawAnnotations(canvas);
        drawCanonicalPois(canvas);
        drawCampaignPois(canvas);
        drawToken(canvas);
        canvas.restore();
    }

    private void drawKingmakerTerritory(Canvas canvas){
        if(geometry==null||kingmakerHexesById.isEmpty())return;float viewScale=Math.max(.0001f,getSourceZoom());boolean pointy="pointy-top".equals(geometry.getOrientation());
        for(Map.Entry<String,JSONObject> entry:kingmakerHexesById.entrySet()){
            MapManifestGeometry.ManifestHex hex=geometry.byId(entry.getKey());if(hex==null)continue;JSONObject state=entry.getValue();boolean claimed=state.optInt("claimed",0)==1,cleared=state.optInt("cleared",0)==1,reconnoitered=state.optInt("reconnoitered",0)==1;if(!claimed&&!cleared&&!reconnoitered)continue;
            if(claimed){kingmakerTerritoryPaint.setStyle(Paint.Style.FILL);kingmakerTerritoryPaint.setColor(Color.argb(72,55,180,95));kingmakerTerritoryPaint.setPathEffect(null);drawHex(canvas,hex.centerX,hex.centerY,hex.width*.98f,hex.height*.98f,pointy,kingmakerTerritoryPaint);}
            kingmakerTerritoryPaint.setStyle(Paint.Style.STROKE);kingmakerTerritoryPaint.setStrokeWidth(Math.max(.8f,(claimed?4.2f:2.5f)/viewScale));kingmakerTerritoryPaint.setColor(claimed?Color.rgb(75,220,115):(cleared?Color.rgb(245,185,65):Color.rgb(105,190,235)));kingmakerTerritoryPaint.setPathEffect(reconnoitered&&!cleared?new DashPathEffect(new float[]{Math.max(3f,8f/viewScale),Math.max(2f,5f/viewScale)},0):null);drawHex(canvas,hex.centerX,hex.centerY,hex.width*.94f,hex.height*.94f,pointy,kingmakerTerritoryPaint);kingmakerTerritoryPaint.setPathEffect(null);
        }
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
        int save = canvas.saveLayer(0, 0, logicalWidth, logicalHeight, null, Canvas.ALL_SAVE_FLAG);
        fogPaint.setColor(Color.argb(fogAlpha, 0, 0, 0));
        canvas.drawRect(0, 0, logicalWidth, logicalHeight, fogPaint);
        Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
        cut.setColor(Color.TRANSPARENT);
        cut.setStyle(Paint.Style.FILL);
        cut.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
        if (geometry != null) {
            for (String id : revealedHexIds) {
                MapManifestGeometry.ManifestHex hex = geometry.byId(id);
                if (hex != null) clearFogAt(canvas, hex.centerX, hex.centerY, cut, hex);
            }
        }
        for (float[] point : manualReveals) clearFogAt(canvas, point[0] * logicalWidth, point[1] * logicalHeight, cut, null);
        MapManifestGeometry.ManifestHex current=geometry==null?null:geometry.byId(currentHexId);
        float currentX=current==null?tokenX*logicalWidth:current.centerX;
        float currentY=current==null?tokenY*logicalHeight:current.centerY;
        clearFogAt(canvas,currentX,currentY,cut,current);
        cut.setXfermode(null);
        canvas.restoreToCount(save);
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

    private void drawCanonicalPois(Canvas canvas) {
        if(geometry==null||canonicalPoisByHex.isEmpty())return;
        float viewScale=Math.max(.0001f,getSourceZoom());
        for(Map.Entry<String,ArrayList<JSONObject>> entry:canonicalPoisByHex.entrySet()){
            MapManifestGeometry.ManifestHex hex=geometry.byId(entry.getKey());if(hex==null||(!masterView&&fog&&!isHexRevealed(hex.id)))continue;
            ArrayList<JSONObject> rows=entry.getValue();if(rows==null||rows.isEmpty())continue;
            JSONObject primary=rows.get(0);
            String placement=primary.optString("placement_status","").toLowerCase(Locale.US);
            boolean approximate=placement.contains("approx")||placement.contains("review")||placement.contains("unassigned");
            float radius=Math.max(4.3f/viewScale,Math.min(hex.width,hex.height)*.085f);
            float x=hex.centerX+hex.width*.23f,y=hex.centerY+hex.height*.19f;
            canonicalPoiPaint.setStyle(approximate?Paint.Style.STROKE:Paint.Style.FILL);
            canonicalPoiPaint.setStrokeWidth(Math.max(.8f,1.7f/viewScale));
            canonicalPoiPaint.setColor(approximate?Color.rgb(238,220,166):Color.rgb(236,190,76));
            canvas.drawCircle(x,y,radius,canonicalPoiPaint);
            canonicalPoiOutlinePaint.setColor(Color.argb(235,45,27,15));
            canonicalPoiOutlinePaint.setStrokeWidth(Math.max(.7f,1.5f/viewScale));
            canvas.drawCircle(x,y,radius,canonicalPoiOutlinePaint);
            canonicalPoiPaint.setStyle(Paint.Style.FILL);
            canonicalPoiPaint.setColor(approximate?Color.rgb(238,220,166):Color.WHITE);
            canonicalPoiPaint.setTextSize(radius*1.25f);
            Paint.FontMetrics fm=canonicalPoiPaint.getFontMetrics();
            canvas.drawText(approximate?"○":"•",x,y-(fm.ascent+fm.descent)/2f,canonicalPoiPaint);
            if(getSourceZoom()>=1.05f){
                String name=primary.optString("name","").trim();
                if(name.length()>0){
                    float textSize=Math.max(8f/viewScale,Math.min(hex.height*.23f,13f/viewScale));
                    canonicalPoiOutlinePaint.setTextSize(textSize);canonicalPoiOutlinePaint.setStrokeWidth(Math.max(.7f,2.0f/viewScale));canonicalPoiOutlinePaint.setColor(Color.argb(230,28,18,12));
                    canonicalPoiPaint.setTextSize(textSize);canonicalPoiPaint.setColor(Color.rgb(255,241,200));canonicalPoiPaint.setTextAlign(Paint.Align.LEFT);canonicalPoiOutlinePaint.setTextAlign(Paint.Align.LEFT);
                    float tx=x+radius*1.35f,ty=y-radius*.25f;
                    canvas.drawText(name,tx,ty,canonicalPoiOutlinePaint);canvas.drawText(name,tx,ty,canonicalPoiPaint);
                    canonicalPoiPaint.setTextAlign(Paint.Align.CENTER);canonicalPoiOutlinePaint.setTextAlign(Paint.Align.CENTER);
                }
            }
        }
    }

    private void drawCampaignPois(Canvas canvas) {
        if(geometry==null||campaignPoisByHex.isEmpty())return;
        float viewScale=Math.max(.0001f,getSourceZoom());
        for(Map.Entry<String,ArrayList<JSONObject>> entry:campaignPoisByHex.entrySet()){
            MapManifestGeometry.ManifestHex hex=geometry.byId(entry.getKey());if(hex==null||(!masterView&&fog&&!isHexRevealed(hex.id)))continue;
            ArrayList<JSONObject> rows=entry.getValue();if(rows==null||rows.isEmpty())continue;
            JSONObject primary=null;int visibleCount=0;boolean hasCampaign=false,hasProcedural=false,hasSecret=false;
            for(JSONObject poi:rows){
                if(poi==null||poi.optString("parent_poi_id","").trim().length()>0)continue;
                boolean hidden=poi.optInt("hidden",0)==1||poi.optBoolean("hidden",false);boolean discovered=poi.optInt("discovered",1)==1||poi.optBoolean("discovered",false);
                if((hidden||!discovered)&&!masterView){continue;}
                if(hidden||!discovered)hasSecret=true;
                if("procedural".equals(poi.optString("source")))hasProcedural=true;else hasCampaign=true;
                if(primary==null)primary=poi;visibleCount++;
            }
            if(primary==null)continue;
            float radius=Math.max(5f/viewScale,Math.min(hex.width,hex.height)*.105f);float x=hex.centerX-hex.width*.24f,y=hex.centerY+hex.height*.22f;
            int fill=hasSecret?Color.rgb(105,85,135):(hasCampaign&&hasProcedural?Color.rgb(210,150,55):(hasProcedural?Color.rgb(90,145,90):Color.rgb(215,135,45)));
            campaignPoiPaint.setColor(fill);canvas.drawCircle(x,y,radius,campaignPoiPaint);campaignPoiOutlinePaint.setStrokeWidth(Math.max(.7f,2f/viewScale));canvas.drawCircle(x,y,radius,campaignPoiOutlinePaint);
            String icon=primary.optString("icon","•").trim();if(icon.length()==0)icon="•";campaignPoiPaint.setColor(Color.WHITE);campaignPoiPaint.setTextSize(radius*1.35f);Paint.FontMetrics fm=campaignPoiPaint.getFontMetrics();canvas.drawText(icon,x,y-(fm.ascent+fm.descent)/2f,campaignPoiPaint);
            if(visibleCount>1){float badge=Math.max(4f/viewScale,radius*.55f);float bx=x+radius*.72f,by=y-radius*.72f;campaignPoiPaint.setColor(Color.rgb(40,30,24));canvas.drawCircle(bx,by,badge,campaignPoiPaint);campaignPoiPaint.setColor(Color.WHITE);campaignPoiPaint.setTextSize(badge*1.4f);Paint.FontMetrics bm=campaignPoiPaint.getFontMetrics();canvas.drawText(String.valueOf(visibleCount),bx,by-(bm.ascent+bm.descent)/2f,campaignPoiPaint);}
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
                dragToken = !positionLocked && Math.hypot(bx - tx, by - ty) < Math.max(22, getRevealRadius() * 1.4f);
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

    private void selectHex(float mapX,float mapY){if(geometry==null)return;MapManifestGeometry.ManifestHex hex=nearestManifestHex(mapX,mapY);if(hex!=null&&listener!=null)listener.onHexSelected(hex.id);}

    private void revealAt(float mapX, float mapY) {
        if (geometry != null) {
            MapManifestGeometry.ManifestHex hex = nearestManifestHex(mapX, mapY);
            if (hex != null) {
                revealedHexIds.add(hex.id);
                return;
            }
        }
        manualReveals.add(new float[]{clamp(mapX / logicalWidth, 0, 1), clamp(mapY / logicalHeight, 0, 1)});
    }

    private void snapToken(float mapX, float mapY, boolean announce) {
        if (geometry != null) {
            MapManifestGeometry.ManifestHex hex = nearestManifestHex(mapX, mapY);
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
        mapTiles.close();overlayTiles.close();recycleBitmap(bitmap);recycleBitmap(overlayBitmap);recycleBitmap(regionsOverlayBitmap);recycleBitmap(politicalOverlayBitmap);bitmap=null;overlayBitmap=null;regionsOverlayBitmap=null;politicalOverlayBitmap=null;
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
