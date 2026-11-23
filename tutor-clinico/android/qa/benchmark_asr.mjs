#!/usr/bin/env node
/**
 * ECO offline ASR audit: compare a reference transcript to app output.
 * Usage: node qa/benchmark_asr.mjs /path/reference.txt /path/eco_output.txt
 * No cloud/API. Report is printed locally; use only fictitious/consented data.
 * WER counts substitutions/deletions/insertions; CER counts Unicode characters.
 */
import fs from "node:fs";
const reference=process.argv[2], hypothesis=process.argv[3];
if(!reference||!hypothesis){
 console.error("Usage: node qa/benchmark_asr.mjs referencia.txt transcricao_eco.txt");
 process.exit(2);
}
const norm=s=>s.toLocaleLowerCase("pt-BR").normalize("NFC")
 .replace(/[^\p{L}\p{N}\s]/gu," ").replace(/\s+/gu," ").trim();
const ref=norm(fs.readFileSync(reference,"utf8")), hyp=norm(fs.readFileSync(hypothesis,"utf8"));
function measure(a,b,details=false){
 const n=a.length,m=b.length;
 if(!n)throw Error("Texto de referência vazio");
 if(!details){
  let previous=Array.from({length:m+1},(_,i)=>i);
  for(let i=1;i<=n;i++){
   const current=new Array(m+1);current[0]=i;
   for(let j=1;j<=m;j++)
    current[j]=Math.min(previous[j]+1,current[j-1]+1,previous[j-1]+(a[i-1]===b[j-1]?0:1));
   previous=current;
  }
  return {distance:previous[m],rate:previous[m]/n};
 }
 const rows=Array.from({length:n+1},()=>new Uint32Array(m+1));
 for(let i=0;i<=n;i++)rows[i][0]=i;
 for(let j=0;j<=m;j++)rows[0][j]=j;
 for(let i=1;i<=n;i++)for(let j=1;j<=m;j++)
  rows[i][j]=Math.min(rows[i-1][j]+1,rows[i][j-1]+1,rows[i-1][j-1]+(a[i-1]===b[j-1]?0:1));
 let i=n,j=m,subs=0,del=0,ins=0;const examples=[];
 while(i||j){
  if(i&&j&&rows[i][j]===rows[i-1][j-1]+(a[i-1]===b[j-1]?0:1)){
   if(a[i-1]!==b[j-1]){subs++;if(examples.length<30)examples.push({expected:a[i-1],recognized:b[j-1]})}
   i--;j--;
  }else if(i&&rows[i][j]===rows[i-1][j]+1){del++;i--}
  else{ins++;j--}
 }
 return {distance:rows[n][m],rate:rows[n][m]/n,substitutions:subs,deletions:del,insertions:ins,examples:examples.reverse()};
}
const words=measure(ref?ref.split(" "):[],hyp?hyp.split(" "):[],true);
const chars=measure(Array.from(ref),Array.from(hyp));
console.log(JSON.stringify({
 referenceWords:ref.split(" ").length,hypothesisWords:hyp.split(" ").length,
 WER:Math.round(words.rate*10000)/100,CER:Math.round(chars.rate*10000)/100,
 substitutions:words.substitutions,deletions:words.deletions,insertions:words.insertions,
 samplesOfMismatches:words.examples,
 note:"WER and CER are percentages, not likelihood of clinical correctness. Review negations and family references manually."
},null,2));
