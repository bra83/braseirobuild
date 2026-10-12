import com.braseiro.pathfinder2e.SceneImageLayoutPolicy;

public final class SceneImageLayoutPolicySelfTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        check(SceneImageLayoutPolicy.previewWidth(1000,640,360)==640,"respeita largura máxima");
        check(SceneImageLayoutPolicy.previewWidth(500,640,360)==500,"usa largura disponível");
        check(SceneImageLayoutPolicy.previewWidth(200,640,360)==200,"não ultrapassa telas estreitas");
        check(SceneImageLayoutPolicy.heightForWidth(360)==640,"proporção 9:16 exata");
        check(SceneImageLayoutPolicy.heightForWidth(270)==480,"proporção 9:16 alternativa");
        System.out.println("SceneImageLayoutPolicySelfTest: 5/5");
    }
}
