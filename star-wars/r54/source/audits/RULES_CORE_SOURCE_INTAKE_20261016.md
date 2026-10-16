# Rules core-source intake — 2026-10-16

This is a limited documentary intake for the two authorized core rulebooks. It supports corpus identity/provenance work only; it is not a mechanics audit or certification.

| Book | Drive ID | Drive/local byte size | Local SHA-256 | PDF pages | Language/edition evidence | Text quality |
|---|---|---:|---|---:|---|---|
| Edge of the Empire Core Rulebook | `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX` | 207,887,235 | `AFCF0FE3B5C9FC132DD7B641B54DCCE703BEE2FB01EE8F9EBE213F2CD3BB85C6` | 457 | Cover/title plus PDF metadata identify Fantasy Flight Games, *Star Wars RPG — Edge of the Empire*. | OCR layer exists but is degraded; page 3 extracts no text. |
| Age of Rebellion Core Rulebook | `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq` | 278,764,931 | `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50` | 466 | Cover/title plus PDF metadata identify Fantasy Flight Games, *Star Wars RPG — Age of Rebellion*. | First three pages are image-only to pypdf text extraction; visual/OCR review is needed for content work. |

## Method and limits

- Compared local sizes with the Google Drive metadata records returned for the exact IDs; sizes matched exactly.
- Rendered and visually inspected page 1 for each PDF, read PDF metadata, counted pages, and computed local SHA-256.
- The Drive metadata connector did not return an MD5/content hash. Therefore a fresh-download byte comparison against Drive is not claimed.
- Only cover/metadata and first-page extraction behavior were assessed. No rule pages were semantically reviewed, no errata comparison was done, and no printed-to-PDF page map was established.
- The other 17 corpus rows have not had their PDF bytes examined. The selected supplement set remains unresolved; no rules, RAG, Gemini, or AoR statuses are upgraded by this intake.

The intended architecture remains hybrid: official source text is retrieved through cited RAG; Gemini may narrate or propose typed actions; deterministic code performs validation and all state/mechanical mutations.
