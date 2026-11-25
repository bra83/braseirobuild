package br.com.tutorclinico;
import java.util.*;
/** Cased BERT WordPiece pair encoder. Refuses truncation instead of losing clinical qualifiers. */
final class NliWordPiece {
    private static boolean punctuation(int cp){
        int t=Character.getType(cp);
        return (cp>=33&&cp<=47)||(cp>=58&&cp<=64)||(cp>=91&&cp<=96)||(cp>=123&&cp<=126)||
            t==Character.CONNECTOR_PUNCTUATION||t==Character.DASH_PUNCTUATION||t==Character.START_PUNCTUATION||
            t==Character.END_PUNCTUATION||t==Character.INITIAL_QUOTE_PUNCTUATION||t==Character.FINAL_QUOTE_PUNCTUATION||t==Character.OTHER_PUNCTUATION;
    }
    static List<Integer> tokens(String text,Map<String,Integer> vocab){
        StringBuilder clean=new StringBuilder();
        text.codePoints().forEach(cp->{
            if(cp==0||cp==0xfffd)return;
            if(Character.isWhitespace(cp)||Character.isSpaceChar(cp)){clean.append(' ');return;}
            int type=Character.getType(cp);
            if(type==Character.CONTROL||type==Character.FORMAT)return;
            if(punctuation(cp)||Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN)
                clean.append(' ').appendCodePoint(cp).append(' ');
            else clean.appendCodePoint(cp);
        });
        List<Integer> out=new ArrayList<>();
        for(String word:java.text.Normalizer.normalize(clean.toString(),java.text.Normalizer.Form.NFC).trim().split("\\s+")){
            if(word.isEmpty())continue;
            if(word.codePointCount(0,word.length())>100){out.add(vocab.get("[UNK]"));continue;}
            ArrayList<Integer> part=new ArrayList<>();
            int start=0;
            while(start<word.length()){
                int end=word.length();Integer found=null;
                while(end>start){
                    found=vocab.get((start==0?"":"##")+word.substring(start,end));
                    if(found!=null)break;
                    end=word.offsetByCodePoints(end,-1);
                }
                if(found==null){part.clear();part.add(vocab.get("[UNK]"));break;}
                part.add(found);start=end;
            }
            out.addAll(part);
        }
        return out;
    }

    static long[][] pair(String a,String b,Map<String,Integer> vocab){
        List<Integer> x=tokens(a,vocab),y=tokens(b,vocab);
        if(x.isEmpty()||y.isEmpty()||x.size()+y.size()+3>256)throw new IllegalArgumentException("Par vazio ou acima de 256 tokens");
        int n=x.size()+y.size()+3;long[] ids=new long[n],mask=new long[n],types=new long[n];
        int k=0;ids[k++]=vocab.get("[CLS]");for(int t:x)ids[k++]=t;ids[k++]=vocab.get("[SEP]");
        for(int t:y){ids[k]=t;types[k++]=1;}ids[k]=vocab.get("[SEP]");types[k]=1;Arrays.fill(mask,1);
        return new long[][]{ids,mask,types};
    }
}
