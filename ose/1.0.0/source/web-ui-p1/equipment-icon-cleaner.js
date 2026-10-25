/* Braseiro OSE: real pixel cleanup of legacy black-bordered equipment assets.
   Presentation only. Source item IDs and equipment rules never change. */
(function(root){
  'use strict';
  const cache=new Map();
  const sourcePattern=/^equipment-icons\/[A-Z0-9_]+\.png$/;
  const isDark=(d,p)=>d[p+3]>90&&Math.max(d[p],d[p+1],d[p+2])<82&&
    Math.max(d[p],d[p+1],d[p+2])-Math.min(d[p],d[p+1],d[p+2])<49;
  function clean(src){
    if(cache.has(src))return cache.get(src);
    const job=new Promise((resolve)=>{
      const image=new Image();
      image.onload=()=>{
        try{
          const w=image.naturalWidth,h=image.naturalHeight;
          if(!w||!h||w*h>4000000){resolve(src);return;}
          const canvas=document.createElement('canvas');canvas.width=w;canvas.height=h;
          const ctx=canvas.getContext('2d',{willReadFrequently:true});
          if(!ctx){resolve(src);return;}
          ctx.drawImage(image,0,0);
          const frame=ctx.getImageData(0,0,w,h),data=frame.data;
          const visited=new Uint8Array(w*h),queue=[];
          const floodSeed=(x,y)=>{const id=y*w+x;if(visited[id]||!isDark(data,id*4))return;visited[id]=1;queue.push(id);};
          for(let x=0;x<w;x++){floodSeed(x,0);floodSeed(x,h-1);}
          for(let y=0;y<h;y++){floodSeed(0,y);floodSeed(w-1,y);}
          /* Some catalog images have a transparent outer padding. Detect a
             mostly-black rectangular ring without touching internal line art. */
          const maxEdge=Math.max(1,Math.floor(Math.min(w,h)*.16));
          for(let y=1;y<maxEdge;y++){
            let dark=0;for(let x=0;x<w;x++)if(isDark(data,(y*w+x)*4))dark++;
            if(dark/w>.70){for(let x=0;x<w;x++)floodSeed(x,y);break;}
          }
          for(let x=1;x<maxEdge;x++){
            let dark=0;for(let y=0;y<h;y++)if(isDark(data,(y*w+x)*4))dark++;
            if(dark/h>.70){for(let y=0;y<h;y++)floodSeed(x,y);break;}
          }
          for(let i=0;i<queue.length;i++){
            const id=queue[i],x=id%w,y=(id/w)|0;
            if(x>0)floodSeed(x-1,y);
            if(x+1<w)floodSeed(x+1,y);
            if(y>0)floodSeed(x,y-1);
            if(y+1<h)floodSeed(x,y+1);
          }
          if(queue.length===0){
            // Asset is already cleanly transparent on disk
            resolve(src);
            return;
          }
          for(const id of queue)data[id*4+3]=0;
          let left=w,top=h,right=-1,bottom=-1;
          for(let y=0;y<h;y++)for(let x=0;x<w;x++)if(data[(y*w+x)*4+3]>20){
            left=Math.min(left,x);right=Math.max(right,x);
            top=Math.min(top,y);bottom=Math.max(bottom,y);
          }
          if(right<left||bottom<top){resolve(src);return;}
          ctx.putImageData(frame,0,0);
          const pad=Math.max(2,Math.round(Math.min(w,h)*.025));
          const out=document.createElement('canvas');
          out.width=right-left+1+2*pad;out.height=bottom-top+1+2*pad;
          out.getContext('2d').drawImage(canvas,left,top,right-left+1,bottom-top+1,pad,pad,right-left+1,bottom-top+1);
          resolve(out.toDataURL('image/png'));
        }catch(e){console.warn('OSE equipment asset correction:',src,e);resolve(src);}
      };
      image.onerror=()=>resolve(src);
      image.src=src;
    });
    cache.set(src,job);return job;
  }
  function refresh(container){
    if(!container)return;
    container.querySelectorAll('img.equipment-art').forEach(img=>{
      if(img.dataset.equipmentCleaning)return;
      const src=img.getAttribute('src')||'';
      if(!sourcePattern.test(src))return;
      img.dataset.equipmentCleaning='1';
      clean(src).then(cleanUrl=>{
        if(img.isConnected&&img.getAttribute('src')===src){
          if(cleanUrl!==src)img.src=cleanUrl;
          img.dataset.equipmentCleaned='yes';
        }
      });
    });
  }
  root.OSEEquipmentArt=Object.freeze({refresh});
})(typeof window!=='undefined'?window:globalThis);
