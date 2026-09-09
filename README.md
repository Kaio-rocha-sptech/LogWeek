# 📝 LogWeek

> **Registre durante a semana. Organize com um clique.**

O **LogWeek** foi criado para tornar os apontamentos semanais mais simples.

Em vez de chegar ao fim da semana tentando lembrar **o que você fez, quando fez e quanto tempo levou**, você pode registrar pequenas notas enquanto trabalha.

No final, o LogWeek reúne seus registros e transforma tudo em um **apontamento semanal organizado e pronto para compartilhar**.

---

## 💡 Por que usar o LogWeek?

Imagine que durante a semana você:

* configurou uma rede;
* participou de uma reunião;
* corrigiu um problema;
* estudou uma nova tecnologia;
* desenvolveu uma funcionalidade.

Na sexta-feira, lembrar de todos esses detalhes pode ser difícil.

Com o LogWeek, você registra cada atividade quando ela acontece e deixa a organização para depois.

```text id="p3z1ax"
Durante a semana                  No final da semana

📝 Pequenas notas                   📋 Apontamento organizado
🕐 Horários registrados      →       📅 Separado por dia
📌 Atividades salvas                ⏱️ Tempo registrado
                                    📄 Arquivo .txt
```

---

## ✨ O que você pode fazer?

### 📝 Registre atividades rapidamente

Crie uma nota com um título simples e, quando precisar, acrescente uma observação.

Por exemplo:

```text id="4w0w1e"
Configuração da VPC

Configurei as sub-redes pública e privada e
revisei algumas regras de comunicação entre
as instâncias.
```

A ideia é não precisar escrever um relatório completo toda vez que terminar uma atividade.

---

### ⏱️ Deixe o LogWeek cuidar dos horários

Ao começar a escrever uma nova atividade, o LogWeek registra o horário inicial.

Quando ela é salva pela primeira vez, o término também é registrado.

Se necessário, você ainda pode corrigir o horário de início.

Assim, você pode se concentrar em registrar **o que fez**, sem precisar controlar manualmente cada horário.

---

### 📅 Acompanhe suas semanas

Seus registros são organizados semanalmente, considerando:

**Segunda-feira → Domingo**

Você pode navegar entre as semanas e consultar as atividades que já registrou.

Isso cria um pequeno histórico do seu trabalho e evita que informações importantes dependam apenas da memória.

---

### ✏️ Corrija quando precisar

Esqueceu alguma informação?

Você pode voltar a um apontamento salvo para:

* editar o título;
* alterar a observação;
* corrigir informações;
* ou excluir o registro.

As alterações ficam refletidas no seu histórico.

---

### 📄 Gere seu apontamento semanal

Depois de registrar suas atividades ao longo da semana, basta selecionar o período desejado e clicar em:

**Gerar apontamento semanal**

O LogWeek reúne as notas salvas e cria automaticamente um arquivo `.txt` organizado.

O relatório pode reunir informações como:

```text id="q56w9z"
LOGWEEK
Apontamento semanal
01/09/2026 - 07/09/2026

SEGUNDA-FEIRA

09:00 - 10:20
Configuração da rede

Revisei as sub-redes e finalizei
a comunicação entre as instâncias.


TERÇA-FEIRA

14:10 - 15:00
Correção da API

Corrigi a validação utilizada durante
o cadastro de usuários.


RESUMO DA SEMANA

Atividades registradas: 2
Tempo registrado: 2h10min
```

Assim, várias pequenas anotações feitas durante a semana se transformam em um único documento.

---

## 🔐 Seus apontamentos ficam na sua conta

O LogWeek possui cadastro e login.

Cada usuário possui seus próprios apontamentos, então ao entrar na aplicação você visualiza apenas as atividades relacionadas à sua conta.

Você também pode encerrar sua sessão através do botão de logout.

---

## 🚀 Como usar

O fluxo foi pensado para ser simples:

```text id="b9jq94"
1. Entre na sua conta
        ↓
2. Comece uma nova nota
        ↓
3. Descreva rapidamente sua atividade
        ↓
4. Salve
        ↓
5. Continue registrando durante a semana
        ↓
6. Escolha a semana desejada
        ↓
7. Gere seu apontamento
```

Você não precisa escrever tudo de uma vez.

A proposta do LogWeek é justamente transformar **pequenos registros feitos naturalmente durante o trabalho** em um relatório mais completo no final da semana.

---

## 🎯 Para quem é o LogWeek?

O projeto pode ser útil para pessoas que precisam registrar atividades recorrentes, como:

* estudantes;
* estagiários;
* desenvolvedores;
* profissionais de tecnologia;
* equipes que realizam apontamentos;
* pessoas que precisam prestar contas das atividades realizadas durante a semana.

Principalmente para quem já pensou:

> *"O que mesmo eu fiz durante essa semana?"*

---

## ⚠️ Algumas coisas importantes

O LogWeek trabalha com as **notas que você salvou**.

Por isso, uma atividade que ainda está sendo escrita e não foi salva não aparecerá no relatório semanal.

Também não existe salvamento automático no momento.

Se duas atividades tiverem horários sobrepostos, seus tempos são contabilizados individualmente.

---

## 🎓 Sobre o projeto

O LogWeek é um **projeto acadêmico** desenvolvido com o objetivo de aplicar conceitos de desenvolvimento web na solução de um problema simples do cotidiano.

A aplicação utiliza:

* HTML;
* CSS;
* JavaScript;
* Java;
* Spring Boot;
* JDBC;
* H2;
* Node.js e Express.

O projeto foi construído priorizando uma interface simples e um fluxo rápido para registrar atividades.

---

## 💻 Executando o projeto localmente

> Esta seção é destinada a quem deseja executar o código do LogWeek.

### Requisitos

Você precisará ter instalado:

* **JDK 21**
* **Node.js 22 ou superior**
* **npm**

### 1. Inicie as páginas

Na raiz do projeto:

```sh
npm ci
npm start
```

### 2. Inicie a API

Em outro terminal:

**Windows**

```powershell
cd backend/logweek-api
.\mvnw.cmd spring-boot:run
```

**Linux/macOS**

```sh
cd backend/logweek-api
sh mvnw spring-boot:run
```

### 3. Abra o LogWeek

Com os dois serviços funcionando, acesse:

**http://localhost:3000**

O banco de dados é preparado automaticamente na primeira execução.

Para testar rapidamente a aplicação, também existe uma conta de demonstração:

```text id="kg8p9m"
Email: demo@email.com
Senha: Logweek123!
```

---

## 📚 Quer entender como ele funciona por dentro?

Este README apresenta o LogWeek principalmente pela perspectiva de quem utiliza a ferramenta.

A arquitetura, endpoints, banco de dados, autenticação, regras de negócio e funcionamento interno estão documentados separadamente em:

**`DOCUMENTACAO_TECNICA.md`**

---

## 🌱 Objetivo

O LogWeek nasceu de uma ideia simples:

**você não deveria precisar reconstruir sua semana inteira de memória para conseguir escrever um apontamento.**

Registre enquanto acontece.

No final da semana, deixe o **LogWeek** organizar para você.
