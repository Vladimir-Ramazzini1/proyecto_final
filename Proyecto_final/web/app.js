const api = {
  atletas: '/api/atletas',
  entrenos: '/api/entrenamientos'
};

function escapeHtml(s){ return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }

async function fetchAtletas(){
  const res = await fetch(api.atletas);
  const data = await res.json();
  const lista = document.getElementById('listaAtletas');
  const select = document.getElementById('selectAtletas');
  if (lista) lista.innerHTML = '';
  if (select) select.innerHTML = '';
  data.forEach(a => {
    if (lista) {
      const card = document.createElement('div');
      card.className = 'atleta-card';
      card.innerHTML = `<div class="atleta-info"><b>${escapeHtml(a.nombreCompleto)}</b><small>${a.disciplina} • ${a.departamento || ''}</small></div><div><button class="btn" onclick="mostrarEntrenos('${a.id}')">Entrenos</button> <button class="btn" onclick="editarAtleta('${a.id}')">Editar</button> <button class="btn" onclick="eliminarAtleta('${a.id}')">Eliminar</button></div>`;
      lista.appendChild(card);
    }

    if (select) {
      const opt = document.createElement('option');
      opt.value = a.id; opt.text = a.nombreCompleto;
      select.appendChild(opt);
    }
  });

  // actualizar resumen
  const resumen = document.getElementById('resumenText');
  if (resumen) resumen.textContent = `Atletas registrados: ${data.length}`;
}

async function editarAtleta(id) {
  try {
    const nombre = prompt('Nuevo nombre (dejar vacío para mantener):');
    const edad = prompt('Nueva edad (dejar vacío para mantener):');
    const disciplina = prompt('Nueva disciplina (dejar vacío para mantener):');
    const departamento = prompt('Nuevo departamento (dejar vacío para mantener):');
    const nacionalidad = prompt('Nueva nacionalidad (dejar vacío para mantener):');
    const fecha = prompt('Nueva fecha de ingreso YYYY-MM-DD (dejar vacío para mantener):');
    const payload = {};
    if (nombre) payload.nombre = nombre;
    if (edad) payload.edad = edad;
    if (disciplina) payload.disciplina = disciplina;
    if (departamento) payload.departamento = departamento;
    if (nacionalidad) payload.nacionalidad = nacionalidad;
    if (fecha) payload.fechaIngreso = fecha;
    const res = await fetch(`/api/atletas/${encodeURIComponent(id)}`, {method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)});
    if (res.ok) { alert('Atleta actualizado'); fetchAtletas(); } else { const t = await res.text(); alert('Error: '+t); }
  } catch(err){ alert('Error: '+err.message); }
}

async function eliminarAtleta(id) {
  try {
    if (!confirm('Eliminar atleta '+id+'?')) return;
    const res = await fetch(`/api/atletas/${encodeURIComponent(id)}`, {method:'DELETE'});
    if (res.status===204) { alert('Atleta eliminado'); fetchAtletas(); } else { const t=await res.text(); alert('Error: '+t); }
  } catch(err){ alert('Error: '+err.message); }
}

document.getElementById('formAtleta').addEventListener('submit', async (e)=>{
  e.preventDefault();
  const fd = new FormData(e.target);
  const payload = Object.fromEntries(fd.entries());
  const res = await fetch(api.atletas, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)});
  if (res.ok){ e.target.reset(); await fetchAtletas(); alert('Atleta registrado'); showPage('atletas'); }
  else { const t = await res.text(); alert('Error al registrar atleta: '+t); }
});

document.getElementById('formEntreno').addEventListener('submit', async (e)=>{
  e.preventDefault();
  const fd = new FormData(e.target);
  const payload = Object.fromEntries(fd.entries());
  if (!payload.fecha) payload.fecha = new Date().toISOString().slice(0,10);
  const res = await fetch(api.entrenos, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)});
  if (res.ok){ e.target.reset(); alert('Entrenamiento registrado'); showPage('entrenamientos'); }
  else { const t = await res.text(); alert('Error al registrar: '+t); }
});

async function mostrarEntrenos(idAtleta){
  const res = await fetch(api.entrenos + '?atletaId=' + encodeURIComponent(idAtleta));
  const data = await res.json();
  if (!data.length) { alert('No hay entrenamientos.'); return; }
  let html = '<ul style="padding-left:16px">';
  data.forEach(e=> html += `<li>${e.fecha} | ${e.tipo} | ${e.valor} <button class="btn" onclick="editarEntreno('${e.id}')">Editar</button> <button class="btn" onclick="eliminarEntreno('${e.id}')">Eliminar</button></li>`);
  html += '</ul>';
  // Mostrar en una nueva ventana simple
  const w = window.open('','Entrenamientos','width=600,height=400');
  w.document.title = 'Entrenamientos de '+idAtleta;
  w.document.body.innerHTML = `<h3>Entrenamientos de ${escapeHtml(idAtleta)}</h3>${html}`;
}

