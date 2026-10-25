'use strict';
/* No Android emulator required; exercise physics math and actual Canvas renderer.
   This test runs as part of the OSE image/nav regression test. */
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const Core=require('./dice3d_core.js');
for(const sides of [4,6,8,10,12,20,100]){
  for(let value=1;value<=sides;value++){
    const receipt={terms:[{count:1,sides,sign:1}],targetRolls:[value]};
    const actual=Core.simulate(receipt,0x5150+sides*101+value,2000);
    assert.ok(actual, 'simulation completed d'+sides+'='+value);
    assert.deepEqual(actual.rolls,[value],'authoritative face unchanged d'+sides+'='+value);
  }
}
for(let v=1;v<=6;v++){
  const expected=[v,21-v,100-v];
  const cfg={terms:[{count:1,sides:6,sign:1},{count:1,sides:20,sign:-1},{count:1,sides:100,sign:1}],targetRolls:expected};
  assert.deepEqual(Core.simulate(cfg,12345+v,2000).rolls,expected);
}
console.log('PASS 166 canonical face simulations (d4-d100 and composed rolls)');

const stage={
  activeClasses:new Set(),canvas:null,
  classList:{add(v){stage.activeClasses.add(v)},remove(v){stage.activeClasses.delete(v)}},
  getBoundingClientRect(){return {width:380,height:310}},
  appendChild(c){stage.canvas=c;c.parentNode=stage}
};
let drawing=0,textThisFrame=[];
const ctx={
  setTransform(){},clearRect(){textThisFrame=[]},fillRect(){},beginPath(){},moveTo(){},
  lineTo(){},closePath(){},fill(){drawing++},stroke(){},save(){},restore(){},
  ellipse(){},arc(){},clip(){},strokeText(){},fillText(label){drawing++;textThisFrame.push(String(label))},
  createRadialGradient(){return {addColorStop(){}}}
};
const document={
  getElementById(id){return id==='dice3dStage'?stage:null},
  createElement(name){
    assert.equal(name,'canvas');
    return {width:0,height:0,parentNode:null,
      setAttribute(){},getContext(){return ctx},
      remove(){if(this.parentNode?.canvas===this)this.parentNode.canvas=null;this.parentNode=null;}
    };
  }
};
let now=0,queue=[],nextFrame=0;
const raf=fn=>{queue.push({id:++nextFrame,fn});return nextFrame};
const caf=id=>{queue=queue.filter(x=>x.id!==id)};
const window={Dice3DCore:Core,requestAnimationFrame:raf,cancelAnimationFrame:caf,devicePixelRatio:2};
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'dice3d_physics_view.js'),'utf8'),
  {window,document,console,requestAnimationFrame:raf,cancelAnimationFrame:caf});
for(const roll of [
  {terms:[{count:1,sides:6}],rolls:[5]},
  {terms:[{count:2,sides:20}],rolls:[7,19]},
  {terms:[{count:1,sides:100}],rolls:[82]},
  {terms:[{count:2,sides:6},{count:1,sides:4},{count:1,sides:100}],rolls:[1,6,3,100]}
]){
  let completed=0;
  assert.equal(window.OSEPhysicsDice.start(roll,roll.rolls.map(value=>({value})),()=>completed++),true);
  let frames=0;
  while(queue.length&&frames<1600){
    const job=queue.shift();now+=1000/60;job.fn(now);frames++;
  }
  assert.equal(completed,1,'onDone once '+JSON.stringify(roll));
  assert.ok(drawing>0,'actual visible face draw calls');
  assert.equal(textThisFrame.length,roll.terms.reduce((n,t)=>n+t.count*(t.sides===100?2:1),0),'one label per canonical die after settling');
  assert.ok(stage.canvas,'canvas attached');
  assert.ok(stage.activeClasses.has('physics-active'));
  window.OSEPhysicsDice.stop();
  assert.ok(!stage.canvas,'canvas removed and animations stopped');
  assert.equal(stage.activeClasses.has('physics-active'),false);
}
const heavy={terms:[{count:19,sides:6}],rolls:Array(19).fill(1)};
assert.equal(window.OSEPhysicsDice.start(heavy,heavy.rolls.map(value=>({value})),()=>{}),false);
assert.equal(stage.activeClasses.has('physics-active'),false);

