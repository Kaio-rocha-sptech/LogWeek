# Documentação Técnica

## 1. Visão geral

O LogWeek permite cadastrar uma conta, entrar, registrar notas sobre atividades, editar ou excluir essas notas, consultar semanas anteriores e gerar um apontamento semanal em arquivo `.txt`.

Cada nota contém título, observação opcional e, nas notas atuais, início e término. O início é registrado na primeira digitação; o término é definido pelo servidor ao salvar pela primeira vez. A semana é calculada a partir da data de início. Os dados de cada usuário são separados pela sessão autenticada.

A aplicação tem dois processos: Express serve arquivos na porta 3000; Spring Boot atende requisições de dados na porta 8080. O H2 funciona dentro do processo Java, sem servidor de banco separado.

## 2. Estrutura do projeto

| Local | Responsabilidade |
| --- | --- |
| `app.js`, `package.json`, `package-lock.json` | Servir as páginas e definir scripts e dependências Node. O lockfile fixa a árvore instalada. |
| `public/*.html` | Estrutura das páginas de apresentação, cadastro, login e dashboard. |
| `public/styles.css` | Aparência compartilhada e adaptação para telas pequenas. |
| `public/js/usuarios.js` | Formulários de cadastro e login. |
| `public/js/dashboard.js` | Estado do editor, horários, lista, filtro semanal, operações HTTP e download. |
| `backend/logweek-api/pom.xml` | Java 21, Spring Boot 4.1.1, JDBC, MVC, H2 e testes. |
| `backend/logweek-api/src/main/java/school/sptech/logweek_api/` | Inicialização, controllers, modelos e auxiliares de senha, tempo e relatório. |
| `backend/logweek-api/src/main/resources/` | Configuração e scripts SQL de inicialização. |
| `backend/logweek-api/src/test/` | Testes automatizados Java de API, datas e persistência. |
| `backend/logweek-api/dados/` | Banco persistido; não é código descartável. |
| `backend/logweek-api/.mvn/`, `mvnw`, `mvnw.cmd` | Maven Wrapper para executar o build sem instalar Maven manualmente. |

`node_modules/` e `target/` são dependências e resultados gerados. Os arquivos da IDE são configurações locais, ignoradas pelo Git. O `.gitignore` exclui dependências instaladas, resultados de compilação, dados locais, logs e configurações pessoais. O `.gitattributes` define finais de linha para uso entre Windows e Unix.

## 3. Backend

### Estrutura e fluxo

`LogweekApiApplication.main()` inicia o Spring Boot. O framework identifica os controllers pelas anotações e injeta `JdbcTemplate` nos construtores. Não há JPA, repositories ou camada de serviços: o SQL está nos métodos que atendem as requisições.

O fluxo usual é: JSON convertido em objeto → sessão e dados validados → SQL parametrizado executado por `JdbcTemplate` → resultado convertido em objeto → resposta JSON. Cada controller reutiliza um `BeanPropertyRowMapper`, que relaciona nomes SQL como `email_usuario` a propriedades Java como `emailUsuario`.

### Controllers

`UsuarioController` recebe cadastro e login, consulta o usuário atual e encerra sessões. Os métodos privados `nomeValido`, `emailValido` e `senhaValida` concentram as regras de cadastro. `emailDaSessao` recupera a identidade autenticada ou lança erro 401. O cadastro faz `INSERT` e busca o usuário inserido pelo email único. O login busca por email e confere o hash antes de abrir a sessão.

`ApontamentoController` lista, busca, cria, edita, exclui e exporta. `verificarUsuario` compara um email opcional com a sessão. `buscar` retorna 404 se o id não existe e 403 se pertence a outro usuário. `validar` verifica título e tamanho do conteúdo. `prepararHorarios` calcula o período de uma nota nova ou preserva o término de uma nota existente. A criação usa `GeneratedKeyHolder` para obter o id gerado e consultar a resposta completa.

### Modelos

`Usuario` representa `id`, `email`, `senha`, `nome` e `criadoEm`. A senha possui `@JsonProperty(WRITE_ONLY)`: pode entrar no JSON, mas não sai nas respostas, inclusive após leitura do hash no banco.

`Apontamento` representa os campos da tabela e expõe três propriedades calculadas: `inicioSemana` (segunda-feira em ISO), `dataApontamento` (data da atividade ou criação antiga) e `duracaoMinutos` (nula se não há período). Seus getters participam da serialização JSON; setters e construtores vazios são necessários ao mapeamento JDBC e à leitura dos corpos HTTP, mesmo sem chamadas explícitas no código.

