package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Catálogo local, determinístico e sem OCR dos pontos clicáveis dos sete mapas
 * ilustrados de Strixhaven que acompanham o app.
 *
 * As coordenadas são normalizadas (0..1) no bitmap original. Os nomes abaixo
 * vêm dos rótulos já presentes nos mapas empacotados. O tempo de deslocamento
 * NÃO é apresentado como regra oficial de D&D: é uma estimativa temporal do
 * app para manter agenda, próxima aula e mundo vivo coerentes.
 */
public final class StrixhavenLocationCatalog {
    private StrixhavenLocationCatalog(){}

    public static final class Hotspot {
        public final String id;
        public final String label;
        public final String category;
        public final String mapId;
        public final String targetMapId;
        public final float x;
        public final float y;
        public final String description;
        public final boolean college;

        Hotspot(String id,String label,String category,String mapId,String targetMapId,float x,float y,String description,boolean college){
            this.id=id;this.label=label;this.category=category;this.mapId=mapId;this.targetMapId=targetMapId;
            this.x=x;this.y=y;this.description=description;this.college=college;
        }
    }

    private static Hotspot h(String id,String label,String category,String map,String target,float x,float y,String description,boolean college){
        return new Hotspot(id,label,category,map,target,x,y,description,college);
    }

    public static List<Hotspot> forMap(String mapId){
        ArrayList<Hotspot> out=new ArrayList<Hotspot>();
        String id=mapId==null?"university":mapId.toLowerCase();
        if("university".equals(id)){
            out.add(h("central","Campus Central","campus","university","central",.50f,.60f,"O coração comum da universidade, com Biblioplex, Archway Commons e serviços estudantis.",false));
            out.add(h("lorehold","Lorehold","college","university","lorehold",.27f,.45f,"O colégio voltado ao passado, história, arqueologia e espírito de descoberta.",true));
            out.add(h("silverquill","Silverquill","college","university","silverquill",.56f,.36f,"O colégio de eloquência, estilo, palavra e poder expressivo.",true));
            out.add(h("quandrix","Quandrix","college","university","quandrix",.80f,.50f,"O colégio de padrões, números, teoria e magia matemática.",true));
            out.add(h("prismari","Prismari","college","university","prismari",.14f,.65f,"O colégio de performance, arte e expressão elemental.",true));
            out.add(h("witherbloom","Witherbloom","college","university","witherbloom",.73f,.68f,"O colégio de essência, crescimento, decadência e ciclos vitais.",true));
        }else if("central".equals(id)){
            out.add(h("biblioplex","Biblioplex","library","central","",.50f,.33f,"A grande biblioteca no centro do campus.",false));
            out.add(h("bows_end_tavern","Bow's End Tavern","tavern","central","",.31f,.52f,"Taverna e ponto de convivência estudantil.",false));
            out.add(h("strixhaven_stadium","Strixhaven Stadium","arena","central","",.16f,.68f,"Estádio para competições e grandes eventos.",false));
            out.add(h("firejolt_cafe","Firejolt Café","cafe","central","",.63f,.64f,"Café frequentado por estudantes e professores.",false));
            out.add(h("archway_commons","Archway Commons","commons","central","",.50f,.75f,"Praça e área de circulação do Campus Central.",false));
            out.add(h("aerojaunt_field","Aerojaunt Field","field","central","",.14f,.33f,"Campo aberto para atividades do campus.",false));
            out.add(h("dapplewings_manor","Captain Dapplewing's Manor","manor","central","",.72f,.14f,"Solar destacado na parte norte do Campus Central.",false));
        }else if("lorehold".equals(id)){
            out.add(h("pillardrop","Pillardrop","landmark","lorehold","",.38f,.28f,"Marco elevado de Lorehold.",false));
            out.add(h("kollema_hall","Kollema Hall","hall","lorehold","",.35f,.52f,"Um dos principais salões de Lorehold.",false));
            out.add(h("effigy_row","Effigy Row","landmark","lorehold","",.57f,.36f,"Fileira monumental no coração de Lorehold.",false));
        }else if("prismari".equals(id)){
            out.add(h("opus_walk","Opus Walk","walk","prismari","",.45f,.34f,"Passeio de obras e performances em Prismari.",false));
            out.add(h("conjurot_hall","Conjurot Hall","hall","prismari","",.60f,.46f,"Salão de atividades e apresentações de Prismari.",false));
            out.add(h("furygale","Furygale","landmark","prismari","",.36f,.64f,"Área marcada por energia elemental e expressão artística.",false));
        }else if("quandrix".equals(id)){
            out.add(h("torus_hall","Torus Hall","hall","quandrix","",.40f,.49f,"Salão central de Quandrix.",false));
            out.add(h("cultivarium","The Cultivarium","garden","quandrix","",.69f,.47f,"Área de cultivo e estudo de padrões naturais.",false));
            out.add(h("arithmodrome","The Arithmodrome","arena","quandrix","",.20f,.73f,"Espaço de experimentação e demonstrações matemágicas.",false));
        }else if("silverquill".equals(id)){
            out.add(h("dramarium","The Dramarium","stage","silverquill","",.18f,.47f,"Espaço de expressão e apresentações de Silverquill.",false));
            out.add(h("grandloft_hall","Grandloft Hall","hall","silverquill","",.53f,.38f,"Grande salão central de Silverquill.",false));
            out.add(h("rose_stage","The Rose Stage","stage","silverquill","",.78f,.64f,"Palco para discursos, performances e eventos.",false));
        }else if("witherbloom".equals(id)){
            out.add(h("sedgemoor","Sedgemoor","marsh","witherbloom","",.48f,.19f,"Região pantanosa e de estudo natural de Witherbloom.",false));
            out.add(h("detention_bog","Detention Bog","marsh","witherbloom","",.79f,.40f,"Pântano isolado marcado no mapa de Witherbloom.",false));
            out.add(h("wiltroot_hall","Wiltroot Hall","hall","witherbloom","",.56f,.54f,"Salão de Witherbloom entre raízes e vegetação densa.",false));
            out.add(h("widdershins_hall","Widdershins Hall","hall","witherbloom","",.31f,.64f,"Um dos principais salões de Witherbloom.",false));
        }
        return Collections.unmodifiableList(out);
    }

    public static Hotspot find(String mapId,String hotspotId){
        if(hotspotId==null)return null;
        for(Hotspot h:forMap(mapId))if(hotspotId.equalsIgnoreCase(h.id))return h;
        return null;
    }

    public static String collegeForMap(String mapId){
        if(mapId==null)return "";String id=mapId.toLowerCase();
        if("lorehold".equals(id)||"prismari".equals(id)||"quandrix".equals(id)||"silverquill".equals(id)||"witherbloom".equals(id))return id;
        return "";
    }
}
