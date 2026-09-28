package com.braseiro.pathfinder2e;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Vincula automaticamente imagens de assets/npc a NPCs sem token.
 * O nome detalhado do arquivo é o manifesto: barman_anao_barba_ruiva.png.
 * Tokens escolhidos manualmente nunca são substituídos.
 */
public final class NpcTokenAutoBinder {
    private static final String[] EXT={".png",".jpg",".jpeg",".webp"};
    private final Context context;
    private final AppDb db;
    private final DriveService drive;
    private final AssetManager assets;
    private final ArrayList<String> catalog=new ArrayList<String>();
    private boolean scanned=false;

    public NpcTokenAutoBinder(Context value,AppDb database){this(value,database,null);}
    public NpcTokenAutoBinder(Context value,AppDb database,DriveService service){context=value.getApplicationContext();db=database;drive=service;assets=context.getAssets();}

    public synchronized int catalogSize(){ensureScanned();return catalog.size();}

    public synchronized Result assignMissingTokens(){
        ensureScanned();List<JSONObject> synced=db.listTokens("","Todas");Result result=new Result(catalog.size()+synced.size());JSONArray npcs=db.listNpcMemories(300);Set<String> alreadyUsed=usedAssetNames(npcs,synced);
        for(int i=0;i<npcs.length();i++){
            JSONObject npc=npcs.optJSONObject(i);if(npc==null)continue;
            if(npc.optString("token_uri","").trim().length()>0){result.preserved++;continue;}
            Match match=bestMatch(npc,alreadyUsed,synced);
            try{
                String uri;
                if(match==null){uri=NpcFallbackAvatarFactory.create(context,npc);result.generated++;result.assigned.put(npc.optString("name","NPC"),"avatar local coerente com a ficha");}
                else{uri=materialize(match,npc.optLong("id"));alreadyUsed.add(match.key);result.assigned.put(npc.optString("name","NPC"),match.label+" ("+match.score+")");}
                db.setNpcToken(npc.optLong("id"),uri);
            }catch(Exception e){result.unmatched.add(npc.optString("name","NPC sem nome"));result.errors.add(npc.optString("name","NPC")+": "+String.valueOf(e.getMessage()));}
        }
        return result;
    }

    public synchronized boolean assignIfMissing(String npcName){
        if(npcName==null||npcName.trim().length()==0)return false;ensureScanned();JSONObject npc=db.findNpcMemoryByName(npcName);if(npc.length()==0||npc.optString("token_uri","").trim().length()>0)return false;
        List<JSONObject> synced=db.listTokens("","Todas");Match match=bestMatch(npc,usedAssetNames(db.listNpcMemories(300),synced),synced);
        try{String uri=match==null?NpcFallbackAvatarFactory.create(context,npc):materialize(match,npc.optLong("id"));db.setNpcToken(npc.optLong("id"),uri);return true;}catch(Exception ignored){return false;}
    }

    private void ensureScanned(){if(scanned)return;catalog.clear();scanFolder("npc");scanned=true;}
    private void scanFolder(String path){try{String[] names=assets.list(path);if(names==null)return;for(String name:names){String child=path.length()==0?name:path+"/"+name;String[] nested=assets.list(child);if(nested!=null&&nested.length>0)scanFolder(child);else if(isImage(name))catalog.add(child);}}catch(Exception ignored){AppLog.ignored("NpcTokenAutoBinder","Falha tolerada; a execução continuou.",ignored);}}
    private static boolean isImage(String name){String n=(name==null?"":name).toLowerCase(Locale.US);for(String ext:EXT)if(n.endsWith(ext))return true;return false;}

    private Set<String> usedAssetNames(JSONArray npcs,List<JSONObject> synced){HashSet<String> set=new HashSet<String>();for(int i=0;i<npcs.length();i++){JSONObject npc=npcs.optJSONObject(i);if(npc==null)continue;String uri=npc.optString("token_uri","");if(uri.contains("npc_avatars/auto_")){String last=Uri.parse(uri).getLastPathSegment();String normalized=normalizeAssetName(last==null?"":last.replaceFirst("^auto_[0-9]+_",""));if(normalized.length()>0)set.add(normalized);}for(JSONObject token:synced)if(uri.length()>0&&uri.equals(token.optString("local_uri")))set.add(normalizeAssetName(token.optString("name")));}return set;}

