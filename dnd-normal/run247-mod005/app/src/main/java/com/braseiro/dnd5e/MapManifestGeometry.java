package com.braseiro.dnd5e;

import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Geometria independente da arte do mapa. IDs nunca são obtidos por OCR.
 * Suporta grades geradas uniformes, grades com número de linhas por paridade
 * de coluna (Mystara V2) e manifestos explícitos legados.
 */
public final class MapManifestGeometry {
    private static final int GEOMETRY_CACHE_LIMIT=2;
    private static final LinkedHashMap<String,MapManifestGeometry> GEOMETRY_CACHE=new LinkedHashMap<String,MapManifestGeometry>(4,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,MapManifestGeometry> eldest){return size()>GEOMETRY_CACHE_LIMIT;}
    };
    public static final class ManifestHex {
        public final String id;
        public final float centerX;
        public final float centerY;
        public final float width;
        public final float height;
        public final int column;
        public final int row;

        ManifestHex(String id, float centerX, float centerY, float width, float height, int column, int row) {
            this.id = id == null ? "" : id;
            this.centerX = centerX;
            this.centerY = centerY;
            this.width = width;
            this.height = height;
            this.column = column;
            this.row = row;
        }
    }

    private final ArrayList<ManifestHex> hexes = new ArrayList<>();
    private final HashMap<String, ManifestHex> byId = new HashMap<>();
    private final HashMap<Long, ManifestHex> byCoordinate = new HashMap<>();
    private final HashMap<String, List<String>> explicitNeighbors = new HashMap<>();
    private String orientation = "flat-top";
    private String label = "";
    private String mapId = "";
    private String setting = "";
    private String worldProfile = "dnd";
    private String preferredOverlayMode = "dynamic";
    private float imageWidth;
    private float imageHeight;
    private float hexWidth;
    private float hexHeight;
    private float originX;
    private float originY;
    private float stepX;
    private float stepY;
    private float alternateColumnOffsetY;
    private int columns;
    private int rows;
    private int rowsEven;
    private int rowsOdd;
    private int[] columnStart;
    private float hexScaleKm;
    private float hexScaleMiles;
    private float minLabelZoom = 1.6f;
    private boolean generated;
    private boolean explicitCenters;

    /**
     * Reconstrói a camada visual/clicável a partir dos centros e vizinhos que já
     * foram importados do dataset canônico. Nenhum centro ou vizinho é inferido
     * do JPG e nenhuma topologia odd-q é recalculada durante o jogo.
     */
    public static MapManifestGeometry fromAtlasDatabase(AppDb db, String packageId, AtlasMapProfile p) throws Exception {/*BR_JSON_WRAP*/try{
        if (db == null) throw new Exception("AppDb ausente para geometria do Atlas.");
        if (p == null) throw new Exception("AtlasMapProfile ausente.");
        if (packageId == null || packageId.trim().isEmpty()) throw new Exception("map_package_id ausente.");
        String cacheKey = "atlasdb:" + packageId.trim() + ":" + p.catalogVersion + ":" + p.hexCount;
        synchronized (GEOMETRY_CACHE) { MapManifestGeometry cached = GEOMETRY_CACHE.get(cacheKey); if (cached != null) return cached; }

        MapManifestGeometry g = baseFromAtlasProfile(p);
        g.generated = false;
        g.explicitCenters = true;
        Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT hex_id,center_x,center_y,neighbors_json FROM map_hex_atlas WHERE package_id=? ORDER BY rowid",
                new String[]{packageId.trim()});
        try {
            int idCol=c.getColumnIndexOrThrow("hex_id"), xCol=c.getColumnIndexOrThrow("center_x"), yCol=c.getColumnIndexOrThrow("center_y"), nCol=c.getColumnIndexOrThrow("neighbors_json");
            while (c.moveToNext()) {
                String id=c.getString(idCol);
                if (id==null || id.trim().isEmpty()) continue;
                float x=(float)c.getDouble(xCol), y=(float)c.getDouble(yCol);
                int[] cr=parseExcelHexId(id);
                g.add(id,x,y,p.hexWidth,p.hexHeight,cr==null?-1:cr[0],cr==null?-1:cr[1]);
                ArrayList<String> neighbors=new ArrayList<String>(6);
                String raw=c.isNull(nCol)?"{}":c.getString(nCol);
                try {
                    JSONObject o=new JSONObject(raw==null?"{}":raw);
                    String[] order={"N","NE","SE","S","SW","NW"};
                    for(String key:order){String nid=o.optString(key,"").trim();if(nid.length()>0&&!"null".equalsIgnoreCase(nid)&&!neighbors.contains(nid))neighbors.add(nid);}
                } catch(Exception invalid) { throw new Exception("neighbors_json inválido em "+id+".",invalid); }
                g.explicitNeighbors.put(id.trim().toUpperCase(Locale.US),Collections.unmodifiableList(neighbors));
            }
        } finally { c.close(); }
        if (g.size()!=p.hexCount) throw new Exception("Atlas indexado contém "+g.size()+" hexes; esperado "+p.hexCount+".");
        for(Map.Entry<String,List<String>> entry:g.explicitNeighbors.entrySet()) for(String nid:entry.getValue()) if(!g.byId.containsKey(nid.toUpperCase(Locale.US))) throw new Exception("Vizinho "+nid+" referenciado por "+entry.getKey()+" não existe no Atlas.");
        synchronized(GEOMETRY_CACHE){GEOMETRY_CACHE.put(cacheKey,g);}
        return g;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static MapManifestGeometry baseFromAtlasProfile(AtlasMapProfile p) {
        MapManifestGeometry g = new MapManifestGeometry();
        g.mapId = p.logicalMapId;
        g.label = p.setting.length() > 0 ? p.setting + " — Atlas" : p.logicalMapId;
        g.setting = p.setting;
        g.worldProfile = p.hostWorldProfile();
        g.preferredOverlayMode = "dynamic";
        g.imageWidth = p.imageWidth;
        g.imageHeight = p.imageHeight;
        g.orientation = p.orientation;
        g.hexWidth = p.hexWidth;
        g.hexHeight = p.hexHeight;
        g.originX = p.originX;
        g.originY = p.originY;
        g.stepX = p.columnStepX;
        g.stepY = p.rowStepY;
        g.alternateColumnOffsetY = p.oddColumnYOffset;
        g.columns = p.columns;
        g.rowsEven = p.rowsEven();
        g.rowsOdd = p.rowsOdd();
        g.rows = Math.max(g.rowsEven, g.rowsOdd);
        g.hexScaleMiles = p.distanceMiles;
        g.hexScaleKm = p.distanceMiles * 1.609344f;
        g.minLabelZoom = 1.6f;
        return g;
    }

    /** Constrói metadados geométricos do perfil para validação/importação; o runtime usa fromAtlasDatabase. */
    public static MapManifestGeometry fromAtlasMapProfile(AtlasMapProfile p) throws Exception {
        if (p == null) throw new Exception("AtlasMapProfile ausente.");
        MapManifestGeometry g = baseFromAtlasProfile(p);
        g.generateGrid();
        if (g.size() != p.hexCount) throw new Exception("AtlasMapProfile gerou " + g.size() + " hexes; esperado " + p.hexCount + ".");
        return g;
    }

    public static MapManifestGeometry parse(String json) throws Exception {/*BR_JSON_WRAP*/try{
        if(json==null)throw new Exception("Manifesto vazio.");
        String cacheKey=json.length()+":"+Integer.toHexString(json.hashCode());
        synchronized(GEOMETRY_CACHE){MapManifestGeometry cached=GEOMETRY_CACHE.get(cacheKey);if(cached!=null)return cached;}
        JSONObject root = new JSONObject(json);
        JSONObject map = root;
        JSONArray maps = root.optJSONArray("maps");
        if (maps != null && maps.length() > 0) {
            map = maps.optJSONObject(0);
            for (int i = 0; i < maps.length(); i++) {
                JSONObject candidate = maps.optJSONObject(i);
                if (candidate != null && candidate.optBoolean("enabled", true)) {
                    map = candidate;
                    break;
                }
            }
        }
        if (map == null) throw new Exception("Manifesto sem definição de mapa.");

        MapManifestGeometry g = new MapManifestGeometry();
        JSONObject layout = map.optJSONObject("layout");
        if (layout == null) layout = root.optJSONObject("layout");
        if (layout == null) layout = map;

        g.mapId = firstString(map, "mapId", "id");
        if (g.mapId.length() == 0) g.mapId = firstString(root, "mapId", "id");
        g.label = firstString(map, "label", "name");
        if (g.label.length() == 0) g.label = firstString(root, "label", "name");
        g.setting = firstString(map, "setting");
        if (g.setting.length() == 0) g.setting = firstString(root, "setting");
        String profile = firstString(map, "worldProfile", "profile");
        if (profile.length() == 0) profile = firstString(root, "worldProfile", "profile");
        if (profile.length() == 0) profile = g.setting.toLowerCase(Locale.US).contains("mystara") || g.mapId.toLowerCase(Locale.US).contains("mystara") ? "mystara" : "dnd";
        g.worldProfile = normalizeProfile(profile);

        JSONObject sourceMap = map.optJSONObject("sourceMap");
        if (sourceMap == null) sourceMap = root.optJSONObject("sourceMap");
        JSONObject image = map.optJSONObject("image");
        if (image == null) image = root.optJSONObject("image");
        g.imageWidth = (float) firstPositive(
                sourceMap == null ? 0 : sourceMap.optDouble("width", 0),
                image == null ? 0 : image.optDouble("width", 0),
                layout.optDouble("imageWidth", 0),
                map.optDouble("imageWidth", 0),
                root.optDouble("imageWidth", 0));
        g.imageHeight = (float) firstPositive(
                sourceMap == null ? 0 : sourceMap.optDouble("height", 0),
                image == null ? 0 : image.optDouble("height", 0),
                layout.optDouble("imageHeight", 0),
                map.optDouble("imageHeight", 0),
                root.optDouble("imageHeight", 0));
        g.orientation = normalizeOrientation(layout.optString("orientation", map.optString("orientation", root.optString("orientation", "flat-top"))));
        g.hexWidth = (float) firstPositive(layout.optDouble("hexWidth", 0), layout.optDouble("cellWidth", 0), map.optDouble("hexWidth", 0));
        g.hexHeight = (float) firstPositive(layout.optDouble("hexHeight", 0), layout.optDouble("cellHeight", 0), map.optDouble("hexHeight", 0));
        if (g.hexWidth <= 0 && g.hexHeight > 0) g.hexWidth = "pointy-top".equals(g.orientation) ? (float) (g.hexHeight * Math.sqrt(3) / 2f) : (float) (g.hexHeight * 2f / Math.sqrt(3));
        if (g.hexHeight <= 0 && g.hexWidth > 0) g.hexHeight = "pointy-top".equals(g.orientation) ? (float) (g.hexWidth * 2f / Math.sqrt(3)) : (float) (g.hexWidth * Math.sqrt(3) / 2f);

        g.columns = layout.optInt("columns", 0);
        g.rows = layout.optInt("rows", 0);
        JSONObject rowsByParity = layout.optJSONObject("rowsByColumnParity");
        if (rowsByParity != null) {
            g.rowsEven = Math.max(0, rowsByParity.optInt("even", 0));
            g.rowsOdd = Math.max(0, rowsByParity.optInt("odd", 0));
            g.rows = Math.max(g.rowsEven, g.rowsOdd);
        } else {
            g.rowsEven = g.rows;
            g.rowsOdd = g.rows;
        }

        g.originX = (float) firstFinite(layout.optDouble("originX", Double.NaN), layout.optDouble("xStart", Double.NaN), g.hexWidth / 2f);
        g.originY = (float) firstFinite(layout.optDouble("originY", Double.NaN), layout.optDouble("yStart", Double.NaN), g.hexHeight / 2f);
        g.stepX = (float) firstPositive(layout.optDouble("columnStepX", 0), layout.optDouble("columnStep", 0), layout.optDouble("stepX", 0), layout.optDouble("horizontalStep", 0));
        g.stepY = (float) firstPositive(layout.optDouble("rowStepY", 0), layout.optDouble("rowStep", 0), layout.optDouble("stepY", 0), layout.optDouble("verticalStep", 0));
        if (g.stepX <= 0) g.stepX = "pointy-top".equals(g.orientation) ? g.hexWidth : g.hexWidth * .75f;
        if (g.stepY <= 0) g.stepY = "pointy-top".equals(g.orientation) ? g.hexHeight * .75f : g.hexHeight;
        g.alternateColumnOffsetY = (float) firstFinite(layout.optDouble("alternateColumnOffsetY", Double.NaN), layout.optDouble("oddColumnOffsetY", Double.NaN), g.stepY / 2f);
        if (g.rows <= 0 && g.columns > 0 && g.imageHeight > 0 && g.stepY > 0) {
            if ("pointy-top".equals(g.orientation)) {
                g.rows = (int) Math.floor((g.imageHeight - g.originY) / g.stepY) + 1;
                g.rowsEven = g.rows;
                g.rowsOdd = g.rows;
            } else {
                g.rowsEven = (int) Math.floor((g.imageHeight - g.originY) / g.stepY) + 1;
                g.rowsOdd = (int) Math.floor((g.imageHeight - (g.originY + g.alternateColumnOffsetY)) / g.stepY) + 1;
                g.rows = Math.max(g.rowsEven, g.rowsOdd);
            }
        }
        if (g.columns <= 0 && g.imageWidth > 0 && g.stepX > 0) {
            g.columns = (int) Math.floor((g.imageWidth - g.originX) / g.stepX) + 1;
        }
        g.explicitCenters = hasExplicitCenters(layout);
        // Em manifestos explícitos, nenhum centro/origem é corrigido por heurística.
        // Apenas grades geradas antigas podem receber normalização legada.
        g.reconcileHexBoundsWithLattice(!g.explicitCenters);

        JSONObject scale = map.optJSONObject("scale");
        if (scale == null) scale = root.optJSONObject("scale");
        JSONObject distance = layout.optJSONObject("distancePerHex");
        double distanceValue = distance == null ? 0 : distance.optDouble("value", 0);
        String distanceUnit = distance == null ? "" : distance.optString("unit", "").toLowerCase(Locale.US);
        g.hexScaleKm = (float) firstPositive(
                map.optDouble("hexScaleKm", 0), root.optDouble("hexScaleKm", 0), layout.optDouble("hexScaleKm", 0),
                scale == null ? 0 : scale.optDouble("kmPerHex", 0), scale == null ? 0 : scale.optDouble("kilometersPerHex", 0),
                distanceUnit.startsWith("km") || distanceUnit.contains("kilomet") ? distanceValue : 0);
        g.hexScaleMiles = (float) firstPositive(
                map.optDouble("hexScaleMiles", 0), root.optDouble("hexScaleMiles", 0), layout.optDouble("hexScaleMiles", 0),
                scale == null ? 0 : scale.optDouble("milesPerHex", 0), distanceUnit.contains("mile") ? distanceValue : 0);
        if (g.hexScaleKm <= 0 && g.hexScaleMiles > 0) g.hexScaleKm = g.hexScaleMiles * 1.609344f;
        if (g.hexScaleMiles <= 0 && g.hexScaleKm > 0) g.hexScaleMiles = g.hexScaleKm / 1.609344f;

        JSONObject rendering = map.optJSONObject("rendering");
        if (rendering == null) rendering = root.optJSONObject("rendering");
        if (rendering != null) {
            g.preferredOverlayMode = rendering.optString("preferredOverlayMode", "dynamic");
            JSONObject dynamic = rendering.optJSONObject("dynamicLabelRendering");
            if (dynamic != null) g.minLabelZoom = (float) Math.max(.1, dynamic.optDouble("minZoomToShowLabels", 1.6));
        }

        JSONArray rowDefinitions = layout.optJSONArray("rowDefinitions");
        if (rowDefinitions == null) rowDefinitions = layout.optJSONArray("rowsDefinition");
        if (rowDefinitions != null) {
            for (int i = 0; i < rowDefinitions.length(); i++) {
                JSONObject row = rowDefinitions.optJSONObject(i);
                if (row == null) continue;
                float y = (float) firstFinite(row.optDouble("yCenter", Double.NaN), row.optDouble("cy", Double.NaN), row.optDouble("y", Double.NaN));
                JSONArray xs = row.optJSONArray("xCenters");
                JSONArray ids = row.optJSONArray("hexIds");
                if (xs != null) for (int j = 0; j < xs.length(); j++) g.add(ids != null ? ids.optString(j, "") : "", (float) xs.optDouble(j), y, g.hexWidth, g.hexHeight, -1, -1);
                JSONArray cells = row.optJSONArray("hexes");
                if (cells == null) cells = row.optJSONArray("cells");
                if (cells != null) for (int j = 0; j < cells.length(); j++) g.parseCell(cells.opt(j), y);
            }
        }

        JSONArray centers = layout.optJSONArray("centers");
        if (centers == null) centers = layout.optJSONArray("hexCenters");
        if (centers != null) for (int i = 0; i < centers.length(); i++) g.parseCell(centers.opt(i), Float.NaN);
        JSONArray explicit = layout.optJSONArray("tokens");
        if (explicit == null) explicit = layout.optJSONArray("hexes");
        if (explicit == null) explicit = layout.optJSONArray("cells");
        if (explicit != null) for (int i = 0; i < explicit.length(); i++) g.parseCell(explicit.opt(i), Float.NaN);

        if (g.hexes.isEmpty()) {
            JSONArray xs = layout.optJSONArray("xCenters");
            JSONArray ys = layout.optJSONArray("yCenters");
            if (xs != null && ys != null) for (int yi = 0; yi < ys.length(); yi++) for (int xi = 0; xi < xs.length(); xi++) g.add("", (float) xs.optDouble(xi), (float) ys.optDouble(yi), g.hexWidth, g.hexHeight, xi, yi);
        }
        if (!g.hexes.isEmpty() && g.explicitCenters) g.deriveLatticeFromExplicitCenters();
        if (g.hexes.isEmpty() && g.columns > 0 && g.rows > 0) g.generateGrid();
        if (g.hexes.isEmpty()) throw new Exception("O manifesto não contém centros de hex reconhecíveis.");
        if (g.hexWidth <= 0) g.hexWidth = 64f;
        if (g.hexHeight <= 0) g.hexHeight = "pointy-top".equals(g.orientation) ? (float) (g.hexWidth * 2f / Math.sqrt(3)) : (float) (g.hexWidth * Math.sqrt(3) / 2f);
        synchronized(GEOMETRY_CACHE){GEOMETRY_CACHE.put(cacheKey,g);}
        return g;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void generateGrid() {
        generated = true;
        columnStart = new int[columns + 1];
        int index = 0;
        for (int c = 0; c < columns; c++) {
            columnStart[c] = index;
            int count = rowCount(c);
            for (int r = 0; r < count; r++) {
                float x;
                float y;
                if ("pointy-top".equals(orientation)) {
                    x = originX + c * stepX + ((r & 1) == 1 ? stepX / 2f : 0f);
                    y = originY + r * stepY;
                } else {
                    x = originX + c * stepX;
                    y = originY + r * stepY + ((c & 1) == 1 ? alternateColumnOffsetY : 0f);
                }
                add(excelColumn(c) + (r + 1), x, y, hexWidth, hexHeight, c, r);
                index++;
            }
        }
        columnStart[columns] = index;
    }

    private int rowCount(int column) {
        if (column < 0 || column >= columns) return 0;
        if (rowsEven > 0 || rowsOdd > 0) return (column & 1) == 0 ? rowsEven : rowsOdd;
        return rows;
    }

    private void parseCell(Object value, float rowY) {
        if (value instanceof JSONObject) {
            JSONObject o = (JSONObject) value;
            double w = firstPositive(o.optDouble("width", 0), o.optDouble("hexWidth", 0), hexWidth);
            double h = firstPositive(o.optDouble("height", 0), o.optDouble("hexHeight", 0), hexHeight);
            double cx = firstFinite(o.optDouble("xCenter", Double.NaN), o.optDouble("centerX", Double.NaN), o.optDouble("cx", Double.NaN));
            double cy = firstFinite(o.optDouble("yCenter", Double.NaN), o.optDouble("centerY", Double.NaN), o.optDouble("cy", Double.NaN));
            if (Double.isNaN(cx)) { double x = o.optDouble("x", Double.NaN); if (!Double.isNaN(x)) cx = w > 0 ? x + w / 2 : x; }
            if (Double.isNaN(cy)) { double y = o.optDouble("y", Double.NaN); if (!Double.isNaN(y)) cy = h > 0 ? y + h / 2 : y; else if (!Float.isNaN(rowY)) cy = rowY; }
            String id = firstString(o, "hexId", "id", "label");
            if (!Double.isNaN(cx) && !Double.isNaN(cy)) add(id, (float) cx, (float) cy, (float) w, (float) h, o.optInt("column", -1), o.optInt("row", -1));
        } else if (value instanceof JSONArray) {
            JSONArray a = (JSONArray) value;
            if (a.length() >= 2) add(a.length() > 2 ? a.optString(2, "") : "", (float) a.optDouble(0), (float) a.optDouble(1), hexWidth, hexHeight, -1, -1);
        } else if (value instanceof Number && !Float.isNaN(rowY)) add("", ((Number) value).floatValue(), rowY, hexWidth, hexHeight, -1, -1);
    }

    private void add(String id, float x, float y, float width, float height, int column, int row) {/*BR_JSON_WRAP*/try{
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isInfinite(x) || Float.isInfinite(y)) return;
        int useColumn=column;
        int useRow=row;
        if ((useColumn < 0 || useRow < 0) && id != null) {
            int[] parsed=parseExcelHexId(id);
            if(parsed!=null){useColumn=parsed[0];useRow=parsed[1];}
        }
        float useWidth = normalizeCellDimension(width, hexWidth);
        float useHeight = normalizeCellDimension(height, hexHeight);
        ManifestHex hex = new ManifestHex(id, x, y, useWidth, useHeight, useColumn, useRow);
        hexes.add(hex);
        if (hex.id.length() > 0) byId.put(hex.id.toUpperCase(Locale.US), hex);
        if(useColumn>=0&&useRow>=0){
            byCoordinate.put(coordinateKey(useColumn,useRow),hex);
            columns=Math.max(columns,useColumn+1);
            rows=Math.max(rows,useRow+1);
            rowsEven=Math.max(rowsEven,(useColumn&1)==0?useRow+1:rowsEven);
            rowsOdd=Math.max(rowsOdd,(useColumn&1)==1?useRow+1:rowsOdd);
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static long coordinateKey(int column,int row){return (((long)column)<<32)^(row&0xffffffffL);}

    private static boolean hasExplicitCenters(JSONObject layout){
        if(layout==null)return false;
        String[] keys={"rowDefinitions","rowsDefinition","centers","hexCenters","tokens","hexes","cells","xCenters","yCenters","hex_index","hexIndex"};
        for(String key:keys){JSONArray a=layout.optJSONArray(key);if(a!=null&&a.length()>0)return true;}
        return false;
    }

    /**
     * Extrai passo e bounding box a partir dos centros explícitos, sem mover um
     * único centro. Isso neutraliza hexWidth/hexHeight legados gravados como
     * raio ou diâmetro e mantém o índice como fonte de verdade.
     */
    private void deriveLatticeFromExplicitCenters(){
        ArrayList<Float> horizontal=new ArrayList<Float>();
        ArrayList<Float> vertical=new ArrayList<Float>();
        for(ManifestHex h:hexes){
            if(h.column<0||h.row<0)continue;
            ManifestHex right=byCoordinate.get(coordinateKey(h.column+1,h.row));
            if(right!=null){float d=Math.abs(right.centerX-h.centerX);if(d>.001f)horizontal.add(d);}
            ManifestHex down=byCoordinate.get(coordinateKey(h.column,h.row+1));
            if(down!=null){float d=Math.abs(down.centerY-h.centerY);if(d>.001f)vertical.add(d);}
        }
        float derivedX=median(horizontal);
        float derivedY=median(vertical);
        if(derivedX>0)stepX=derivedX;
        if(derivedY>0)stepY=derivedY;
        if("pointy-top".equals(orientation)){
            if(stepX>0)hexWidth=stepX;
            if(stepY>0)hexHeight=stepY/.75f;
        }else{
            if(stepX>0)hexWidth=stepX/.75f;
            if(stepY>0)hexHeight=stepY;
            alternateColumnOffsetY=stepY/2f;
        }
        ArrayList<ManifestHex> original=new ArrayList<ManifestHex>(hexes);
        hexes.clear();byId.clear();byCoordinate.clear();
        int oldColumns=columns,oldRows=rows,oldEven=rowsEven,oldOdd=rowsOdd;
        columns=0;rows=0;rowsEven=0;rowsOdd=0;
        for(ManifestHex h:original)add(h.id,h.centerX,h.centerY,hexWidth,hexHeight,h.column,h.row);
        columns=Math.max(columns,oldColumns);rows=Math.max(rows,oldRows);rowsEven=Math.max(rowsEven,oldEven);rowsOdd=Math.max(rowsOdd,oldOdd);
    }

    private static float median(ArrayList<Float> values){
        if(values==null||values.isEmpty())return 0f;
        Collections.sort(values);
        int n=values.size();
        return (n&1)==1?values.get(n/2):(values.get(n/2-1)+values.get(n/2))/2f;
    }

    /**
     * Em uma malha hexagonal, os passos entre centros determinam a caixa real
     * do hex. Isso impede que um manifesto legado que gravou diâmetro/raio em
     * vez da bounding box desenhe hexes com o dobro do tamanho.
     */
    private void reconcileHexBoundsWithLattice(boolean allowLegacyCoordinateNormalization) {
        float expectedWidth = 0f;
        float expectedHeight = 0f;
        if ("pointy-top".equals(orientation)) {
            if (stepX > 0) expectedWidth = stepX;
            if (stepY > 0) expectedHeight = stepY / .75f;
        } else {
            if (stepX > 0) expectedWidth = stepX / .75f;
            if (stepY > 0) expectedHeight = stepY;
        }
        hexWidth = reconcileDimension(hexWidth, expectedWidth);
        hexHeight = reconcileDimension(hexHeight, expectedHeight);
    }

    private static float reconcileDimension(float declared, float expected) {
        if (expected <= 0) return declared;
        if (declared <= 0) return expected;
        float ratio = declared / expected;
        return ratio < .80f || ratio > 1.20f ? expected : declared;
    }

    private static boolean looksDoubled(float value, float expected) {
        if (value <= 0 || expected <= 0) return false;
        float ratio = value / expected;
        return ratio >= 1.75f && ratio <= 2.25f;
    }

    private float normalizeCellDimension(float value, float canonical) {
        if (canonical <= 0) return value > 0 ? value : 1f;
        if (value <= 0) return canonical;
        float ratio = value / canonical;
        return ratio < .80f || ratio > 1.20f ? canonical : value;
    }

    /**
     * Resolve o hex realmente sob o ponto usando os centros/bounds já carregados
     * do contrato. Não infere ID pela imagem e não altera a topologia.
     */
    public ManifestHex hitTest(float mapX,float mapY) {
        ManifestHex nearest=nearest(mapX,mapY);
        if(nearest==null)return null;
        if(containsPoint(nearest,mapX,mapY))return nearest;
        // Em bordas compartilhadas/erros mínimos de ponto flutuante, testa apenas
        // os vizinhos PRECOMPUTADOS do pacote, nunca uma vizinhança reconstruída.
        List<String> linked=explicitNeighbors.get(nearest.id.toUpperCase(Locale.US));
        if(linked!=null)for(String id:linked){ManifestHex h=byId(id);if(h!=null&&containsPoint(h,mapX,mapY))return h;}
        // Fora da cobertura cartográfica não transforma arbitrariamente o ponto
        // no hex mais próximo.
        return null;
    }

    private boolean containsPoint(ManifestHex h,float x,float y){
        if(h==null||h.width<=0f||h.height<=0f)return false;
        float nx=Math.abs(x-h.centerX)/(h.width*.5f);
        float ny=Math.abs(y-h.centerY)/(h.height*.5f);
        final float eps=.035f;
        if("pointy-top".equals(orientation)){
            if(ny>1f+eps)return false;
            return ny<=.5f+eps ? nx<=1f+eps : nx<=2f*(1f-ny)+eps;
        }
        if(nx>1f+eps)return false;
        return nx<=.5f+eps ? ny<=1f+eps : ny<=2f*(1f-nx)+eps;
    }

    public ManifestHex nearest(float mapX, float mapY) {
        if (hexes.isEmpty()) return null;
        if (columns > 0 && rows > 0 && stepX > 0 && stepY > 0) {
            int approximateColumn;
            int approximateRow;
            if ("pointy-top".equals(orientation)) {
                approximateRow = Math.round((mapY - originY) / Math.max(.0001f, stepY));
                float offset = (approximateRow & 1) == 1 ? stepX / 2f : 0f;
                approximateColumn = Math.round((mapX - originX - offset) / Math.max(.0001f, stepX));
            } else {
                approximateColumn = Math.round((mapX - originX) / Math.max(.0001f, stepX));
                float offset = (approximateColumn & 1) == 1 ? alternateColumnOffsetY : 0f;
                approximateRow = Math.round((mapY - originY - offset) / Math.max(.0001f, stepY));
            }
            ManifestHex best = null;
            float bestDistance = Float.MAX_VALUE;
            for (int c = approximateColumn - 2; c <= approximateColumn + 2; c++) {
                for (int r = approximateRow - 2; r <= approximateRow + 2; r++) {
                    ManifestHex candidate = at(c, r);
                    if (candidate == null) continue;
                    float dx = candidate.centerX - mapX;
                    float dy = candidate.centerY - mapY;
                    float distance = dx * dx + dy * dy;
                    if (distance < bestDistance) { bestDistance = distance; best = candidate; }
                }
            }
            if (best != null) return best;
        }
        ManifestHex best = null;
        float bestDistance = Float.MAX_VALUE;
        for (ManifestHex candidate : hexes) {
            float dx = candidate.centerX - mapX;
            float dy = candidate.centerY - mapY;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) { bestDistance = distance; best = candidate; }
        }
        return best;
    }

    public ManifestHex at(int column, int row) {
        if (column < 0 || row < 0 || column >= columns || row >= rowCount(column)) return null;
        ManifestHex indexed=byCoordinate.get(coordinateKey(column,row));
        if(indexed!=null)return indexed;
        if (generated && columnStart != null) {
            int index = columnStart[column] + row;
            return index >= 0 && index < hexes.size() ? hexes.get(index) : null;
        }
        return null;
    }

    public ManifestHex byId(String id) {
        if (id == null) return null;
        ManifestHex direct = byId.get(id.trim().toUpperCase(Locale.US));
        if (direct != null) return direct;
        int[] parsed = parseExcelHexId(id);
        return parsed == null ? null : at(parsed[0], parsed[1]);
    }

    public List<String> neighbors(String id) {
        if(id!=null){List<String> explicit=explicitNeighbors.get(id.trim().toUpperCase(Locale.US));if(explicit!=null)return explicit;}
        ManifestHex center = byId(id);
        if (center == null || center.column < 0 || center.row < 0) return Collections.emptyList();
        int c = center.column;
        int r = center.row;
        int[][] offsets;
        if ("pointy-top".equals(orientation)) {
            offsets = (r & 1) == 0 ? new int[][]{{-1,0},{1,0},{-1,-1},{0,-1},{-1,1},{0,1}} : new int[][]{{-1,0},{1,0},{0,-1},{1,-1},{0,1},{1,1}};
        } else {
            offsets = (c & 1) == 0 ? new int[][]{{0,-1},{0,1},{-1,-1},{-1,0},{1,-1},{1,0}} : new int[][]{{0,-1},{0,1},{-1,0},{-1,1},{1,0},{1,1}};
        }
        ArrayList<String> result = new ArrayList<>(6);
        for (int[] offset : offsets) {
            ManifestHex neighbor = at(c + offset[0], r + offset[1]);
            if (neighbor != null && neighbor.id.length() > 0) result.add(neighbor.id);
        }
        return result;
    }

    public List<ManifestHex> visible(float left, float top, float right, float bottom) {
        if (columns <= 0 || rows <= 0 || stepX <= 0 || stepY <= 0) {
            ArrayList<ManifestHex> result = new ArrayList<>();
            float marginX = Math.max(1f, hexWidth);
            float marginY = Math.max(1f, hexHeight);
            for (ManifestHex hex : hexes) if (hex.centerX >= left-marginX && hex.centerX <= right+marginX && hex.centerY >= top-marginY && hex.centerY <= bottom+marginY) result.add(hex);
            return result;
        }
        int c0 = Math.max(0, (int) Math.floor((left - originX - hexWidth) / Math.max(1f, stepX)) - 1);
        int c1 = Math.min(columns - 1, (int) Math.ceil((right - originX + hexWidth) / Math.max(1f, stepX)) + 1);
        ArrayList<ManifestHex> result = new ArrayList<>();
        for (int c = c0; c <= c1; c++) {
            float offset = "pointy-top".equals(orientation) ? 0f : ((c & 1) == 1 ? alternateColumnOffsetY : 0f);
            int r0 = Math.max(0, (int) Math.floor((top - originY - offset - hexHeight) / Math.max(1f, stepY)) - 1);
            int r1 = Math.min(rowCount(c) - 1, (int) Math.ceil((bottom - originY - offset + hexHeight) / Math.max(1f, stepY)) + 1);
            for (int r = r0; r <= r1; r++) { ManifestHex hex = at(c, r); if (hex != null) result.add(hex); }
        }
        return result;
    }

    public static String excelColumn(int zeroBased) {
        if (zeroBased < 0) return "";
        int value = zeroBased + 1;
        StringBuilder result = new StringBuilder();
        while (value > 0) { int rem = (value - 1) % 26; result.append((char) ('A' + rem)); value = (value - 1) / 26; }
        return result.reverse().toString();
    }

    public static int[] parseExcelHexId(String id) {
        if (id == null) return null;
        String value = id.trim().toUpperCase(Locale.US);
        int split = 0;
        while (split < value.length() && value.charAt(split) >= 'A' && value.charAt(split) <= 'Z') split++;
        if (split == 0 || split == value.length()) return null;
        int column = 0;
        for (int i = 0; i < split; i++) column = column * 26 + (value.charAt(i) - 'A' + 1);
        int row;
        try { row = Integer.parseInt(value.substring(split)); } catch (Exception e) { return null; }
        return new int[]{column - 1, row - 1};
    }

    public List<ManifestHex> all() { return Collections.unmodifiableList(hexes); }
    public int size() { return hexes.size(); }
    public String getOrientation() { return orientation; }
    public String getLabel() { return label; }
    public String getMapId() { return mapId; }
    public String getSetting() { return setting; }
    public String getWorldProfile() { return worldProfile; }
    public String getPreferredOverlayMode() { return preferredOverlayMode; }
    public float getImageWidth() { return imageWidth; }
    public float getImageHeight() { return imageHeight; }
    public float getHexWidth() { return hexWidth; }
    public float getHexHeight() { return hexHeight; }
    public float getOriginX() { return originX; }
    public float getOriginY() { return originY; }
    public float getStepX() { return stepX; }
    public float getStepY() { return stepY; }
    public float getAlternateColumnOffsetY() { return alternateColumnOffsetY; }
    public int getColumns() { return columns; }
    public int getRows() { return rows; }
    public int getRowsEven() { return rowsEven > 0 ? rowsEven : rows; }
    public int getRowsOdd() { return rowsOdd > 0 ? rowsOdd : rows; }
    public float getHexScaleKm() { return hexScaleKm; }
    public float getHexScaleMiles() { return hexScaleMiles; }
    /** Quilômetros por pixel lógico, calibrados pelo passo horizontal centro-a-centro. */
    public float getKmPerPixel() { return stepX > 0f && hexScaleKm > 0f ? hexScaleKm / stepX : 0f; }
    /** Distância contínua entre centros, sem assumir que cada mudança de hex vale um número fixo. */
    public float distanceKm(String fromId,String toId) {
        ManifestHex a=byId(fromId),b=byId(toId);float kpp=getKmPerPixel();
        if(a==null||b==null||kpp<=0f)return 0f;
        return (float)Math.hypot(b.centerX-a.centerX,b.centerY-a.centerY)*kpp;
    }
    public float distanceKmFromHexToPoint(String fromId,float x,float y) {
        ManifestHex a=byId(fromId);float kpp=getKmPerPixel();
        if(a==null||kpp<=0f)return 0f;
        return (float)Math.hypot(x-a.centerX,y-a.centerY)*kpp;
    }
    public float routeDistanceKm(String startId,List<String> route) {
        float total=0f;String previous=startId;
        if(route!=null)for(String id:route){total+=distanceKm(previous,id);previous=id;}
        return total;
    }
    public float getMinLabelZoom() { return minLabelZoom; }
    public boolean isGenerated() { return generated; }
    public boolean hasExplicitCenters() { return explicitCenters; }

    private static String normalizeProfile(String raw) { String v = raw == null ? "" : raw.toLowerCase(Locale.US); return v.contains("mystara") ? "mystara" : "dnd"; }
    private static String normalizeOrientation(String raw) { String value = raw == null ? "" : raw.toLowerCase(Locale.US); return value.contains("point") || value.contains("ponta") ? "pointy-top" : "flat-top"; }
    private static String firstString(JSONObject object, String... keys) { if (object == null) return ""; for (String key : keys) { String value = object.optString(key, "").trim(); if (value.length() > 0) return value; } return ""; }
    private static double firstPositive(double... values) { for (double value : values) if (!Double.isNaN(value) && value > 0) return value; return 0; }
    private static double firstFinite(double... values) { for (double value : values) if (!Double.isNaN(value) && !Double.isInfinite(value)) return value; return Double.NaN; }
}
