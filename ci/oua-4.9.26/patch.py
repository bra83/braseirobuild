from pathlib import Path
import json,re,sys
if len(sys.argv)!=2: raise SystemExit("usage: patch_4926.py <root>")
root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")

types_rel="lib/master/types.ts"
types=read(types_rel)
anchor="  lastLongRestDayKey?: string;"
if anchor not in types: raise SystemExit("lastLongRestDayKey type anchor missing")
if "lastLongRestWorldMinute?: number;" not in types:
    types=types.replace(anchor,anchor+"\n  lastLongRestWorldMinute?: number;",1)
write(types_rel,types)

cmd_rel="lib/master/commands.ts"
cmd=read(cmd_rel)
start=cmd.find("    case 'resolve_rest': {")
end=cmd.find("\n    case 'advance_time':",start)
if start<0 or end<0: raise SystemExit("resolve_rest block not found")
old=cmd[start:end]
new="""    case 'resolve_rest': {
      const startDayKey = `${nextState.worldClock.year}:${nextState.worldClock.season}:${nextState.worldClock.day}`;
      const stampField = command.kind === 'short' ? 'lastShortRestDayKey' : 'lastLongRestDayKey';
      const legacyLongRestStamp = command.kind === 'long'
        && nextState.company.lastLongRestDayKey === startDayKey
        && !Number.isFinite(Number(nextState.company.lastLongRestWorldMinute));
      if (nextState.company[stampField] === startDayKey && !legacyLongRestStamp) {
        throw new Error(command.kind === 'short'
          ? 'Descanso Curto já realizado neste dia.'
          : 'Descanso Prolongado já concluído neste dia. Avance o tempo antes de descansar novamente.');
      }

      const cramWisdom = command.kind === 'short'
        ? Math.max(0, ...nextCharacters.filter(c => partyIds.has(c.id) && /\\bcram\\b/i.test(c.sheet.other.virtues || '')).map(c => Number(c.sheet.attributes.wisdom) || 0))
        : 0;

      for (const characterId of partyIds) {
        updateHero(characterId, sheet => {
          let next = structuredClone(sheet);
          const strength = Math.max(0, Number(next.attributes.strength) || 0) * restStrengthMultiplier(next);
          if (command.kind === 'short') {
            if (!next.attributes.wounded) next.attributes.enduranceCurrent = Math.min(next.attributes.enduranceMax, next.attributes.enduranceCurrent + strength + cramWisdom);
          } else {
            if (next.attributes.wounded) next.attributes.enduranceCurrent = Math.min(next.attributes.enduranceMax, next.attributes.enduranceCurrent + strength);
            else next.attributes.enduranceCurrent = next.attributes.enduranceMax;
            if (next.attributes.hopeCurrent <= 0) next.attributes.hopeCurrent = Math.min(next.attributes.hopeMax, 1);
            if (command.safeRefuge) next.attributes.fatigue = Math.max(0, next.attributes.fatigue - 1);
            if (Number((next.attributes as any).woundDays || 0) > 0) {
              (next.attributes as any).woundDays = Math.max(0, Number((next.attributes as any).woundDays) - 1);
              if ((next.attributes as any).woundDays <= 0 && !(next.attributes as any).dying) {
                next.attributes.wounded = false;
                (next.attributes as any).woundSeverity = undefined;
              }
            }
          }
          return recomputeConditions(next);
        });
      }

      const restMinutes = command.kind === 'short' ? 60 : 480;
      const advanced = advanceWorldClock(nextState.worldClock, restMinutes);
      nextState = { ...nextState, worldClock: advanced.clock };
      const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, restMinutes);
      nextState = simulated.master;
      nextNpcs = simulated.npcs;

      const endDayKey = `${nextState.worldClock.year}:${nextState.worldClock.season}:${nextState.worldClock.day}`;
      nextState.company = {
        ...nextState.company,
        [stampField]: command.kind === 'long' ? endDayKey : startDayKey,
        ...(command.kind === 'long' ? { lastLongRestWorldMinute: Math.max(0, Number(nextState.worldClock.elapsedMinutes) || 0) } : {}),
      };

      appendLivingActivity(
        'rest',
        command.kind === 'short' ? 'Descanso Curto' : 'Descanso Prolongado',
        command.kind === 'short'
          ? 'A Companhia descansou por pelo menos uma hora.'
          : 'A Companhia dormiu por aproximadamente oito horas; o relógio e o Mundo Vivo avançaram.',
        nextState.position.currentHexId || undefined,
      );
      break;
    }"""
cmd=cmd[:start]+new+cmd[end:]
write(cmd_rel,cmd)

# Version bump
pkg=root/"package.json"
d=json.loads(pkg.read_text(encoding="utf-8"));d["version"]="4.9.26";pkg.write_text(json.dumps(d,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8");t=re.sub(r'"version":\s*"4\.9\.25"','"version": "4.9.26"',t,count=2);lock.write_text(t,encoding="utf-8")
v="lib/version.ts";t=read(v);t=re.sub(r"APP_VERSION = '4\.9\.25'","APP_VERSION = '4.9.26'",t);t=re.sub(r"APP_VERSION_CODE = 49025","APP_VERSION_CODE = 49026",t);write(v,t)
g="android/app/build.gradle.kts";t=read(g);t=re.sub(r"versionCode\s*=\s*49025","versionCode = 49026",t);t=re.sub(r'versionName\s*=\s*"4\.9\.25"','versionName = "4.9.26"',t);write(g,t)
m="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt";t=read(m);t=t.replace("index.html?v=49025&immersive=1","index.html?v=49026&immersive=1");write(m,t)

whole="\n".join(read(p) for p in [types_rel,cmd_rel,m])
checks={
 "long-rest-8h":"const restMinutes = command.kind === 'short' ? 60 : 480;" in whole,
 "long-rest-world-clock":"advanceWorldClock(nextState.worldClock, restMinutes)" in whole,
 "long-rest-living-world":"simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, restMinutes)" in whole,
 "legacy-stamp-self-heal":"legacyLongRestStamp" in whole and "lastLongRestWorldMinute" in whole,
 "stamp-after-rest":"command.kind === 'long' ? endDayKey : startDayKey" in whole,
 "kokoro-preserved":"isNeuralTtsProfile" in t and "TextToSpeech.QUEUE_ADD" in t,
 "version":"4.9.26" in read(v) and "49026" in read(g),
}
for k,vv in checks.items(): print(("PASS" if vv else "FAIL"),k)
bad=[k for k,vv in checks.items() if not vv]
if bad: raise SystemExit("PATCH_4926_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.26_LONG_REST_CLOCK_OK")
