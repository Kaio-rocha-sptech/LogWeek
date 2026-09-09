# LogWeek

O **LogWeek** é uma aplicação web desenvolvida para facilitar a criação de apontamentos semanais.

A proposta é permitir que o usuário registre pequenas notas sobre suas atividades ao longo da semana e, posteriormente, gere automaticamente um relatório semanal organizado em arquivo `.txt`.

---

## 1. Funcionalidades

A aplicação permite:

* Criar uma conta;
* Realizar login;
* Manter o usuário autenticado durante o uso da aplicação;
* Criar apontamentos;
* Editar apontamentos existentes;
* Excluir apontamentos;
* Consultar apontamentos de semanas anteriores;
* Visualizar o período correspondente à semana selecionada;
* Registrar início e término das atividades;
* Gerar um relatório semanal;
* Exportar o relatório em `.txt`;
* Encerrar a sessão através do logout.

Os apontamentos são associados ao usuário autenticado, evitando que um usuário tenha acesso aos registros pertencentes a outra conta.

---

# 2. Arquitetura da aplicação

O LogWeek é dividido em duas partes principais:

```text
Frontend
HTML + CSS + JavaScript
        |
        | HTTP / JSON
        v
Backend
Java + Spring Boot
        |
        | JDBC
        v
Banco de dados
```

O frontend é responsável pela interface e pela interação do usuário.

O backend é responsável pelas regras da aplicação, autenticação, validações, acesso aos dados e geração do relatório semanal.

A comunicação entre frontend e backend acontece através de requisições HTTP utilizando `fetch()`.

---

# 3. Frontend

O frontend foi desenvolvido utilizando:

* HTML;
* CSS;
* JavaScript Vanilla;
* Node.js/Express para disponibilizar os arquivos localmente.

O servidor frontend é executado na porta:

```text
3000
```

A API Spring Boot é executada na porta:

```text
8080
```

Por isso, durante o desenvolvimento, a aplicação trabalha principalmente com:

```text
Frontend
http://localhost:3000

Backend
http://localhost:8080
```

---

## Estrutura do frontend

Os arquivos HTML são responsáveis por manter a maior parte da estrutura visual da aplicação.

O JavaScript é utilizado principalmente para:

* Capturar informações dos formulários;
* Fazer requisições para a API;
* Receber as respostas do backend;
* Preencher informações dinâmicas;
* Renderizar os apontamentos;
* Controlar o editor;
* Calcular informações relacionadas às semanas;
* Iniciar o download do relatório.

Essa separação foi escolhida para evitar a criação desnecessária de grandes estruturas HTML através do JavaScript.

---

## usuarios.js

O arquivo `usuarios.js` é responsável principalmente pelas funcionalidades relacionadas a:

* Cadastro;
* Login;
* Comunicação dos formulários com a API.

Os dados preenchidos pelo usuário são transformados em JSON e enviados através de `fetch()`.

Exemplo simplificado do fluxo:

```text
Formulário
    ↓
usuarios.js
    ↓
fetch()
    ↓
API Spring
    ↓
Resposta
```

Quando o login é realizado com sucesso, o usuário é redirecionado para o dashboard.

---

## dashboard.js

O `dashboard.js` concentra as funcionalidades relacionadas aos apontamentos.

Entre suas responsabilidades estão:

* Buscar apontamentos;
* Criar apontamentos;
* Editar apontamentos;
* Excluir apontamentos;
* Controlar a nota atualmente selecionada;
* Filtrar apontamentos por semana;
* Registrar horários;
* Atualizar informações da interface;
* Gerar o relatório semanal;
* Realizar logout.

As requisições são centralizadas em uma função auxiliar, evitando repetir a mesma configuração de `fetch()` em várias partes do código.

---

## Renderização dos apontamentos

A estrutura visual dos cards fica definida diretamente no HTML através de um:

```html
<template id="modeloNota">
```

O JavaScript copia esse modelo utilizando:

```javascript
cloneNode(true)
```

e preenche apenas as informações que mudam entre os apontamentos.

Dessa forma, a estrutura principal continua no HTML e o JavaScript fica responsável somente pelos dados dinâmicos.

As informações fornecidas pelo usuário são inseridas utilizando `textContent`.

---

# 4. Comunicação com a API

As requisições do frontend são enviadas diretamente para a API Spring Boot.

Exemplo:

```javascript
fetch(API_URL + caminho, opcoes)
```

Como frontend e backend utilizam portas diferentes, eles possuem origens diferentes para o navegador.

Por isso, os controllers utilizados pelo frontend possuem configuração CORS permitindo as origens locais utilizadas pelo projeto.

Exemplo:

```java
@CrossOrigin(
    origins = {
        "http://localhost:3000",
        "http://127.0.0.1:3000"
    },
    allowCredentials = "true"
)
```

O `allowCredentials` é necessário porque a aplicação utiliza uma sessão para manter o usuário autenticado.

---

# 5. Autenticação e sessão

O LogWeek utiliza sessão HTTP para identificar o usuário autenticado.

