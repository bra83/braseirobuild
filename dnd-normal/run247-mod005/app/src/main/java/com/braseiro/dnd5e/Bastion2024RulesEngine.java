package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Core Bastion procedures from the 2024 Dungeon Master's Guide. Never rolls dice automatically. */
public final class Bastion2024RulesEngine {
    public static final String BOOK="Dungeon Master's Guide (2024)";
    private Bastion2024RulesEngine(){}

    public static JSONObject descriptor(){
        JSONObject o=new JSONObject();
        try{o.put("id","core2024_bastions").put("book",BOOK).put("status","compiled_phase_1")
                .put("auto_rolls",false).put("procedures",new JSONArray()
                        .put("core2024.bastion.acquire")
                        .put("core2024.bastion.turn")
                        .put("core2024.bastion.basic_facility_build")
                        .put("core2024.bastion.basic_facility_enlarge")
                        .put("core2024.bastion.special_facility_count")
                        .put("core2024.bastion.order.validate")
                        .put("core2024.bastion.special_facility_catalog")
                        .put("core2024.bastion.special_facility_validate")
                        .put("core2024.bastion.special_facility_order_resolve")
                        .put("core2024.bastion.event.resolve")
                        .put("core2024.bastion.event.effects"));}catch(Exception ignored){}
        return o;
    }

    public static JSONObject execute(String id,JSONObject in){JSONObject x=in==null?new JSONObject():in;
        if("core2024.bastion.acquire".equals(id))return acquire(x);
        if("core2024.bastion.turn".equals(id))return turn(x);
        if("core2024.bastion.basic_facility_build".equals(id))return buildBasic(x);
        if("core2024.bastion.basic_facility_enlarge".equals(id))return enlargeBasic(x);
        if("core2024.bastion.special_facility_count".equals(id))return specialCount(x);
        if("core2024.bastion.order.validate".equals(id))return validateOrder(x);
        if("core2024.bastion.special_facility_catalog".equals(id))return Bastion2024SpecialFacilitiesEngine.catalog(x);
        if("core2024.bastion.special_facility_validate".equals(id))return Bastion2024SpecialFacilitiesEngine.validate(x);
        if("core2024.bastion.special_facility_order_resolve".equals(id))return Bastion2024SpecialFacilitiesEngine.resolveOrder(x);
        if("core2024.bastion.event.resolve".equals(id))return x.optBoolean("resolve_full_event",false)?Bastion2024EventResolutionEngine.resolve(x):resolveEvent(x);
        if("core2024.bastion.event.effects".equals(id))return Bastion2024EventResolutionEngine.resolve(x);
        return blocked("unknown_bastion_rule");
    }

