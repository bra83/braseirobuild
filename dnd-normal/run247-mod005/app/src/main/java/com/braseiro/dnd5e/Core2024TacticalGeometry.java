package com.braseiro.dnd5e;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** RUN200: geometria 2D determinística sobre o tabuleiro tático persistido.
 * Uma célula vale 5 pés; centros de tokens representam criaturas Pontuais para
 * seleção de alvos. Não afirma linha de visão, cobertura ou volumes 3D.
 * A assinatura inclui TODAS as posições: movimento entre prévia e commit invalida.
 */
public final class Core2024TacticalGeometry {
    private Core2024TacticalGeometry() {}
    public static final int MAX_CELLS=100, MAX_TOKENS=256;
    public static final class Token {
        public final long id; public final double x,y;
        public Token(long id,double x,double y){this.id=id;this.x=x;this.y=y;}
    }
    public static final class Plan {
        public final String error,signature;
        public final List<Long> targetIds;
        private Plan(String error,String signature,List<Long> ids){this.error=error;this.signature=signature;this.targetIds=ids;}
        public boolean ok(){return error.length()==0;}
    }
    private static Plan blocked(String reason){return new Plan(reason,"",Collections.<Long>emptyList());}
    private static double cx(Token t,int width){return t.x*width;}
    private static double cy(Token t,int height){return t.y*height;}
    private static boolean finite(double n){return !Double.isNaN(n)&&!Double.isInfinite(n);}
    public static Plan plan(String spell,int width,int height,String sceneKey,String obstacleSignature,
                            List<Token> all,long casterId,int centerX,int centerY,int direction,long touchId){
        if(width<1||height<1||width>MAX_CELLS||height>MAX_CELLS)return blocked("invalid_grid_size");
        if(sceneKey==null||sceneKey.trim().isEmpty())return blocked("scene_not_bound");
        if(all==null||all.isEmpty()||all.size()>MAX_TOKENS)return blocked("invalid_token_count");
        if(!"fireball".equals(spell)&&!"burning_hands".equals(spell)&&!"cure_wounds".equals(spell))
            return blocked("spell_not_compiled");
        List<Token> tokens=new ArrayList<Token>(all);
        Collections.sort(tokens,new Comparator<Token>(){public int compare(Token a,Token b){return Long.compare(a.id,b.id);}});
        Token caster=null;long previous=0;
        for(Token t:tokens){
            if(t==null||t.id<=0||t.id==previous||!finite(t.x)||!finite(t.y)||
                t.x<0||t.x>=1||t.y<0||t.y>=1)return blocked("invalid_or_duplicate_position");
            previous=t.id;
            if(t.id==casterId)caster=t;
        }
        if(caster==null)return blocked("caster_position_missing");
        if("fireball".equals(spell)){
            if(centerX<0||centerX>=width||centerY<0||centerY>=height)return blocked("invalid_impact_cell");
            // 150 ft, geometric distance to chosen grid-cell center.
            if(Math.hypot(cx(caster,width)-(centerX+.5),cy(caster,height)-(centerY+.5))*5>150.000001)
                return blocked("spell_out_of_range");
        }
        if("burning_hands".equals(spell)&&(direction<0||direction>7))return blocked("invalid_cone_direction");
        if("cure_wounds".equals(spell)&&touchId<=0)return blocked("touch_target_required");
        double[] dirsX={1,Math.sqrt(.5),0,-Math.sqrt(.5),-1,-Math.sqrt(.5),0,Math.sqrt(.5)};
        double[] dirsY={0,Math.sqrt(.5),1,Math.sqrt(.5),0,-Math.sqrt(.5),-1,-Math.sqrt(.5)};
        List<Long> targets=new ArrayList<Long>();
        for(Token t:tokens){
            double dx=cx(t,width)-cx(caster,width),dy=cy(t,height)-cy(caster,height);
            boolean hit=false;
            if("fireball".equals(spell)){
                hit=Math.hypot(cx(t,width)-(centerX+.5),cy(t,height)-(centerY+.5))*5<=20.000001;
            }else if("burning_hands".equals(spell)){
                double projection=dx*dirsX[direction]+dy*dirsY[direction];
                double lateral=Math.abs(dx*dirsY[direction]-dy*dirsX[direction]);
                // Cone: 15 ft from the caster, width at each point equals distance from origin.
                hit=t.id!=casterId&&projection>0&&projection<=3.000001&&
                    lateral<=projection*.5+0.000001&&Math.hypot(dx,dy)<=3.000001;
            }else{
                // A touch spell may target the caster or an adjacent 5-foot square.
                hit=t.id==touchId&&Math.max(Math.abs(Math.floor(cx(t,width))-Math.floor(cx(caster,width))),
                    Math.abs(Math.floor(cy(t,height))-Math.floor(cy(caster,height))))<=1;
            }
            if(hit)targets.add(t.id);
        }
        if("cure_wounds".equals(spell)&&targets.size()!=1)return blocked("touch_target_out_of_reach");
        if(targets.isEmpty())return blocked("no_targets_in_area");
        if(targets.size()>64)return blocked("too_many_affected_targets");
        StringBuilder raw=new StringBuilder("v1|").append(spell).append('|').append(width).append('|')
            .append(height).append('|').append(sceneKey).append('|').append(obstacleSignature==null?"":obstacleSignature)
            .append('|').append(casterId).append('|').append(centerX).append('|').append(centerY)
            .append('|').append(direction).append('|').append(touchId);
        for(Token t:tokens)raw.append('|').append(t.id).append(':')
            .append(Double.doubleToLongBits(t.x)).append(':').append(Double.doubleToLongBits(t.y));
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder(64);
            for(byte b:digest)hex.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));
            return new Plan("",hex.toString(),Collections.unmodifiableList(targets));
        }catch(Exception e){return blocked("geometry_signature_unavailable");}
    }
    public static boolean exactTargets(Plan p,List<Long> chosen){
        if(p==null||!p.ok()||chosen==null||chosen.size()!=p.targetIds.size())return false;
        List<Long> ids=new ArrayList<Long>(chosen);Collections.sort(ids);
        for(int i=0;i<ids.size();i++)if(!ids.get(i).equals(p.targetIds.get(i)))return false;
        return true;
    }
}
