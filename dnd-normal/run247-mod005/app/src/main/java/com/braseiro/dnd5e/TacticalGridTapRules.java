package com.braseiro.dnd5e;

/** G08 screen -> persisted-grid cell adapter. No Android dependencies.
 * The UI must submit this cell to AppDb's preview/commit path, never persist it directly. */
public final class TacticalGridTapRules {
    private TacticalGridTapRules() {}
    public static final class Cell {
        public final int x,y;public final String blocked;
        private Cell(int x,int y,String reason){this.x=x;this.y=y;this.blocked=reason;}
        public boolean valid(){return blocked.isEmpty();}
    }
    public static Cell fromTap(int viewWidth,int viewHeight,int gridWidth,int gridHeight,
                               float pixelX,float pixelY){
        if(viewWidth<=0||viewHeight<=0||gridWidth<1||gridWidth>100||gridHeight<1||gridHeight>100)
            return new Cell(-1,-1,"grid_unavailable");
        if(Float.isNaN(pixelX)||Float.isInfinite(pixelX)||Float.isNaN(pixelY)||Float.isInfinite(pixelY)||
           pixelX<0f||pixelY<0f||pixelX>=viewWidth||pixelY>=viewHeight)
            return new Cell(-1,-1,"outside_board");
        int x=(int)Math.floor((double)pixelX*gridWidth/viewWidth);
        int y=(int)Math.floor((double)pixelY*gridHeight/viewHeight);
        if(x<0||x>=gridWidth||y<0||y>=gridHeight)
            return new Cell(-1,-1,"cell_out_of_bounds");
        return new Cell(x,y,"");
    }
}
