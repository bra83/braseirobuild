package br.com.tutorclinico;
import java.util.Arrays;
/** Session-local anonymous acoustic clusters, max four. Ambiguous windows remain unassigned. */
public final class SpeakerClusters {
    private static final int MAX_GROUPS=4;
    private final double[][] means=new double[MAX_GROUPS][];
    private final int[] counts=new int[MAX_GROUPS];
    synchronized void clear(){Arrays.fill(means,null);Arrays.fill(counts,0);}
    synchronized String assign(double[] input){
        if(input==null||input.length==0)return "";
        double norm=0;for(double x:input)norm+=x*x;
        if(!Double.isFinite(norm)||norm<1e-9)return "";
        double[] v=input.clone();norm=Math.sqrt(norm);for(int i=0;i<v.length;i++)v[i]/=norm;
        if(means[0]==null){means[0]=v;counts[0]=1;return "falante1";}
        if(v.length!=means[0].length)return "";
        int best=-1,free=-1,n=0;double highest=-2,second=-2;
        for(int i=0;i<MAX_GROUPS;i++){
            if(means[i]==null){if(free<0)free=i;continue;}
            n++;double score=dot(v,means[i]);
            if(score>highest){second=highest;highest=score;best=i;}else if(score>second)second=score;
        }
        if(highest>=(n==1?0.70:0.65)&&(n==1||highest-second>=0.15)){
            update(best,v);return "falante"+(best+1);
        }
        // Only create a new cluster when acoustically distinct from every existing group.
        if(free>=0&&highest<=0.35){means[free]=v;counts[free]=1;return "falante"+(free+1);}
        return "";
    }
    private static double dot(double[] a,double[] b){double d=0;for(int i=0;i<a.length;i++)d+=a[i]*b[i];return d;}
    private void update(int k,double[] v){
        double[] a=means[k];double weight=1.0/Math.min(10,++counts[k]),n=0;
        for(int i=0;i<a.length;i++){a[i]=(1-weight)*a[i]+weight*v[i];n+=a[i]*a[i];}
        if(n>0){n=Math.sqrt(n);for(int i=0;i<a.length;i++)a[i]/=n;}
    }
}
