import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
const p="app/src/main/assets/vtt/r46/r46-shell.bundle.js";
test("P15: bundle embarcado não contradiz TTS integrado",()=>{
 const s=fs.readFileSync(p,"utf8");
 assert.equal(s.includes("integra\u00E7\u00E3o real ser confirmada"),false);
 assert.equal(s.includes("TTS integrado:"),true);
 assert.equal(s.includes("Gemini/Charon quando configurado"),true);
});
