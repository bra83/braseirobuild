package br.com.tutorclinico;
import java.util.Arrays;
public final class EcoEmbeddingCacheTest {
 static void check(boolean x,String msg){if(!x)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  EcoEmbeddingCache c=new EcoEmbeddingCache();float[] v=new float[512];v[0]=1;
  long a=c.open("case:a","model-v1");c.put(a,"Texto de teste",v);v[0]=0;
  check(c.get(a,"Texto de teste")[0]==1,"defensive copy on put");
  float[] copy=c.get(a,"Texto de teste");copy[0]=0;check(c.get(a,"Texto de teste")[0]==1,"defensive copy on read");
  check(c.get(a,"Texto de teste corrigido")==null,"edited text miss");
  check(c.open("case:a","model-v1")==a,"reuse scope");
  long b=c.open("case:b","model-v1");check(c.size()==0&&c.get(b,"Texto de teste")==null,"case switch");
  c.put(a,"Texto antigo",v);check(c.size()==0,"stale request blocked");
  c.put(b,"Texto de teste",v);long theory=c.open("library","model-v1");check(c.size()==0,"library isolated");
  c.put(theory,"Teoria",v);c.clear();c.put(theory,"Teoria",v);check(c.size()==0,"lock invalidation");
  long n=c.open("case:a","model-v2");for(int i=0;i<EcoEmbeddingCache.LIMIT+20;i++)c.put(n,"fixture "+i,v);
  check(c.size()==EcoEmbeddingCache.LIMIT&&c.get(n,"fixture 0")==null&&c.get(n,"fixture "+(EcoEmbeddingCache.LIMIT+19))!=null,"bounded LRU");
  c.clear();check(c.size()==0,"clear");
  System.out.println("PASS ECO embedding cache exact text, scope, epoch, memory limit and purge");
 }
}
