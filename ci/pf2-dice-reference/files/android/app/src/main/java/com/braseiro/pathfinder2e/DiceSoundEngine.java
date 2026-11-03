package com.braseiro.pathfinder2e;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.util.HashMap;

/** Sons curtos de colisão por material, carregados localmente. */
public final class DiceSoundEngine {
    private final SoundPool pool; private final HashMap<String,Integer> sounds=new HashMap<String,Integer>();
    public DiceSoundEngine(Context c){AudioAttributes aa=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();pool=new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(aa).build();sounds.put("plastic",pool.load(c,R.raw.dice_plastic,1));sounds.put("stone",pool.load(c,R.raw.dice_stone,1));sounds.put("metal",pool.load(c,R.raw.dice_metal,1));sounds.put("bone",pool.load(c,R.raw.dice_bone,1));sounds.put("wood",pool.load(c,R.raw.dice_wood,1));sounds.put("crystal",pool.load(c,R.raw.dice_crystal,1));sounds.put("glass",pool.load(c,R.raw.dice_glass,1));sounds.put("obsidian",pool.load(c,R.raw.dice_obsidian,1));sounds.put("ivory",pool.load(c,R.raw.dice_ivory,1));sounds.put("leather",pool.load(c,R.raw.dice_leather,1));}
    public void play(DiceAppearance d,double impulse,int sides){if(d==null||"silent".equalsIgnoreCase(d.sound))return;String key=d.sound==null||"auto".equalsIgnoreCase(d.sound)?normalize(d.material):normalize(d.sound);Integer id=sounds.get(key);if(id==null)id=sounds.get("plastic");float volume=(float)Math.max(.08,Math.min(.72,impulse/7.0));float rate=(float)Math.max(.76,Math.min(1.25,1.08-(sides-6)*.009));pool.play(id,volume,volume,1,0,rate);}
    private static String normalize(String s){String x=s==null?"plastic":s.toLowerCase();if(x.contains("pedra"))return "stone";if(x.contains("metal"))return "metal";if(x.contains("osso"))return "bone";if(x.contains("madeira"))return "wood";if(x.contains("cristal"))return "crystal";if(x.contains("vidro"))return "glass";if(x.contains("obsidian"))return "obsidian";if(x.contains("marfim"))return "ivory";if(x.contains("couro"))return "leather";if(soundsKey(x))return x;return "plastic";}
    private static boolean soundsKey(String x){return "plastic".equals(x)||"stone".equals(x)||"metal".equals(x)||"bone".equals(x)||"wood".equals(x)||"crystal".equals(x)||"glass".equals(x)||"obsidian".equals(x)||"ivory".equals(x)||"leather".equals(x);}
    public void release(){pool.release();}
}
