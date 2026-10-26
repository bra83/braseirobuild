(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;if(root)root.Dice3DCore=api;if(typeof globalThis!=='undefined')globalThis.Dice3DCore=api;})(typeof self!=='undefined'?self:(typeof globalThis!=='undefined'?globalThis:this),function(){'use strict';
const V={add:(a,b)=>[a[0]+b[0],a[1]+b[1],a[2]+b[2]],sub:(a,b)=>[a[0]-b[0],a[1]-b[1],a[2]-b[2]],mul:(a,s)=>[a[0]*s,a[1]*s,a[2]*s],dot:(a,b)=>a[0]*b[0]+a[1]*b[1]+a[2]*b[2],cross:(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],len:a=>Math.hypot(a[0],a[1],a[2]),norm:a=>{let l=Math.hypot(a[0],a[1],a[2])||1;return[a[0]/l,a[1]/l,a[2]/l]}};
const Q={norm:q=>{let l=Math.hypot(...q)||1;return q.map(x=>x/l)},mul:(a,b)=>[a[0]*b[0]-a[1]*b[1]-a[2]*b[2]-a[3]*b[3],a[0]*b[1]+a[1]*b[0]+a[2]*b[3]-a[3]*b[2],a[0]*b[2]-a[1]*b[3]+a[2]*b[0]+a[3]*b[1],a[0]*b[3]+a[1]*b[2]-a[2]*b[1]+a[3]*b[0]],rot:(q,v)=>{let u=[q[1],q[2],q[3]],uv=V.cross(u,v),uuv=V.cross(u,uv);return V.add(v,V.add(V.mul(uv,2*q[0]),V.mul(uuv,2)));},axis:(axis,ang)=>{let n=V.norm(axis),s=Math.sin(ang/2);return[Math.cos(ang/2),n[0]*s,n[1]*s,n[2]*s]},fromTo:(a,b)=>{a=V.norm(a);b=V.norm(b);let d=V.dot(a,b);if(d>0.999999)return[1,0,0,0];if(d<-.999999){let ax=Math.abs(a[0])<.8?V.cross(a,[1,0,0]):V.cross(a,[0,1,0]);return Q.axis(ax,Math.PI)}let c=V.cross(a,b);return Q.norm([1+d,c[0],c[1],c[2]])}};
function faceNormal(verts,face){let a=verts[face[0]],b=verts[face[1]],c=verts[face[2]],n=V.norm(V.cross(V.sub(b,a),V.sub(c,a))),ctr=[0,0,0];face.forEach(i=>ctr=V.add(ctr,verts[i]));if(V.dot(n,ctr)<0)n=V.mul(n,-1);return n}
function normalizeMesh(v,f){let r=Math.max(...v.map(V.len));v=v.map(p=>V.mul(p,1/r));let normals=f.map(x=>faceNormal(v,x)),centers=f.map(x=>{let c=[0,0,0];x.forEach(i=>c=V.add(c,v[i]));return V.mul(c,1/x.length)});return{vertices:v,faces:f,normals,centers,radius:1}}
function mesh4(){return normalizeMesh([[1,1,1],[-1,-1,1],[-1,1,-1],[1,-1,-1]],[[0,2,1],[0,1,3],[0,3,2],[1,2,3]])}
function mesh6(){let v=[];for(let z of[-1,1])for(let y of[-1,1])for(let x of[-1,1])v.push([x,y,z]);return normalizeMesh(v,[[0,1,3,2],[4,6,7,5],[0,4,5,1],[2,3,7,6],[0,2,6,4],[1,5,7,3]])}
function mesh8(){let v=[[1,0,0],[-1,0,0],[0,1,0],[0,-1,0],[0,0,1],[0,0,-1]];return normalizeMesh(v,[[4,0,2],[4,2,1],[4,1,3],[4,3,0],[5,2,0],[5,1,2],[5,3,1],[5,0,3]])}
function mesh10(){
    // Trapezoedro pentagonal: dez faces em formato de pipa, como um d10 real.
    // Duas coroas defasadas evitam o antigo aspecto de bipirâmide triangular.
    const h=1.30,r=1.0;
    // A genuine pentagonal trapezohedron has ten COPLANAR kite faces.
    // z=.34 produced nonplanar quads and phantom broken d10/d100 facets.
    const c=Math.cos(Math.PI/5),z=h*(1-c)/(1+c),v=[[0,0,h],[0,0,-h]];
    for(let i=0;i<5;i++){let a=2*Math.PI*i/5-Math.PI/2;v.push([r*Math.cos(a),r*Math.sin(a),z])}
    for(let i=0;i<5;i++){let a=2*Math.PI*i/5-Math.PI/2+Math.PI/5;v.push([r*Math.cos(a),r*Math.sin(a),-z])}
    const f=[];
    for(let i=0;i<5;i++){let ui=2+i,un=2+((i+1)%5),li=7+i,lp=7+((i+4)%5);f.push([0,ui,li,un]);f.push([1,li,ui,lp])}
    return normalizeMesh(v,f)
  }
function icosa(){let p=(1+Math.sqrt(5))/2,v=[[-1,p,0],[1,p,0],[-1,-p,0],[1,-p,0],[0,-1,p],[0,1,p],[0,-1,-p],[0,1,-p],[p,0,-1],[p,0,1],[-p,0,-1],[-p,0,1]],f=[[0,11,5],[0,5,1],[0,1,7],[0,7,10],[0,10,11],[1,5,9],[5,11,4],[11,10,2],[10,7,6],[7,1,8],[3,9,4],[3,4,2],[3,2,6],[3,6,8],[3,8,9],[4,9,5],[2,4,11],[6,2,10],[8,6,7],[9,8,1]];return normalizeMesh(v,f)}
function mesh12(){let ico=icosa(),dv=ico.centers.map(V.norm),faces=[];for(let vi=0;vi<ico.vertices.length;vi++){let adj=[];ico.faces.forEach((f,fi)=>{if(f.includes(vi))adj.push(fi)});let axis=V.norm(ico.vertices[vi]),tmp=Math.abs(axis[2])<.9?V.norm(V.cross(axis,[0,0,1])):V.norm(V.cross(axis,[0,1,0])),tmp2=V.cross(axis,tmp);adj.sort((a,b)=>{let ca=dv[a],cb=dv[b],aa=Math.atan2(V.dot(ca,tmp2),V.dot(ca,tmp)),bb=Math.atan2(V.dot(cb,tmp2),V.dot(cb,tmp));return aa-bb});faces.push(adj)}return normalizeMesh(dv,faces)}
const meshes={4:mesh4(),6:mesh6(),8:mesh8(),10:mesh10(),12:mesh12(),20:icosa()};
function rng(seed){let x=(seed|0)||0x12345678;return()=>{x^=x<<13;x^=x>>>17;x^=x<<5;return(x>>>0)/4294967296}}
function randomQuat(r){let u1=r(),u2=r(),u3=r();return Q.norm([Math.sqrt(1-u1)*Math.sin(2*Math.PI*u2),Math.sqrt(1-u1)*Math.cos(2*Math.PI*u2),Math.sqrt(u1)*Math.sin(2*Math.PI*u3),Math.sqrt(u1)*Math.cos(2*Math.PI*u3)])}
class Body{constructor(sides,id,r,opts={}){this.sides=sides;this.id=id;this.total=Math.max(1,opts.total||1);this.mesh=meshes[sides];this.size=opts.size||.78;this.mass=1+this.size*.35;this.invMass=1/this.mass;this.inertia=.42*this.mass*this.size*this.size;this.invI=1/this.inertia;let total=this.total,cols=total<=4?total:Math.ceil(Math.sqrt(total)),row=Math.floor(id/cols),col=id%cols,xStep=total<=4?1.25:Math.min(1.55,7.2/Math.max(1,cols-1)),x=(col-(cols-1)/2)*xStep,y=(total<=4?-3.35:-3.9)+row*(total>12?.82:.72);this.p=[x+(r()-.5)*.34,y+(r()-.5)*.28,5.2+r()*2.4+row*.08];this.v=[(r()-.5)*4.2,4.3+r()*2.6,(r()-.25)*1.1];this.q=randomQuat(r);this.w=[(r()-.5)*18,(r()-.5)*18,(r()-.5)*18];this.settle=0;this.done=false;this.valueMap=opts.valueMap||Array.from({length:sides},(_,i)=>i+1);this.displayMap=opts.displayMap||this.valueMap.map(String);this.targetFaceValue=opts.targetFaceValue;this.tag=opts.tag||'';this.themeSides=opts.themeSides||sides;this.termIndex=Number.isInteger(opts.termIndex)?opts.termIndex:-1;this.logicalIndex=Number.isInteger(opts.logicalIndex)?opts.logicalIndex:-1;this.lastImpulse=0}}
function worldVerts(b){return b.mesh.vertices.map(v=>V.add(b.p,V.mul(Q.rot(b.q,v),b.size)))}
function speed2(v){return V.dot(v,v)}
function wake(b){if(!b.done)return;b.done=false;b.settle=0;b.faceIndex=undefined;b.faceValue=undefined}
function stepBody(b,dt,event){if(b.done)return;b.v[2]-=9.81*dt;b.p=V.add(b.p,V.mul(b.v,dt));let wl=V.len(b.w);if(wl>1e-7)b.q=Q.norm(Q.mul(Q.axis(b.w,wl*dt),b.q));let vv=worldVerts(b),min=vv.reduce((m,p)=>p[2]<m[2]?p:m,vv[0]),floorContact=false;if(min[2]<0){floorContact=true;b.p[2]-=min[2];let r=V.sub(min,b.p),contact=V.add(b.v,V.cross(b.w,r));if(contact[2]<0){let j=-(1+.34)*contact[2]/(b.invMass+Math.max(.02,b.invI*(r[0]*r[0]+r[1]*r[1])));b.v[2]+=j*b.invMass;b.w=V.add(b.w,V.mul(V.cross(r,[0,0,j]),b.invI));b.v[0]*=.865;b.v[1]*=.865;b.w[0]*=.935;b.w[1]*=.935;b.w[2]*=.955;b.lastImpulse=j;if(j>.65&&event)event('impact',Math.min(1,j/8),b)}}let rad=b.size;for(let k=0;k<2;k++){let val=b.p[k],wide=b.total>12,lim=k===0?(wide?5.2:4.15):(wide?5.8:4.85);if(val-rad<-lim){b.p[k]=-lim+rad;b.v[k]=Math.abs(b.v[k])*.46;b.w[(k+1)%3]+=b.v[k]*.55}else if(val+rad>lim){b.p[k]=lim-rad;b.v[k]=-Math.abs(b.v[k])*.46;b.w[(k+1)%3]-=b.v[k]*.55}}let linearDamp=floorContact?.992:.9972,angularDamp=floorContact?.988:.9945;b.v=V.mul(b.v,Math.pow(linearDamp,dt*120));b.w=V.mul(b.w,Math.pow(angularDamp,dt*120));if(b.p[2]<1.55&&speed2(b.v)<.022&&speed2(b.w)<.065)b.settle+=dt;else b.settle=0;if(b.settle>.44)settle(b)}
function collide(a,b,event){if(a.done&&b.done)return;let d=V.sub(b.p,a.p),dist=V.len(d),rr=(a.size+b.size)*.88;if(dist>=rr||dist<1e-6)return;let n=V.mul(d,1/dist),over=rr-dist,rv=V.dot(V.sub(b.v,a.v),n);if((a.done||b.done)&&(Math.abs(rv)>.035||over>.035)){wake(a);wake(b)}if(a.done&&b.done)return;a.p=V.add(a.p,V.mul(n,-over*.5));b.p=V.add(b.p,V.mul(n,over*.5));if(rv<0){let j=-(1+.42)*rv/(a.invMass+b.invMass);a.v=V.add(a.v,V.mul(n,-j*a.invMass));b.v=V.add(b.v,V.mul(n,j*b.invMass));let tangent=V.cross(n,[0,0,1]);a.w=V.add(a.w,V.mul(tangent,-j*.18*a.invI));b.w=V.add(b.w,V.mul(tangent,j*.18*b.invI));if(j>.8&&event)event('impact',Math.min(1,j/7),a)}}
function topIndex(b){let best=0,z=-Infinity;b.mesh.normals.forEach((n,i)=>{let wz=Q.rot(b.q,n)[2];if(wz>z){z=wz;best=i}});return best}
function settle(b){let i;if(b.targetFaceValue!==undefined&&b.valueMap){let idx=b.valueMap.indexOf(b.targetFaceValue);i=(idx>=0)?idx:topIndex(b)}else{i=topIndex(b)}let wn=Q.rot(b.q,b.mesh.normals[i]),rot=Q.fromTo(wn,[0,0,1]);b.q=Q.norm(Q.mul(rot,b.q));let vv=worldVerts(b),minz=Math.min(...vv.map(v=>v[2]));b.p[2]+=.025-minz;b.v=[0,0,0];b.w=[0,0,0];b.done=true;b.settle=99;b.faceIndex=i;b.faceValue=b.valueMap[i]}
function separateSettled(bodies){for(let pass=0;pass<40;pass++){let moved=false;for(let i=0;i<bodies.length;i++)for(let j=i+1;j<bodies.length;j++){let a=bodies[i],b=bodies[j],dx=b.p[0]-a.p[0],dy=b.p[1]-a.p[1],dist=Math.hypot(dx,dy),rr=(a.size+b.size)*.90;if(dist>=rr)continue;if(dist<1e-6){let ang=((i+1)*2.399963229728653+(j+1)*.73)%(Math.PI*2);dx=Math.cos(ang);dy=Math.sin(ang);dist=1}else{dx/=dist;dy/=dist}let push=(rr-dist)*.58;a.p[0]-=dx*push;a.p[1]-=dy*push;b.p[0]+=dx*push;b.p[1]+=dy*push;for(let x of[a,b]){let rx=x.size,wide=x.total>12,limX=(wide?5.2:4.15)-rx,limY=(wide?5.8:4.85)-rx;x.p[0]=Math.max(-limX,Math.min(limX,x.p[0]));x.p[1]=Math.max(-limY,Math.min(limY,x.p[1]))}moved=true}if(!moved)break}}
class World{
  constructor(cfg,seed){this.cfg=cfg||{};this.seed=(seed>>>0)||(((Date.now()^((Math.random()*0xffffffff)|0))|0)>>>0);this.r=rng(this.seed);this.bodies=[];this.time=0;this.impacts=0;this.create()}
  normalizedTerms(){if(Array.isArray(this.cfg.terms)&&this.cfg.terms.length)return this.cfg.terms.map(t=>({count:Math.max(1,t.count|0),sides:t.sides|0,sign:(t.sign|0)<0?-1:1}));let c=Math.max(1,this.cfg.count|0),s=this.cfg.sides|0;return[{count:c,sides:s,sign:1}]}
  physicalCount(){if(this.cfg.mode!=='Normal'&&this.cfg.count===1&&this.cfg.sides===20&&!(Array.isArray(this.cfg.terms)&&this.cfg.terms.length>1))return 2;let n=0;for(const t of this.normalizedTerms())n+=t.count*(t.sides===100?2:1);return n}
  addStandard(sides,total,termIndex,logicalIndex,themeSides,tag,targetFaceValue){let n=this.bodies.length,size=total===1?1.05:(total<=4?.86:(total>12?.68:.72));this.bodies.push(new Body(sides,n,this.r,{total,size,tag:tag||'',themeSides,termIndex,logicalIndex,targetFaceValue}))}
  create(){
    const terms=this.normalizedTerms(),total=this.physicalCount(),mode=this.cfg.mode||'Normal';
    const targetRolls=Array.isArray(this.cfg.targetRolls)?this.cfg.targetRolls:(Array.isArray(this.cfg.rolls)?this.cfg.rolls:null);
    let targetIdx=0;
    if(terms.length===1&&terms[0].count===1&&terms[0].sides===20&&mode!=='Normal'){
      let t0=targetRolls?targetRolls[0]:undefined;
      let t1=targetRolls?targetRolls[1]:undefined;
      this.addStandard(20,total,0,0,20,'A',t0);this.addStandard(20,total,0,1,20,'B',t1);return;
    }
    for(let ti=0;ti<terms.length;ti++){
      const t=terms[ti];
      for(let li=0;li<t.count;li++){
        let targetVal=targetRolls?targetRolls[targetIdx++]:undefined;
        if(t.sides===100){
          const pctSize=total>12?.68:.82;
          let targetTens,targetOnes;
          if(targetVal!==undefined){
            let tv=Number(targetVal)||100;
            if(tv===100){targetTens=0;targetOnes=0;}
            else{targetTens=Math.floor(tv/10);targetOnes=tv%10;}
          }
          this.bodies.push(new Body(10,this.bodies.length,this.r,{
            total,size:pctSize,valueMap:[0,1,2,3,4,5,6,7,8,9],
            displayMap:['00','10','20','30','40','50','60','70','80','90'],
            tag:'%',themeSides:100,termIndex:ti,logicalIndex:li,
            targetFaceValue:targetTens
          }));
          this.bodies.push(new Body(10,this.bodies.length,this.r,{
            total,size:pctSize,valueMap:[0,1,2,3,4,5,6,7,8,9],
            displayMap:['0','1','2','3','4','5','6','7','8','9'],
            tag:'1',themeSides:100,termIndex:ti,logicalIndex:li,
            targetFaceValue:targetOnes
          }));
        }else{
          this.addStandard(t.sides,total,ti,li,t.sides,t.count>1?String(li+1):'',targetVal);
        }
      }
    }
  }
  step(dt,event){this.time+=dt;for(let b of this.bodies)stepBody(b,dt,event);for(let i=0;i<this.bodies.length;i++)for(let j=i+1;j<this.bodies.length;j++)collide(this.bodies[i],this.bodies[j],event);if(this.time>14){for(let b of this.bodies)if(!b.done)settle(b);separateSettled(this.bodies)}else if(this.bodies.every(b=>b.done))separateSettled(this.bodies)}
  done(){return this.bodies.every(b=>b.done)}
  result(){
    if(!this.done())throw new Error('not settled');
    const physical=this.bodies.map(b=>b.faceValue),terms=this.normalizedTerms(),mode=this.cfg.mode||'Normal',rolls=[];
    if(terms.length===1&&terms[0].count===1&&terms[0].sides===20&&mode!=='Normal')rolls.push(physical[0],physical[1]);
    else{
      let at=0;
      for(const t of terms)for(let i=0;i<t.count;i++){
        if(t.sides===100){let value=physical[at++]*10+physical[at++];rolls.push(value===0?100:value)}
        else rolls.push(physical[at++]);
      }
    }
    return{rolls,physicalFaces:physical,time:this.time,seed:this.seed,terms};
  }
}
function simulate(cfg,seed,maxSteps=2000){let w=new World(cfg,seed);for(let i=0;i<maxSteps&&!w.done();i++)w.step(1/120,null);return w.done()?w.result():null}
return{V,Q,meshes,World,simulate,topIndex};});
