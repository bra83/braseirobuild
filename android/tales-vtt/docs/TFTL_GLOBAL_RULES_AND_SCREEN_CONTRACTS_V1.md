# TALES FROM THE LOOP VTT
## GLOBAL RULES + SCREEN CONTRACTS V1
Profile: **TALES / KIDS**
Status: **MECHANICAL LOCK**
Purpose: remove mechanical ambiguity before any further concept or implementation work.

---

# 0. GLOBAL PROFILE LOCK

This document is authoritative for the TALES/KIDS profile only.

**Never import Things from the Flood mechanics into this profile.**
Flood/Teens must use a separate rules profile and separate state schema.

No screen may:
- invent a mechanic;
- reveal GM-secret state;
- mutate campaign state merely because the player inspected a screen;
- replace a narrative decision with a tactical videogame subsystem;
- expose a mechanic before it is relevant to the current scene.

---

# 1. GLOBAL MECHANICAL CONTRACT

## 1.1 Scene flow
- Play proceeds scene by scene.
- The Gamemaster frames the scene, presents people/events/problems and asks what the Kid does.
- Player input describes an intention/action in the fiction.
- A roll appears only when the Gamemaster decides the action requires a roll.
- Scene navigation itself never resolves an action.

## 1.2 Standard test
Pool:
`ATTRIBUTE + SKILL + APPLICABLE BONUSES - ACTIVE CONDITION PENALTIES`

If no suitable Skill exists:
`ATTRIBUTE ONLY`

Success:
- every `6` = 1 success;
- normally 1 success is enough;
- extreme Problems may require 2 or 3 successes;
- NPC Special Attributes may set the number of successes required when directly applicable.

Forbidden:
- initiative;
- combat rounds;
- enemy HP;
- NPC attack rolls;
- automatic tactical turns.

## 1.3 Conditions
Mild Conditions:
- Chateado
- Assustado
- Exausto
- Ferido/Machucado

Each active mild Condition:
`-1 die to all rolls`

If all four mild Conditions are marked and another Condition is suffered:
`QUEBRADO`

Broken:
- automatic failure on rolls until healed;
- Kids do not die.

## 1.4 Luck
`MAX LUCK = 15 - AGE`

- refills after each session;
- cannot be saved between sessions;
- after a failed roll, spend 1 Luck to reroll failed dice;
- maximum 1 Luck spent per roll;
- the new result stands.

## 1.5 Push
- may be used once on the roll;
- mark 1 Condition first;
- reroll all dice that are not sixes;
- may be used after failure;
- may also be used after success when pursuing additional Bonus Effects;
- cannot Push again;
- Luck/Pride may still be used if otherwise eligible.

## 1.6 Pride
- grants `+1 automatic success`, never +1 die;
- normally once per Mystery;
- for a multi-session Mystery / Mystery Landscape, may refresh once per session as allowed by the core rule;
- may be activated after a failed or successful test;
- player must explain how Pride helps.

## 1.7 Help
- one plausible Kid may help;
- helper describes what they do;
- +1 die;
- max 1 helper per roll;
- Gamemaster has final say;
- helper shares consequences if the roll fails;
- no Help when all Kids must overcome the same Problem simultaneously;
- Bonus Effects may still transfer successes where a Skill allows it.

## 1.8 Items
### Iconic Item
- usable only when appropriate;
- Gamemaster has final say;
- `+2 dice`.

### Mystery Items
- may have a specific `+1`, `+2` or `+3` bonus;
- bonus belongs to that actual Item, not to a generic object category;
- may be constructed/found/modified by rules and fiction;
- ordinary objects do not automatically gain a universal bonus;
- Mystery Items disappear or break between Mysteries unless transformed into an Iconic Item, according to the core rule.

## 1.9 Extra successes / Bonus Effects
After satisfying the Problem:
- leftover successes may buy only Bonus Effects defined for the Skill and allowed by the current situation;
- the Gamemaster decides which listed effects are applicable;
- UI must not show a universal list detached from the Skill being rolled.

## 1.10 NPCs
- NPCs never roll dice.
- Gamemaster decides NPC success/failure.
- Kids may roll to avoid or overcome Problems caused by NPC actions.
- Special Attributes may increase required successes when directly applicable.
- In Extended Trouble, do NOT separately apply NPC Special Attributes; they are included in Threat Level.

