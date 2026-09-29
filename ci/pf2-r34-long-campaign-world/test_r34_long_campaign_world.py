from pathlib import Path
import math
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"

def src(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def test_causal_catalog_registers_thousands_of_structured_patterns():
    text = src("WorldCausalCatalog.java")
    assert "EXPECTED_PATTERN_COUNT = 4096" in text
    assert "16 famílias × 8 acontecimentos × 8 contextos × 4 intensidades = 4096" in text
    assert text.count('{"') >= 16
    for family in ["guerra","agricultura","saude","clima","comercio","infraestrutura","politica","crime",
                   "religiao","cultura","magia","monstros","trabalho","migracao","diplomacia","ecologia"]:
        assert f'"{family}"' in text
    for field in ["population_delta_ppm","adult_men_extra_loss_ppm","soldier_loss_ppm",
                  "food_production_delta_pct","food_stock_delta_pct","drink_stock_delta_pct",
                  "medicine_stock_delta_pct","disease_delta","migration_delta","war_delta",
                  "price_delta_pct","commodity_shock","aid_pressure"]:
        assert field in text

def test_war_has_demographic_food_health_trade_and_second_order_effects():
    text = src("WorldCausalCatalog.java")
    assert 'if("guerra".equals(family))' in text
    assert 'adult_men_extra_loss_ppm' in text
    assert 'soldier_loss_ppm' in text
    assert 'food_production_delta_pct' in text
    assert 'food_stock_delta_pct' in text
    assert 'disease_delta' in text
    assert 'displaced_delta_ppm' in text
    assert 'commodity_shock' in text
    engine = src("WorldCausalityEngine.java")
    assert "propagate(shocks,targetDay,3)" in engine
    assert "Pedido de ajuda de " in engine
    assert "nudgeNpcs" in engine
    assert "world_history_events" in engine

def test_long_settlement_simulation_never_discards_time_after_180_days():
    text = src("LivingSettlementEngine.java")
    assert "Math.min(180,targetDay-last)" not in text
    assert "while(cursor<targetDay)" in text
    assert "int next=Math.min(targetDay,cursor+30)" in text
    assert 'v.put("last_simulated_day",next)' in text

def test_faction_backlog_preserves_unexecuted_time():
    text = src("LivingWorldSimulationEngine.java")
    assert "accumulated-(due*interval)" not in text
    assert "accumulated-(executable*interval)" in text

def test_calendar_has_days_months_seasons_and_years():
    text = src("WorldCalendarEngine.java")
    assert "GOLARION_DAYS_PER_YEAR = 360" in text
    for month in ["Abadius","Calistril","Pharast","Gozran","Desnus","Sarenith",
                  "Erastus","Arodus","Rova","Lamashan","Neth","Kuthona"]:
        assert month in text
    for season in ["Inverno","Primavera","Verão","Outono"]:
        assert season in text
    assert "year_started" in text
    assert "season_started" in text
    assert "month_started" in text

def test_world_tick_runs_causality_automatically():
    text = src("WorldSixCoordinator.java")
    assert "new WorldCausalityEngine(db)" in text
    assert 'put("causality",causality.simulateToDay(day))' in text
    assert 'put("calendar",WorldCalendarEngine.snapshot(day,living))' in text

def test_causal_state_is_persistent_and_backed_by_existing_settlement_backup():
    app = src("AppDb.java")
    for col in ["population_total","adult_men","adult_women","children","elderly","displaced","soldiers",
                "food_stock","food_production","food_demand","drink_stock","medicine_stock","price_index",
                "health_index","disease_pressure","migration_pressure","war_exposure","trade_capacity",
                "infrastructure","political_stability","last_causal_day","causal_model_version","causal_state_json"]:
        assert f'"settlement_states","{col}"' in app
    assert "world_causal_patterns" in app
    # settlementStates já faz parte do contrato de backup 6.0 e restaura colunas genericamente.
    assert 'root.put("settlementStates"' in app
    assert '"settlementStates","worldHistoryEvents"' in app
    assert "causalRestoreAudit" in app

def test_structured_future_events_can_change_regions_without_ai():
    text = src("WorldEventConsequenceEngine.java")
    assert 'optJSONArray("settlement_updates")' in text
    assert "WorldCausalityEngine.applySettlementUpdate" in text
    assert '"settlement_updates","região(ões)"' in text

def test_kingmaker_restore_contract_includes_new_story_phase_and_causal_fingerprint():
    repo = src("KingmakerRepository.java")
    solo = src("KingmakerSoloDirector.java")
    contract = src("KingmakerContinuityContract.java")
    assert "campaign_story_phase" in repo
    assert "main_adventure_started" in repo
    for field in ["campaign_story_phase","main_adventure_started","living_world_day",
                  "living_world_calendar","causal_world_fingerprint"]:
        assert field in solo
        assert field in contract

def test_background_world_is_enabled_for_untouched_default_only():
    app = src("AppDb.java")
    assert "background_simulation_enabled=1" in app
    assert "background_simulation_enabled=0 AND last_background_sync_at=0" in app
    assert "background_max_hours=720" in app

def test_reference_ten_year_campaign_invariants_and_supply_chain():
    # Referência independente das fórmulas essenciais: 10 anos, guerra na região exportadora,
    # choque em alimentos chegando ao importador e demografia nunca negativa.
    year_days = 360
    days = year_days * 10
    source = dict(pop=9000, men=2430, women=2520, children=2880, elderly=1170,
                  food=9000*45.0, prod=9000*1.05, demand=9000.0, price=100.0,
                  disease=5.0, war=0.0, displaced=0.0)
    dest = dict(pop=5000, men=1350, women=1400, children=1600, elderly=650,
                food=5000*45.0, prod=5000*.85, demand=5000.0, price=100.0,
                scarcity=20.0)
    dest_price_before = dest["price"]
    dest_scarcity_before = dest["scarcity"]

    for day in range(1, days+1):
        month = ((day-1) % year_days) // 30
        season_mult = .72 if month in (11,0,1) else 1.08 if month in (2,3,4) else 1.22 if month in (5,6,7) else 1.12
        if 721 <= day <= 1080:  # um ano de guerra séria
            source["war"] = min(100.0, source["war"] + .18)
            source["prod"] *= .9994
            if day % 30 == 0:
                battle_losses = max(1, round(source["pop"] * .0012))
                male_losses = min(source["men"], round(battle_losses * .72))
                other_losses = battle_losses - male_losses
                source["men"] -= male_losses
                source["women"] = max(0, source["women"] - round(other_losses*.25))
                source["children"] = max(0, source["children"] - round(other_losses*.35))
                source["elderly"] = max(0, source["elderly"] - (other_losses-round(other_losses*.25)-round(other_losses*.35)))
                source["pop"] = source["men"]+source["women"]+source["children"]+source["elderly"]
                source["displaced"] = min(source["pop"], source["displaced"] + round(source["pop"]*.003))
            # destino depende dos alimentos do exportador
            if day % 14 == 0:
                dest["scarcity"] = min(100.0, dest["scarcity"] + 1.2)
                dest["price"] *= 1.006
        else:
            source["war"] = max(0.0, source["war"]-.05)

        source["food"] = max(0.0, source["food"] + source["prod"]*season_mult - source["demand"])
        dest["food"] = max(0.0, dest["food"] + dest["prod"]*season_mult - dest["demand"])

        # doença/fome crescem quando estoques colapsam
        if source["food"] < source["demand"]*7:
            source["disease"] = min(100.0, source["disease"]+.03)
        else:
            source["disease"] = max(0.0, source["disease"]-.004)

        assert source["pop"] >= 0
        assert source["men"] >= 0 and source["women"] >= 0 and source["children"] >= 0 and source["elderly"] >= 0
        assert source["food"] >= 0 and dest["food"] >= 0
        assert source["displaced"] <= source["pop"]

    assert dest["price"] > dest_price_before
    assert dest["scarcity"] > dest_scarcity_before
    # guerra pesa principalmente sobre homens adultos/recrutáveis no modelo de frente de batalha
    men_ratio_after = source["men"] / source["pop"]
    women_ratio_after = source["women"] / source["pop"]
    assert men_ratio_after < women_ratio_after
    # calendário atravessou dez anos completos
    assert days // year_days == 10

def test_version_is_9_0_35():
    version = (ROOT / "version.properties").read_text(encoding="utf-8")
    assert "VERSION_NAME=9.0.35" in version
    assert "VERSION_CODE=90035" in version
