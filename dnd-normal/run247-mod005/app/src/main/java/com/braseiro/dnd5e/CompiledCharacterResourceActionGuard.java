package com.braseiro.dnd5e;
/** Strict input contract for atomic, replay-proof character resource effects. */
public final class CompiledCharacterResourceActionGuard {
  private CompiledCharacterResourceActionGuard() {}
  public static String rejection(String channel,String family,String actionId,long characterId,String kind,String key,int amount){
    if(!"PLAYER_REACTION".equals(channel))return "blocked_invalid_action_channel";
    if(!"character_resource".equals(family))return "blocked_unknown_mechanic_family";
    if(actionId==null||actionId.length()<12||actionId.length()>96)return "blocked_invalid_action_id";
    for(int i=0;i<actionId.length();i++){char c=actionId.charAt(i);
      if(!((c>='0'&&c<='9')||(c>='a'&&c<='z')||(c>='A'&&c<='Z')||c=='-'||c=='_'))return "blocked_invalid_action_id";
    }
    if(characterId<=0)return "blocked_invalid_character";
    if("class_resource".equals(kind)){
      if(key==null||key.isEmpty()||key.length()>80)return "blocked_invalid_resource_key";
      for(int i=0;i<key.length();i++){char c=key.charAt(i);
        if(!(Character.isLetterOrDigit(c)||c=='_'||c=='-'||c=='.'))return "blocked_invalid_resource_key";
      }
      if(amount<1||amount>20)return "blocked_invalid_amount";
    }else if("spell_slot".equals(kind)){
      if(amount<1||amount>9||key!=null&&!key.isEmpty())return "blocked_invalid_spell_slot";
    }else if("pact_slot".equals(kind)){
      if(amount!=1||key!=null&&!key.isEmpty())return "blocked_invalid_pact_slot";
    }else return "blocked_unsupported_resource_kind";
    return "";
  }
  public static boolean persisted(boolean characterPresent,boolean spent,boolean replayed,boolean rejected,boolean receiptPresent){
    return characterPresent&&spent&&!replayed&&!rejected&&receiptPresent;
  }
}