O fluxo ocorre da seguinte maneira:

```text
Usuário envia email e senha
        ↓
POST /usuarios/login
        ↓
Backend valida os dados
        ↓
Sessão é criada
        ↓
Navegador recebe o cookie da sessão
        ↓
Dashboard é aberto
        ↓
Próximas requisições enviam o cookie
        ↓
Backend identifica o usuário
```

Para que o navegador envie o cookie nas requisições entre as portas `3000` e `8080`, o frontend utiliza:

```javascript
credentials: "include"
```

Essa configuração deve estar presente tanto durante o login quanto nas requisições autenticadas seguintes.

O backend utiliza a sessão para determinar qual usuário está realizando a operação.

Dessa forma, os apontamentos são vinculados ao usuário autenticado no servidor.

---

# 6. Backend

O backend foi desenvolvido utilizando:

* Java;
* Spring Boot;
* Spring Web;
* JDBC;
* `JdbcTemplate`.

A aplicação não utiliza JPA ou Repository.

O acesso ao banco é realizado diretamente através do `JdbcTemplate`, mantendo o funcionamento mais simples e direto.

O fluxo principal é:

```text
Requisição HTTP
      ↓
Controller
      ↓
Validação
      ↓
JdbcTemplate
      ↓
SQL
      ↓
Banco
      ↓
Objeto Java
      ↓
JSON
      ↓
Frontend
```

---

# 7. Controllers

## UsuarioController

O `UsuarioController` concentra as operações relacionadas aos usuários.

Entre suas responsabilidades estão:

* Cadastro;
* Login;
* Identificação do usuário atual;
* Controle da sessão;
* Logout.

O login verifica as credenciais informadas e, quando são válidas, registra o usuário na sessão.

As próximas requisições podem utilizar essa sessão para identificar quem está utilizando a aplicação.

---

## ApontamentoController

O `ApontamentoController` concentra as operações relacionadas aos apontamentos.

Entre suas responsabilidades estão:

* Listar apontamentos;
* Buscar apontamentos;
* Criar apontamentos;
* Editar apontamentos;
* Excluir apontamentos;
* Exportar o relatório semanal.

Antes de realizar operações protegidas, o backend verifica se existe um usuário autenticado.

Caso não exista uma sessão válida, a API pode retornar:

```text
401 Unauthorized
```

Também é feita a verificação de propriedade para impedir que um usuário manipule apontamentos pertencentes a outra conta.

---

# 8. CRUD de apontamentos

O LogWeek implementa as quatro operações principais de um CRUD.

### Create

Criação de um novo apontamento:

```text
POST /apontamentos
```

### Read

Listagem dos apontamentos:

```text
GET /apontamentos
```

Também existe a consulta individual quando necessária:

```text
GET /apontamentos/{id}
```

### Update

Atualização de um apontamento:

```text
PUT /apontamentos/{id}
```

### Delete

Exclusão:

```text
DELETE /apontamentos/{id}
```

Depois das operações que alteram dados, o frontend atualiza a lista para apresentar o estado mais recente dos apontamentos.

---

# 9. Fluxo de criação de um apontamento

Quando o usuário começa a preencher uma nova nota, o frontend registra o horário inicial da atividade.

Ao salvar:

```text
Usuário escreve a nota
        ↓
JavaScript coleta os dados
        ↓
POST /apontamentos
        ↓
Backend identifica o usuário pela sessão
        ↓
Backend valida os dados
        ↓
JdbcTemplate executa INSERT
        ↓
Banco salva o apontamento
        ↓
API retorna o apontamento
        ↓
Frontend atualiza a interface
```

Quando uma nota já existente está selecionada, o frontend utiliza `PUT` em vez de `POST`.

---

# 10. Organização semanal

Uma das principais regras do LogWeek é a organização das notas por semana.

O sistema calcula o início da semana e utiliza essa informação para organizar os apontamentos.

A semana utilizada pelo sistema começa na:

```text
Segunda-feira
```

e termina no:

```text
Domingo
```

O frontend utiliza essas informações para apresentar períodos e permitir que o usuário consulte seus registros de diferentes semanas.

Parte dessa lógica fica no JavaScript para atualização da interface, enquanto o backend mantém as informações utilizadas oficialmente pelos apontamentos.

---

# 11. Horários

O sistema registra informações relacionadas ao período da atividade.

O início pode ser registrado quando o usuário começa a preencher a nota.

No primeiro salvamento, o backend determina o término da atividade.

Essas informações permitem apresentar a duração do apontamento e utilizá-la posteriormente no relatório semanal.

As funções relacionadas a horário e semana são uma das partes mais importantes da regra de negócio do LogWeek.

---

# 12. Relatório semanal

O usuário pode gerar um relatório contendo os apontamentos da semana selecionada.

A requisição utilizada é:

```text
POST /apontamentos/exportar
```

O backend recebe a semana, seleciona os apontamentos correspondentes e monta o conteúdo do relatório.

O resultado é retornado como texto.