### Validações e erros

- Nome: de 6 a 100 caracteres, somente letras e espaços; mínimo conferido após `trim`.
- Email: normalizado com `trim` e minúsculas, até 254 caracteres, um `@` e domínio entre `email.com`, `gmail.com`, `hotmail.com`, `outlook.com` e `sptech.school`. Essa lista é uma regra existente, não uma validação universal de emails.
- Senha: de 8 a 128 caracteres, pelo menos uma letra e um símbolo entre `@ ! # $ * & - _ = +`. Número não é obrigatório.
- Título: obrigatório, não pode ser branco e tem limite de 200 caracteres. Conteúdo: opcional, nulo vira texto vazio, limite de 100000 caracteres.
- Início: data/hora válida, menor ou igual ao término. O servidor ignora término enviado pelo cliente.
- Semana de exportação: obrigatória e reconhecida pelo parser; datas brasileiras impossíveis são rejeitadas com resolução estrita.

Erros esperados usam `ResponseStatusException`: 400 para entrada inválida, 401 para falta de autenticação, 403 para outro proprietário e 404 para id ausente. Email duplicado é capturado como `DuplicateKeyException` e retorna 409. O Spring trata JSON malformado e tipos de parâmetros inválidos. `server.error.include-message=always` permite ao frontend usar `message` da resposta; falhas internas não previstas continuam como 500.

## 4. Frontend

### Páginas e responsabilidades

`app.js` usa `express.static` com índice automático desativado e atende `/` com `login.html`. `index.html` continua disponível por URL explícita e oferece links de navegação; foi preservado porque é uma rota pública, mesmo sem links internos apontando para ela.

`login.html` e `cadastro.html` carregam `usuarios.js`. `FormData` coleta os campos com `name`, o JavaScript cria um objeto e o envia como JSON. O botão é bloqueado enquanto a requisição está em andamento. Cadastro bem-sucedido abre o login; login bem-sucedido abre o dashboard.

`dashboard.html` contém o formulário, horários, seletor de semana, lista e botões. O CSS define layout e responsividade. A estrutura de cada card (título, data, trecho, rodapé e botões) está no `<template id="modeloNota">` do HTML. Esse elemento é um modelo nativo: seu conteúdo não aparece até ser copiado. `renderizarNotas` copia o card com `cloneNode(true)`, preenche os campos e o `value` dos botões com o id da nota. Os eventos de editar/excluir estão nos atributos `onclick` do próprio template. As opções de semana continuam criadas pelo JavaScript porque sua quantidade e seus valores dependem dos dados recebidos. Títulos e observações são inseridos com `textContent`, sem interpretar HTML fornecido pelo usuário. Não há preview Markdown nem bibliotecas Marked/DOMPurify na versão atual.

### Estado do editor e comunicação

`notaSelecionadaId` decide entre POST e PUT; `inicioEm`, `fimEm` e `notaAntiga` controlam horários; `notasCarregadas` guarda a lista retornada pela API; `salvando` evita salvamentos concorrentes e impede trocar a seleção durante o salvamento.

Os dois scripts usam `http://localhost:8080` e `credentials: "include"`. As operações de dados não passam pelo Express. O dashboard centraliza respostas HTTP em `requisitar`: 401 limpa o cache do usuário e abre o login; outras falhas mostram a mensagem retornada. `sessionStorage` guarda apenas um cache visual dos dados do usuário; a API sempre usa a sessão do servidor para autorizar.

`iniciarPagina` prepara o editor vazio, consulta `/auth/me`, mostra o nome e carrega as notas. Ao salvar, `dadosDaNota` envia título, conteúdo, início e semana. Para notas atuais, a API recalcula a semana. O retorno preenche o editor com os valores oficiais e atualiza a lista para a semana salva. A exclusão pede confirmação no navegador e limpa o editor se a nota excluída estava selecionada.

### Eventos no HTML

Os eventos do frontend ficam nos atributos HTML, sem `addEventListener` ou atribuição de handlers pelo JavaScript. As funções continuam nos arquivos JavaScript já carregados pelas páginas; não é necessário colocar toda a lógica dentro de `<script>` no HTML.

