# RUN240 G05 hosted Android failures

Source: Actions run 37994781702, artifact 11647385186.

Verified: RUN230 SHA256 and RUN231-RUN238 patches; zero-fuzz replay succeeded; Android Java compile succeeded; Robolectric suite failed.

Failures: backup21 restores presentation-only combatant fields (`total_cover`, `cover`, `effective_ac`, `effective_dex_save_bonus`) as SQLite columns; fix by exporting database rows and dropping legacy UI fields on import. RUN236 recovery fixture uses noncanonical `poisoned` although the typed timer contract requires `envenenado`. RUN238 action-economy fixture manually toggles active_turn without initializing turn resources; use nextTurn. Preserve diagnostic stack traces until CI rerun.

No Android G05 closure claim until hosted test run passes, UI to SQLite backup21 integration passes, and Drive readback is verified.
