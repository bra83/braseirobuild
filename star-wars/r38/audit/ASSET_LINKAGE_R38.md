# R38 — CATALOG → UI VISUAL LINKAGE AUDIT

- Manifest assets: **369**
- Explicit equipment catalog visual/image/asset binding lines: **0**
- Explicit vehicle catalog visual/image/asset binding lines: **0**
- Image-library/native-image bridge tokens found in R46 runtime/bundle: **True**
- Candidate visual/image bridge methods actually referenced by R46: **6**
- Scene Gemini image path present in R46: **True**

## Requested category matrix

| Category | Created/cataloged | Explicit mechanical-ID→visual binding | R46 image-library linkage proven | Status |
|---|---|---|---|---|
| ITENS | SIM | NÃO/SEM PROVA | SIM | **LACUNA/SEM PROVA** |
| VEÍCULOS/NAVES | SIM | NÃO/SEM PROVA | SIM | **LACUNA/SEM PROVA** |
| NPC/DROIDS/BEASTS | SIM | NÃO/SEM PROVA | SIM | **LACUNA/SEM PROVA** |
| LOCAIS/MAPAS | SIM | NÃO/SEM PROVA | SIM | **PROVADO** |
| CENAS | SIM | NÃO/SEM PROVA | SIM | **PROVADO** |

## Bridge methods referenced by R46

- `getImageAsset` → app/src/main/assets/vtt/r46/r46-shell.bundle.js
- `imageAsset` → app/src/main/assets/vtt/r46/r46-shell.bundle.js
- `imageAssetRuntime` → app/src/main/assets/vtt/r46/r46-shell.bundle.js
- `imageMapCatalog` → app/src/main/assets/vtt/r46/r46-shell.bundle.js
- `imageVisualEntityCatalog` → app/src/main/assets/vtt/r46/r46-shell.bundle.js
- `searchImageLibrary` → app/src/main/assets/vtt/r46/r46-shell.bundle.js

## Provenance freeze

```json
{
  "run": "R46",
  "baseCheckpoint": "R45",
  "purpose": "Freeze the validated visual subset and make unresolved Kainrath media non-blocking without inventing remote filenames.",
  "manifestVersion": "3.8.0",
  "validatedCounts": {
    "total": 369,
    "local": 148,
    "external": 221,
    "vehicles": 127,
    "droids": 23,
    "creatures": 17,
    "maps": 18
  },
  "policy": {
    "allowedHost": "kainrath.wordpress.com",
    "allowedPathPrefix": "/wp-content/uploads/",
    "indexOnlyDirectMediaActuallyObserved": true,
    "inferRemoteFilename": false,
    "inferMechanicsFromImage": false,
    "mechanicalBindingForUnvalidatedVisuals": null
  },
  "closure": {
    "validatedSubsetReadyForGameplay": true,
    "unresolvedRemoteMediaBlocksGameplay": false,
    "unresolvedItemsAreDeferredOptionalVisualExtensions": true,
    "shellMutationRequired": false,
    "manifestMutationRequired": false
  },
  "observedPostsWithoutDirectMediaUrl": [
    {
      "title": "Void-Spider TX-3 Air Taxi",
      "sourcePageUrl": "https://kainrath.wordpress.com/2023/01/08/void-spider-tx-3-air-taxi/",
      "status": "source_post_observed_direct_media_url_not_observed"
    },
    {
      "title": "A-1 Deluxe Floater",
      "sourcePageUrl": "https://kainrath.wordpress.com/2023/01/08/a-1-deluxe-floater/",
      "status": "source_post_observed_direct_media_url_not_observed"
    },
    {
      "title": "XP-38A Landspeeder",
      "sourcePageUrl": "https://kainrath.wordpress.com/2023/01/08/xp-38a-landspeeder/",
      "status": "source_post_observed_direct_media_url_not_observed"
    },
    {
      "title": "Scavenger Tug",
      "sourcePageUrl": "https://kainrath.wordpress.com/2022/11/04/scavenger-tug/",
      "status": "source_post_observed_direct_media_url_not_observed"
    },
    {
      "title": "GS-100 Salvage Ship",
      "sourcePageUrl": "https://kainrath.wordpress.com/2022/11/04/gs-100-salvage-ship/",
      "status": "source_post_observed_direct_media_url_not_observed"
    },
    {
      "title": "Antiquated Battle Droid",
      "sourcePageUrl": "https://kainrath.wordpress.com/2016/01/01/antiquated-battle-droid-2/",
      "status": "source_post_observed_direct_media_url_not_observed"
    }
  ],
  "deferred": {
    "vehicles": [
      "Gymsnor-4 Light Freighter",
      "G9 Rigger Light Freighter",
      "Seltiss-2 Caravel",
      "CSS-1 Corellian Star Shuttle",
      "CEC HWK-1000 Light Freighter",
      "HT-2200 Medium Freighter",
      "SoroSuub Luxury 3000 Space Yacht",
      "YT-2400 Light Freighter",
      "YT-1300 Light Freighter",
      "Starwind Pleasure Yacht",
      "Ghtroc 720 Light Freighter",
      "ILH-KK Citadel-Class Light Freighter",
      "Storm IV Cloud Car",
      "T-16 Skyhopper",
      "T-47 Airspeeder",
      "Wheeled/tracked category: direct media URLs not yet observed",
      "Void-Spider TX-3 Air Taxi",
      "A-1 Deluxe Floater",
      "XP-38A Landspeeder",
      "Scavenger Tug",
      "GS-100 Salvage Ship"
    ],
    "beasts": [
      "Cybernetically-Enhanced Nexu",
      "Sand Panther",
      "Sacorrian Grey Bear",
      "Spooked Ronto",
      "Kowakian Monkey-Lizard",
      "Spade-Headed Smooka",
      "Lurking Scrange",
      "Lesser Bogwing",
      "Gigantic Dragonsnake",
      "Flying Leaf-Tail",
      "Kaadu"
    ],
    "droids": [
      "Antiquated Battle Droid"
    ]
  },
  "reason": "A source post/title is not sufficient evidence for a remote media filename. Entries remain deferred until a direct Kainrath wp-content/uploads URL is actually observed."
}
```

## Rule-catalog explicit visual binding excerpts

### Equipment
- none

### Vehicles