## 1.11 Kid vs Kid
- all involved Kids state their intentions;
- they roll simultaneously;
- more successes wins;
- UI must not invent an initiative order.
- Any unresolved tie detail not explicitly represented in the verified source remains GM-controlled rather than being invented by the interface.

## 1.12 Complications after failure
Failure must move the fiction.
The GM may:
- impose/offer a Condition where appropriate;
- introduce a new Problem;
- use a Complication;
- allow crucial progress with a cost or new problem.
UI never assumes that failure means “nothing happens”.

---

# 2. VISIBILITY CONTRACT

PLAYER MAY SEE ONLY:
- current scene information already revealed;
- known locations;
- revealed clues;
- revealed rumors;
- revealed NPCs;
- revealed clue connections;
- current Kid and group state;
- mechanics currently relevant to the player's decision.

PLAYER MUST NEVER SEE:
- secret GM map locations;
- hidden clue count;
- missing-clue placeholders;
- `???` markers that prove a secret location exists;
- future Mystery phases if GM keeps them hidden;
- unrevealed NPC motives;
- hidden Threat/Countdown data not explicitly made player-facing;
- false fog-of-war hints;
- secret campaign truth.

A hidden entity is not rendered at all.

---

# 3. RECOVERY CONTRACT

## 3.1 Anchor
Anchor:
- is a person;
- cannot be another Kid.

Healing through Anchor requires a scene in which:
- Kid has one or more Conditions;
- Kid allows Anchor to care/support them;
- there is physical or mental closeness;
- GM does not put the Kid in Trouble during that scene;
- if the Kid creates Trouble, no healing occurs.

Successful qualifying scene:
`heal all Conditions`

## 3.2 Hideout
- group shares one Hideout;
- GM may not have NPCs find it unless Kids show it;
- GM does not put Kids in Trouble there;
- Kids may create their own Trouble;
- 2+ Kids present and physically/mentally close may heal all Conditions;
- if they create Trouble during that scene, no healing occurs.

## 3.3 Lead/comfort Skill interaction
A Kid may use the appropriate Skill rule to comfort another Kid:
- requires the rule-specific roll;
- can heal one Condition on success;
- failure shares/suffers the stated consequence;
- cannot heal Broken using this Skill.
This is separate from Anchor/Hideout full recovery.

---

# 4. MYSTERY FLOW CONTRACT

Mystery phases:
1. Introducing the Kids
2. Introducing the Mystery
3. Solving the Mystery
4. Showdown / Confrontation
5. Aftermath / Consequences
6. Change

Important:
- GM may announce phase transitions or keep the phase structure hidden.
- Everyday Life scenes can occur throughout the Mystery.
- Solving the Mystery is mainly: visit Locations, talk to people, discover Clues, overcome Problems and alternate with Everyday Life.
- player map and GM secret map are distinct.

---

# 5. EXTENDED TROUBLE CONTRACT

Use when one roll is insufficient and every Kid must contribute.

## State A — Stakes
GM states:
- what the Kids are trying to achieve;
- what happens if they fail.

## State B — Threat Level
Threat target:
- Normal = `2 × number of Kids`
- Difficult = `3 × number of Kids`
- Almost Impossible = `4 × number of Kids`

Do not add NPC Special Attributes separately.

## State C — Plan
Kids:
- decide what they will do;
- choose plausible Skills;
- decide acting order.
GM may reject an obviously unreasonable Skill use.

## State D — Scenes
- each Kid gets one scene;
- each Kid rolls once for the chosen Skill;
- normal roll rules apply;
- Push may be used;
- successes accumulate toward Threat Level;
- Bonus Effects may be used if applicable;
- Leadership may be used to create a dice pool instead of contributing directly when the rule permits.

## State E — Result
If total successes >= Threat Level without extra Conditions:
`FULL SUCCESS`

If total successes < half Threat Level:
`COMPLETE FAILURE`

If total successes >= half but < Threat Level:
`COMPROMISE AVAILABLE`

## State F — Compromise
Voluntary:
- Kids may mark additional Conditions;
- 1 extra Condition = 1 success toward missing target;
- may become Broken if they choose;
- if target is reached this way, they achieve part of their objective;
- GM determines the compromise details.

No generic “final climax roll”.

---

# 6. CHANGE + XP CONTRACT

