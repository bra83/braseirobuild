package com.braseiro.pathfinder2e;
import java.util.HashSet;import java.util.Set;
public final class ManualIdentityPolicySelfTest {
 private static int checks=0;
 public static void main(String[] args){
  Set<String> keys=new HashSet<String>();for(int i=1;i<=39;i++)keys.add(ManualIdentityPolicy.duplicateKey("Lost Omens "+i+".pdf"));check(keys.size()==39,"volumes numerados independentes");
  check(ManualIdentityPolicy.duplicateKey("Player Core comprimido.pdf").equals(ManualIdentityPolicy.duplicateKey("Player Core.pdf")),"compressão não duplica manual");
  check(ManualIdentityPolicy.isPf2AuthoritativeTitle("Pathfinder 2e Player Core Remaster.pdf"),"Player Core PF2e é autoridade");
  check(ManualIdentityPolicy.isPf2LoreTitle("Lost Omens World Guide Golarion.pdf"),"Lost Omens é lore PF2e");
  check(ManualIdentityPolicy.isPf1Title("Pathfinder 1e Core Rulebook.pdf"),"PF1 reconhecido como legado");
  check(!ManualIdentityPolicy.mechanicalAuthorityAllowed("Pathfinder 1e Core Rulebook.pdf","PF1"),"PF1 bloqueado mecanicamente");
  check(!ManualIdentityPolicy.mechanicalAuthorityAllowed("Dungeons & Dragons Player Handbook.pdf","D&D 5e"),"D&D bloqueado mecanicamente");
  check(!ManualIdentityPolicy.mechanicalAuthorityAllowed("Rules Cyclopedia Mystara.pdf","Mystara"),"Mystara bloqueado mecanicamente");
  check(ManualIdentityPolicy.mechanicalAuthorityAllowed("Pathfinder 2e Player Core.pdf","Pathfinder 2e Remaster"),"PF2e Remaster permitido");
  check(ManualIdentityPolicy.isPf2AuthoritativeTitle("Pathfinder Kingmaker Adventure Path"),"Kingmaker deve ser autoridade PF2e");
  check(ManualIdentityPolicy.isPf2AuthoritativeTitle("Kingmaker Companion Guide"),"Kingmaker Companion Guide deve ser autoridade PF2e");
  System.out.println("OK: "+checks+" verificações de identidade e firewall PF2e.");
 }
 private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
