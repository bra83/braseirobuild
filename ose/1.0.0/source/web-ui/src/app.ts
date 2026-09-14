import { fixtures, type FixtureId, FIXED_CLOCK, FIXTURE_REVISION } from './fixtures.js';
import { BRIDGE_VERSION, postBridge } from './bridge.js';

const A='./assets/';
const img=(n:string,c='',a='')=>`<img ${a?`data-anchor="${a}"`:''} class="${c}" data-canonical-asset="${n}" src="${A}${n}" alt="">`;
const fixture=()=>{const q=new URLSearchParams(location.search).get('fixture') as FixtureId|null;return q&&q in fixtures?q:'session-prestart'};
const overlayEnabled=()=>new URLSearchParams(location.search).get('conceptOverlay')==='1';

function header(title:string,subtitle:string){return `<header class="ose-header">
  <span class="ose-logo-box" data-anchor="header_logo">${img('001_logo_ose.png','ose-logo')}</span>
  <h1 data-anchor="header_title">${title}</h1>
  <p data-anchor="header_subtitle">${subtitle}</p>
  <button class="top-icon menu" data-anchor="menu" aria-label="Menu">${img('002_icone_menu.png')}</button>
  <button class="top-icon settings" data-anchor="settings" aria-label="Configurações">${img('003_icone_configuracoes.png')}</button>
  <i class="header-divider"></i>
</header>`}

function bottomNav(){return `<nav class="ose-bottom-nav">
  <button class="selected" data-anchor="bottom_session">${img('006_icone_pergaminho.png')}<span>SESSÃO</span></button>
  <button data-anchor="bottom_map">${img('013_icone_mapa.png')}<span>MAPA</span></button>
  <button data-anchor="bottom_sheet">${img('012_icone_livros.png')}<span>FICHA</span></button>
  <button data-anchor="bottom_bag">${img('015_icone_mochila.png')}<span>BOLSA</span></button>
</nav>`}

const railItem=(asset:string,label:string,anchor:string)=>`<button class="rail-item" data-anchor="${anchor}">${img(asset)}<span>${label}</span></button>`;
const quick=(asset:string,label:string,anchor:string)=>`<button class="quick-item" data-anchor="${anchor}">${img(asset)}<span>${label}</span></button>`;

function prestart(){return `<main class="screen ose-session prestart-strict">
  ${header('NOVA SESSÃO','SESSION_PRESTART • narrativa ainda não iniciada')}
  <section class="ready-block">
    ${img('006_icone_pergaminho.png','ready-icon','ready_icon')}
    <h2 data-anchor="ready_title">PRONTO PARA COMEÇAR</h2>
    <p data-anchor="ready_subtitle">Party e posição inicial definidas.</p>
  </section>
  <section class="prep-panel" data-anchor="prep_panel">
    ${img('075_painel_pergaminho_rustico.png','prep-panel-art')}
    <div class="prep-content">
      <h2>PREPARAÇÃO DA SESSÃO</h2>
      <dl>
        <dt>POSIÇÃO INICIAL</dt><dd>Entrada das Cavernas</dd>
        <dt>PARTY</dt><dd>4 aventureiros • pronta</dd>
        <dt>ESTADO</dt><dd>Narrativa ainda não iniciada</dd>
      </dl>
      <p class="prep-note"><b>PLAYER_ACTION</b> permanece indisponível.<br>Nenhum tempo, posição, NPC ou<br>consequência mecânica avançou.</p>
    </div>
  </section>
  <aside class="prestart-rail" aria-label="Atalhos da sessão">
    ${railItem('014_icone_grupo.png','PARTY','rail_party')}
    ${railItem('013_icone_mapa.png','MAPA','rail_map')}
    ${railItem('011_icone_d20.png','DADOS','rail_dice')}
    ${railItem('015_icone_mochila.png','BOLSA','rail_bag')}
  </aside>
  <button class="start-cta" data-anchor="start_cta"><span>COMEÇAR A NARRAR</span></button>
  <div class="prestart-action" data-anchor="player_action">${img('075_painel_pergaminho_rustico.png','prestart-action-art')}<span>PLAYER_ACTION</span></div>
  <small class="prestart-action-note">indisponível</small>
  <button class="prestart-help-icon" data-anchor="tts_control" aria-label="Ajuda do mestre">${img('OSE_SESSION_A501_icone_gm_help.png')}</button>
  <button class="prestart-gm" data-anchor="gm_help">GM_HELP</button>
  <small class="prestart-gm-note">não avança mundo</small>
  <section class="prestart-tts-note"><span>TTS</span><b>indisponível até existir narração.</b></section>
  ${bottomNav()}
</main>`}

