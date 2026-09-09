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


function agoraSaoPaulo() {
    const partes = new Intl.DateTimeFormat("en-CA", {
        timeZone: "America/Sao_Paulo",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hourCycle: "h23"
    }).formatToParts(new Date());

    const valores = {};

    partes.forEach(function (parte) {
        valores[parte.type] = parte.value;
    });

    return valores.year + "-" +
        valores.month + "-" +
        valores.day + "T" +
        valores.hour + ":" +
        valores.minute;
}


function inicioDaSemana(data) {
    const dia = new Date(data.slice(0, 10) + "T12:00:00Z");

    dia.setUTCDate(
        dia.getUTCDate() - (dia.getUTCDay() + 6) % 7
    );

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


async function requisitar(caminho, opcoes) {

    opcoes = opcoes || {};

    opcoes.credentials = "include";

    try {

        const resposta = await fetch(
            API_URL + caminho,
            opcoes
        );

        if (resposta.status === 401) {
            window.location.href = "login.html";
            return;
        }

        if (!resposta.ok) {

            const erro = await resposta.json().catch(function () {
                return {};
            });

            throw new Error(
                erro.message ||
                "Não foi possível concluir a operação."
            );
        }

        return resposta;

    } catch (erro) {

        if (erro.message === "Failed to fetch") {
            throw new Error(
                "Não foi possível conectar ao servidor."
            );
        }

        throw erro;
    }
}


function formatarDuracao(minutos) {

    if (minutos < 60) {
        return minutos + " min";
    }

    return Math.floor(minutos / 60) +
        "h " +
        String(minutos % 60).padStart(2, "0") +
        "min";
}


function atualizarHorarios() {

    campoInicio.value = inicioEm
        ? inicioEm.slice(11, 16)
        : "";

    campoInicio.disabled = notaAntiga || salvando;

    let textoFim = "Fim: automático ao salvar";

    if (notaAntiga) {

        textoFim = "Horário não registrado";

    } else if (fimEm) {

        textoFim = "Fim: " + fimEm.slice(11, 16);

        if (fimEm.slice(0, 10) !== inicioEm.slice(0, 10)) {
            textoFim += " (" + dataCurta(fimEm) + ")";
        }
    }

    document.getElementById("fimNota").textContent = textoFim;

    document.getElementById("dataNota").textContent =
        inicioEm ? dataCurta(inicioEm) : "";

    let duracao = "";

    if (inicioEm && fimEm) {

        const inicio = Date.parse(inicioEm + ":00-03:00");
        const fim = Date.parse(fimEm + ":00-03:00");

        const minutos = Math.round(
            (fim - inicio) / 60000
        );

        if (minutos >= 0) {
            duracao = "Duração: " + formatarDuracao(minutos);
        } else {
            duracao = "Início posterior ao término";
        }
    }

    document.getElementById("duracaoNota").textContent = duracao;

    semanaAtual.textContent =
        semanaOriginal ||
        periodoSemana(
            inicioDaSemana(inicioEm || agoraSaoPaulo())
        );
}


function registrarInicio() {

    if (inicioEm || notaSelecionadaId !== null) {
        return;
    }

    if (
        !campoTitulo.value.trim() &&
        !campoConteudo.value.trim()
    ) {
        return;
    }

    inicioEm = agoraSaoPaulo();

    atualizarHorarios();
}


function corrigirInicio() {

    if (notaAntiga || !campoInicio.value) {
        atualizarHorarios();
        return;
    }

    const data = (inicioEm || agoraSaoPaulo()).slice(0, 10);

    inicioEm = data + "T" + campoInicio.value;

    atualizarHorarios();
}


function limparNota() {

    if (salvando) {
        return;
    }

    notaSelecionadaId = null;
    inicioEm = null;
    fimEm = null;
    semanaOriginal = "";
    notaAntiga = false;

    campoTitulo.value = "";
    campoConteudo.value = "";

    document.getElementById("editorTitulo").textContent =
        "Nova nota";

    mostrarMensagem("", "");

    atualizarHorarios();

    campoTitulo.focus();
}


function editarNota(id) {

    if (salvando) {
        return;
    }

    const nota = notasCarregadas.find(function (nota) {
        return nota.id === id;
    });

    if (nota) {
        preencherNota(nota);
    }
}


function preencherNota(nota) {

    notaSelecionadaId = nota.id;

    inicioEm = nota.inicioEm
        ? nota.inicioEm.replace(" ", "T").slice(0, 16)
        : null;

    fimEm = nota.fimEm
        ? nota.fimEm.replace(" ", "T").slice(0, 16)
        : null;

    notaAntiga = !inicioEm;

    semanaOriginal = notaAntiga
        ? nota.semana
        : "";

    campoTitulo.value = nota.titulo || "";
    campoConteudo.value = nota.conteudo || "";

    document.getElementById("editorTitulo").textContent =
        "Editar nota";

    mostrarMensagem("", "");

    atualizarHorarios();

    campoTitulo.focus();
}


function dadosDaNota() {

    return {
        titulo: campoTitulo.value.trim(),
        conteudo: campoConteudo.value.trim(),
        inicioEm: inicioEm,
        semana: semanaOriginal ||
            periodoSemana(
                inicioDaSemana(
                    inicioEm || agoraSaoPaulo()
                )
            )
    };
}


function bloquearEdicao(bloqueado) {

    salvando = bloqueado;

    campoTitulo.disabled = bloqueado;
    campoConteudo.disabled = bloqueado;
    campoInicio.disabled = bloqueado || notaAntiga;

    document.getElementById("salvarNota").disabled = bloqueado;
    document.getElementById("novaNota").disabled = bloqueado;

    document.getElementById("salvarNota").textContent =
        bloqueado
            ? "Salvando..."
            : "Salvar nota";
}


async function salvarApontamento() {

    if (salvando) {
        return;
    }

    registrarInicio();

    const dados = dadosDaNota();

    if (!dados.titulo) {

        mostrarMensagem(
            "Informe o título da nota.",
            "erro"
        );

        return;
    }

    const fimLimite = fimEm || agoraSaoPaulo();

    if (inicioEm && inicioEm > fimLimite) {

        mostrarMensagem(
            "O início deve ser anterior ou igual ao término.",
            "erro"
        );

        return;
    }

    const editando = notaSelecionadaId !== null;

    bloquearEdicao(true);

    try {

        const caminho = editando
            ? "/apontamentos/" + notaSelecionadaId
            : "/apontamentos";

        const resposta = await requisitar(caminho, {

            method: editando
                ? "PUT"
                : "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify(dados)
        });

        const nota = await resposta.json();

        preencherNota(nota);

        await carregarNotas(nota.inicioSemana);

        mostrarMensagem(
            editando
                ? "Nota atualizada."
                : "Nota salva.",
            "sucesso"
        );

    } catch (erro) {

        mostrarMensagem(
            erro.message,
            "erro"
        );

    } finally {

        bloquearEdicao(false);
    }
}


function semanaDaNota(nota) {

    return nota.inicioSemana ||
        inicioDaSemana(
            nota.inicioEm ||
            nota.criadoEm ||
            agoraSaoPaulo()
        );
}


function atualizarSemanas(preferida) {

    const atual = inicioDaSemana(
        agoraSaoPaulo()
    );

    const selecionada =
        preferida ||
        seletorSemana.value ||
        atual;

    const semanas = [atual];

    notasCarregadas.forEach(function (nota) {

        const semana = semanaDaNota(nota);

        if (semanas.indexOf(semana) < 0) {
            semanas.push(semana);
        }
    });

    if (semanas.indexOf(selecionada) < 0) {
        semanas.push(selecionada);
    }

    semanas.sort().reverse();

    seletorSemana.innerHTML = "";

    semanas.forEach(function (semana) {

        const opcao = document.createElement("option");

        opcao.value = semana;

        opcao.textContent =
            periodoSemana(semana) +
            (semana === atual ? " · atual" : "");

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

    notesList
        .querySelectorAll(".note-item")
        .forEach(function (item) {
            item.remove();
        });

    carregandoNotas.hidden = true;

    let quantidade = 0;

    notasCarregadas.forEach(function (nota) {

        if (
            semanaDaNota(nota) !==
            seletorSemana.value
        ) {
            return;
        }

        quantidade++;

        const item =
            modeloNota.content
                .firstElementChild
                .cloneNode(true);

        item.querySelector(".note-title").textContent =
            nota.titulo;

        item.querySelector(".note-date").textContent =
            dataCurta(
                nota.dataApontamento ||
                nota.criadoEm ||
                agoraSaoPaulo()
            );

        const texto =
            (nota.conteudo || "")
                .replace(/\s+/g, " ")
                .trim();

        item.querySelector(".note-preview").textContent =
            texto.length > 95
                ? texto.slice(0, 92) + "..."
                : texto;

        if (nota.inicioEm) {

            item.querySelector(".note-time").textContent =
                nota.inicioEm.slice(11, 16) +
                " · " +
                formatarDuracao(
                    nota.duracaoMinutos || 0
                );

        } else {

            item.querySelector(".note-time").textContent =
                "Horário não registrado";
        }

        item.querySelector(".edit").value = nota.id;
        item.querySelector(".delete").value = nota.id;

        notesList.appendChild(item);
    });

    semNotas.hidden = quantidade > 0;
}


async function carregarNotas(preferida) {

    const resposta =
        await requisitar("/apontamentos");

    const notas =
        await resposta.json();

    mostrarNotas(
        notas,
        preferida
    );
}


async function atualizarLista() {

    try {

        await carregarNotas();

    } catch (erro) {

        mostrarMensagem(
            erro.message,
            "erro"
        );
    }
}


async function excluirNota(id) {

    if (
        salvando ||
        !window.confirm("Excluir esta nota?")
    ) {
        return;
    }

    try {

        await requisitar(
            "/apontamentos/" + id,
            {
                method: "DELETE"
            }
        );

        if (notaSelecionadaId === id) {
            limparNota();
        }

        await carregarNotas();

        mostrarMensagem(
            "Nota excluída.",
            "sucesso"
        );

    } catch (erro) {

        mostrarMensagem(
            erro.message,
            "erro"
        );
    }
}


async function exportarNotas() {

    const botao =
        document.getElementById("exportarNotas");

    if (botao.disabled) {
        return;
    }

    botao.disabled = true;

    const semana =
        seletorSemana.value;

    try {

        const resposta =
            await requisitar(
                "/apontamentos/exportar",
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    body: JSON.stringify({
                        semana: semana
                    })
                }
            );

        const arquivo =
            await resposta.blob();

        const url =
            URL.createObjectURL(arquivo);

        downloadRelatorio.href = url;

        downloadRelatorio.download =
            "logweek-" + semana + ".txt";

        downloadRelatorio.click();

        setTimeout(function () {
            URL.revokeObjectURL(url);
        }, 1000);

        mostrarMensagem(
            "Apontamento semanal gerado.",
            "sucesso"
        );

    } catch (erro) {

        mostrarMensagem(
            erro.message,
            "erro"
        );

    } finally {

        botao.disabled = false;
    }
}


async function sair() {

    window.location.href = "login.html";
}


async function iniciarPagina() {

    limparNota();

    atualizarSemanas();

    try {

        await carregarNotas();

    } catch (erro) {

        mostrarMensagem(
            erro.message,
            "erro"
        );
    }
}

