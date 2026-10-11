import com.braseiro.dnd5e.adventure.*;
import org.json.JSONObject;
public class RunLab {
 public static void main(String[] args){
  UniversalGithubLab.EngineFactory factory=()->new AdventureEngine();
  JSONObject first=UniversalGithubLab.run(factory);System.out.println("FIRST="+first.optString("status")+" STAGE="+first.optString("stage")+" "+first.optJSONObject("detail"));
  if(!first.optBoolean("ok"))System.exit(1);
  JSONObject second=UniversalGithubLab.run(factory);System.out.println("SECOND="+second.optString("status")+" STAGE="+second.optString("stage")+" "+second.optJSONObject("detail"));
  if(!second.optBoolean("ok"))System.exit(1);
  System.out.println("INTEGRATION PASSED: " + first.getJSONArray("modules").length()+" GitHub modules, repeatable, persistence-reopen simulated");
 }
}