    private Match bestMatch(JSONObject npc,Set<String> alreadyUsed,List<JSONObject> synced){
        String corpus=normalize(npc.optString("name")+" "+npc.optString("summary")+" "+npc.optString("visual_description")+" "+npc.optString("goals")+" "+npc.optString("attitude")+" "+npc.optString("relationship")+" "+npc.optString("stats_json"));
        if(corpus.length()<3||(catalog.isEmpty()&&synced.isEmpty()))return null;Match best=null;
        for(String asset:catalog){String file=normalizeAssetName(asset);int score=score(corpus,file);if(alreadyUsed.contains(file))score-=3;if(best==null||score>best.score)best=new Match(asset,"",file,asset,score,null);}
        for(JSONObject token:synced){String uri=token.optString("local_uri","").trim();if(uri.length()==0&&(drive==null||!drive.isConnected()))continue;String label=token.optString("name",token.optString("label")),file=normalizeAssetName(label);int score=score(corpus,file);if(alreadyUsed.contains(file))score-=3;if(best==null||score>best.score)best=new Match("",uri,file,label,score,token);}
        // Sem características compatíveis, é melhor deixar sem token do que exibir um NPC errado.
        return best!=null&&best.score>=12?best:null;
    }

    private int score(String npc,String file){
        int score=0;HashSet<String> seen=new HashSet<String>();for(String word:file.split(" ")){if(word.length()<3||!seen.add(word))continue;if(containsWord(npc,word))score+=word.length()>=8?7:(word.length()>=5?5:3);}
        score+=groupScore(npc,file,"homem,masculino,male,rapaz,menino","homem,masculino,male,rapaz,menino",16);
        score+=groupScore(npc,file,"mulher,feminino,female,moca,moça,menina","mulher,feminino,female,moca,moça,menina",16);
        score+=groupScore(npc,file,"anao,anão,dwarf","anao,anão,dwarf",18);
        score+=groupScore(npc,file,"ana,anã,dwarf mulher","ana,anã,dwarf female",18);
        score+=groupScore(npc,file,"elfo,elfa,elf,elven","elfo,elfa,elf,elven",18);
        score+=groupScore(npc,file,"halfling,hobbit,pequenino","halfling,hobbit,pequenino",18);
        score+=groupScore(npc,file,"orc,meio orc,half orc","orc,meio orc,half orc",18);
        score+=groupScore(npc,file,"tiefling","tiefling",18);
        score+=groupScore(npc,file,"dragonborn,draconato","dragonborn,draconato",18);
        score+=groupScore(npc,file,"crianca,criança,menino,menina,child,kid","crianca,criança,menino,menina,child,kid",14);
        score+=groupScore(npc,file,"velho,velha,idoso,idosa,ancião,anciao,old,elderly","velho,velha,idoso,idosa,ancião,anciao,old,elderly",12);
        score+=groupScore(npc,file,"jovem,young","jovem,young",8);
        score+=groupScore(npc,file,"barman,taverneiro,estalajadeiro,innkeeper,bartender","barman,taverneiro,estalajadeiro,innkeeper,bartender",16);
        score+=groupScore(npc,file,"mendigo,beggar","mendigo,beggar",16);
        score+=groupScore(npc,file,"andarilho,viajante,viajante itinerante,wanderer,traveler,traveller","andarilho,viajante,wanderer,traveler,traveller",14);
        score+=groupScore(npc,file,"guarda,soldado,guard,soldier","guarda,soldado,guard,soldier",14);
        score+=groupScore(npc,file,"mago,maga,feiticeiro,feiticeira,wizard,mage,sorcerer","mago,maga,feiticeiro,feiticeira,wizard,mage,sorcerer",14);
        score+=groupScore(npc,file,"ladino,ladina,rogue,thief,ladrao,ladrão","ladino,ladina,rogue,thief,ladrao,ladrão",14);
        score+=groupScore(npc,file,"arqueiro,arqueira,arco,flecha,archer,bow","arqueiro,arqueira,arco,flecha,archer,bow",12);
        score+=groupScore(npc,file,"ferreiro,ferreira,blacksmith","ferreiro,ferreira,blacksmith",14);
        score+=groupScore(npc,file,"mercador,mercadora,comerciante,merchant,trader","mercador,mercadora,comerciante,merchant,trader",14);
        score+=groupScore(npc,file,"padre,sacerdote,sacerdotisa,clerigo,clérigo,priest,cleric","padre,sacerdote,sacerdotisa,clerigo,clérigo,priest,cleric",14);
        score+=groupScore(npc,file,"escriba,escrivao,escrivão,registrador,encarregado,oficial,burocrata,scribe,clerk,registrar","escriba,escrivao,escrivão,registrador,oficial,burocrata,scribe,clerk,registrar",18);
        score+=groupScore(npc,file,"oculos,óculos,glasses,monoculo,monóculo","oculos,óculos,glasses,monoculo,monóculo",8);
        score+=groupScore(npc,file,"pena,prancheta,livro,registro,quill,clipboard,ledger","pena,prancheta,livro,registro,quill,clipboard,ledger",8);
        score+=groupScore(npc,file,"loiro,loira,louro,loura,blond,blonde","loiro,loira,louro,loura,blond,blonde",8);
        score+=groupScore(npc,file,"ruivo,ruiva,redhead,cabelo vermelho","ruivo,ruiva,redhead,cabelo vermelho",8);
        score+=groupScore(npc,file,"barba,barbudo,beard","barba,barbudo,beard",8);
        score-=conflict(npc,file,"homem,masculino,male,rapaz","mulher,feminino,female,moca,moça",22);
        score-=conflict(npc,file,"mulher,feminino,female,moca,moça","homem,masculino,male,rapaz",22);
        score-=conflict(npc,file,"crianca,criança,child,kid,menino,menina","velho,velha,idoso,idosa,old,elderly",18);
        score-=conflict(npc,file,"velho,velha,idoso,idosa,old,elderly","crianca,criança,child,kid,menino,menina",18);
        return score;
    }

