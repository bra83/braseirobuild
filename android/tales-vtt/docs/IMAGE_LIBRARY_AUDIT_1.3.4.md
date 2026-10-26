# Image library audit — 1.3.4

No new scene artwork was generated in this revision. This audit only inventories the current integrated library and defines the next missing images.

## Current integrated scene library

- 63 scene PNGs are integrated into the Android drawable library and allowed by the Motor Barbara scene firewall.
- The filename-level weather balance is visibly skewed: at least 15 assets are explicitly rain variants, 6 sunset, 1 mist, 1 winter, 1 morning, while no filename-level dedicated storm/snow/clear-weather variants exist.
- The strongest coverage is Tales/Kids home + school + Loop exploration.
- The weakest coverage is Flood/Teens social life, city scenes, aftermath/drama, neutral NPC interactions and non-rain travel/weather.

## Kid portraits

The old group cards were displaying archetype icons rather than proper character portraits, and the Android portrait resolver only had explicit cases for 4 Tales archetypes. Four already-generated character portraits have now replaced those displayed images and are the active random-Kid portrait pool:

- kid_portrait_01
- kid_portrait_02
- kid_portrait_03
- kid_portrait_04

Legacy save keys for the four previously displayed type images resolve to the new portraits, so old saves do not fall back to the bad artwork.

No additional Kid portrait generation is required to make the current four-Kid group look correct. For long-term variety, 8–12 extra Kid portraits would be useful later, but they are outside the 100-scene library target.

## 37 scene images still missing to reach the 100-scene target

### Tales/Kids everyday & town — 5
1. After-school diner/café
2. Bicycle rack outside school or shops
3. Friend's house / garage hangout
4. Dry, clear residential afternoon
5. Snowy residential winter afternoon

### Flood/Teens home & social — 8
6. Teen arriving home late
7. Difficult phone call at home
8. Meeting an Anchor in a quiet place
9. Private/intimate conversation between Teens
10. Small house party
11. Leaving home after an argument
12. Morning after a bad night
13. Teen alone listening to music, Flood tone

### Flood/Teens town & nightlife — 7
14. School parking lot at dusk
15. Diner at night
16. Nearly empty town square at night
17. Commercial street closing for the evening
18. Gas station after midnight
19. Teen basement/house party
20. Solitary walk through town at night

### Flood/Teens adventure & danger — 5
21. Dark rural road
22. Night chase
23. Dry underground service tunnel
24. Fenced electrical substation
25. Abandoned rail/maintenance tunnel

### Flood/Teens drama & aftermath — 5
26. Group immediately after an argument
27. Friction splitting the group
28. Teen isolated from the others
29. Recovery after danger with an Anchor
30. Scar discovery / emotional aftermath

### Shared travel & weather — 3
31. Bus ride at sunset
32. Forest in clear morning light
33. Forest at night under clear weather

### Shared investigation — 4
34. Discovering a clue in context
35. Conversation with an NPC in a neutral everyday location
36. Finding a strange object
37. Escaping an unknown location / immediate aftermath

These 37 are deliberately biased away from rain because the existing 63 already over-represent rainy scenes.
