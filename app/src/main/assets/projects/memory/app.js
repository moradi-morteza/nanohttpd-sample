const grid = document.getElementById('grid');
const movesEl = document.getElementById('moves');
const timeEl  = document.getElementById('time');
const restart = document.getElementById('restart');

const EMOJIS = ['🍕','🍩','🍪','🍎','🍓','🍒','🍇','🍋','🍔','🍟','🌮','🥐'];
const SIZE_MOBILE = 4*3;   // 12 cards on small screens
const SIZE_DESKTOP = 6*3;  // 18 cards on wider screens

let first = null, lock = false, moves = 0, matched = 0, timer = null, seconds = 0;

function shuffle(a){ for(let i=a.length-1;i>0;i--){ const j=(Math.random()* (i+1))|0; [a[i],a[j]]=[a[j],a[i]];} return a; }

function cardTemplate(symbol, id){
  const el = document.createElement('div');
  el.className = 'card';
  el.dataset.symbol = symbol;
  el.dataset.id = id;
  el.innerHTML = `
    <div class="face front"></div>
    <div class="face back">${symbol}</div>
  `;
  el.addEventListener('click', () => onFlip(el));
  return el;
}

function onFlip(el){
  if (lock || el.classList.contains('flipped') || el.classList.contains('matched')) return;

  if (!timer) startTimer();
  el.classList.add('flipped');

  if (!first) {
    first = el;
    return;
  }

  moves++;
  movesEl.textContent = moves;

  const same = first.dataset.symbol === el.dataset.symbol && first.dataset.id !== el.dataset.id;
  if (same) {
    first.classList.add('matched');
    el.classList.add('matched');
    first = null;
    matched += 2;
    if (matched === grid.querySelectorAll('.card').length) finish();
  } else {
    lock = true;
    setTimeout(() => {
      first.classList.remove('flipped');
      el.classList.remove('flipped');
      first = null;
      lock = false;
    }, 600);
  }
}

function layoutSize(){
  return window.matchMedia('(min-width:700px)').matches ? SIZE_DESKTOP : SIZE_MOBILE;
}

function newDeck() {
  const n = layoutSize() / 2;
  const pool = shuffle(EMOJIS.slice()).slice(0, n);
  const deck = shuffle(pool.concat(pool).map((s, i) => ({ s, i })));
  return deck;
}

function render(){
  grid.innerHTML = '';
  for (const {s, i} of newDeck()){
    grid.appendChild(cardTemplate(s, i));
  }
  moves = 0; matched = 0; movesEl.textContent = '0';
  seconds = 0; timeEl.textContent = '0';
  stopTimer(); timer = null;
  first = null; lock = false;
}

function startTimer(){
  timer = setInterval(() => {
    seconds++;
    timeEl.textContent = seconds.toString();
  }, 1000);
}
function stopTimer(){ if(timer){ clearInterval(timer); timer=null; } }

function finish(){
  stopTimer();
  setTimeout(() => {
    alert(`Done! Moves: ${moves}, Time: ${seconds}s`);
  }, 200);
}

restart.addEventListener('click', render);
window.addEventListener('resize', () => {
  // Rebuild grid when layout breakpoint crosses
  const prev = grid.childElementCount;
  render();
  // keep user from raging when grid size changes mid-game
  console.log('grid rebuilt', prev, '->', grid.childElementCount);
});

render();
