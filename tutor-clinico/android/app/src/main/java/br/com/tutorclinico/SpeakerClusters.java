package br.com.tutorclinico;
import java.util.Arrays;
/** Session-local anonymous acoustic clusters. Never persisted; thresholds are experimental. */
public final class SpeakerClusters {
    private final double[][] means=new double[2][];
    private final int[] counts=new int[2];
    synchronized void clear(){Arrays.fill(means,null);Arrays.fill(counts,0);}
    synchronized String assign(double[] v){
        double norm=0;for(double x:v)norm+=x*x;
        if(!Double.isFinite(norm)||norm<1e-9)return "";
        norm=Math.sqrt(norm);for(int i=0;i<v.length;i++)v[i]/=norm;
        if(means[0]==null){means[0]=v.clone();counts[0]=1;return "falante1";}
        if(v.length!=means[0].length)return "";
        double a=dot(v,means[0]);
        if(means[1]==null){
            if(a>=0.70){update(0,v);return "falante1";}
            if(a<=0.35){means[1]=v.clone();counts[1]=1;return "falante2";}
            return "";
        }
        double b=dot(v,means[1]);int k=a>=b?0:1;
        if(Math.max(a,b)<0.65||Math.abs(a-b)<0.15)return "";
        update(k,v);return k==0?"falante1":"falante2";
    }
    private static double dot(double[] a,double[] b){double d=0;for(int i=0;i<a.length;i++)d+=a[i]*b[i];return d;}
    private void update(int k,double[] v){
        double[] a=means[k];double weight=1.0/Math.min(10,++counts[k]),n=0;
        for(int i=0;i<a.length;i++){a[i]=(1-weight)*a[i]+weight*v[i];n+=a[i]*a[i];}
        if(n>0){n=Math.sqrt(n);for(int i=0;i<a.length;i++)a[i]/=n;}
    }
}
