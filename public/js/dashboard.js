const API_URL = "http://localhost:8080";

const campoTitulo = document.getElementById("titulo");
const campoConteudo = document.getElementById("conteudo");
const campoInicio = document.getElementById("inicio");
const seletorSemana = document.getElementById("seletorSemana");
const semanaAtual = document.getElementById("semanaAtual");
const notesList = document.getElementById("notesList");
const modeloNota = document.getElementById("modeloNota");
const carregandoNotas = document.getElementById("carregandoNotas");
const semNotas = document.getElementById("semNotas");
const downloadRelatorio = document.getElementById("downloadRelatorio");
const mensagem = document.getElementById("mensagem");
let notaSelecionadaId = null;
let inicioEm = null;
let fimEm = null;
let semanaOriginal = "";
let notaAntiga = false;
let salvando = false;
let notasCarregadas = [];

// Horario civil de Sao Paulo, independente do fuso do computador.
function agoraSaoPaulo() {
  const partes = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Sao_Paulo", year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hourCycle: "h23"
  }).formatToParts(new Date());
  const valores = {};
  partes.forEach(function(parte) { valores[parte.type] = parte.value; });
  return valores.year + "-" + valores.month + "-" + valores.day + "T" + valores.hour + ":" + valores.minute;
}

function inicioDaSemana(data) {
  const dia = new Date(data.slice(0, 10) + "T12:00:00Z");
  dia.setUTCDate(dia.getUTCDate() - (dia.getUTCDay() + 6) % 7);
  return dia.toISOString().slice(0, 10);
}

function dataCurta(data) {
  const partes = data.slice(0, 10).split("-");
  return partes[2] + "/" + partes[1] + "/" + partes[0];
}

function periodoSemana(semana) {
  const fim = new Date(semana + "T12:00:00Z");
  fim.setUTCDate(fim.getUTCDate() + 6);
  return dataCurta(semana) + " - " + dataCurta(fim.toISOString());
}

function mostrarMensagem(texto, classe) {
  mensagem.textContent = texto;
  mensagem.className = "message " + (classe || "");
}

function requisitar(caminho, opcoes) {
  opcoes = opcoes || {};
  opcoes.credentials = "include";
  return fetch(API_URL + caminho, opcoes).then(function(resposta) {
    if (resposta.status === 401) {
      sessionStorage.removeItem("usuarioLogado");
      window.location.href = "login.html";
      throw new Error("Sua sessão terminou. Entre novamente.");
    }
    if (!resposta.ok) {
      return resposta.json().catch(function() { return {}; }).then(function(erro) {
        throw new Error(erro.message || "Não foi possível concluir a operação.");
      });
    }
    return resposta;
  }).catch(function(erro) {
    if (erro.message === "Failed to fetch") throw new Error("Não foi possível conectar ao servidor.");
    throw erro;
  });
}

function formatarDuracao(minutos) {
  if (minutos < 60) return minutos + " min";
  return Math.floor(minutos / 60) + "h " + String(minutos % 60).padStart(2, "0") + "min";
}

function atualizarHorarios() {
  campoInicio.value = inicioEm ? inicioEm.slice(11, 16) : "";
  campoInicio.disabled = notaAntiga || salvando;
  document.getElementById("fimNota").textContent = notaAntiga ? "Horário não registrado" :
    fimEm ? "Fim: " + fimEm.slice(11, 16) + (fimEm.slice(0, 10) !== inicioEm.slice(0, 10) ? " (" + dataCurta(fimEm) + ")" : "") : "Fim: automático ao salvar";
  document.getElementById("dataNota").textContent = inicioEm ? dataCurta(inicioEm) : "";
  let duracao = "";
  if (inicioEm && fimEm) {
    const minutos = Math.round((Date.parse(fimEm + ":00-03:00") - Date.parse(inicioEm + ":00-03:00")) / 60000);
    duracao = minutos >= 0 ? "Duração: " + formatarDuracao(minutos) : "Início posterior ao término";
  }
  document.getElementById("duracaoNota").textContent = duracao;
  semanaAtual.textContent = semanaOriginal || periodoSemana(inicioDaSemana(inicioEm || agoraSaoPaulo()));
}

