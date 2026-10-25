/* Braseiro OSE — physical polyhedral dice.
   Rendering/motion only: the official referee supplies all values beforehand.
   The OSE physics core provides rigid body gravity, angular velocity, contacts,
   bounces and targeted final face alignment; this file never rolls for rules. */
(function(root){
'use strict';
const core=root.Dice3DCore;
const stage=()=>document.getElementById('dice3dStage');
const light=[0.28,-0.42,0.86], camera=[0,-0.62,0.785];
const palettes={
  4:[154,104,58],6:[143,106,63],8:[105,89,117],
  10:[126,66,60],12:[82,108,92],20:[111,92,63],100:[112,66,60]
};
let active=null,sequence=0;
function stop(){
  if(!active)return;
  active.stopped=true;
  if(active.frame!=null)cancelAnimationFrame(active.frame);
  active.canvas.remove();
  active.stage.classList.remove('physics-active');
  active=null;
}
function color(rgb,lightness){
  const channel=x=>Math.max(0,Math.min(255,Math.round(x*lightness)));
  return 'rgb('+rgb.map(channel).join(',')+')';
}
function dot(a,b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
function projected(p,width,height,scale){
  return [width*.5+p[0]*scale, height*.73+(p[1]*.52-p[2]*.78)*scale];
}
function polygonArea(p){
  let a=0;
  for(let i=0;i<p.length;i++)a+=p[i][0]*p[(i+1)%p.length][1]-p[(i+1)%p.length][0]*p[i][1];
  return Math.abs(a)*.5;
}
function render(session){
  const {ctx,canvas,world}=session;
  const width=session.cssWidth,height=session.cssHeight;
  if(!width||!height)return;
  ctx.setTransform(session.dpr,0,0,session.dpr,0,0);
  ctx.clearRect(0,0,width,height);
  const grd=ctx.createRadialGradient(width*.5,height*.48,5,width*.5,height*.55,width*.68);
  grd.addColorStop(0,'rgba(100,68,43,.46)');
  grd.addColorStop(1,'rgba(28,17,13,.14)');
  ctx.fillStyle=grd;
  ctx.fillRect(0,0,width,height);
  const scale=Math.min(width/10.4,height/11.4);
  ctx.save();
  ctx.strokeStyle='rgba(181,133,91,.055)';
  ctx.lineWidth=1;
  for(let y=0;y<height;y+=17){ctx.beginPath();ctx.moveTo(0,y);ctx.lineTo(width,y+10);ctx.stroke();}
  ctx.restore();
  for(const body of world.bodies){
    const s=projected([body.p[0],body.p[1],0],width,height,scale);
    ctx.save();
    ctx.fillStyle='rgba(0,0,0,'+Math.max(.05,.30-body.p[2]*.033)+')';
    ctx.beginPath();
    ctx.ellipse(s[0],s[1]+2,body.size*scale*(1.3+Math.min(body.p[2],4)*.04),body.size*scale*.49,0,0,Math.PI*2);
    ctx.fill();
    ctx.restore();
  }
  const faces=[];
  for(const body of world.bodies){
    const mesh=body.mesh, verts=mesh.vertices.map(v=>core.V.add(body.p,core.V.mul(core.Q.rot(body.q,v),body.size)));
    const palette=palettes[body.themeSides]||palettes[body.sides]||palettes[20];
    for(let i=0;i<mesh.faces.length;i++){
      const normal=core.Q.rot(body.q,mesh.normals[i]);
      const exposure=dot(normal,camera);
      if(exposure<=.015)continue;
      const v=mesh.faces[i].map(idx=>projected(verts[idx],width,height,scale));
      if(polygonArea(v)<1.8)continue;
      const center=core.V.add(body.p,core.V.mul(core.Q.rot(body.q,mesh.centers[i]),body.size));
      const depth=dot(center,camera);
      const illumination=.48+Math.max(0,dot(normal,light))*.58;
      const raw=body.displayMap?.[i]??(i+1);
      faces.push({points:v,center:projected(center,width,height,scale),depth,color:color(palette,illumination),
        label:String(raw),exposure,area:polygonArea(v),body});
    }
  }
  faces.sort((a,b)=>a.depth-b.depth);
  for(const face of faces){
    ctx.beginPath();
    face.points.forEach((point,i)=>i?ctx.lineTo(point[0],point[1]):ctx.moveTo(point[0],point[1]));
    ctx.closePath();
    ctx.fillStyle=face.color;
    ctx.fill();
    ctx.lineWidth=1.35;
    ctx.strokeStyle='rgba(23,14,11,.8)';
    ctx.stroke();
    const fontSize=Math.max(8,Math.min(21,Math.sqrt(face.area)*.45));
    if(face.exposure>.24 && fontSize>=8.5){
      ctx.textAlign='center';
      ctx.textBaseline='middle';
      ctx.font='800 '+fontSize+'px Georgia, serif';
      ctx.lineWidth=Math.max(2,fontSize*.23);
      ctx.strokeStyle='rgba(25,16,12,.88)';
      ctx.strokeText(face.label,face.center[0],face.center[1]);
      ctx.fillStyle='#fff1cf';
      ctx.fillText(face.label,face.center[0],face.center[1]);
    }
  }
}
function resize(session){
  const bounds=session.stage.getBoundingClientRect();
  const width=Math.max(1,Math.round(bounds.width)),height=Math.max(1,Math.round(bounds.height));
  const dpr=Math.min(2,Math.max(1,root.devicePixelRatio||1));
  if(width===session.cssWidth&&height===session.cssHeight&&dpr===session.dpr)return;
  session.cssWidth=width;session.cssHeight=height;session.dpr=dpr;
  session.canvas.width=Math.round(width*dpr);
  session.canvas.height=Math.round(height*dpr);
}
function hashSeed(rolls,terms){
  let h=0x811c9dc5;
  const input=JSON.stringify([rolls,terms,++sequence]);
  for(let i=0;i<input.length;i++){h^=input.charCodeAt(i);h=Math.imul(h,16777619);}
  return h>>>0;
}
function start(cfg,dice,onDone){
  stop();
  if(!core||!stage()||!root.requestAnimationFrame||!root.cancelAnimationFrame)return false;
  if(!Array.isArray(dice)||!dice.length)return false;
  const terms=Array.isArray(cfg.terms)&&cfg.terms.length?cfg.terms.map(t=>({
    count:Number(t.count),sides:Number(t.sides),sign:t.sign===-1?-1:1
  })):[{count:dice.length,sides:Number(cfg.sides),sign:1}];
  const physicalCount=terms.reduce((sum,t)=>sum+t.count*(t.sides===100?2:1),0);
  // Large bursts fall back to the original FL animation without blocking a phone.
  if(physicalCount>18)return false;
  let canvas,container;
  try{
    container=stage();
    const raw=dice.map(d=>d.value);
    const world=new core.World({terms,targetRolls:raw,mode:'Normal'},hashSeed(raw,terms));
    canvas=document.createElement('canvas');
    canvas.className='ose-physics-canvas';
    canvas.setAttribute('aria-hidden','true');
    const ctx=canvas.getContext('2d',{alpha:true});
    if(!ctx)return false;
    const session={stage:container,canvas,ctx,world,frame:null,stopped:false,cssWidth:0,cssHeight:0,dpr:1,last:null,accumulator:0};
    container.appendChild(canvas);
    container.classList.add('physics-active');
    active=session;
    const tick=now=>{
      if(active!==session||session.stopped)return;
      resize(session);
      const dt=session.last===null?1/60:Math.min(.05,Math.max(1/240,(now-session.last)/1000));
      session.last=now;
      session.accumulator+=dt;
      let substeps=0;
      while(session.accumulator>=1/120&&substeps<7&&!world.done()){
        world.step(1/120,null);
        session.accumulator-=1/120;
        substeps++;
      }
      if(substeps===7)session.accumulator=Math.min(session.accumulator,1/120);
      render(session);
      if(world.done()){
        let valid=false;
        try{
          const result=world.result();
          valid=result.rolls.length===raw.length&&result.rolls.every((r,i)=>r===raw[i]);
        }catch(e){console.warn('[OSE physics] unable to read settled faces',e);}
        if(!valid){console.error('[OSE physics] presentation face mismatch: fallback to official CSS faces');stop();return;}
        if(typeof onDone==='function')onDone();
      }else session.frame=requestAnimationFrame(tick);
    };
    session.frame=requestAnimationFrame(tick);
    return true;
  }catch(error){
    console.error('[OSE physics] initialization failure',error);
    if(canvas&&canvas.parentNode)canvas.remove();
    if(container)container.classList.remove('physics-active');
    active=null;
    return false;
  }
}
root.OSEPhysicsDice=Object.freeze({start,stop});
})(typeof window!=='undefined'?window:globalThis);
