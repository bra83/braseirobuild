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
let drawing=0;
const ctx={
  setTransform(){},clearRect(){},fillRect(){},beginPath(){},moveTo(){},
  lineTo(){},closePath(){},fill(){drawing++},stroke(){},save(){},restore(){},
  ellipse(){},strokeText(){},fillText(){drawing++},
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
  assert.ok(stage.canvas,'canvas attached');
  assert.ok(stage.activeClasses.has('physics-active'));
  window.OSEPhysicsDice.stop();
  assert.ok(!stage.canvas,'canvas removed and animations stopped');
  assert.equal(stage.activeClasses.has('physics-active'),false);
}
const heavy={terms:[{count:19,sides:6}],rolls:Array(19).fill(1)};
assert.equal(window.OSEPhysicsDice.start(heavy,heavy.rolls.map(value=>({value})),()=>{}),false);
assert.equal(stage.activeClasses.has('physics-active'),false);
console.log('PASS physical Canvas animations, lifecycle and FL CSS fallback');
