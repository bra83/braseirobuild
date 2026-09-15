from pathlib import Path

p=Path('app/src/main/assets/www/core/combat-service.js')
s=p.read_text()

if 'function applyGrappleEndRoundDamageInPlace(' not in s:
    raise SystemExit('grapple round hook not installed')

s=s.replace(
    "let transition='NEXT_SIDE',mortalInjury=[],effectExpirations=[];",
    "let transition='NEXT_SIDE',mortalInjury=[],effectExpirations=[],grappleEndRound=[];",
    1,
)
s=s.replace(
    "const grappleEndRound=[...applyGrappleEndRoundDamageInPlace(next,combat,completedRound)];",
    "grappleEndRound=[...applyGrappleEndRoundDamageInPlace(next,combat,completedRound)];",
    1,
)
s=s.replace(
    "transition='NEW_ROUND';return{transition,mortalInjury,effectExpirations,targetTurnExpirations,grappleEndRound};}",
    "transition='NEW_ROUND';}",
    1,
)
s=s.replace(
    "return{transition,mortalInjury,effectExpirations,targetTurnExpirations};",
    "return{transition,mortalInjury,effectExpirations,targetTurnExpirations,grappleEndRound};",
    1,
)

if "let transition='NEXT_SIDE',mortalInjury=[],effectExpirations=[],grappleEndRound=[];" not in s:
    raise SystemExit('grappleEndRound accumulator normalization failed')
if "transition='NEW_ROUND';return{transition,mortalInjury,effectExpirations,targetTurnExpirations,grappleEndRound};}" in s:
    raise SystemExit('early round-transition return still present')

p.write_text(s)
print('WWN grapple round hook cleanup preserved')