function registrarInicio() {
  if (inicioEm || notaSelecionadaId !== null) return;
  if (!campoTitulo.value.trim() && !campoConteudo.value.trim()) return;
  inicioEm = agoraSaoPaulo();
  atualizarHorarios();
}

function corrigirInicio() {
  if (notaAntiga || !campoInicio.value) { atualizarHorarios(); return; }
  const data = (inicioEm || agoraSaoPaulo()).slice(0, 10);
  inicioEm = data + "T" + campoInicio.value;
  atualizarHorarios();
}

function limparNota() {
  if (salvando) return;
  notaSelecionadaId = null;
  inicioEm = null;
  fimEm = null;
  semanaOriginal = "";
  notaAntiga = false;
  campoTitulo.value = "";
  campoConteudo.value = "";
  document.getElementById("editorTitulo").textContent = "Nova nota";
  mostrarMensagem("", "");
  atualizarHorarios();
  campoTitulo.focus();
}

function editarNota(id) {
  if (salvando) return;
  const nota = notasCarregadas.find(function(nota) { return nota.id === id; });
  if (nota) preencherNota(nota);
}

function preencherNota(nota) {
  notaSelecionadaId = nota.id;
  inicioEm = nota.inicioEm ? nota.inicioEm.replace(" ", "T").slice(0, 16) : null;
  fimEm = nota.fimEm ? nota.fimEm.replace(" ", "T").slice(0, 16) : null;
  notaAntiga = !inicioEm;
  semanaOriginal = notaAntiga ? nota.semana : "";
  campoTitulo.value = nota.titulo || "";
  campoConteudo.value = nota.conteudo || "";
  document.getElementById("editorTitulo").textContent = "Editar nota";
  mostrarMensagem("", "");
  atualizarHorarios();
  campoTitulo.focus();
}

function dadosDaNota() {
  return { titulo: campoTitulo.value.trim(), conteudo: campoConteudo.value.trim(),
    inicioEm: inicioEm, semana: semanaOriginal || periodoSemana(inicioDaSemana(inicioEm || agoraSaoPaulo())) };
}

function bloquearEdicao(bloqueado) {
  salvando = bloqueado;
  campoTitulo.disabled = bloqueado;
  campoConteudo.disabled = bloqueado;
  campoInicio.disabled = bloqueado || notaAntiga;
  document.getElementById("salvarNota").disabled = bloqueado;
  document.getElementById("novaNota").disabled = bloqueado;
  document.getElementById("salvarNota").textContent = bloqueado ? "Salvando..." : "Salvar nota";
}

function salvarApontamento() {
  if (salvando) return Promise.resolve();
  registrarInicio();
  const dados = dadosDaNota();
  if (!dados.titulo) { mostrarMensagem("Informe o título da nota.", "erro"); return Promise.resolve(); }
  const fimLimite = fimEm || agoraSaoPaulo();
  if (inicioEm && inicioEm > fimLimite) {
    mostrarMensagem("O início deve ser anterior ou igual ao término.", "erro");
    return Promise.resolve();
  }
  const editando = notaSelecionadaId !== null;
  bloquearEdicao(true);
  return requisitar("/apontamentos" + (editando ? "/" + notaSelecionadaId : ""), {
    method: editando ? "PUT" : "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify(dados)
  }).then(function(resposta) { return resposta.json(); }).then(function(nota) {
    preencherNota(nota);
    // O filtro acompanha a semana da nota salva.
    return carregarNotas(nota.inicioSemana).then(function() {
      mostrarMensagem(editando ? "Nota atualizada." : "Nota salva.", "sucesso");
    });
  }).catch(function(erro) { mostrarMensagem(erro.message, "erro"); })
    .finally(function() { bloquearEdicao(false); });
}

function semanaDaNota(nota) {
  return nota.inicioSemana || inicioDaSemana(nota.inicioEm || nota.criadoEm || agoraSaoPaulo());
}

