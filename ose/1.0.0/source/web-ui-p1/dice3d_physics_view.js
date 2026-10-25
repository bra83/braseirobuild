/* Braseiro OSE — physical polyhedral dice.
   Rendering/motion only: the official referee supplies all values beforehand.
   The OSE physics core provides rigid body gravity, angular velocity, contacts,
   bounces and targeted final face alignment; this file never rolls for rules. */
(function(root){
'use strict';
const core=root.Dice3DCore;
const stage=()=>document.getElementById('dice3dStage');
const light=[0.24,-0.38,0.89], camera=[0,-0.56,0.83];
const palettes={
  4:[186,124,70],   // Antique hammered bronze
  6:[198,178,148],  // Aged carved ivory bone
  8:[134,102,152],  // Runic dark amethyst / iron
  10:[170,68,58],   // Dragon-scale crimson
  12:[82,132,102],  // Weathered verdigris moss
  20:[192,154,74],  // Ancient lustrous gold
  100:[156,58,52]   // Blood lacquer & obsidian
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
  return [width*.5+p[0]*scale, height*.50+(p[1]*.48-p[2]*.72)*scale];
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

  // Forbidden Lands carved wooden tray backdrop with ambient vignette
  const grd=ctx.createRadialGradient(width*.5,height*.50,width*.08,width*.5,height*.52,width*.65);
  grd.addColorStop(0,'rgba(88,56,36,.58)');
  grd.addColorStop(.55,'rgba(42,26,17,.42)');
  grd.addColorStop(1,'rgba(14,9,7,.25)');
  ctx.fillStyle=grd;
  ctx.fillRect(0,0,width,height);

  // Subtle wood grain lines
  ctx.save();
  ctx.strokeStyle='rgba(196,145,98,.065)';
  ctx.lineWidth=1;
  for(let y=0;y<height;y+=14){ctx.beginPath();ctx.moveTo(0,y);ctx.lineTo(width,y+8);ctx.stroke();}
  ctx.restore();

  // Dynamic responsive scale: prominent, legible, filling available tray space
  const total=world.bodies.length;
  const scale=total<=2 ? Math.min(width/5.4,height/5.8) :
              total<=4 ? Math.min(width/6.2,height/6.6) :
              total<=8 ? Math.min(width/7.2,height/7.8) :
              Math.min(width/8.8,height/9.4);

  // Realistic contact and ambient drop shadows under each die
  for(const body of world.bodies){
    const s=projected([body.p[0],body.p[1],0],width,height,scale);
    const z=Math.max(0,body.p[2]);
    const shadowAlpha=Math.max(.08,.48-z*.07);
    const rad=body.size*scale;
    ctx.save();
    // Ambient spread shadow
    ctx.fillStyle='rgba(10,6,4,'+shadowAlpha+')';
    ctx.beginPath();
    ctx.ellipse(s[0],s[1]+4,rad*(1.22+z*.05),rad*.52,0,0,Math.PI*2);
    ctx.fill();
    // Sharp dark contact core when resting on or near tray floor
    if(z<1.0){
      ctx.fillStyle='rgba(4,2,1,'+((1.0-z)*.55)+')';
      ctx.beginPath();
      ctx.ellipse(s[0],s[1]+2,rad*.92,rad*.38,0,0,Math.PI*2);
      ctx.fill();
    }
    ctx.restore();
  }

  // Project and sort polyhedral faces (Painter's algorithm with depth)
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
      const diff=Math.max(0,dot(normal,light));
      // Specular sheen for resin / metal polyhedrals
      const half=core.V.norm(core.V.add(light,camera));
      const spec=Math.pow(Math.max(0,dot(normal,half)),16)*.38;
      const illumination=Math.min(1.45,.40+diff*.60+spec);
      const raw=body.displayMap?.[i]??(i+1);
      faces.push({points:v,center:projected(center,width,height,scale),depth,color:color(palette,illumination),
        label:String(raw),exposure,area:polygonArea(v),body,faceIndex:i,spec});
    }
  }
  faces.sort((a,b)=>a.depth-b.depth);

  // Face labels must fit their actual polygons: area-based fonts alone caused
  // overlapping d20 numbers and percentile markings outside adjacent faces.
  // Once settled, display only the canonical face on each physical die.
  const labelled=new Set(),moving=new Map();
  for(const face of faces){
    if(face.body.done){
      if(face.faceIndex===face.body.faceIndex)labelled.add(face);
    }else if(face.exposure>.40){
      const list=moving.get(face.body)||[];list.push(face);moving.set(face.body,list);
    }
  }
  for(const candidates of moving.values()){
    candidates.sort((a,b)=>b.exposure*b.area-a.exposure*a.area);
    for(const face of candidates.slice(0,2))labelled.add(face);
  }
  function inscriptionRadius(face){
    const [cx,cy]=face.center;
    let radius=Infinity;
    for(let i=0;i<face.points.length;i++){
      const a=face.points[i],b=face.points[(i+1)%face.points.length];
      const dx=b[0]-a[0],dy=b[1]-a[1];
      radius=Math.min(radius,Math.abs(dx*(cy-a[1])-dy*(cx-a[0]))/Math.max(.0001,Math.hypot(dx,dy)));
    }
    return radius;
  }
  // Render each visible 3D facet
  for(const face of faces){
    ctx.beginPath();
    face.points.forEach((point,i)=>i?ctx.lineTo(point[0],point[1]):ctx.moveTo(point[0],point[1]));
    ctx.closePath();
    ctx.fillStyle=face.color;
    ctx.fill();

    // Distinct chamfered edge stroke
    ctx.lineWidth=Math.max(1.4,Math.min(2.8,scale*.036));
    ctx.strokeStyle='rgba(20,12,8,.92)';
    ctx.stroke();

    // Engrave only the face(s) chosen above. Clip ink to the face boundary,
    // and adapt font size to its shortest distance from the center to an edge.
    if(labelled.has(face)){
      const radius=inscriptionRadius(face);
      const fontSize=Math.min(29,Math.sqrt(face.area)*.47,radius*1.34,
        (radius*1.65)/(face.label.length*.63));
      if(fontSize>=8.2){
        ctx.save();
        ctx.beginPath();
        face.points.forEach((p,i)=>i?ctx.lineTo(p[0],p[1]):ctx.moveTo(p[0],p[1]));
        ctx.closePath();
        ctx.clip();
        ctx.textAlign='center';
        ctx.textBaseline='middle';
        ctx.font='800 '+fontSize+'px Georgia, "Times New Roman", serif';
        ctx.lineWidth=Math.max(1.5,fontSize*.20);
        ctx.strokeStyle='rgba(16,8,4,.95)';
        ctx.strokeText(face.label,face.center[0],face.center[1],radius*1.75);
        ctx.fillStyle='#fff6de';
        ctx.fillText(face.label,face.center[0],face.center[1],radius*1.75);
        if(face.label==='6'||face.label==='9'){
          const dotY=face.center[1]+fontSize*.36;
          const dotR=Math.max(.9,fontSize*.065);
          ctx.beginPath();
          if(typeof ctx.arc==='function')ctx.arc(face.center[0],dotY,dotR,0,Math.PI*2);
          else ctx.ellipse(face.center[0],dotY,dotR,dotR,0,0,Math.PI*2);
          ctx.fill();
        }
        ctx.restore();
      }
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