// Ver entrenamientos desde la sección de registro
const btnVer = document.getElementById('btnVerEntrenos');
if (btnVer) btnVer.addEventListener('click', ()=>{
  const sel = document.getElementById('selectAtletas');
  if (!sel) return; const id = sel.value; if (!id) { alert('Seleccione un atleta'); return; }
  mostrarEntrenos(id);
});

async function editarEntreno(id) {
  try {
    const fecha = prompt('Nueva fecha YYYY-MM-DD (dejar vacío para mantener):');
    const tipo = prompt('Nuevo tipo (RESISTENCIA/TECNICA/FUERZA) (dejar vacío para mantener):');
    const valor = prompt('Nuevo valor (dejar vacío para mantener):');
    const ubic = prompt('Nueva ubicación (nacional/internacional) (dejar vacío para mantener):');
    const pais = prompt('Nuevo país (dejar vacío para mantener):');
    const payload = {};
    if (fecha) payload.fecha = fecha;
    if (tipo) payload.tipo = tipo;
    if (valor) payload.valor = valor;
    if (ubic) payload.ubicacion = ubic;
    if (pais) payload.pais = pais;
    const res = await fetch(`/api/entrenamientos/${encodeURIComponent(id)}`, {method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)});
    if (res.ok) { alert('Entrenamiento actualizado'); } else { const t = await res.text(); alert('Error: '+t); }
  } catch(err){ alert('Error: '+err.message); }
}

async function eliminarEntreno(id) {
  try {
    if (!confirm('Eliminar entrenamiento '+id+'?')) return;
    const res = await fetch(`/api/entrenamientos/${encodeURIComponent(id)}`, {method:'DELETE'});
    if (res.status===204) { alert('Entrenamiento eliminado'); } else { const t=await res.text(); alert('Error: '+t); }
  } catch(err){ alert('Error: '+err.message); }
}

// navigation
function showPage(id) {
  document.querySelectorAll('.page').forEach(p=> p.classList.remove('active'));
  const target = document.getElementById('page-'+id) || document.getElementById('page-'+id);
  if (target) target.classList.add('active');
  document.querySelectorAll('.nav-item').forEach(b=> b.classList.remove('active'));
  const btn = document.querySelector(`.nav-item[data-target="${id}"]`);
  if (btn) btn.classList.add('active');
  // acciones al abrir
  if (id === 'atletas') fetchAtletas();
}

document.querySelectorAll('.nav-item').forEach(b=> b.addEventListener('click', ()=> showPage(b.dataset.target)));

// Planilla: generar and export
document.getElementById('btnGenerarPlanilla').addEventListener('click', async ()=>{
  const mes = document.getElementById('planMes').value || new Date().getMonth()+1;
  const anio = document.getElementById('planAnio').value || new Date().getFullYear();
  const res = await fetch(`/api/planilla?mes=${encodeURIComponent(mes)}&anio=${encodeURIComponent(anio)}`);
  if (!res.ok) { alert('Error generando planilla'); return; }
  const data = await res.json();
  const cont = document.getElementById('planillaResultado');
  if (!data.length) { cont.innerHTML = '<small>No hay pagos para el periodo.</small>'; return; }
  let html = '<table style="width:100%;border-collapse:collapse;color:inherit"><thead><tr><th style="text-align:left">Atleta</th><th style="text-align:right">Pago (Q)</th></tr></thead><tbody>';
  let total = 0;
  data.forEach(row => { html += `<tr><td>${escapeHtml(row.nombre)}</td><td style="text-align:right">${Number(row.pago).toFixed(2)}</td></tr>`; total += Number(row.pago); });
  html += `</tbody><tfoot><tr><td style="font-weight:700">TOTAL</td><td style="text-align:right;font-weight:700">${total.toFixed(2)}</td></tr></tfoot></table>`;
  cont.innerHTML = html;
});

document.getElementById('btnExportarPlanilla').addEventListener('click', async ()=>{
  const mes = document.getElementById('planMes').value || new Date().getMonth()+1;
  const anio = document.getElementById('planAnio').value || new Date().getFullYear();
  const url = `/api/export/planilla?mes=${encodeURIComponent(mes)}&anio=${encodeURIComponent(anio)}`;
  downloadUrl(url, `planilla_${mes}_${anio}.csv`);
});

async function downloadUrl(url, filename){
  try {
    const res = await fetch(url);
    if (!res.ok) { const t = await res.text(); alert('Error: '+t); return; }
    const blob = await res.blob();
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
  } catch(err) { alert('Error descargando: '+err.message); }
}

// export buttons
const expAt = document.getElementById('expAtletas');
if (expAt) expAt.addEventListener('click', ()=> downloadUrl('/api/export/atletas','atletas.csv'));
const expEn = document.getElementById('expEntr');
if (expEn) expEn.addEventListener('click', ()=> downloadUrl('/api/export/entrenamientos','entrenamientos.csv'));
const expEs = document.getElementById('expEstad');
if (expEs) expEs.addEventListener('click', ()=> downloadUrl('/api/export/estadisticas','estadisticas.txt'));

// init
showPage('dashboard');
fetchAtletas().catch(err=>console.error(err));