O navegador transforma a resposta em um arquivo utilizando `Blob` e inicia o download.

O arquivo utiliza o formato:

```text
.txt
```

Essa abordagem permite gerar um relatório organizado sem utilizar bibliotecas externas de PDF ou documentos.

---

# 13. Principais endpoints

| Método | Endpoint                 | Responsabilidade               |
| ------ | ------------------------ | ------------------------------ |
| POST   | `/usuarios`              | Cadastrar usuário              |
| POST   | `/usuarios/login`        | Realizar login                 |
| GET    | `/usuarios/atual`        | Consultar usuário autenticado  |
| POST   | `/usuarios/logout`       | Encerrar sessão                |
| GET    | `/apontamentos`          | Listar apontamentos do usuário |
| GET    | `/apontamentos/{id}`     | Buscar apontamento             |
| POST   | `/apontamentos`          | Criar apontamento              |
| PUT    | `/apontamentos/{id}`     | Editar apontamento             |
| DELETE | `/apontamentos/{id}`     | Excluir apontamento            |
| POST   | `/apontamentos/exportar` | Gerar relatório semanal        |

Todos os endpoints relacionados aos apontamentos protegidos dependem da sessão do usuário.

---

# 14. Status HTTP

A API utiliza códigos HTTP para indicar o resultado das operações.

Entre os principais estão:

| Status             | Significado                                         |
| ------------------ | --------------------------------------------------- |
| `200 OK`           | Operação realizada com sucesso                      |
| `201 Created`      | Registro criado                                     |
| `204 No Content`   | Operação concluída sem conteúdo de resposta         |
| `400 Bad Request`  | Dados enviados são inválidos                        |
| `401 Unauthorized` | Usuário não está autenticado                        |
| `403 Forbidden`    | Usuário não possui permissão para acessar o recurso |
| `404 Not Found`    | Registro não encontrado                             |
| `409 Conflict`     | Conflito com um registro existente                  |

---

# 15. Segurança das consultas

As consultas SQL utilizam parâmetros através do `JdbcTemplate`.

Exemplo conceitual:

```java
jdbcTemplate.query(
    "SELECT * FROM apontamento WHERE email_usuario = ?",
    mapper,
    email
);
```

O valor recebido não é concatenado diretamente na String SQL.

Além disso, a identificação do usuário utilizada nas operações protegidas é controlada pelo backend através da sessão.

Isso evita depender somente de informações enviadas pelo navegador para determinar o proprietário de um apontamento.

---

# 16. Separação de responsabilidades

De forma resumida:

### HTML

Responsável pela estrutura das páginas.

### CSS

Responsável pela aparência e responsividade.

### JavaScript

Responsável por:

* Eventos;
* Requisições;
* Dados dinâmicos;
* Controle da interface.

### Spring Boot

Responsável por:

* API;
* Regras de negócio;
* Sessão;
* Validações;
* Comunicação com o banco;
* Relatório semanal.

### Banco de dados

Responsável pela persistência dos usuários e apontamentos.

---

# 17. Como executar

## Frontend

Na raiz do projeto:

```bash
npm install
npm start
```

Depois acesse:

```text
http://localhost:3000
```

---

## Backend

Entre no diretório do backend:

```bash
cd backend/logweek-api
```

No Windows:

```bash
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS:

```bash
./mvnw spring-boot:run
```

A API será disponibilizada em:

```text
http://localhost:8080
```

---

# 18. Fluxo completo

O funcionamento principal pode ser resumido da seguinte maneira:

```text
              USUÁRIO
                 |
                 v
           HTML / CSS
                 |
                 v
            JavaScript
                 |
              fetch()
                 |
                 v
          Spring Boot
                 |
          sessão + regras
                 |
                 v
           JdbcTemplate
                 |
                 v
          Banco de dados
```

No login:

```text
Login
  ↓
Backend valida
  ↓
Cria sessão
  ↓
Dashboard
```

Durante o uso:

```text
Dashboard
   ↓
cookie da sessão
   ↓
API identifica usuário
   ↓
CRUD dos apontamentos
```

E na exportação:

```text
Semana selecionada
       ↓
Backend reúne as notas
       ↓
Gera o relatório
       ↓
Resposta em texto
       ↓
Blob no navegador
       ↓
Arquivo .txt
```

---

# 19. Considerações finais

O LogWeek foi desenvolvido priorizando uma arquitetura simples e compatível com os conceitos trabalhados durante o desenvolvimento acadêmico.

O projeto utiliza tecnologias separadas de forma clara:

```text
HTML/CSS → estrutura e aparência
JavaScript → interação e requisições
Spring Boot → API e regras
JdbcTemplate → acesso aos dados
Banco → persistência
```

A aplicação evita abstrações desnecessárias e mantém o fluxo das funcionalidades visível no código, facilitando tanto a manutenção quanto a explicação técnica do projeto.

As principais regras de negócio estão relacionadas à autenticação por sessão, controle dos apontamentos de cada usuário, registro dos períodos das atividades, organização semanal e geração automática do apontamento semanal em `.txt`.
