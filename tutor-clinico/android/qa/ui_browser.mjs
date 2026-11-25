import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
const require=createRequire(path.join(process.env.ECO_UI_DEPS,'package.json'));
const {chromium}=require('playwright-core');
const html=fs.readFileSync(new URL('../app/src/main/assets/index.html',import.meta.url),'utf8').replaceAll('{{ECO_APP_ICON}}','data:image/svg+xml,%3Csvg%20xmlns=%22http://www.w3.org/2000/svg%22%20viewBox=%220%200%2032%2032%22%3E%3Ccircle%20cx=%2216%22%20cy=%2216%22%20r=%2215%22%20fill=%22%2399dd55%22/%3E%3C/svg%3E');
const fixture={version:1,uiTheme:'dark',cases:[{id:'browser-a',nome:'Caso fictício A',sessoes:[]},{id:'browser-b',nome:'Caso fictício B',sessoes:[]}],appointments:[]};
const browser=await chromium.launch({executablePath:process.env.ECO_CHROME,headless:true,args:['--no-sandbox']});
const out=path.resolve('build/eco-ui');fs.mkdirSync(out,{recursive:true});
try{
 const page=await browser.newPage({viewport:{width:393,height:852},deviceScaleFactor:1,isMobile:true,hasTouch:true});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.setContent(html);
 await page.evaluate(f=>{let raw=JSON.stringify(f);window.NativeVault={loadState:()=>raw,saveState:x=>{raw=x;return 'OK'},getEngineInfo:()=>JSON.stringify({semanticAvailable:true,nliAvailable:true,rerankerAvailable:true,llmAvailable:true}),printReport:()=>{throw Error('Unexpected print')}};window.appUnlock();},fixture);
 const home=()=>page.locator('#bottom [data-id="home"]').click();
 const tile=id=>page.locator('.eco-home-grid [data-id="'+id+'"]');
 for(const width of [360,393,415]){
  await page.setViewportSize({width,height:852});
  for(const theme of ['dark','light']){
   if(await page.locator('html').getAttribute('data-theme')!==theme)await page.locator('#themeToggle').click();
   assert.equal(await page.locator('html').getAttribute('data-theme'),theme);
   const metrics=await page.locator('.eco-home-tile').evaluateAll(tiles=>tiles.map(t=>{const r=t.getBoundingClientRect();return {width:r.width,height:r.height,x:r.x,y:r.y,scrollWidth:t.scrollWidth,clientWidth:t.clientWidth,scrollHeight:t.scrollHeight,clientHeight:t.clientHeight}}));
   assert.equal(metrics.length,16);
   assert.ok(metrics.every(r=>Math.abs(r.width-r.height)<=2&&r.scrollWidth<=r.clientWidth+2&&r.scrollHeight<=r.clientHeight+2),'square dimensions or clipped tile');
   assert.ok(Math.abs(metrics[0].y-metrics[1].y)<=1&&metrics[2].y>metrics[0].y,'expected two columns');
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'horizontal overflow');
   await page.screenshot({path:path.join(out,'home-'+width+'-'+theme+'.png'),fullPage:true});
  }
 }
 const modules=await page.evaluate(()=>ecoHomeModules.map(x=>[x[0],x[4]]));
 for(const [id,route] of modules){
   await home();await tile(id).click();
   if(route==='case'||route==='manual'){
     await page.locator('.eco-home-pick [data-id="browser-b"]').click();
     assert.equal(await page.evaluate(()=>openedId),'browser-b');
     assert.equal(await page.evaluate(()=>screen),route==='manual'?'manualSession':'case');
     if(route==='case')assert.equal(await page.evaluate(()=>tab),id);
     if(['assistenteIA','sonhos','genograma','documentos','atendimento'].includes(id))await page.screenshot({path:path.join(out,id+'.png'),fullPage:true});
   }else assert.equal(await page.evaluate(()=>screen),route==='new'?'wizard':id);
 }
 await home();await tile('manual').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 await page.locator('#manualSessTexto').fill('Nota de teste sem áudio.');
 await page.locator('[data-act="saveManualSession"]').click();
 assert.ok(await page.evaluate(()=>state.cases.find(x=>x.id==='browser-b').sessoes.some(s=>s.texto==='Nota de teste sem áudio.')));
 assert.equal(await page.evaluate(()=>state.cases.find(x=>x.id==='browser-a').sessoes.length),0);
 // Dream form uses literal suggestions, preserves professional input, and does not auto-save.
 await home();await tile('sonhos').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 await page.locator('#dreamTexto').fill('Sonhei com minha mãe na cozinha. Não senti medo. Senti alegria. Vi uma porta.');
 await page.locator('#dreamPersonagens').fill('Figura já revisada');
 await page.locator('[data-act="ecoDreamExtractFields"]').click();
 assert.equal(await page.locator('#dreamPersonagens').inputValue(),'Figura já revisada');
 assert.match(await page.locator('#dreamLugares').inputValue(),/cozinha/);
 assert.equal(await page.locator('#dreamAfetos').inputValue(),'alegria');
 assert.match(await page.locator('#dreamSimbolos').inputValue(),/porta/);
 assert.equal(await page.evaluate(()=>state.cases.find(c=>c.id==='browser-b').sonhosList?.length||0),0);
 const samples=await page.evaluate(()=>{
  const c={id:'test-dream',sessoes:[{id:'s1',data:'2026-11-25',origem:'manual',texto:'Sonhei com uma casa. Senti medo.'}],sonhosList:[]};
  const d=ecoDreamCandidates(c)[0];c.sonhosList.push({...d,originKey:'old-key',originalQuote:d.texto});
  return {first:d,duplicates:ecoDreamCandidates(c).length,sizes:[1,3,10,100].map(count=>ecoCloudSize(count,[{count:1},{count:100}]))};
 });
 assert.match(samples.first.texto,/Sonhei/);assert.equal(samples.duplicates,0);
 assert.ok(samples.sizes.every((x,i,a)=>i===0||x>a[i-1]));
 await home();await tile('genograma').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 const graph=page.locator('.eco-genogram-svg');assert.equal(await graph.count(),1);
 assert.equal(await graph.getAttribute('viewBox'),'0 0 360 480');
 assert.ok(await graph.locator('g.gen-node title').count()>=1);
 await graph.screenshot({path:path.join(out,'genograma-legivel.png')});
 assert.deepEqual(errors,[]);
 console.log('PASS ECO real Chromium DOM: 16 destinations, 3 widths, 2 themes, square layout, manual record isolation');
}finally{await browser.close();}
