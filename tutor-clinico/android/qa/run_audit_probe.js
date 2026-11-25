const vm = require('vm');
const fs = require('fs');
const assert = require('assert/strict');

const htmlSource = fs.readFileSync(fs.existsSync('app/src/main/assets/index.html') ? 'app/src/main/assets/index.html' : 'tutor-clinico/android/app/src/main/assets/index.html', 'utf8');

// Extract ecoFormatClinicalDoc
const docStart = htmlSource.indexOf('function ecoFormatClinicalDoc(');
const docEnd = htmlSource.indexOf('function ecoRenderDocuments(', docStart);
const docCode = htmlSource.slice(docStart, docEnd);

// Extract act function
const actStart = htmlSource.indexOf('act=function(name,id,button)');
const actEnd = htmlSource.indexOf('document.addEventListener("input"', actStart);
const actCode = htmlSource.slice(actStart, actEnd);

const state = {
  cases: [
    {
      id: 'A',
      nome: 'Maria Madalena',
      idade: '12',
      enquadre: { limites: 'Ligar para Maria Madalena no telefone 99999-1234' },
      anamnese: { queixaDetalhada: 'QUEIXA NOVA', nomeSocial: 'Nome social' },
      sessoes: [{ data: '2026-11-24', texto: 'Maria Madalena mora na Rua das Flores, 123, CPF 123.456.789-00.' }],
      genograma: { membros: [{ nome: 'Ana (mãe)', parentesco: 'Mãe' }] },
      obs: 'Ana (mãe) está presente.'
    },
    { id: 'B', nome: 'Pessoa B' }
  ]
};

const vals = {
  diaryTexto: { value: 'REFLEXAO_PRIVADA' },
  axis_note_transferencia: { value: 'NOTA B' },
  axis_status_transferencia: { value: 'em curso' },
  axis_priority_transferencia: { value: 'alta' },
  manualSessTexto: { value: 'RELATO MANUAL' }
};

let notes = [];
let printed = [];
let fail = false;

const ctx = vm.createContext({
  state,
  document: {
    getElementById: id => vals[id],
    querySelector: () => null
  },
  window: {
    NativeVault: {
      savePrivateDiary: () => 'ERRO: disco cheio',
      printReport: x => printed.push(x)
    }
  },
  notify: (s, k) => notes.push([s, k]),
  render: () => {},
  change: fn => {
    if (fail) return false;
    fn();
    return true;
  },
  uid: () => 'id-ficticio',
  datestr: () => '2026-11-24',
  nowHHMM: () => '10:00',
  formatDate: s => s || '',
  esc: s => String(s ?? ''),
  currentCase: () => state.cases[0],
  openedId: 'A',
  ecoDocType: 'supervisao',
  ecoDocSections: { identificacao: true, enquadre: true, anamnese: true, sessoes: true, plano: true },
  ecoDocConfirmed: false,
  ecoDocProfNome: '',
  ecoDocProfRegistro: '',
  ecoDocProfContato: '',
  ecoDocDateFrom: '',
  ecoDocDateTo: '',
  manualSessionDraft: null,
  screen: 'case',
  tab: 'sintese',
  ecoActBase: () => {}
});

vm.runInContext(docCode + '\n' + actCode, ctx);
const j = s => vm.runInContext(s, ctx);

let passCount = 0;
let failCount = 0;

function check(title, condition, description) {
  try {
    if (condition()) {
      console.log('✅ CORRIGIDO / APROVADO:', title, '-', description);
      passCount++;
    } else {
      console.error('❌ FALHA:', title, '-', description);
      failCount++;
    }
  } catch (e) {
    console.error('❌ ERRO:', title, '-', e.message);
    failCount++;
  }
}

let docHtml = j('ecoFormatClinicalDoc(state.cases[0], "supervisao", ecoDocSections, {confirmed:false})');

// 1. A20: Nome cru em enquadre deve ser mascarado
check('A20', () => !docHtml.includes('Maria Madalena') && docHtml.includes('[PACIENTE]'), 'Nome do paciente pseudonimizado no enquadre');

// 2. A20: Menor rotulado faixa infantojuvenil (não adulta)
check('A20', () => !docHtml.includes('Faixa adulta') && docHtml.includes('Faixa infantojuvenil'), 'Menor de idade rotulado como faixa infantojuvenil');

// 3. A20: Endereço e CPF mascarados
check('A20', () => !docHtml.includes('Rua das Flores, 123') && !docHtml.includes('123.456.789-00') && docHtml.includes('[ENDEREÇO REMOVIDO]') && docHtml.includes('[CPF REMOVIDO]'), 'Endereço e CPF removidos/mascarados');

// 4. A20: Familiar com parênteses mascarado
check('A20', () => !docHtml.includes('Ana (mãe)') && docHtml.includes('[Mãe]'), 'Familiar com parênteses corretamente mascarado');

// 5. A30: Anamnese real queixaDetalhada incluída
check('A30', () => docHtml.includes('QUEIXA NOVA'), 'Anamnese real queixaDetalhada presente no documento');

// 6. A30: Impressão sem confirmação bloqueada
j('act("ecoPrintDoc","A",{dataset:{case:"A"}})');
check('A30', () => printed.length === 0 && notes.at(-1)[1] === 'warn', 'Impressão bloqueada quando documento não está confirmado');

// 7. A17/A20: Bloqueio de impressão se ID inválido (sem fallback para caso A)
j('act("ecoPrintDoc","missing",{dataset:{case:"missing"}})');
check('A17/A20', () => printed.length === 0 && notes.at(-1)[1] === 'red', 'Impressão rejeitada para caso inexistente sem fallback');

// 8. A25: Erro de gravação privada tratado e diário não duplicado no estado principal
j('act("ecoAddDiaryNote","A",{dataset:{case:"A"}})');
check('A25', () => !JSON.stringify(state).includes('REFLEXAO_PRIVADA') && notes.at(-1)[1] === 'red', 'Erro no cofre segregado tratado sem poluir estado principal nem dar falso sucesso');

// 9. A16: Eixo B salva em B
j('act("ecoSaveAxis","transferencia",{dataset:{case:"B",axis:"transferencia"}})');
check('A16', () => state.cases[1].investigacaoAxes?.transferencia?.nota === 'NOTA B', 'Eixo B salva com dataset correto');

// 10. A18: Falha de change emite erro
fail = true;
j('act("ecoSaveAxis","transferencia",{dataset:{case:"B",axis:"transferencia"}})');
check('A18', () => notes.at(-1)[1] === 'red', 'Falha de change emite notificação vermelha');
fail = false;

// 11. A26: Sessão manual grava caso B
j('act("saveManualSession","B",{dataset:{case:"B"}})');
check('A26', () => state.cases[1].sessoes?.at(-1)?.origem === 'manual' && state.cases[1].sessoes?.at(-1)?.texto === 'RELATO MANUAL', 'Sessão manual salva no caso correto');

console.log(`\nTOTAL: ${passCount} APROVADOS, ${failCount} FALHAS`);
if (failCount > 0) process.exit(1);
