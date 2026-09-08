const API_URL = "http://localhost:8080";

function mostrarMensagem(texto, classe) {
  const mensagem = document.getElementById("mensagem");
  mensagem.textContent = texto;
  mensagem.className = "message " + (classe || "");
}

function dadosDoFormulario(formulario) {
  const dados = {};
  const campos = new FormData(formulario);

  campos.forEach(function(valor, chave) {
    dados[chave] = valor;
  });

  return dados;
}

function enviarDados(caminho, dados, aoConcluir) {
  const botao = document.querySelector('button[type="submit"]');
  if (botao.disabled) return;
  botao.disabled = true;
  return fetch(API_URL + caminho, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(dados)
  })
    .then(function(resposta) {
      if (!resposta.ok) {
        return resposta.json().catch(function() { return {}; }).then(function(erro) {
          throw new Error(erro.message || "Confira os dados informados.");
        });
      }

      return resposta.json();
    })
    .then(function(resultado) {
      mostrarMensagem("Dados enviados com sucesso.", "sucesso");

      if (aoConcluir) {
        aoConcluir(resultado);
      }
    })
    .catch(function(erro) {
      mostrarMensagem(erro.message === "Failed to fetch" ? "Nao foi possivel conectar. Verifique se o servidor esta ligado." : erro.message, "erro");
    }).finally(function() { botao.disabled = false; });
}

function entrar(formulario) {
  const dados = dadosDoFormulario(formulario);
  return enviarDados("/login", dados, function(resultado) {
    sessionStorage.setItem("usuarioLogado", JSON.stringify(resultado));
    window.location.href = "dashboard.html";
  });
}

function cadastrar(formulario) {
  const dados = dadosDoFormulario(formulario);
  return enviarDados("/usuarios", dados, function() {
    window.location.href = "login.html";
  });
}