Between Mysteries:
- all Conditions heal;
- Problem, Pride, Iconic Item and Relationships may change if appropriate;
- Drive may change between Mysteries;
- if birthday passed: +1 Attribute and -1 maximum Luck;
- when Kid reaches 16, player creates a new Kid.

XP:
- after Mystery, or after session if Mystery continues;
- answer five XP questions;
- +1 XP for every “yes”;
- participating Kid gets at least the participation XP;
- 5 XP increases one Skill by 1;
- Skill maximum = 5;
- may save max 10 XP;
- XP may be spent only before or after a session, never during play.

---

# 7. SCREEN CONTRACTS

## SCREEN 01 — CENA
Status: **CONTRACT LOCKED / VISUAL ~70%**

Primary job:
`present fiction + receive player intention`

Must show:
- current visible scene;
- current visible location;
- GM narration;
- optional TTS for visible narration;
- player's last declared action when relevant;
- free player action input;
- at most a small number of optional GM suggestions.

Must NOT show by default:
- dice;
- Luck;
- Push;
- Pride;
- Help;
- Item bonus math;
- hidden clues;
- hidden locations;
- turn order;
- combat HUD.

Transition:
- player declares action;
- GM resolves narratively OR emits validated `REQUEST_TEST`;
- if test requested -> ENCRENCA.

---

## SCREEN 02 — ENCRENCA / TESTE
Status: **CONTRACT LOCKED / VISUAL APPROVED**

Primary job:
`resolve one requested test`

Pre-roll:
- exact fictional reason;
- Attribute;
- Skill or Attribute-only;
- active Condition penalty;
- already-approved Item modifiers;
- Help only if plausible;
- final pool;
- successes required.

Post-roll:
- dice result;
- successes;
- success/failure state;
- Luck only when failed and unused;
- Push only when unused/eligible;
- Pride only when available;
- applicable Bonus Effects from that Skill when extra successes exist;
- GM consequence/next narrative step.

Must NOT:
- roll for NPC;
- invent item bonus;
- show all resources as permanently active;
- invent initiative.

Substate:
`KID_VS_KID`
- intentions stated;
- simultaneous rolls;
- compare successes;
- no initiative order.

---

## SCREEN 03 — KID
Status: **CONTRACT LOCKED / VISUAL NEEDS MECHANICAL CORRECTION**

Primary job:
`inspect the Kid`

Shows:
- Name / Type / Age / Description;
- 4 Attributes;
- 12 Skills under correct Attributes;
- current Luck / max Luck;
- Conditions;
- Broken;
- Problem;
- Drive;
- Pride and usage state;
- Anchor;
- Iconic Item;
- Relationships;
- XP;
- favorite music as descriptive only.

Mutations:
- normal play: read-only except actions explicitly allowed by rules/state;
- Conditions are NOT manually toggled as arbitrary player controls;
- Pride is not a generic ficha button; activation happens in a relevant roll state;
- XP cannot increase Attributes;
- Skill increase costs 5 XP only before/after session.

Creation/edit flows must be separate states from active play.

---

## SCREEN 04 — GRUPO
Status: **CONTRACT LOCKED**

Primary job:
`inspect Kids, relationships and Hideout`

Shows:
- Kids in group;
- current visible Conditions/status;
- descriptive Kid-to-Kid relationships;
- shared Hideout;
- revealed group notes.

Hideout recovery:
- appears only when recovery prerequisites can be satisfied;
- requires 2+ Kids and closeness;
- recovery completes only if scene ends without self-created Trouble.

Must NOT:
- assign numerical relationship bonuses not defined by rules;
- allow direct Condition editing;
- expose secret NPC data.

---

## SCREEN 05 — MISTÉRIO
Status: **CONTRACT LOCKED**

Primary job:
`player-facing investigation memory`

Shows only:
- revealed Mystery premise/objective;
- revealed rumors;
- revealed clues;
- revealed leads;
- player-known NPCs;
- known clue relationships.

Must NOT:
- show hidden clue total;
- show “2 of 7 clues” unless 7 is itself known;
- show undiscovered cards/placeholders;
- let player toggle a clue/rumor to discovered;
- expose Mystery Truth;
- force-show current Mystery phase if GM keeps phases hidden.

Clues are revealed by game state, not by clicking checkboxes.

---

## SCREEN 06 — MAPA
Status: **CONTRACT LOCKED**

