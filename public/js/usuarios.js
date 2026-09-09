const API_URL = "http://localhost:8080";

function mostrarMensagem(texto, classe) {
    const mensagem = document.getElementById("mensagem");

    mensagem.textContent = texto;
    mensagem.className = "message " + (classe || "");
}

async function enviarDados(formulario, caminho, dados) {

    const resposta = await fetch(API_URL + caminho, {
        method: "POST",

        credentials: "include",

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify(dados)
    });

    if (!resposta.ok) {
        const erro = await resposta.json().catch(function () {
            return {};
        });

        throw new Error(
            erro.message || "Confira os dados informados."
        );
    }

    return await resposta.json();
}

async function entrar(formulario) {
    const usuario = await enviarDados(
        formulario,
        "/usuarios/login",
        {
            email: formulario.elements.email.value,
            senha: formulario.elements.senha.value
        }
    );

    if (usuario) {
        window.location.href = "dashboard.html";
    }
}

async function cadastrar(formulario) {
    const usuario = await enviarDados(
        formulario,
        "/usuarios",
        {
            nome: formulario.elements.nome.value,
            email: formulario.elements.email.value,
            senha: formulario.elements.senha.value
        }
    );

    if (usuario) {
        window.location.href = "login.html";
    }
}