// 1. Abrir/fechar repetidamente
for(let cycle=0;cycle<10;cycle++){
  const r={terms:[{count:2,sides:6}],rolls:[3,4]};
  assert.equal(window.OSEPhysicsDice.start(r,r.rolls.map(value=>({value})),()=>{}),true);
  assert.ok(stage.canvas,'canvas attached on open cycle '+cycle);
  window.OSEPhysicsDice.stop();
  assert.ok(!stage.canvas,'canvas detached on close cycle '+cycle);
  assert.equal(stage.activeClasses.has('physics-active'),false);
}

// 2. Rolagem após rolagem (sucessão)
for(let seq=0;seq<5;seq++){
  const r={terms:[{count:1,sides:20}],rolls:[seq+1]};
  let done=0;
  assert.equal(window.OSEPhysicsDice.start(r,r.rolls.map(value=>({value})),()=>done++),true);
  let frames=0;
  while(queue.length&&frames<1600){
    const job=queue.shift();now+=1000/60;job.fn(now);frames++;
  }
  assert.equal(done,1,'sequence completed '+seq);
  window.OSEPhysicsDice.stop();
}

// 3. Cancelamento durante a animação (mid-flight stop)
{
  const r={terms:[{count:3,sides:6}],rolls:[1,2,3]};
  let done=0;
  assert.equal(window.OSEPhysicsDice.start(r,r.rolls.map(value=>({value})),()=>done++),true);
  for(let f=0;f<5&&queue.length;f++){
    const job=queue.shift();now+=1000/60;job.fn(now);
  }
  // Mid-flight stop
  window.OSEPhysicsDice.stop();
  assert.ok(!stage.canvas,'canvas removed after mid-flight stop');
  assert.equal(stage.activeClasses.has('physics-active'),false);
  assert.equal(done,0,'onDone not called on abort');
}

// 4. Resolução de tela pequena (280x200) e grande (415x915)
for(const [w,h] of [[280,200],[360,800],[415,915]]){
  stage.getBoundingClientRect=()=>({width:w,height:h});
  const r={terms:[{count:1,sides:8}],rolls:[7]};
  let done=0;
  assert.equal(window.OSEPhysicsDice.start(r,r.rolls.map(value=>({value})),()=>done++),true);
  let frames=0;
  while(queue.length&&frames<1600){
    const job=queue.shift();now+=1000/60;job.fn(now);frames++;
  }
  assert.equal(done,1,'completed for viewport '+w+'x'+h);
  window.OSEPhysicsDice.stop();
}
// restore default size
stage.getBoundingClientRect=()=>({width:380,height:310});

// 5. Falha de Canvas / context null
{
  const brokenDoc={
    getElementById(id){return id==='dice3dStage'?stage:null},
    createElement(){return {setAttribute(){},getContext(){return null},parentNode:null,remove(){}}}
  };
  const brokenWin={Dice3DCore:Core,requestAnimationFrame:raf,cancelAnimationFrame:caf,devicePixelRatio:1};
  vm.runInNewContext(fs.readFileSync(path.join(__dirname,'dice3d_physics_view.js'),'utf8'),
    {window:brokenWin,document:brokenDoc,console,requestAnimationFrame:raf,cancelAnimationFrame:caf});
  const r={terms:[{count:1,sides:6}],rolls:[4]};
  assert.equal(brokenWin.OSEPhysicsDice.start(r,r.rolls.map(value=>({value})),()=>{}),false);
  assert.equal(stage.activeClasses.has('physics-active'),false);
}

console.log('PASS physical Canvas animations, lifecycle, stress and FL CSS fallback');
