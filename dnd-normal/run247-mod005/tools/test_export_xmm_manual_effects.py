import unittest

import export_xmm_manual_effects as exporter


class ManualEffectInventoryTests(unittest.TestCase):
    def test_inventory_is_source_pointer_unique_and_uses_contract_schema(self):
        source=[{"name":"Test Monster","action":[
            {"name":"Terror","entries":["{@actSave wis} {@dc 12} {@actSaveFail} "
                "The target has the {@condition Frightened|XPHB} and "
                "{@condition Paralyzed|XPHB} conditions."]}]}]
        compiled=[{"id":"Test Monster","actions":[
            {"name":"Terror","save_ability":"wis","save_dc":12,
             "condition_on_fail":"frightened, paralyzed",
             "manual_unmodeled_source_effect_required":True}]}]

        inventory=exporter.build_inventory(source,compiled,"proof-sha")

        self.assertEqual(1,inventory["manual_unmodeled_effect_count"])
        self.assertFalse(inventory["full_monster_combat_automation_certified"])
        row=inventory["entries"][0]
        self.assertEqual("/monster/0/action/0/entries/0",row["source_pointer"])
        self.assertEqual("Test Monster",row["monster_id"])
        self.assertEqual("Terror",row["action_id"])
        self.assertEqual("MANUAL_UNSUPPORTED",row["coverage_status"])
        self.assertEqual("save_failure_condition",row["family"])
        self.assertEqual("wis",row["mechanical_contract"]["save_ability"])
        self.assertEqual("",row["proof_sha"])
        self.assertEqual(
            {"source_pointer","monster_id","action_id","family","mechanical_contract",
             "engine_owner","coverage_status","test_id","proof_sha","blocker_reason"},
            set(row),
        )

    def test_source_bound_save_condition_preview_stays_blocked_for_host_integration(self):
        source=[{"name":"Test Monster","action":[
            {"name":"Terror","entries":["{@actSave wis} {@dc 12} {@actSaveFail} "
                "The target has the {@condition Frightened|XPHB} and "
                "{@condition Paralyzed|XPHB} conditions."]}]}]
        compiled=[{"id":"Test Monster","actions":[
            {"name":"Terror","save_ability":"wis","save_dc":12,
             "condition_on_fail":"frightened, paralyzed",
             "manual_unmodeled_source_effect_required":True}]}]
        profile={"id":"Test Monster","action":"Terror",
            "source_pointer":"/monster/0/action/0/entries/0",
            "save_ability":"wis","save_dc":12}
        inventory=exporter.build_inventory(source,compiled,"proof-sha",[profile])
        row=inventory["entries"][0]
        self.assertEqual("BLOCKED_MISSING_CONTEXT",row["coverage_status"])
        self.assertEqual("proof-sha",row["proof_sha"])
        self.assertIn("SaveConditionPreviewEngineTest#allFiftyEightProfilesRemainSourceBoundAndManual",
            row["test_id"])
        self.assertEqual(1,inventory["tipado_preview_pendente_integracao_count"])

    def test_recurring_damage_preview_is_not_reported_as_applied(self):
        source=[{"name":"Test Monster","action":[{"name":"Tick","entries":[
            "At the start of each of its turns, the target takes {@damage 1d8} damage. "
            "It repeats the save at the end of each of its turns."]}]}]
        compiled=[{"id":"Test Monster","actions":[{"name":"Tick",
            "condition_recurring_damage":"1d8",
            "condition_recurring_damage_type":"poison",
            "condition_recurring_damage_tick":"target_turn_start",
            "manual_unmodeled_source_effect_required":True}]}]
        profile={"id":"Test Monster","action":"Tick",
            "ptr":"/monster/0/action/0/entries/0","damage":"1d8"}
        inventory=exporter.build_inventory(source,compiled,"proof-sha",[],[profile])
        row=inventory["entries"][0]
        self.assertEqual("condition_recurring_damage",row["family"])
        self.assertEqual("BLOCKED_MISSING_CONTEXT",row["coverage_status"])
        self.assertEqual("proof-sha",row["proof_sha"])
        self.assertIn("RecurringDamagePreviewEngineTest",row["test_id"])

if __name__=="__main__":
    unittest.main()
