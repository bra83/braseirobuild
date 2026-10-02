import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] || process.cwd());
const file = path.join(root, 'party-app-audit.tsx');
let source = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n');

if (!source.includes("campaignStartSelection={{ mode: 'STAR_OF_MIST' }}")) {
  const oldBlock = `    canStartAdventure={true}
    isStoryLoading={false}`;
  const newBlock = `    canStartAdventure={true}
    campaignStartSelection={{ mode: 'STAR_OF_MIST' }}
    onCampaignStartSelectionChange={() => {}}
    isStoryLoading={false}`;
  if (!source.includes(oldBlock)) {
    throw new Error('OUA 4.9.206: party-app-audit opening anchor missing');
  }
  source = source.replace(oldBlock, newBlock);
  fs.writeFileSync(file, source, 'utf8');
}

console.log('OUA 4.9.206 visual audit props fixed');