    private static JSONObject acquire(JSONObject in){JSONObject o=base(333);try{
        int level=in.optInt("character_level",0);boolean enabled=in.optBoolean("bastions_enabled",false);
        if(!enabled)return blocked(o,"bastions_not_enabled_by_dm");
        if(level<5)return blocked(o,"bastion_available_at_level_5");
        return o.put("eligible",true).put("acquisition_level",5)
                .put("initial_basic_facilities",new JSONArray().put(new JSONObject().put("space","Cramped").put("free",true)).put(new JSONObject().put("space","Roomy").put("free",true)))
                .put("initial_special_facilities",specialTotal(level)).put("special_facilities_must_meet_requirements",true)
                .put("player_controls_shape_style_and_function",true);
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject turn(JSONObject in){JSONObject o=base(333);try{
        int days=Math.max(0,in.optInt("days_since_last_bastion_turn",0));
        int frequency=Math.max(1,in.optInt("campaign_turn_frequency_days",7));
        boolean present=in.optBoolean("owner_present",false),remote=in.optBoolean("remote_communication_available",false);
        String requested=norm(in.optString("requested_order"));
        boolean ready=days>=frequency;
        o.put("default_frequency_days",7).put("campaign_frequency_days",frequency).put("turn_ready",ready);
        if(!ready)return o.put("status","blocked").put("requires_resolution",true).put("reason","bastion_turn_not_due_yet");
        if(!present&&!remote)return o.put("effective_order","Maintain").put("forced_maintain",true).put("event_roll_required",true).put("event_roll_must_be_provided_by_dm",true);
        if(requested.isEmpty())return blocked(o,"requested_order_required_when_owner_can_command");
        JSONObject order=validateOrder(new JSONObject().put("order",requested));
        if(!"ok".equals(order.optString("status")))return blocked(o,"invalid_bastion_order");
        return o.put("effective_order",order.optString("canonical_order")).put("forced_maintain",false)
                .put("event_roll_required","Maintain".equals(order.optString("canonical_order")))
                .put("event_roll_must_be_provided_by_dm","Maintain".equals(order.optString("canonical_order")));
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject buildBasic(JSONObject in){JSONObject o=base(335);try{
        String size=norm(in.optString("space"));int cost,days,maxSquares;
        if(any(size,"cramped","apertado")){cost=500;days=20;maxSquares=4;}
        else if(any(size,"roomy","espacoso","espaçoso")){cost=1000;days=45;maxSquares=16;}
        else if(any(size,"vast","vasto")){cost=3000;days=125;maxSquares=36;}
        else return blocked(o,"space_cramped_roomy_or_vast_required");
        return o.put("space",canonicalSpace(size)).put("cost_gp",cost).put("days_required",days).put("maximum_5ft_squares",maxSquares)
                .put("owner_presence_required",false).put("parallel_basic_projects_allowed",true).put("basic_facility_game_effects",false);
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject enlargeBasic(JSONObject in){JSONObject o=base(335);try{
        String from=norm(in.optString("from_space")),to=norm(in.optString("to_space"));
        if(any(from,"cramped","apertado")&&any(to,"roomy","espacoso","espaçoso"))return o.put("from","Cramped").put("to","Roomy").put("cost_gp",500).put("days_required",25).put("in_game_benefit",false);
        if(any(from,"roomy","espacoso","espaçoso")&&any(to,"vast","vasto"))return o.put("from","Roomy").put("to","Vast").put("cost_gp",2000).put("days_required",80).put("in_game_benefit",false);
        return blocked(o,"only_cramped_to_roomy_or_roomy_to_vast_allowed");
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject specialCount(JSONObject in){JSONObject o=base(335);try{
        int level=in.optInt("character_level",0);if(level<5)return blocked(o,"character_level_5_or_higher_required");
        return o.put("special_facilities_total",specialTotal(level)).put("level_5_total",2).put("level_9_total",4).put("level_13_total",5).put("level_17_total",6)
                .put("unique_unless_description_says_otherwise",true).put("must_meet_level_and_prerequisites",true)
                .put("can_replace_one_special_facility_each_level_gain",true);
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static JSONObject validateOrder(JSONObject in){JSONObject o=base(336);try{
        String x=norm(in.optString("order"));String canonical="";
        if(any(x,"craft","fabricar"))canonical="Craft";
        else if(any(x,"empower","fortalecer","empoderar"))canonical="Empower";
        else if(any(x,"harvest","colher"))canonical="Harvest";
        else if(any(x,"maintain","manter"))canonical="Maintain";
        else if(any(x,"recruit","recrutar"))canonical="Recruit";
        else if(any(x,"research","pesquisar"))canonical="Research";
        else if(any(x,"trade","comerciar","negociar"))canonical="Trade";
        else return blocked(o,"unknown_bastion_order");
        o.put("canonical_order",canonical).put("valid",true).put("whole_bastion_order","Maintain".equals(canonical));
        if("Maintain".equals(canonical))o.put("prohibits_other_orders_this_turn",true).put("bastion_event_roll_required",true).put("auto_rolls",false);
        else o.put("issued_to_special_facility",true).put("maintain_not_active_same_turn",true);
        return o;
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    /** Resolves only the percentile band. Nested dice/results remain explicit player/DM inputs. */
    private static JSONObject resolveEvent(JSONObject in){JSONObject o=base(350);try{
        if(!in.has("d100_roll"))return blocked(o,"d100_roll_required_no_auto_rolls");int r=in.optInt("d100_roll",-1);if(r==0)r=100;if(r<1||r>100)return blocked(o,"d100_roll_must_be_1_to_100_or_00_as_100");
        String event=r<=50?"All Is Well":r<=55?"Attack":r<=58?"Criminal Hireling":r<=63?"Extraordinary Opportunity":r<=72?"Friendly Visitors":r<=76?"Guest":r<=79?"Lost Hirelings":r<=83?"Magical Discovery":r<=91?"Refugees":r<=98?"Request for Aid":"Treasure";
        o.put("d100_roll",r).put("event",event).put("resolved_band_only",true).put("nested_rolls_auto_resolved",false);
        if("Attack".equals(event))o.put("next_input","six_d6_results_for_defender_losses");
        else if("Criminal Hireling".equals(event))o.put("next_input","one_d6_for_bribe_hundreds_gp_if_retained");
        else if("Extraordinary Opportunity".equals(event))o.put("choice_required","pay_500_gp_to_accept_or_decline");
        else if("Friendly Visitors".equals(event))o.put("next_input","one_d6_for_payment_hundreds_gp");
        else if("Guest".equals(event))o.put("next_input","one_d4_guest_type");
        else if("Lost Hirelings".equals(event))o.put("choice_or_roll_required","select_or_randomly_determine_special_facility");
        else if("Magical Discovery".equals(event))o.put("result","choose_an_uncommon_potion_or_scroll_at_no_cost");
        else if("Refugees".equals(event))o.put("next_inputs",new JSONArray().put("two_d4_refugee_count").put("one_d6_payment_hundreds_gp"));
        else if("Request for Aid".equals(event))o.put("next_inputs",new JSONArray().put("number_of_defenders_sent").put("one_d6_per_defender_results"));
        else if("Treasure".equals(event))o.put("next_input","second_d100_treasure_table_roll");
        else o.put("next_input","one_d8_flavor_detail_optional");
        return o;
    }catch(Exception e){return blocked(o,"invalid_inputs");}}

    private static int specialTotal(int level){return level>=17?6:level>=13?5:level>=9?4:level>=5?2:0;}
    private static String canonicalSpace(String s){if(any(s,"cramped","apertado"))return "Cramped";if(any(s,"roomy","espacoso","espaçoso"))return "Roomy";return "Vast";}
    private static JSONObject base(int page){JSONObject o=new JSONObject();try{o.put("book",BOOK).put("page",page).put("status","ok").put("auto_rolls",false);}catch(Exception ignored){}return o;}
    private static JSONObject blocked(String reason){return blocked(base(0),reason);}private static JSONObject blocked(JSONObject o,String reason){try{o.put("status","blocked").put("requires_resolution",true).put("reason",reason);}catch(Exception ignored){}return o;}
    private static String norm(String s){return s==null?"":Normalizer.normalize(s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","").trim();}
    private static boolean any(String s,String... xs){for(String x:xs)if(s.equals(norm(x)))return true;return false;}
}
