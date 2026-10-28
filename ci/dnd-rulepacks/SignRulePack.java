import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Base64;

/** Signs exact UTF-8 pack bytes with the SAME key used for the D&D Android APK. */
public final class SignRulePack {
    public static void main(String[] args)throws Exception{
        if(args.length!=6)throw new IllegalArgumentException(
            "usage: input output keystore alias storepass keypass");
        KeyStore ks=KeyStore.getInstance(new java.io.File(args[2]),args[4].toCharArray());
        Key key=ks.getKey(args[3],args[5].toCharArray());
        if(!(key instanceof PrivateKey))throw new SecurityException("Android signer private key missing");
        PrivateKey privateKey=(PrivateKey)key;
        String algorithm="RSA".equalsIgnoreCase(privateKey.getAlgorithm())?"SHA256withRSA":
            "EC".equalsIgnoreCase(privateKey.getAlgorithm())?"SHA256withECDSA":"";
        if(algorithm.isEmpty())throw new SecurityException("Unsupported APK signing key algorithm");
        byte[] data=Files.readAllBytes(Path.of(args[0]));
        if(data.length==0||data.length>65536)throw new SecurityException("Rule pack size invalid");
        java.security.Signature signature=java.security.Signature.getInstance(algorithm);
        signature.initSign(privateKey);signature.update(data);
        byte[] signed=signature.sign();
        Files.writeString(Path.of(args[1]),Base64.getEncoder().encodeToString(signed)+"\n");
        System.out.println("PACK_SIGNED_WITH_APK_KEY="+algorithm);
    }
}
