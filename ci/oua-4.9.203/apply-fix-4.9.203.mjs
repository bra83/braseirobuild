import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>fs.writeFileSync(path.join(root,rel),text,'utf8');

function must(text,needle,label){
  if(!text.includes(needle)) throw new Error('OUA 4.9.203 anchor missing: '+label);
}

let panel=read('components/JourneyResolutionPanel.tsx');

must(panel,
  "const [mountVigour, setMountVigour] = useState<Record<string, number>>({});",
  'journey state'
);

panel=panel.replace(
  "const [mountVigour, setMountVigour] = useState<Record<string, number>>({});",
  `const [mountVigour, setMountVigour] = useState<Record<string, number>>({});
  const [pendingManualRoll, setPendingManualRoll] = useState<{ label: string; run: () => Promise<void> } | null>(null);

  const requestRollJourney = (...args: Parameters<Props['onRollJourney']>): Promise<void> => {
    const [kind, characterId, skill] = args;
    const hero = characters.find(character => character.id === characterId);
    const kindLabel = kind === 'march' ? 'Jogada de Marcha' : kind === 'event' ? 'Teste do Evento' : 'Teste final da Jornada';
    const heroName = hero?.sheet.identity.name || hero?.name || 'Herói';
    setPendingManualRoll({
      label: kindLabel + ' · ' + skill + ' · ' + heroName,
      run: () => props.onRollJourney(...args),
    });
    return Promise.resolve();
  };

  const requestGenerateEvent = (): Promise<void> => {
    setPendingManualRoll({
      label: 'Dados do Evento de Jornada',
      run: () => props.onGenerateEvent(),
    });
    return Promise.resolve();
  };`
);

panel=panel.replaceAll('props.onRollJourney(', 'requestRollJourney(');
panel=panel.replaceAll('props.onGenerateEvent()', 'requestGenerateEvent()');

const gateAnchor = "  if (masterState.company.eyeAwarenessEnabled === undefined) {";
must(panel,gateAnchor,'roll gate insertion point');
panel=panel.replace(
  gateAnchor,
  `  if (pendingManualRoll) {
    return (
      <div style={box}>
        <div style={{ fontSize: 10, letterSpacing: '.12em', opacity: .72 }}>ROLAGEM MANUAL</div>
        <h3 style={{ margin: '3px 0 5px', fontSize: 18 }}>{pendingManualRoll.label}</h3>
        <p style={{ margin: '0 0 10px', fontSize: 12, lineHeight: 1.45, opacity: .84 }}>
          Nenhum dado foi rolado ainda. Confira o teste e toque em <strong>Rolar</strong> quando quiser lançar os dados.
        </p>
        <button
          disabled={props.busy}
          onClick={() => {
            const staged = pendingManualRoll;
            setPendingManualRoll(null);
            void staged.run();
          }}
          style={primaryButton}
        >
          Rolar
        </button>
        <button
          disabled={props.busy}
          onClick={() => setPendingManualRoll(null)}
          style={{ ...primaryButton, marginTop: 7, borderColor: '#655844', background: '#201b15', color: '#dcc9a9' }}
        >
          Cancelar
        </button>
      </div>
    );
  }

${gateAnchor}`
);

write('components/JourneyResolutionPanel.tsx',panel);

let version=read('lib/version.ts');
version=version.replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.203'");
version=version.replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49203');
write('lib/version.ts',version);

const pkgPath=path.join(root,'package.json');
const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));
pkg.version='4.9.203';
fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');

let gradle=read('android/app/build.gradle.kts');
gradle=gradle.replace(/versionCode\s*=\s*\d+/,'versionCode = 49203');
gradle=gradle.replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.203"');
write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.203 explicit journey dice roll gate applied');
