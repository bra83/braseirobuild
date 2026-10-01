import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root=path.resolve(process.argv[2]||process.cwd());
const scriptDir=path.dirname(fileURLToPath(import.meta.url));
const templates=path.join(scriptDir,'templates');
const copy=(srcRel,dstRel)=>{const out=path.join(root,dstRel);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,fs.readFileSync(path.join(templates,srcRel),'utf8'),'utf8');};
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>fs.writeFileSync(path.join(root,rel),text,'utf8');

copy('App.tsx','App.tsx');
copy('services/geminiService.ts','services/geminiService.ts');
copy('components/TokenPicker.tsx','components/TokenPicker.tsx');
copy('lib/partyTokenLibrary.ts','lib/partyTokenLibrary.ts');
copy('lib/humanMasterProfile.ts','lib/humanMasterProfile.ts');
copy('lib/narrativeSanitizer.ts','lib/narrativeSanitizer.ts');


function replaceOnce(text, oldText, newText, label){
  if(!text.includes(oldText)) throw new Error('OUA 4.9.202 journey anchor missing: '+label);
  return text.replace(oldText,newText);
}

// Journey UX repair reported from real-device playtest.
let journeyPanel=read('components/JourneyResolutionPanel.tsx');
journeyPanel=journeyPanel.replace('zIndex: 96,','zIndex: 5000,');
journeyPanel=journeyPanel.replace(
  'Chegada · Fadiga de Jornada',
  'Fim da Jornada · resolver e encerrar'
);
journeyPanel=journeyPanel.replace(
  'Cada Herói com Fadiga pendente pode reduzi-la com Vigor da montaria e uma rolagem final de Viagem. A Fadiga restante só será aplicada ao confirmar o encerramento.',
  'A Fadiga de Jornada só existe quando regras ou Eventos realmente a geraram; viajar por si só não cria Fadiga automática. Se houver Fadiga pendente, cada Herói pode reduzi-la com Vigor da montaria e uma rolagem final de Viagem. Se estiver em 0, basta encerrar a Jornada.'
);
journeyPanel=journeyPanel.replace(
  'Aplicar Fadiga restante e encerrar Jornada',
  "{requiredEnd.length ? 'Aplicar Fadiga restante e encerrar Jornada' : 'Encerrar Jornada agora'}"
);
journeyPanel=journeyPanel.replace(
  'Para exibição de magia, a regra opcional exige julgamento do Mestre: menor +1, maior +2, poderosa +3.',
  'Isto NÃO lança magia e NÃO é uma habilidade do personagem. Use somente quando a ficção já mostrou uma exibição de magia e você quer registrar o aumento da Vigilância do Olho: menor +1, maior +2, poderosa +3.'
);
journeyPanel=journeyPanel.replace('>Magia +1</button>', '>Registrar exibição menor · Olho +1</button>');
journeyPanel=journeyPanel.replace('>Magia +2</button>', '>Registrar exibição maior · Olho +2</button>');
journeyPanel=journeyPanel.replace('>Magia +3</button>', '>Registrar exibição poderosa · Olho +3</button>');
journeyPanel=journeyPanel.replace(
  '<summary style={{ cursor: \'pointer\', fontWeight: 900 }}>Vigilância do Olho · ajustes do Mestre</summary>',
  '<summary style={{ cursor: \'pointer\', fontWeight: 900 }}>Vigilância do Olho · {masterState.company.eyeAwareness}/{masterState.company.eyeHuntThreshold || 16}</summary><div style={{fontSize:11,opacity:.76,marginTop:5}}>Última alteração: {masterState.company.eyeLastChangeReason || \'nenhum gatilho registrado\'}</div>'
);
write('components/JourneyResolutionPanel.tsx',journeyPanel);

// A movement to another hex/location is always a new narrative scene.
let app=read('App.tsx');
app=replaceOnce(
  app,
  "'[MOVIMENTO DE JORNADA JÁ APLICADO] A Companhia chegou a ' + move.locationName + '. O movimento, o tempo e a Marcha já foram aplicados localmente. NÃO faça Jogada de Marcha, NÃO gere nem resolva Evento de Jornada e NÃO encerre a Jornada. Reavalie explicitamente os NPCs: preserve quem plausivelmente viajou ou pertence ao novo local; retire somente quem de fato saiu; introduza NPCs apenas quando a ficção atual justificar.'",
  "'[MOVIMENTO DE JORNADA JÁ APLICADO · NOVA CENA] A Companhia chegou a ' + move.locationName + '. Abra uma NOVA CENA de chegada: não reutilize o título, enquadramento ou descrição da cena anterior. Descreva o novo hex/local, clima, período e sinais sensoriais concretos conforme o perfil canônico do Mestre Humano. O movimento, o tempo e a Marcha já foram aplicados localmente. NÃO faça Jogada de Marcha, NÃO gere nem resolva Evento de Jornada e NÃO encerre a Jornada. Reavalie explicitamente os NPCs: preserve quem plausivelmente viajou ou pertence ao novo local; retire somente quem de fato saiu; introduza NPCs apenas quando a ficção atual justificar.'",
  'movement must open new scene'
);
app=replaceOnce(
  app,
  "const handleCompleteJourneyManual = async () => {\n    await commitJourneyAction([{ type: 'player_complete_journey', reason: 'Jogador confirmou as rolagens finais e o encerramento da Jornada.' }], 'encerrar Jornada manualmente', 'Encerrar Jornada');\n  };",
  "const handleCompleteJourneyManual = async () => {\n    const completed = await commitJourneyAction([{ type: 'player_complete_journey', reason: 'Jogador confirmou as rolagens finais e o encerramento da Jornada.' }], 'encerrar Jornada manualmente', 'Encerrar Jornada');\n    setIsStoryLoading(true);\n    setStoryError(null);\n    await processTurn('[JORNADA ENCERRADA · NOVA CENA] A Jornada foi encerrada mecanicamente pelo jogador. Abra uma nova cena de chegada no local atual usando integralmente o perfil canônico do Mestre Humano. Mostre consequências visíveis da viagem e devolva uma decisão concreta ao jogador. NÃO reabra nem resolva outra Jornada automaticamente.', false, completed);\n  };",
  'journey completion arrival scene'
);
write('App.tsx',app);
copy('lib/humanMasterProfile.ts','lib/humanMasterProfile.ts');
copy('lib/narrativeSanitizer.ts','lib/narrativeSanitizer.ts');

let version=read('lib/version.ts');
version=version.replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.202'");
version=version.replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49202');
write('lib/version.ts',version);

const pkgPath=path.join(root,'package.json');
const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));
pkg.version='4.9.202';
fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');

let gradle=read('android/app/build.gradle.kts');
gradle=gradle.replace(/versionCode\s*=\s*\d+/,'versionCode = 49202');
gradle=gradle.replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.202"');
write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.202 profile hard gate + token recrop applied');
