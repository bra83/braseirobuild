package android.content.res;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
public class AssetManager {
  public InputStream open(String name){return new ByteArrayInputStream(new byte[0]);}
}