| Evento no HTML | Função e comportamento |
| --- | --- |
| `body onload="iniciarPagina()"` | Consulta a sessão e carrega as notas no dashboard. Não há uma segunda chamada automática no final do JavaScript. |
| `onclick="atualizarLista()"` | Recarrega as notas pela API; erros aparecem na mensagem da página. |
| `onsubmit="entrar(this); return false;"` | Lê o formulário e envia POST de login. |
| `onsubmit="cadastrar(this); return false;"` | Lê o formulário e envia POST de cadastro. |
| `onsubmit="salvarApontamento(); return false;"` | Envia POST ou PUT da nota, conforme a seleção. |
| `onclick` nos botões do card | Usa o id guardado em `value` para editar a nota carregada ou excluí-la pela API. |
| `onclick="limparNota()"` | Limpa o editor para uma nova nota. |
| `onclick="exportarNotas()"` / `onclick="sair()"` | Solicita o relatório ou encerra a sessão. |
| `oninput="registrarInicio()"` | Registra o início na primeira digitação do título ou conteúdo. |
| `onchange="corrigirInicio()"` / `onchange="renderizarNotas()"` | Corrige o horário ou aplica a semana selecionada. |

Os formulários usam `onsubmit` para funcionar tanto pelo botão quanto pela tecla Enter, preservando a validação nativa de campos obrigatórios e email. `return false` impede o envio tradicional que recarregaria a página. `this` representa o formulário (no submit) ou o botão (nos cards). `Number(this.value)` converte o id do botão para número.

Fluxo: ação declarada no HTML → função JavaScript nomeada → `fetch` quando necessário → resposta tratada → campos da interface atualizados. Os helpers de requisição existentes continuam centralizando tratamento de erros e envio de credenciais.

## 5. Banco de dados

| Tabela | Campos e responsabilidades |
| --- | --- |
| `usuarios` | `id` identity e chave primária; `nome` obrigatório; `email` único; `senha` com hash; `criado_em` com data automática. |
| `apontamento` | `id` identity; `email_usuario` obrigatório; `semana`; `titulo`; `conteudo` CLOB; `criado_em`; `atualizado_em`; `inicio_em`; `fim_em`. |

A relação é 1:N: um usuário possui vários apontamentos. `apontamento.email_usuario` referencia `usuarios.email`; o índice `idx_apontamento_usuario` auxilia as consultas por proprietário. Não existe endpoint para alterar ou excluir usuários.

`schema.sql` cria tabelas e índice se ausentes. As colunas de início/fim são adicionadas com `ADD COLUMN IF NOT EXISTS` para compatibilidade com bancos antigos; remover esse trecho impediria a atualização desses bancos. `data.sql` insere a conta demo e uma nota apenas quando o email demo ainda não existe. Reiniciar não deve recriar notas demo excluídas nem sobrescrever edições.

O JDBC usa parâmetros `?` para dados do usuário, evitando montar SQL com texto recebido. A listagem ordena por `criado_em DESC, id DESC`. A edição atualiza título, conteúdo, início, semana e `atualizado_em`, preservando `fim_em`. A exclusão usa id e email da sessão. A exportação consulta todas as notas do usuário e filtra a semana em Java para também interpretar formatos antigos.

O arquivo do banco é relativo ao diretório de execução: `jdbc:h2:file:./dados/logweek`. Os testes HTTP Java usam bancos em memória distintos; `PersistenciaTests` usa um arquivo H2 temporário e fecha/reabre a aplicação normalmente. Não abra uma segunda instância contra o mesmo arquivo H2; use outra URL para testes ou pare a primeira instância de forma controlada.

## 6. Fluxo completo da aplicação

### Cadastro e entrada

Usuário preenche cadastro → HTML aplica restrições básicas → `usuarios.js` envia POST `/usuarios` → controller normaliza e valida → `SenhaUtil` gera hash → JDBC insere usuário → API retorna 201 sem senha → navegador abre login.

Usuário informa credenciais → POST `/login` → JDBC busca email → hash é conferido → sessão anterior é invalidada e uma nova sessão guarda `emailUsuario` → cookie é recebido → dashboard consulta `/auth/me` e `/apontamentos` com esse cookie.

### Registrar e corrigir atividade

Usuário começa a digitar → `registrarInicio` captura hora civil de São Paulo → usuário salva → POST `/apontamentos` → sessão define proprietário → API valida título e período, fixa término e calcula segunda-feira → INSERT → resposta 201 com id e campos calculados → editor passa a editar essa nota e lista acompanha a semana.