    private static int groupScore(String npc,String file,String npcWords,String fileWords,int value){return containsAny(npc,npcWords)&&containsAny(file,fileWords)?value:0;}
    private static int conflict(String npc,String file,String npcWords,String incompatible,int value){return containsAny(npc,npcWords)&&containsAny(file,incompatible)?value:0;}
    private static boolean containsAny(String text,String csv){for(String raw:csv.split(","))if(containsWord(text,normalize(raw)))return true;return false;}
    private static boolean containsWord(String text,String word){return text.equals(word)||text.startsWith(word+" ")||text.endsWith(" "+word)||text.contains(" "+word+" ");}

    private File copyAsset(String asset,long npcId)throws Exception{
        String lower=asset.toLowerCase(Locale.US);String ext=".png";for(String candidate:EXT)if(lower.endsWith(candidate)){ext=candidate;break;}
        File dir=new File(context.getFilesDir(),"npc_avatars");if(!dir.exists()&&!dir.mkdirs())throw new Exception("não foi possível criar npc_avatars");
        String descriptive=normalizeAssetName(asset).replace(' ','_');if(descriptive.length()>80)descriptive=descriptive.substring(0,80);File out=new File(dir,"auto_"+npcId+"_"+descriptive+ext);InputStream in=assets.open(asset);FileOutputStream stream=new FileOutputStream(out,false);byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))>0)stream.write(buffer,0,n);in.close();stream.close();return out;
    }

    private String materialize(Match match,long npcId)throws Exception{
        if(match.localUri.length()>0)return match.localUri;
        if(match.asset.length()>0)return Uri.fromFile(copyAsset(match.asset,npcId)).toString();
        if(match.token==null||drive==null||!drive.isConnected())throw new Exception("token remoto indisponível");
        InputStream in=drive.downloadFile(match.token.optString("drive_id"));String mime=match.token.optString("mime_type","image/png");Uri stored=ImageStorageManager.importImage(context,in,mime,"npc_avatars","auto_"+npcId+"_"+match.key.replace(' ','_'),2000);db.setTokenLocalUri(match.token.optLong("id"),stored.toString());return stored.toString();
    }

    private static String normalizeAssetName(String asset){String n=asset==null?"":asset.replace('\\','/');int slash=n.lastIndexOf('/');if(slash>=0)n=n.substring(slash+1);int dot=n.lastIndexOf('.');if(dot>0)n=n.substring(0,dot);return normalize(n);}
    private static String normalize(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}

    private static final class Match{final String asset,localUri,key,label;final int score;final JSONObject token;Match(String a,String uri,String k,String l,int s,JSONObject t){asset=a;localUri=uri;key=k;label=l;score=s;token=t;}}

    public static final class Result{
        public final int catalogSize;public int preserved=0;public int generated=0;public final LinkedHashMap<String,String> assigned=new LinkedHashMap<String,String>();public final List<String> unmatched=new ArrayList<String>();public final List<String> errors=new ArrayList<String>();
        Result(int size){catalogSize=size;}
        public String summary(){StringBuilder b=new StringBuilder();b.append("Tokens na pasta NPC: ").append(catalogSize).append("\nNPCs vinculados agora: ").append(assigned.size()).append("\nTokens existentes preservados: ").append(preserved).append("\nAvatares locais gerados: ").append(generated).append("\nSem correspondência segura: ").append(unmatched.size()).append("\nErros: ").append(errors.size());if(!assigned.isEmpty()){b.append("\n\nVinculados:");for(String name:assigned.keySet())b.append("\n• ").append(name).append(" → ").append(assigned.get(name));}if(!unmatched.isEmpty()){b.append("\n\nAinda sem token:");for(String name:unmatched)b.append("\n• ").append(name);}if(!errors.isEmpty()){b.append("\n\nErros:");for(String error:errors)b.append("\n• ").append(error);}return b.toString();}
    }
}