function active(keyboard=false){const f=fixtures['session-active'];return `<main class="screen ose-session active-strict ${keyboard?'kbd-open':''}">
  ${header('CRIPTA SOB O OUTEIRO','SESSION_ACTIVE • narrativa em andamento')}
  <section class="active-status" aria-label="Estado da sessão">
    <div data-anchor="status_light">${img('008_status_tocha_30m.png')}</div>
    <div data-anchor="status_move">${img('010_status_movimento_90.png')}</div>
    <div data-anchor="status_dice">${img('011_icone_d20.png')}</div>
    <div data-anchor="status_party">${img('014_icone_grupo.png')}</div>
  </section>
  <section class="narration-panel" data-anchor="narration_panel">
    ${img('OSE_SESSION_A502_frame_narracao_flexivel.png','narration-art')}
    <div class="narration-text"><strong>MESTRE</strong><p>${f.narrative}</p></div>
  </section>
  <button class="active-tts" data-anchor="tts_control" aria-label="Ouvir TTS">${img('OSE_SESSION_NEW_A701_icone_tts_ouvir.png')}<span>OUVIR<br>TTS</span></button>
  <section class="player-action-panel" data-anchor="player_action_panel">
    ${img('072_painel_pergaminho_pautado.png','player-action-art')}
    <div class="player-action-content"><h2>AÇÃO / PLAYER_ACTION</h2><label for="playerAction">O que você faz?</label><textarea id="playerAction" aria-label="PLAYER_ACTION" placeholder="Texto do jogador...">${keyboard?'Examino a porta sem abri-la.':''}</textarea></div>
  </section>
  <button class="send-button" data-anchor="send_button">ENVIAR</button>
  <button class="active-help-icon" aria-label="Ajuda do mestre">${img('OSE_SESSION_A501_icone_gm_help.png')}</button>
  <button class="active-gm" data-anchor="gm_help">GM_HELP</button>
  <small class="active-gm-note">não avança mundo</small>
  <section class="shortcut-row">
    ${quick('013_icone_mapa.png','MAPA','shortcut_map')}
    ${quick('012_icone_livros.png','FICHA','shortcut_sheet')}
    ${quick('011_icone_d20.png','DADOS','shortcut_dice')}
    ${quick('015_icone_mochila.png','BOLSA','shortcut_bag')}
  </section>
  <section class="history-block">
    <i class="history-divider"></i><h2 data-anchor="history_title">HISTÓRICO DA SESSÃO</h2>
    <div class="history-scroll"><p>A narração continua em rolagem vertical. Entradas anteriores permanecem acessíveis sem empurrar PLAYER_ACTION e GM_HELP para o mesmo canal. O concept demonstra conteúdo extenso sem transformar a tela em componente board.</p></div>
  </section>
  ${bottomNav()}
</main>${keyboard?keyboardMarkup():''}`}

function keyboardMarkup(){return `<div class="keyboard-sim">${['QWERTYUIOP','ASDFGHJKL','ZXCVBNM'].map(r=>`<div>${[...r].map(k=>`<span>${k}</span>`).join('')}</div>`).join('')}<em>espaço</em></div>`}
function conceptOverlay(id:FixtureId){if(!overlayEnabled())return'';const concept=id==='session-prestart'?'CONCEPT_SESSION_PRESTART_415x915.png':'CONCEPT_SESSION_ACTIVE_415x915.png';return `<img class="concept-overlay" src="${A}${concept}" alt="Concept overlay">`}

const id=fixture();
document.documentElement.dataset.clock=FIXED_CLOCK;
document.body.innerHTML=(id==='session-prestart'?prestart():active(id==='keyboard-open'))+conceptOverlay(id);
document.body.dataset.ready='true';
postBridge({version:BRIDGE_VERSION,type:'ViewState',payload:{fixture:id,fixtureRevision:FIXTURE_REVISION,clock:FIXED_CLOCK}});
