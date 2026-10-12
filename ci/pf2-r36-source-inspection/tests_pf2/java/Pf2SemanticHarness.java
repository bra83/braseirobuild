import com.braseiro.pathfinder2e.Pathfinder2eRulesEngine;
import com.braseiro.pathfinder2e.PathfinderNarrativeContract;
import com.braseiro.pathfinder2e.PathfinderTokenPromptBuilder;
import org.json.JSONObject;

public final class Pf2SemanticHarness {
    private static int passed=0;
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);passed++;}
    public static void main(String[] args)throws Exception{
        Pathfinder2eRulesEngine e=new Pathfinder2eRulesEngine();
        check(e.degree(20,20,25)==Pathfinder2eRulesEngine.Degree.SUCCESS,"nat20 improves failure");
        check(e.degree(1,21,20)==Pathfinder2eRulesEngine.Degree.FAILURE,"nat1 worsens success");
        check(e.degree(10,30,20)==Pathfinder2eRulesEngine.Degree.CRITICAL_SUCCESS,"dc+10 crit");
        check(e.degree(10,9,20)==Pathfinder2eRulesEngine.Degree.CRITICAL_FAILURE,"dc-10 crit fail");
        check(e.mapPenalty(2,false)==-5 && e.mapPenalty(3,false)==-10,"map normal");
        check(e.mapPenalty(2,true)==-4 && e.mapPenalty(3,true)==-8,"map agile");
        check(e.proficiency(5,"trained")==7,"trained proficiency");
        check(e.proficiency(5,"expert")==9,"expert proficiency");
        check(e.proficiency(5,"master")==11,"master proficiency");
        check(e.proficiency(5,"legendary")==13,"legendary proficiency");
        check(e.proficiency(5,"untrained")==0,"untrained proficiency");

        JSONObject mods=new JSONObject().put("str",4).put("dex",3).put("con",2).put("int_score",1).put("wis",2).put("cha",0);
        JSONObject ranks=new JSONObject().put("athletics","trained").put("stealth","expert");
        JSONObject sheet=new JSONObject().put("level",5).put("attribute_model","pf2_remaster_modifiers").put("attribute_modifiers",mods).put("skill_ranks",ranks).put("perception_rank","trained");
        JSONObject hero=new JSONObject().put("name","Tavi").put("species","Goblin").put("class_name","Guerreiro").put("sheet_json",sheet.toString()).put("level",5).put("str",4).put("dex",3).put("wis",2);
        check(e.attributeModifier(hero,"Força")==4,"native modifier");
        check(e.checkBonus(hero,"","Atletismo")==11,"athletics = str4 + lvl5+trained2");
        check(e.checkBonus(hero,"","Furtividade")==12,"stealth = dex3 + lvl5+expert4");
        check(e.initiativeBonus(hero,"Percepção")==9,"perception initiative");
        check(e.initiativeBonus(hero,"Furtividade")==12,"stealth initiative approach");
        JSONObject legacy=new JSONObject().put("level",1).put("str",18);
        check(e.attributeModifier(legacy,"Força")==4,"legacy score migration");
        check(e.validCondition("frightened 2") && e.validCondition("amedrontado 2"),"valued condition");
        check(!e.validCondition("poisoned"),"no D&D poisoned condition fallback");
        JSONObject budget=e.turnBudget(1,false,true,false,0);
        check(budget.optInt("actions_max")==4 && budget.optBoolean("quickened_action_restricted"),"quickened restricted action budget");
        JSONObject dying=e.dying(1,0,0,false,true);
        check(dying.optInt("dying")==3 && !dying.optBoolean("dead"),"critical failed recovery");
        check(PathfinderNarrativeContract.prompt().contains("sessão realmente acontecendo"),"live table narrative");
        check(PathfinderNarrativeContract.prompt().contains("Mundo Vivo"),"living world contract");
        check(PathfinderTokenPromptBuilder.build(hero,"taverna").contains("Goblin"),"token ancestry description");
        check(PathfinderTokenPromptBuilder.describeForScene(hero).contains("Guerreiro"),"scene identity description");
        System.out.println("PF2_SEMANTIC_ASSERTIONS="+passed);
    }
}
