// ==UserScript==
// @name         Keiba EV Pro Bridge for netkeiba
// @namespace    https://yamase-takiyoshi.github.io/keiba-app/
// @version      1.1.0
// @description  netkeibaのJRA/NAR出馬表・結果データをGitHub Pages版 競馬EV Proへ渡すブリッジ
// @match        https://race.netkeiba.com/*
// @match        https://nar.netkeiba.com/*
// @grant        none
// ==/UserScript==
(function(){
  'use strict';
  const APP_URL='https://yamase-takiyoshi.github.io/keiba-app/';
  const KEY='KEIBA_BRIDGE_LAST';
  function text(el){return (el&&el.textContent||'').replace(/\s+/g,' ').trim()}
  function z2h(s){return String(s||'').replace(/[０-９]/g,ch=>String.fromCharCode(ch.charCodeAt(0)-0xFEE0))}
  function yenNum(s){const n=Number(z2h(s).replace(/,/g,'').replace(/円/g,''));return Number.isFinite(n)?n:0}
  function getRaceId(){const h=document.body?document.body.innerHTML:'';const m=location.href.match(/[?&]race_id=(\d{10,})/)||h.match(/race_id=(\d{10,})/);return m?m[1]:''}
  function isNAR(){return location.hostname.includes('nar.netkeiba.com')}
  function detectPage(){if(location.href.includes('/result.html'))return'result';if(location.href.includes('/shutuba.html'))return'shutuba';return'unknown'}
  function parseHorses(){const horses=[];document.querySelectorAll('tr').forEach(tr=>{const horseLink=tr.querySelector('a[href*="/horse/"]');if(!horseLink)return;const row=text(tr);let horseNo=Number(text(tr.querySelector('.Umaban,.Horse_Num,[class*="Umaban"]')));if(!horseNo){const cells=[...tr.children].map(td=>Number(z2h(text(td)).match(/^\d{1,2}$/)?.[0]||0));horseNo=cells.find(n=>Number.isInteger(n)&&n>=1&&n<=18)||0}if(!Number.isInteger(horseNo)||horseNo<1||horseNo>18)return;const name=text(horseLink);let oddsText=text(tr.querySelector('.Odds,[class*="Odds"],[class*="odds"]'));if(!oddsText){const m=z2h(row).match(/\b\d{1,2}\.\d\b/);oddsText=m?m[0]:''}const odds=Number((z2h(oddsText).match(/\d+(?:\.\d+)?/)||[])[0]||0);let popularity=Number((z2h(text(tr.querySelector('.Ninki,[class*="Ninki"],[class*="Popular"]'))).match(/\d+/)||[])[0]||0);horses.push({horseNo,name,odds,popularity,raw:row.slice(0,300)})});const seen=new Set();return horses.filter(h=>{if(seen.has(h.horseNo))return false;seen.add(h.horseNo);return true}).sort((a,b)=>a.horseNo-b.horseNo)}
  function normalizeCombo(s){return z2h(s).replace(/[→＞>－ー―ｰ−]/g,'-').replace(/\s+/g,'').replace(/番/g,'')}
  function parsePayoutText(){const body=z2h(document.body.innerText||'');const types=['単勝','複勝','枠連','馬連','ワイド','馬単','3連複','3連単'];const payouts=[];types.forEach((type,i)=>{const start=body.indexOf(type);if(start<0)return;let end=body.length;for(let j=i+1;j<types.length;j++){const k=body.indexOf(types[j],start+type.length);if(k>0){end=Math.min(end,k)}}const block=body.slice(start,end).slice(0,900);const lines=block.split(/\n+/).map(x=>x.trim()).filter(Boolean);lines.forEach(line=>{const money=[...line.matchAll(/([\d,]+)円/g)].map(m=>yenNum(m[1]));money.forEach(yen=>{if(yen>=100&&yen%10===0){const before=line.split(String(yen).replace(/\B(?=(\d{3})+(?!\d))/g,','))[0]||line;let combo=(before.match(/(\d{1,2}\s*[\-→＞>－ー―ｰ−]\s*\d{1,2}(?:\s*[\-→＞>－ー―ｰ−]\s*\d{1,2})?)/)||[])[1]||'';if(!combo&&type==='単勝')combo=(line.match(/(?:単勝)?\s*(\d{1,2})\s/)||[])[1]||'';payouts.push({type,combo:normalizeCombo(combo),payout:yen,raw:line.slice(0,180)})}})});});const uniq=[];const set=new Set();payouts.forEach(p=>{const key=p.type+'|'+p.combo+'|'+p.payout;if(!set.has(key)){set.add(key);uniq.push(p)}});return uniq}
  function parseFinish(){const order=[];document.querySelectorAll('tr').forEach(tr=>{const horseLink=tr.querySelector('a[href*="/horse/"]');if(!horseLink)return;const cells=[...tr.children].map(td=>z2h(text(td)));const rank=Number(cells.find(v=>/^\d{1,2}$/.test(v))||0);if(!rank||rank>18)return;const nums=cells.map(v=>Number(v)).filter(n=>Number.isInteger(n)&&n>=1&&n<=18);let horseNo=0;for(const n of nums){if(n!==rank){horseNo=n;break}}if(!horseNo&&nums.length)horseNo=nums[nums.length-1];if(horseNo)order[rank-1]=horseNo});return order.filter(Boolean)}
  function collect(){const page=detectPage();const data={source:'netkeiba',host:location.hostname,url:location.href,raceId:getRaceId(),type:isNAR()?'NAR':'JRA',page,collectedAt:new Date().toISOString(),horses:page==='shutuba'?parseHorses():[],result:page==='result'?{finishOrderHorseNo:parseFinish(),payouts:parsePayoutText()}:null};localStorage.setItem(KEY,JSON.stringify(data));try{window.name='KEIBA_BRIDGE:'+JSON.stringify(data)}catch(e){}return data}
  function postToAppWindow(w,data){let n=0;const timer=setInterval(()=>{n++;try{w.postMessage({kind:'KEIBA_BRIDGE_DATA',payload:data},APP_URL.replace(/\/$/,''))}catch(e){}if(n>20)clearInterval(timer)},400)}
  function openApp(){const data=collect();const w=window.open(APP_URL,'_blank');if(w)postToAppWindow(w,data)}
  function inject(){if(document.getElementById('keibaBridgeBtn'))return;const btn=document.createElement('button');btn.id='keibaBridgeBtn';btn.textContent='🏆 競馬分析アプリへ送る';btn.style.cssText='position:fixed;right:14px;bottom:14px;z-index:2147483647;padding:12px 16px;border-radius:999px;border:1px solid #d6a51f;background:#1f1f1f;color:#ffd766;font-size:14px;font-weight:800;box-shadow:0 8px 24px rgba(0,0,0,.4);';btn.onclick=openApp;document.body.appendChild(btn)}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(inject,800));else setTimeout(inject,800);
})();