Ao corrigir a nota, o JavaScript envia PUT com o id. O backend verifica propriedade, conserva o término já salvo, atualiza os campos permitidos e retorna a nota atualizada. Conteúdo ausente em uma edição vira vazio; PUT não funciona como atualização parcial de todos os campos.

### Consultar, excluir e exportar

GET `/apontamentos` → notas ficam em memória no navegador → seletor filtra por `inicioSemana` → lista mostra somente a semana escolhida. Não há busca textual nem filtro SQL dinâmico implementados.

Excluir → confirmação → DELETE por id → verificação de propriedade → DELETE no banco → 204 → lista recarregada.

Gerar apontamento → POST `/apontamentos/exportar` com semana selecionada → notas da sessão são consultadas → relatório filtra, ordena, agrupa e soma → resposta de texto → navegador cria uma URL Blob temporária e a atribui ao link fixo do HTML → download `.txt` → URL temporária é liberada.

## 7. Funcionalidades mais complexas

### Autenticação e proteção de senha

**Responsabilidade:** identificar o usuário e separar seus dados.

**Onde está implementada:** `UsuarioController.validarLogin`, `emailDaSessao`, `sair`, `SenhaUtil`; configuração de sessão em `application.properties`; `requisitar` no dashboard.

**Como funciona:** o cadastro gera salt aleatório de 16 bytes. PBKDF2 com HMAC-SHA256, 600000 iterações e saída de 256 bits transforma a senha. O banco guarda o texto `salt:hash`: nas contas cadastradas, salt e hash são codificados em Base64; a conta demo usa um salt textual fixo no script SQL. O login recalcula o hash e compara com `MessageDigest.isEqual`. Após sucesso, uma nova sessão recebe o email. O cookie é HttpOnly e SameSite=Lax; a sessão expira após 30 minutos sem atividade. Logout invalida a sessão.

**Por que foi feita dessa maneira:** os detalhes criptográficos ficam fora do controller e a identidade fica no servidor. Assim, enviar outro email ou alterar o sessionStorage não dá acesso a notas alheias.

### Horários, duração e cálculo semanal

**Responsabilidade:** registrar o período e agrupar pela semana de início.

**Onde está implementada:** `TempoApontamento`, getters calculados de `Apontamento`, `ApontamentoController.prepararHorarios`; `registrarInicio`, `corrigirInicio`, `atualizarHorarios`, `inicioDaSemana` no dashboard.

**Como funciona:** frontend e backend usam hora civil de São Paulo e precisão de minutos. A segunda-feira é calculada subtraindo os dias decorridos desde segunda. Domingo pertence à semana anterior. O frontend usa operações UTC ao calcular dias civis para não deslocar datas pelo fuso do computador. O backend fixa o término na criação; futuras edições podem corrigir o início, mas não reabrir o período. Uma nota que atravessa a meia-noite ou outra semana fica integralmente na semana de início.

**Por que foi feita dessa maneira:** evita duplicar períodos a cada salvamento e mantém frontend, JSON e relatório com uma semana comum. A interface permite corrigir a hora, mas não oferece um seletor de data para retroagir a atividade.

### Compatibilidade com notas antigas

**Responsabilidade:** manter acessíveis registros sem início/fim e semanas em texto.

**Onde está implementada:** `TempoApontamento.lerSemana`, `semanaDaNota`, `dataDaNota`; `prepararHorarios`; `RelatorioSemanal.textoSimples`; `preencherNota` no dashboard.

**Como funciona:** o parser aceita data ISO, início de período brasileiro e formatos anteriores como `24 ago - 30 ago 2026`. Para registros sem início, conserva a semana salva; se ela não puder ser interpretada, usa a semana da criação. Edição de nota antiga conserva a semana e não inventa horários. A exportação remove marcações antigas somente do texto exportado, sem regravar o conteúdo no banco. Se criação e semana divergem, o relatório usa um grupo específico de registros sem data da atividade.

**Por que foi feita dessa maneira:** a versão antiga não registrava períodos completos; a aplicação preserva os dados conhecidos em vez de atribuir tempos fictícios.

### Relatório semanal e download

**Responsabilidade:** produzir um resumo legível com as atividades salvas.

**Onde está implementada:** `ApontamentoController.exportar`, `RelatorioSemanal.gerar`, `TempoApontamento.duracao`, `exportarNotas` no dashboard.

