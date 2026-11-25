package br.com.tutorclinico;
import java.io.*;
import java.text.Normalizer;
import java.util.*;
/** XLM-R unigram segmentation, checked against the pinned HF tokenizer at build time. */
final class RerankTokenizer {
 static final class Piece {final int id;final double score;Piece(int i,double s){id=i;score=s;}}
 final Map<String,Piece> vocab=new HashMap<>();int max=1;double unknown=-100;
 RerankTokenizer(Reader reader)throws IOException{
  try(BufferedReader b=new BufferedReader(reader)){String line;int id=0;double min=0;while((line=b.readLine())!=null){int k=line.lastIndexOf('\t');if(k<0)throw new IOException("Vocabulário inválido");String p=line.substring(0,k);double s=Double.parseDouble(line.substring(k+1));if(!Double.isFinite(s))throw new IOException("Score inválido");vocab.put(p,new Piece(id++,s));max=Math.max(max,p.length());min=Math.min(min,s);}unknown=min-10;}
  if(vocab.size()<100000||vocab.get("<s>").id!=0||vocab.get("</s>").id!=2||vocab.get("<unk>").id!=3)throw new IOException("Vocabulário incompatível");
 }
 List<Integer> encode(String s){
  s=Normalizer.normalize(s,Normalizer.Form.NFKC).replaceAll("[\\p{Z}\\s]+"," ").trim();
  if(s.isEmpty())return Collections.emptyList();
  s="▁"+s.replace(' ','▁');int n=s.length();
  double[] dp=new double[n+1];Arrays.fill(dp,Double.NEGATIVE_INFINITY);dp[0]=0;int[] prev=new int[n+1],ids=new int[n+1];Arrays.fill(prev,-1);
  for(int i=0;i<n;i++){if(!Double.isFinite(dp[i]))continue;int single=i+Character.charCount(s.codePointAt(i));boolean hasSingle=false;
   for(int j=single;j<=Math.min(n,i+max);){Piece p=vocab.get(s.substring(i,j));if(p!=null&&p.id>3){if(j==single)hasSingle=true;double v=dp[i]+p.score;if(v>dp[j]){dp[j]=v;prev[j]=i;ids[j]=p.id;}}if(j==n)break;j+=Character.charCount(s.codePointAt(j));}
   if(!hasSingle&&dp[i]+unknown>dp[single]){dp[single]=dp[i]+unknown;prev[single]=i;ids[single]=3;}
  }
  List<Integer> result=new ArrayList<>();for(int j=n;j>0;){if(prev[j]<0)throw new IllegalArgumentException("Tokenização inválida");result.add(ids[j]);j=prev[j];}Collections.reverse(result);
  List<Integer> collapsed=new ArrayList<>();for(int id:result)if(id!=3||collapsed.isEmpty()||collapsed.get(collapsed.size()-1)!=3)collapsed.add(id);return collapsed;
 }
 long[] pair(String a,String b){List<Integer> x=encode(a),y=encode(b);if(x.isEmpty()||y.isEmpty()||x.size()+y.size()+4>384)throw new IllegalArgumentException("Par acima de 384 tokens ou vazio");long[] p=new long[x.size()+y.size()+4];int i=0;p[i++]=0;for(int t:x)p[i++]=t;p[i++]=2;p[i++]=2;for(int t:y)p[i++]=t;p[i]=2;return p;}
}