function atualizarSemanas(preferida) {
  const atual = inicioDaSemana(agoraSaoPaulo());
  const selecionada = preferida || seletorSemana.value || atual;
  const semanas = [atual];
  notasCarregadas.forEach(function(nota) {
    const semana = semanaDaNota(nota);
    if (semanas.indexOf(semana) < 0) semanas.push(semana);
  });
  if (semanas.indexOf(selecionada) < 0) semanas.push(selecionada);
  semanas.sort().reverse();
  seletorSemana.innerHTML = "";
  semanas.forEach(function(semana) {
    const opcao = document.createElement("option");
    opcao.value = semana;
    opcao.textContent = periodoSemana(semana) + (semana === atual ? " · atual" : "");
    seletorSemana.appendChild(opcao);
  });
  seletorSemana.value = selecionada;
}

function mostrarNotas(notas, preferida) {
  notasCarregadas = notas || [];
  atualizarSemanas(preferida);
  renderizarNotas();
}

function renderizarNotas() {
  // Mantém as mensagens do HTML e remove apenas os cards da lista anterior.
  notesList.querySelectorAll(".note-item").forEach(function(item) { item.remove(); });
  carregandoNotas.hidden = true;
  let quantidade = 0;
  notasCarregadas.forEach(function(nota) {
    if (semanaDaNota(nota) !== seletorSemana.value) return;
    quantidade++;
    const item = modeloNota.content.firstElementChild.cloneNode(true);
    item.querySelector(".note-title").textContent = nota.titulo;
    item.querySelector(".note-date").textContent = dataCurta(nota.dataApontamento || nota.criadoEm || agoraSaoPaulo());
    const texto = (nota.conteudo || "").replace(/\s+/g, " ").trim();
    item.querySelector(".note-preview").textContent = texto.length > 95 ? texto.slice(0, 92) + "..." : texto;
    item.querySelector(".note-time").textContent = nota.inicioEm ? nota.inicioEm.slice(11, 16) + " · " + formatarDuracao(nota.duracaoMinutos || 0) : "Horário não registrado";
    item.querySelector(".edit").value = nota.id;
    item.querySelector(".delete").value = nota.id;
    notesList.appendChild(item);
  });
  semNotas.hidden = quantidade > 0;
}

function carregarNotas(preferida) {
  return requisitar("/apontamentos").then(function(resposta) { return resposta.json(); })
    .then(function(notas) { mostrarNotas(notas, preferida); })
    .catch(function(erro) {
      mostrarMensagem(erro.message, "erro");
      throw erro;
    });
}

function atualizarLista() {
  // carregarNotas já mostra a mensagem caso a requisição falhe.
  return carregarNotas().catch(function() {});
}

function excluirNota(id) {
  if (salvando || !window.confirm("Excluir esta nota?")) return;
  return requisitar("/apontamentos/" + id, {method: "DELETE"}).then(function() {
    if (notaSelecionadaId === id) limparNota();
    return carregarNotas();
  }).then(function() { mostrarMensagem("Nota excluída.", "sucesso"); })
    .catch(function(erro) { mostrarMensagem(erro.message, "erro"); });
}

function exportarNotas() {
  const botao = document.getElementById("exportarNotas");
  if (botao.disabled) return;
  botao.disabled = true;
  const semana = seletorSemana.value;
  return requisitar("/apontamentos/exportar", {
    method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify({semana: semana})
  }).then(function(resposta) { return resposta.blob(); }).then(function(arquivo) {
    const url = URL.createObjectURL(arquivo);
    downloadRelatorio.href = url;
    downloadRelatorio.download = "logweek-" + semana + ".txt";
    downloadRelatorio.click();
    setTimeout(function() { URL.revokeObjectURL(url); }, 1000);
    mostrarMensagem("Apontamento semanal gerado.", "sucesso");
  }).catch(function(erro) { mostrarMensagem(erro.message, "erro"); })
    .finally(function() { botao.disabled = false; });
}

function sair() {
  return requisitar("/auth/logout", {method: "POST"}).then(function() {
    sessionStorage.removeItem("usuarioLogado");
    window.location.href = "login.html";
  }).catch(function(erro) { mostrarMensagem(erro.message, "erro"); });
}

function iniciarPagina() {
  limparNota();
  atualizarSemanas();
  requisitar("/auth/me").then(function(resposta) { return resposta.json(); }).then(function(usuario) {
    sessionStorage.setItem("usuarioLogado", JSON.stringify(usuario));
    document.getElementById("userLabel").textContent = usuario.nome;
    return carregarNotas();
  }).catch(function(erro) { mostrarMensagem(erro.message, "erro"); });
}