**Como funciona:** escolhe notas da semana, soma durações disponíveis, ordena por data, início e id, agrupa por dia e escreve título e observação. Quando há passagem da meia-noite, inclui a data do término. Notas sem período mostram “Horário não registrado” e não acrescentam minutos. O cabeçalho e o resumo informam quantidade, duração total e período. Semana vazia produz relatório com zero notas. O arquivo chama-se `logweek-AAAA-MM-DD.txt` e usa UTF-8.

**Por que foi feita dessa maneira:** usa texto simples, sem dependência de geração de documentos, e mantém o processamento oficial no backend. Apenas notas salvas entram no arquivo; rascunhos não são enviados na exportação.

## 8. Principais endpoints

Base: `http://localhost:8080`, sem prefixo adicional. Campos opcionais de email precisam coincidir com a sessão.

| Método | Endpoint | Responsabilidade | Entrada | Retorno |
| --- | --- | --- | --- | --- |
| POST | `/usuarios` | Cadastrar | JSON `nome`, `email`, `senha` | 201 usuário sem senha; 400/409 |
| POST | `/login` | Abrir sessão | JSON `email`, `senha` | 200 usuário e cookie; 400/401 |
| GET | `/auth/me` | Consultar sessão | Cookie | 200 usuário; 401 |
| POST | `/auth/logout` | Encerrar sessão | Cookie, se houver; sem corpo | 204, inclusive sem sessão |
| GET | `/apontamentos` | Listar notas próprias | Cookie; query `emailUsuario` opcional | 200 array, inclusive vazio; 401/403 |
| GET | `/apontamentos/{id}` | Buscar uma nota | Cookie e id inteiro | 200 nota; 400/401/403/404 |
| POST | `/apontamentos` | Criar nota | Cookie; JSON `titulo` obrigatório; `conteudo`, `inicioEm` e `emailUsuario` opcionais | 201 nota; 400/401/403 |
| PUT | `/apontamentos/{id}` | Editar nota | Cookie, id, título obrigatório; conteúdo e início conforme regras | 200 nota; 400/401/403/404 |
| DELETE | `/apontamentos/{id}` | Excluir nota | Cookie e id | 204 sem corpo; 400/401/403/404 |
| POST | `/apontamentos/exportar` | Gerar TXT semanal | Cookie; JSON `semana`; `emailUsuario` opcional | 200 `text/plain;charset=UTF-8` com attachment; 400/401/403 |

Na criação, **título é obrigatório**; apenas conteúdo e início são opcionais. Sem início, o servidor usa o próprio término e a duração fica zero. Semana é calculada pelo servidor para notas atuais, mesmo que venha no corpo. Nas notas antigas, a edição preserva a semana original.

Exemplo de criação:

```json
{"titulo":"Configuração da rede","conteudo":"Revisei as sub-redes.","inicioEm":"2026-09-07T14:00"}
```

O horário precisa ser anterior ou igual ao momento do primeiro salvamento. A resposta inclui `id`, `emailUsuario`, `semana`, `titulo`, `conteudo`, `criadoEm`, `atualizadoEm`, `inicioEm`, `fimEm`, `inicioSemana`, `dataApontamento` e `duracaoMinutos`. Campos de data/hora persistidos podem vir como texto SQL com espaço; o frontend normaliza a parte usada no editor.

CORS permite `http://localhost:3000` com credenciais nos dois controllers. A rota `/h2-console` é a interface administrativa local do H2, não um endpoint de negócio.

## 9. Como executar o projeto

