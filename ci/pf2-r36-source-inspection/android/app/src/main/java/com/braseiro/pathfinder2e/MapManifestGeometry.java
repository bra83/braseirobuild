package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Geometria cartográfica Pathfinder independente da arte do mapa.
 * O hexId é sempre calculado pelo manifesto; overlays jamais são lidos por OCR.
 */
public final class MapManifestGeometry {
    private static final int GEOMETRY_CACHE_LIMIT=2;
    private static final LinkedHashMap<String,MapManifestGeometry> CACHE=new LinkedHashMap<String,MapManifestGeometry>(4,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,MapManifestGeometry> eldest){return size()>GEOMETRY_CACHE_LIMIT;}
    };

    public static final class ManifestHex {
        public final String id;
        public final float centerX,centerY,width,height;
        public final int column,row;
        ManifestHex(String id,float x,float y,float w,float h,int c,int r){this.id=id==null?"":id;centerX=x;centerY=y;width=w;height=h;column=c;row=r;}
    }

    private final ArrayList<ManifestHex> hexes=new ArrayList<>();
    private final HashMap<String,ManifestHex> byId=new HashMap<>();
    private final HashMap<Long,ManifestHex> byCoordinate=new HashMap<>();
    private String orientation="flat-top",label="",mapId="",logicalMapId="",mapPackageIdentity="",atlasNamespace="",coordinateFrameId="",setting="Pathfinder 2e",worldProfile="pf2",preferredOverlayMode="raster";
    private String columnIdPolicy="excel_one_based",rowIdPolicy="one_based_integer",hexIdPattern="";
    private float imageWidth,imageHeight,hexWidth,hexHeight,originX,originY,stepX,stepY,alternateColumnOffsetY,hexScaleKm,hexScaleMiles,minLabelZoom=1.6f;
    private int columns,rows,rowsEven,rowsOdd;
    private int[] columnStart;
    private boolean generated,explicitCenters;

    public static MapManifestGeometry parse(String json)throws Exception{
        if(json==null||json.trim().isEmpty())throw new Exception("Manifesto vazio.");
        String key=json.length()+":"+Integer.toHexString(json.hashCode());
        synchronized(CACHE){MapManifestGeometry cached=CACHE.get(key);if(cached!=null)return cached;}
        JSONObject root=new JSONObject(json), map=root;
        JSONArray maps=root.optJSONArray("maps");
        if(maps!=null&&maps.length()>0){map=maps.optJSONObject(0);for(int i=0;i<maps.length();i++){JSONObject c=maps.optJSONObject(i);if(c!=null&&c.optBoolean("enabled",true)){map=c;break;}}}
        if(map==null)throw new Exception("Manifesto sem definição de mapa.");
        JSONObject layout=map.optJSONObject("layout");if(layout==null)layout=root.optJSONObject("layout");if(layout==null)layout=map;
        MapManifestGeometry g=new MapManifestGeometry();
        g.mapId=firstString(map,"mapId","id");if(g.mapId.isEmpty())g.mapId=firstString(root,"mapId","id");
        JSONObject identity=map.optJSONObject("identity");if(identity==null)identity=root.optJSONObject("identity");
        if(identity!=null){g.logicalMapId=identity.optString("logical_map_id","").trim();g.mapPackageIdentity=identity.optString("map_package_id","").trim();g.atlasNamespace=identity.optString("atlas_namespace","").trim();g.coordinateFrameId=identity.optString("coordinate_frame_id","").trim();}
        if(g.logicalMapId.isEmpty())g.logicalMapId=g.mapId;
        g.label=firstString(map,"label","name");if(g.label.isEmpty())g.label=firstString(root,"label","name");
        g.setting=firstString(map,"setting");if(g.setting.isEmpty())g.setting=firstString(root,"setting");if(g.setting.isEmpty())g.setting="Pathfinder 2e";
        g.worldProfile="pf2";
        JSONObject sourceMap=map.optJSONObject("sourceMap");if(sourceMap==null)sourceMap=root.optJSONObject("sourceMap");
        JSONObject image=map.optJSONObject("image");if(image==null)image=root.optJSONObject("image");
        g.imageWidth=(float)firstPositive(sourceMap==null?0:sourceMap.optDouble("width",0),image==null?0:image.optDouble("width",0),layout.optDouble("imageWidth",0),map.optDouble("imageWidth",0),root.optDouble("imageWidth",0));
        g.imageHeight=(float)firstPositive(sourceMap==null?0:sourceMap.optDouble("height",0),image==null?0:image.optDouble("height",0),layout.optDouble("imageHeight",0),map.optDouble("imageHeight",0),root.optDouble("imageHeight",0));
        g.orientation=normalizeOrientation(layout.optString("orientation",map.optString("orientation",root.optString("orientation","flat-top"))));
        g.hexWidth=(float)firstPositive(layout.optDouble("hexWidth",0),layout.optDouble("cellWidth",0),map.optDouble("hexWidth",0));
        g.hexHeight=(float)firstPositive(layout.optDouble("hexHeight",0),layout.optDouble("cellHeight",0),map.optDouble("hexHeight",0));
        if(g.hexWidth<=0&&g.hexHeight>0)g.hexWidth="pointy-top".equals(g.orientation)?(float)(g.hexHeight*Math.sqrt(3)/2):(float)(g.hexHeight*2/Math.sqrt(3));
        if(g.hexHeight<=0&&g.hexWidth>0)g.hexHeight="pointy-top".equals(g.orientation)?(float)(g.hexWidth*2/Math.sqrt(3)):(float)(g.hexWidth*Math.sqrt(3)/2);
        g.columns=layout.optInt("columns",0);g.rows=layout.optInt("rows",0);
        JSONObject parity=layout.optJSONObject("rowsByColumnParity");if(parity!=null){g.rowsEven=Math.max(0,parity.optInt("even",0));g.rowsOdd=Math.max(0,parity.optInt("odd",0));g.rows=Math.max(g.rowsEven,g.rowsOdd);}else{g.rowsEven=g.rows;g.rowsOdd=g.rows;}
        g.originX=(float)firstFinite(layout.optDouble("originX",Double.NaN),layout.optDouble("xStart",Double.NaN),g.hexWidth/2f);
        g.originY=(float)firstFinite(layout.optDouble("originY",Double.NaN),layout.optDouble("yStart",Double.NaN),g.hexHeight/2f);
        g.stepX=(float)firstPositive(layout.optDouble("adjacentColumnCenterDx",0),layout.optDouble("columnStepX",0),layout.optDouble("columnStep",0),layout.optDouble("stepX",0),layout.optDouble("horizontalStep",0));
        g.stepY=(float)firstPositive(layout.optDouble("sameColumnCenterDy",0),layout.optDouble("rowStepY",0),layout.optDouble("rowStep",0),layout.optDouble("stepY",0),layout.optDouble("verticalStep",0));
        if(g.stepX<=0)g.stepX="pointy-top".equals(g.orientation)?g.hexWidth:g.hexWidth*.75f;
        if(g.stepY<=0)g.stepY="pointy-top".equals(g.orientation)?g.hexHeight*.75f:g.hexHeight;
        g.alternateColumnOffsetY=(float)firstFinite(layout.optDouble("alternateColumnOffsetY",Double.NaN),layout.optDouble("oddColumnOffsetY",Double.NaN),g.stepY/2f);
        g.columnIdPolicy=layout.optString("columnIdPolicy",g.columnIdPolicy).trim().toLowerCase(Locale.US);
        g.rowIdPolicy=layout.optString("rowIdPolicy",g.rowIdPolicy).trim().toLowerCase(Locale.US);
        g.hexIdPattern=layout.optString("hexIdPattern","").trim();
        JSONObject scale=map.optJSONObject("scale");if(scale==null)scale=root.optJSONObject("scale");JSONObject d=layout.optJSONObject("distancePerHex");double dv=d==null?0:d.optDouble("value",0);String du=d==null?"":d.optString("unit","").toLowerCase(Locale.US);
        g.hexScaleKm=(float)firstPositive(map.optDouble("hexScaleKm",0),root.optDouble("hexScaleKm",0),layout.optDouble("hexScaleKm",0),scale==null?0:scale.optDouble("kmPerHex",0),scale==null?0:scale.optDouble("kilometersPerHex",0),(du.startsWith("km")||du.contains("kilomet"))?dv:0);
        g.hexScaleMiles=(float)firstPositive(map.optDouble("hexScaleMiles",0),root.optDouble("hexScaleMiles",0),layout.optDouble("hexScaleMiles",0),scale==null?0:scale.optDouble("milesPerHex",0),du.contains("mile")?dv:0);
        if(g.hexScaleKm<=0&&g.hexScaleMiles>0)g.hexScaleKm=g.hexScaleMiles*1.609344f;if(g.hexScaleMiles<=0&&g.hexScaleKm>0)g.hexScaleMiles=g.hexScaleKm/1.609344f;
        if(g.hexScaleMiles<=0&&(g.mapId.startsWith("pathfinder2e-inner-sea-")&&g.mapId.contains("-canonical-")||"pathfinder:golarion:inner_sea".equals(g.logicalMapId))){g.hexScaleMiles=40f;g.hexScaleKm=40f*1.609344f;}
        JSONObject rendering=map.optJSONObject("rendering");if(rendering==null)rendering=root.optJSONObject("rendering");if(rendering!=null){g.preferredOverlayMode=rendering.optString("preferredOverlayMode",g.preferredOverlayMode);JSONObject dyn=rendering.optJSONObject("dynamicLabelRendering");if(dyn!=null)g.minLabelZoom=(float)Math.max(.1,dyn.optDouble("minZoomToShowLabels",1.6));}
        g.explicitCenters=hasExplicitCenters(layout);
        // Legacy-only calibration for the immutable V5 raster. V6 GRID NATIVE HIRES is
        // already rectified and therefore uses the manifest coordinates without runtime transforms.
        g.applyRuntimeRasterCalibration();
        parseExplicit(layout,g);
        if(g.hexes.isEmpty()&&g.columns>0&&g.rows>0)g.generateGrid();
        if(g.hexes.isEmpty())throw new Exception("O manifesto não contém uma grade reconhecível.");
        if(g.imageWidth<=0||g.imageHeight<=0)throw new Exception("O manifesto não informa dimensões válidas da imagem.");
        synchronized(CACHE){CACHE.put(key,g);}return g;
    }

    private void applyRuntimeRasterCalibration(){
        if(!"pathfinder2e-inner-sea-2135x2720-canonical-v5-max-compat".equals(mapId)||explicitCenters)return;
        final float rasterScale=1.004556327f;
        originX=originX*rasterScale-4.7674317f;
        originY=originY*rasterScale+2.5002666f;
        hexWidth*=rasterScale;
        hexHeight*=rasterScale;
        stepX*=rasterScale;
        stepY*=rasterScale;
        alternateColumnOffsetY*=rasterScale;
    }

    private static void parseExplicit(JSONObject layout,MapManifestGeometry g){
        JSONArray rows=layout.optJSONArray("rowDefinitions");if(rows==null)rows=layout.optJSONArray("rowsDefinition");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;float y=(float)firstFinite(row.optDouble("yCenter",Double.NaN),row.optDouble("cy",Double.NaN),row.optDouble("y",Double.NaN));JSONArray xs=row.optJSONArray("xCenters"),ids=row.optJSONArray("hexIds");if(xs!=null)for(int j=0;j<xs.length();j++)g.add(ids==null?"":ids.optString(j,""),(float)xs.optDouble(j),y,g.hexWidth,g.hexHeight,j,i);JSONArray cells=row.optJSONArray("hexes");if(cells==null)cells=row.optJSONArray("cells");if(cells!=null)for(int j=0;j<cells.length();j++)g.parseCell(cells.opt(j),y);}
        JSONArray centers=layout.optJSONArray("centers");if(centers==null)centers=layout.optJSONArray("hexCenters");if(centers!=null)for(int i=0;i<centers.length();i++)g.parseCell(centers.opt(i),Float.NaN);
        JSONArray explicit=layout.optJSONArray("tokens");if(explicit==null)explicit=layout.optJSONArray("hexes");if(explicit==null)explicit=layout.optJSONArray("cells");if(explicit!=null)for(int i=0;i<explicit.length();i++)g.parseCell(explicit.opt(i),Float.NaN);
    }

    private void generateGrid(){generated=true;columnStart=new int[columns+1];int index=0;for(int c=0;c<columns;c++){columnStart[c]=index;for(int r=0;r<rowCount(c);r++){float x,y;if("pointy-top".equals(orientation)){x=originX+c*stepX+((r&1)==1?stepX/2f:0f);y=originY+r*stepY;}else{x=originX+c*stepX;y=originY+r*stepY+((c&1)==1?alternateColumnOffsetY:0f);}add(formatHexId(c,r),x,y,hexWidth,hexHeight,c,r);index++;}}columnStart[columns]=index;}
    private String formatHexId(int c,int r){if(columnIdPolicy.contains("zero_based_integer")||hexIdPattern.contains("{column}")){String row=rowIdPolicy.contains("two_digits")||hexIdPattern.contains("02d")?String.format(Locale.US,"%02d",r):String.valueOf(r);return c+"."+row;}return excelColumn(c)+(r+1);}
    private int rowCount(int c){if(c<0||c>=columns)return 0;if(rowsEven>0||rowsOdd>0)return (c&1)==0?rowsEven:rowsOdd;return rows;}
    private void parseCell(Object value,float rowY){if(value instanceof JSONObject){JSONObject o=(JSONObject)value;float w=(float)firstPositive(o.optDouble("width",0),o.optDouble("hexWidth",0),hexWidth),h=(float)firstPositive(o.optDouble("height",0),o.optDouble("hexHeight",0),hexHeight);double cx=firstFinite(o.optDouble("xCenter",Double.NaN),o.optDouble("centerX",Double.NaN),o.optDouble("cx",Double.NaN)),cy=firstFinite(o.optDouble("yCenter",Double.NaN),o.optDouble("centerY",Double.NaN),o.optDouble("cy",Double.NaN));if(Double.isNaN(cx)){double x=o.optDouble("x",Double.NaN);if(!Double.isNaN(x))cx=x;}if(Double.isNaN(cy)){double y=o.optDouble("y",Double.NaN);if(!Double.isNaN(y))cy=y;else if(!Float.isNaN(rowY))cy=rowY;}String id=firstString(o,"hexId","id","label");int c=o.optInt("column",-1),r=o.optInt("row",-1);if((c<0||r<0)&&!id.isEmpty()){int[] p=parseHexId(id);if(p!=null){c=p[0];r=p[1];}}if(!Double.isNaN(cx)&&!Double.isNaN(cy))add(id,(float)cx,(float)cy,w,h,c,r);}else if(value instanceof JSONArray){JSONArray a=(JSONArray)value;if(a.length()>=2)add(a.length()>2?a.optString(2,""):"",(float)a.optDouble(0),(float)a.optDouble(1),hexWidth,hexHeight,-1,-1);}}
    private void add(String id,float x,float y,float w,float h,int c,int r){if(!Float.isFinite(x)||!Float.isFinite(y))return;if((c<0||r<0)&&id!=null){int[] p=parseHexId(id);if(p!=null){c=p[0];r=p[1];}}ManifestHex hex=new ManifestHex(id,x,y,w>0?w:hexWidth,h>0?h:hexHeight,c,r);hexes.add(hex);if(!hex.id.isEmpty())byId.put(hex.id.toUpperCase(Locale.US),hex);if(c>=0&&r>=0){byCoordinate.put(key(c,r),hex);columns=Math.max(columns,c+1);rows=Math.max(rows,r+1);if((c&1)==0)rowsEven=Math.max(rowsEven,r+1);else rowsOdd=Math.max(rowsOdd,r+1);}}
    private static long key(int c,int r){return (((long)c)<<32)^(r&0xffffffffL);}
    private static boolean hasExplicitCenters(JSONObject l){if(l==null)return false;for(String k:new String[]{"rowDefinitions","rowsDefinition","centers","hexCenters","tokens","hexes","cells"}){JSONArray a=l.optJSONArray(k);if(a!=null&&a.length()>0)return true;}return false;}

    public ManifestHex nearest(float x,float y){if(hexes.isEmpty())return null;if(columns>0&&rows>0&&stepX>0&&stepY>0){int ac,ar;if("pointy-top".equals(orientation)){ar=Math.round((y-originY)/Math.max(.0001f,stepY));float off=(ar&1)==1?stepX/2f:0f;ac=Math.round((x-originX-off)/Math.max(.0001f,stepX));}else{ac=Math.round((x-originX)/Math.max(.0001f,stepX));float off=(ac&1)==1?alternateColumnOffsetY:0f;ar=Math.round((y-originY-off)/Math.max(.0001f,stepY));}ManifestHex best=null;float bd=Float.MAX_VALUE;for(int c=ac-2;c<=ac+2;c++)for(int r=ar-2;r<=ar+2;r++){ManifestHex h=at(c,r);if(h==null)continue;float dx=h.centerX-x,dy=h.centerY-y,d=dx*dx+dy*dy;if(d<bd){bd=d;best=h;}}if(best!=null)return best;}ManifestHex best=null;float bd=Float.MAX_VALUE;for(ManifestHex h:hexes){float dx=h.centerX-x,dy=h.centerY-y,d=dx*dx+dy*dy;if(d<bd){bd=d;best=h;}}return best;}
    public ManifestHex at(int c,int r){if(c<0||r<0||c>=columns||r>=rowCount(c))return null;ManifestHex h=byCoordinate.get(key(c,r));if(h!=null)return h;if(generated&&columnStart!=null){int i=columnStart[c]+r;return i>=0&&i<hexes.size()?hexes.get(i):null;}return null;}
    public ManifestHex byId(String id){if(id==null)return null;ManifestHex direct=byId.get(id.trim().toUpperCase(Locale.US));if(direct!=null)return direct;int[] p=parseHexId(id);return p==null?null:at(p[0],p[1]);}
    public List<String> neighbors(String id){ManifestHex center=byId(id);if(center==null||center.column<0||center.row<0)return Collections.emptyList();int c=center.column,r=center.row;int[][] offsets;if("pointy-top".equals(orientation))offsets=(r&1)==0?new int[][]{{-1,0},{1,0},{-1,-1},{0,-1},{-1,1},{0,1}}:new int[][]{{-1,0},{1,0},{0,-1},{1,-1},{0,1},{1,1}};else offsets=(c&1)==0?new int[][]{{0,-1},{0,1},{-1,-1},{-1,0},{1,-1},{1,0}}:new int[][]{{0,-1},{0,1},{-1,0},{-1,1},{1,0},{1,1}};ArrayList<String> out=new ArrayList<>(6);for(int[] o:offsets){ManifestHex h=at(c+o[0],r+o[1]);if(h!=null&&!h.id.isEmpty())out.add(h.id);}return out;}
    public List<ManifestHex> visible(float left,float top,float right,float bottom){if(columns<=0||rows<=0||stepX<=0||stepY<=0){ArrayList<ManifestHex> o=new ArrayList<>();for(ManifestHex h:hexes)if(h.centerX>=left-hexWidth&&h.centerX<=right+hexWidth&&h.centerY>=top-hexHeight&&h.centerY<=bottom+hexHeight)o.add(h);return o;}int c0=Math.max(0,(int)Math.floor((left-originX-hexWidth)/Math.max(1f,stepX))-1),c1=Math.min(columns-1,(int)Math.ceil((right-originX+hexWidth)/Math.max(1f,stepX))+1);ArrayList<ManifestHex> o=new ArrayList<>();for(int c=c0;c<=c1;c++){float off="pointy-top".equals(orientation)?0f:((c&1)==1?alternateColumnOffsetY:0f);int r0=Math.max(0,(int)Math.floor((top-originY-off-hexHeight)/Math.max(1f,stepY))-1),r1=Math.min(rowCount(c)-1,(int)Math.ceil((bottom-originY-off+hexHeight)/Math.max(1f,stepY))+1);for(int r=r0;r<=r1;r++){ManifestHex h=at(c,r);if(h!=null)o.add(h);}}return o;}

    public static int[] parseHexId(String id){if(id==null)return null;String v=id.trim();if(v.matches("\\d+\\.\\d{1,3}")){String[] p=v.split("\\.");try{return new int[]{Integer.parseInt(p[0]),Integer.parseInt(p[1])};}catch(Exception e){return null;}}return parseExcelHexId(v);}
    public static int[] parseExcelHexId(String id){if(id==null)return null;String v=id.trim().toUpperCase(Locale.US);int split=0;while(split<v.length()&&v.charAt(split)>='A'&&v.charAt(split)<='Z')split++;if(split==0||split==v.length())return null;int c=0;for(int i=0;i<split;i++)c=c*26+(v.charAt(i)-'A'+1);try{return new int[]{c-1,Integer.parseInt(v.substring(split))-1};}catch(Exception e){return null;}}
    public static String excelColumn(int zero){if(zero<0)return "";int v=zero+1;StringBuilder b=new StringBuilder();while(v>0){int r=(v-1)%26;b.append((char)('A'+r));v=(v-1)/26;}return b.reverse().toString();}

    public List<ManifestHex> all(){return Collections.unmodifiableList(hexes);}public int size(){return hexes.size();}public String getOrientation(){return orientation;}public String getLabel(){return label;}public String getMapId(){return mapId;}public String getLogicalMapId(){return logicalMapId;}public String getMapPackageIdentity(){return mapPackageIdentity;}public String getAtlasNamespace(){return atlasNamespace;}public String getCoordinateFrameId(){return coordinateFrameId;}public String getSetting(){return setting;}public String getWorldProfile(){return worldProfile;}public String getPreferredOverlayMode(){return preferredOverlayMode;}public float getImageWidth(){return imageWidth;}public float getImageHeight(){return imageHeight;}public float getHexWidth(){return hexWidth;}public float getHexHeight(){return hexHeight;}public float getOriginX(){return originX;}public float getOriginY(){return originY;}public float getStepX(){return stepX;}public float getStepY(){return stepY;}public float getAlternateColumnOffsetY(){return alternateColumnOffsetY;}public int getColumns(){return columns;}public int getRows(){return rows;}public int getRowsEven(){return rowsEven>0?rowsEven:rows;}public int getRowsOdd(){return rowsOdd>0?rowsOdd:rows;}public float getHexScaleKm(){return hexScaleKm;}public float getHexScaleMiles(){return hexScaleMiles;}public float getKmPerPixel(){return stepX>0&&hexScaleKm>0?hexScaleKm/stepX:0;}public float distanceKm(String a,String b){ManifestHex x=byId(a),y=byId(b);float k=getKmPerPixel();return x==null||y==null||k<=0?0:(float)Math.hypot(y.centerX-x.centerX,y.centerY-x.centerY)*k;}public float distanceKmFromHexToPoint(String a,float x,float y){ManifestHex h=byId(a);float k=getKmPerPixel();return h==null||k<=0?0:(float)Math.hypot(x-h.centerX,y-h.centerY)*k;}public float routeDistanceKm(String start,List<String> route){float total=0;String p=start;if(route!=null)for(String id:route){total+=distanceKm(p,id);p=id;}return total;}public float getMinLabelZoom(){return minLabelZoom;}public boolean isGenerated(){return generated;}public boolean hasExplicitCenters(){return explicitCenters;}
    private static String normalizeOrientation(String raw){String v=raw==null?"":raw.toLowerCase(Locale.US);return v.contains("point")||v.contains("ponta")?"pointy-top":"flat-top";}private static String firstString(JSONObject o,String...keys){if(o==null)return "";for(String k:keys){String v=o.optString(k,"").trim();if(!v.isEmpty())return v;}return "";}private static double firstPositive(double...v){for(double x:v)if(!Double.isNaN(x)&&x>0)return x;return 0;}private static double firstFinite(double...v){for(double x:v)if(!Double.isNaN(x)&&!Double.isInfinite(x))return x;return Double.NaN;}
}
