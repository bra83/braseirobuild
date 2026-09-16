import fs from 'node:fs';
const read=p=>fs.readFileSync(p,'utf8');
const base='app/src/main/assets/www/';
const journey=read(base+'jornada.js');
const index=read(base+'index.html');
const legacy15=read(base+'scr015.js');
const legacy16=read(base+'scr016.js');
function ok(v,m){if(!v)throw new Error(m)}
ok(!journey.includes("location.href='scr015.html'"),'Jornada still routes player into Wave3 fixture runtime');
ok(!index.includes('href="scr015.html"'),'Home still exposes legacy encounter runtime');
ok(!index.includes('href="scr016.html"'),'Home still exposes legacy result runtime');
ok(index.includes('href="scr022.html"'),'Home must expose canonical combat runtime');
ok(legacy15.startsWith("if(globalThis.BraseiroNative){location.replace('jornada.html')"),'SCR015 lacks native ownership guard');
ok(legacy16.startsWith("if(globalThis.BraseiroNative){location.replace('jornada.html')"),'SCR016 lacks native ownership guard');
console.log('runtime route ownership regression: PASS');