1. Instale JDK 21 e Node.js 22 ou superior com npm; verifique `java -version`, `node --version` e `npm --version`. A revisão usou Java 21.0.11 e Node 25.9.0.
2. Na raiz, execute `npm install` (ou `npm ci` para instalação exata pelo lockfile) e `npm start`.
3. Em outro terminal, entre em `backend/logweek-api` e execute `.\mvnw.cmd spring-boot:run`. Em sistemas Unix, use `sh mvnw spring-boot:run`.
4. Aguarde a inicialização Java. O H2 será aberto e os scripts SQL serão executados automaticamente. Não é necessário instalar outro banco nem executar SQL manualmente.
5. Abra [LogWeek](http://localhost:3000), cadastre uma conta ou use `demo@email.com` / `Logweek123!`.
6. No IntelliJ, importe o `pom.xml`, escolha JDK 21, configure o diretório de trabalho para `backend/logweek-api` e execute `LogweekApiApplication`.

As portas 3000 e 8080 precisam estar disponíveis. Abrir o HTML por `file://`, usar `127.0.0.1` ou mudar uma porta sem ajustar a base da API e o CORS não corresponde à configuração atual.

O H2 Console fica em [Console local](http://localhost:8080/h2-console): JDBC `jdbc:h2:file:./dados/logweek`, usuário `sa`, senha vazia. A configuração está em `application.properties`. Para testes externos, uma instância pode receber `--server.port=18080` e `--spring.datasource.url=jdbc:h2:file:CAMINHO_TEMPORARIO/logweek`, sem alterar esse arquivo.

Para compilar, testar e empacotar:

```powershell
# Raiz
npm run check
# Backend
cd backend/logweek-api
.\mvnw.cmd clean verify
# Executar o pacote gerado, ainda no diretório do backend
java -jar target/logweek-api-0.0.1-SNAPSHOT.jar
```

O primeiro uso do Wrapper/build requer acesso ao Maven Central. Falha de rede não significa falha de código; em ambiente restrito é necessário permitir o download. Verifique se `node` e `npm` pertencem à mesma instalação caso o terminal não encontre o npm.

## 10. Observações importantes

- A versão atual é configurada para execução local: console H2 habilitado, conta demo, HTTP e origem fixa. Publicação exige uma configuração própria para essas condições; não foi realizada nesta revisão.
- A API carrega todas as notas do usuário; o filtro visual e o filtro do relatório são feitos em memória. Não há paginação ou busca textual.
- Durações são somadas sem descontar sobreposição entre notas. Isso preserva a regra atual. Tempos representam hora civil, sem offset armazenado; o editor calcula duração com `-03:00`, adequado às atividades atuais, mas não modela mudanças históricas de horário de verão.
- `criado_em` e `atualizado_em` vêm do relógio do banco; início/fim usam São Paulo. Em máquinas com outro fuso, esses campos podem representar convenções distintas, especialmente em registros antigos.
- O parser legado usa a data inicial para identificar a semana; o final do texto não é uma segunda validação de intervalo. Para novas integrações, prefira `semana` ISO da segunda-feira retornada como `inicioSemana`.
- O frontend remove espaços das extremidades do conteúdo ao salvar. A API aceita conteúdo vazio e trata ausência como vazio também em PUT.
- Navegar para outra nota, iniciar uma nova ou sair não oferece confirmação para descartar rascunho. Não há salvamento automático.
- SQL de compatibilidade, getters/setters usados por reflexão, Maven Wrapper, lockfile e página `index.html` fazem parte do projeto. O banco e as configurações da IDE são apenas locais e ficam fora do versionamento.
- Os testes Java de integração executam HTTP real e usam bancos isolados. Não há uma suíte JavaScript no repositório: `npm run check` verifica apenas a sintaxe de `app.js`, `usuarios.js` e `dashboard.js`.


### Convenções de nomes

**LogWeek** é o nome apresentado ao usuário. `logweek` e `logweek-api` são identificadores técnicos usados no pacote npm, artefato Maven, banco e arquivos exportados. `LogweekApiApplication` e `school.sptech.logweek_api` mantêm os nomes do código Java. `LOGWEEK` é a versão em maiúsculas usada no cabeçalho do relatório e no painel.

Uma **nota** é o registro individual de uma atividade; **apontamento semanal** é o relatório que reúne essas notas. O nome técnico `/apontamentos` da API e a tabela `apontamento` representam os registros individuais e foram preservados para manter o contrato da aplicação.

### Verificação para distribuição

Execute `npm ci` e `npm run check` na raiz, e `mvnw.cmd clean verify` dentro do backend (ou `sh mvnw clean verify` em Unix). Uma instalação nova não precisa receber o arquivo do banco: `spring.sql.init.mode=always` executa os scripts idempotentes de criação e dados demo. `PersistenciaTests` verifica esse fluxo com um arquivo temporário e confere a preservação de edição e exclusão ao reabrir a aplicação.

A configuração também explicita UTF-8 dos scripts SQL, mensagens de erro enviadas ao frontend e as propriedades de sessão descritas acima. Não são necessárias configurações pessoais de IDE para executar pelo terminal.

Validação em 08/09/2026: instalação das dependências pelo lockfile em diretório temporário e `npm run check` aprovados; build `clean verify` aprovado com 8 testes Java, sem falhas. Foram conferidas as regras de exclusão do Git para banco, dependências e arquivos locais. A verificação de sintaxe não substitui testes funcionais do frontend; não há suíte JavaScript versionada. O Maven emitiu avisos de autoanexação Mockito/Byte Buddy no JDK 21, sem impedir os testes.