Primary job:
`inspect the player map`

Shows:
- only known/revealed locations;
- player-known notes;
- current known location when appropriate.

A hidden location:
`DOES NOT RENDER`

Forbidden:
- `???` pin for undiscovered location;
- unknown-danger marker;
- hidden radiation/threat info;
- secret topology;
- direct “teleport/move here” state mutation.

If player chooses a destination:
- button means `declare travel intention`;
- it returns to CENA / GM;
- GM determines travel scene, time and Problems.

---

## SCREEN 07 — CONEXÕES
Status: **CONTRACT LOCKED**

Primary job:
`visualize only known clue relationships`

Nodes:
- revealed clues;
- revealed people;
- revealed locations;
- other explicitly known investigation facts.

Edges:
- only relationships/connections already discovered or stated.

Forbidden:
- hidden nodes;
- dangling lines implying a secret node;
- “missing clue” slots;
- completion percentages derived from hidden truth.

This screen is an alternate visualization of MISTÉRIO state, not a new source of information.

---

## SCREEN 08 — LOCAL
Status: **CONTRACT LOCKED**

Primary job:
`inspect/explore one currently active revealed Location`

Shows:
- GM-visible-to-player description;
- people currently revealed;
- clues already noticed/revealed;
- relevant environmental facts.

Player action:
- describe what the Kid does;
- GM resolves;
- if Problem requires roll -> ENCRENCA.

Must NOT:
- auto-discover a clue because screen opened;
- expose every clue placed at the Location;
- expose GM Problem suggestions;
- become a tactical map.

LOCAL may be implemented as a contextual CENA state rather than a permanent main-nav destination.

---

## SCREEN 09 — COTIDIANO
Status: **CONTRACT LOCKED**

Primary job:
`run an Everyday Life scene`

Use same narrative logic as CENA.

Scene may involve:
- Problem;
- Pride;
- Relationships;
- Anchor;
- family/school/friends;
- ordinary life with no apparent Problem.

Must NOT:
- invent a checklist subsystem;
- treat chores as universal mechanical objectives;
- label Everyday Life only as “Phase 1”;
- guarantee recovery merely by tapping Anchor.

Anchor healing uses the full Recovery Contract.

Everyday Life scenes should remain short and narrative-first.

---

## SCREEN 10 — CLÍMAX
Status: **CONTRACT LOCKED**

Primary job:
`run Showdown; use Extended Trouble only when required`

Two valid modes:

### A. Normal Showdown
If one or more ordinary Problems are enough:
- use CENA + ENCRENCA;
- no special “climax roll” mechanic.

### B. Extended Trouble
Use exact state machine:
`STAKES -> THREAT -> PLAN -> KID SCENES -> RESULT -> OPTIONAL COMPROMISE`

Must NOT:
- reduce the whole showdown to one final generic roll;
- expose generic “final resources” panel;
- apply NPC Special Attributes on top of Threat Level;
- skip each Kid's scene.

---

## SCREEN 11 — ENCERRAMENTO
Status: **CONTRACT LOCKED**

Primary job:
`Aftermath + Change + XP`

Shows:
- consequence/aftermath narration;
- one Everyday Life consequence scene per Kid where applicable;
- healed Conditions between Mysteries;
- Change choices;
- birthday change if applicable;
- five XP questions;
- XP gained;
- optional valid Skill increase before/after session.

Must NOT:
- spend XP during active play;
- increase Attribute using 5 XP;
- exceed Skill 5;
- store more than 10 XP.

---

## SCREEN 12 — MOCHILA
Status: **CONTRACT LOCKED**

Primary job:
`inspect possessed Items`

Shows:
- Iconic Item;
- Mystery Items actually possessed;
- description;
- specific Item bonus if it actually exists;
- relevant state such as consumable only if the actual Item has such a property.

Using an Item:
- does not directly add dice from Mochila;
- sends `PROPOSE_ITEM_USE` to current scene/test;
- validator/GM confirms applicability;
- then modifier enters test pool.

Must NOT:
- assume camera/radio/binoculars/etc have universal fixed bonuses;
- create weight/slot encumbrance rules not present;
- apply Item bonus outside a relevant action.

---

## SCREEN 13 — MENU
Status: **PRODUCT CONTRACT LOCKED / NON-MECHANICAL**

Primary job:
`utilities only`

