export const SHARED_NAV_ITEMS=Object.freeze([
  Object.freeze({id:'jornada',label:'JORNADA',href:'jornada.html'}),
  Object.freeze({id:'personagem',label:'PERSONAGEM',href:'ficha.html'}),
  Object.freeze({id:'mochila',label:'MOCHILA',href:'mochila.html'}),
  Object.freeze({id:'mapa',label:'MAPA',href:'scr014.html'}),
  Object.freeze({id:'diario',label:'DIÁRIO',href:'diario.html'}),
]);
export function installSharedBottomNav({root=document.body,active='jornada',zIndex=900}={}){
  root.querySelector?.('[data-braseiro-shared-nav]')?.remove();
  const nav=document.createElement('nav');nav.dataset.braseiroSharedNav='true';nav.setAttribute('aria-label','Navegação principal');
  Object.assign(nav.style,{position:'absolute',left:'0',bottom:'0',width:'415px',height:'86px',zIndex:String(zIndex),margin:'0',padding:'0'});
  const img=document.createElement('img');img.src='assets/shared_chrome/bottom_nav_reference_415x86.png';img.alt='';Object.assign(img.style,{position:'absolute',inset:'0',width:'415px',height:'86px',pointerEvents:'none',filter:active==='jornada'?'none':'grayscale(1) brightness(.82)'});nav.appendChild(img);
  SHARED_NAV_ITEMS.forEach((item,index)=>{const a=document.createElement('a');a.href=item.href;a.dataset.nav=item.id;a.setAttribute('aria-label',item.label);const current=item.id===active;if(current)a.setAttribute('aria-current','page');Object.assign(a.style,{position:'absolute',left:`${index*83}px`,top:'0',width:'83px',height:'86px',display:'block',WebkitTapHighlightColor:'transparent',background:current&&active!=='jornada'?'linear-gradient(180deg,rgba(190,145,70,0) 55%,rgba(190,145,70,.16) 100%)':'transparent',boxShadow:current&&active!=='jornada'?'inset 0 -3px 0 rgba(205,162,85,.95)':'none'});nav.appendChild(a)});
  root.appendChild(nav);return nav;
}