May contain:
- save/load;
- campaign snapshots;
- import/export;
- accessibility;
- TTS settings;
- API/configuration;
- rules-profile display;
- diagnostics.

Must NOT:
- directly resolve a roll;
- edit Conditions as a shortcut;
- change XP/Skills outside legitimate flow;
- reveal GM-secret campaign data to player;
- mix Tales and Flood state.

Profile boundary:
`TALES/KIDS` and `FLOOD/TEENS` are separate mechanical profiles.

---

# 8. STATE-MUTATION FIREWALL

Only validated game events may mutate mechanical state.

Suggested event classes:

- PLAYER_ACTION_DECLARED
- GM_NARRATION
- REQUEST_TEST
- APPLY_HELP
- APPLY_ITEM
- ROLL_RESULT
- SPEND_LUCK
- PUSH_ROLL
- USE_PRIDE
- APPLY_CONDITION
- BECOME_BROKEN
- HEAL_CONDITIONS
- REVEAL_CLUE
- REVEAL_LOCATION
- REVEAL_CONNECTION
- START_EXTENDED_TROUBLE
- SET_THREAT_LEVEL
- SUBMIT_EXTENDED_ACTION
- APPLY_EXTENDED_RESULT
- ACCEPT_COMPROMISE
- AWARD_XP
- SPEND_XP
- APPLY_CHANGE
- SAVE_SNAPSHOT

Screens request events.
Screens do not directly rewrite authoritative state.

---

# 9. AI / GM OUTPUT CONTRACT

AI narrative and mechanical payload must be separate.

Recommended shape:

```json
{
  "narrative": "player-visible narration",
  "events": [
    {
      "type": "REQUEST_TEST",
      "attribute": "Mente",
      "skill": "Investigar",
      "reason": "Examinar o painel",
      "required_successes": 1
    }
  ]
}
```

Before any event reaches the UI:
1. schema validation;
2. Tales/Kids profile validation;
3. mechanical-rule validation;
4. visibility validation;
5. campaign-state validation.

If validation fails:
- event is rejected;
- narrative may be preserved if safe;
- system requests a corrected event;
- UI never invents a replacement mechanic.

---

# 10. THREE-SESSION FINISH PLAN

## SESSION 1 — RULES + CONTRACTS
**DONE by this document**
Deliverable:
- global mechanical lock;
- visibility firewall;
- recovery lock;
- Extended Trouble state machine;
- Change/XP lock;
- 13 screen contracts;
- state-mutation firewall;
- AI event contract.

Exit gate:
`RULES_CONTRACT_GATE = PASS`

## SESSION 2 — VISUAL + REAL SCREEN STATES
Do only:
1. freeze approved ENCRENCA;
2. finish CENA from 70% to approved;
3. mechanically correct KID without losing approved visual language;
4. create/correct MISTÉRIO, MAPA, MOCHILA, CLÍMAX, COTIDIANO;
5. create GRUPO, CONEXÕES, LOCAL, ENCERRAMENTO, MENU;
6. every screen gets:
   - 415×915 implementation;
   - real Chromium/Compose capture;
   - readability check;
   - visual approval state.

No new rule design is allowed in Session 2.

Exit gate:
`VISUAL_SCREEN_GATE = PASS`

## SESSION 3 — WIRING + REGRESSION
Do only:
1. connect screen states to authoritative events;
2. connect AI narrative/event validation;
3. prove hidden-state firewall;
4. prove test lifecycle:
   CENA -> ENCRENCA -> RESULT -> CENA;
5. prove Mystery reveal lifecycle;
6. prove Map known-only behavior;
7. prove Anchor/Hideout recovery;
8. prove Extended Trouble full state machine;
9. prove XP/Change;
10. navigation, save/load, persistence;
11. real 415×915 capture set;
12. rules regression audit;
13. no intermediate Android ZIPs;
14. final project package only after all gates pass.

Exit gate:
`TALES_KIDS_VTT_CORE = PASS`

---

# 11. CURRENT SCORE AFTER THIS LOCK

Rules understood/audited:
**~95%+ of player-facing core needed by these screens**

Screen contracts:
**13/13 defined**

Rules + contracts phase:
**PASS for TALES/KIDS core screen architecture**

Remaining work is no longer “figure out the rules”.
Remaining work is:
`render -> implement -> wire -> test`.
