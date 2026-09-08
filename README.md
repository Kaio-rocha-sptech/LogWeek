# LogWeek

**Registre suas atividades ao longo da semana e transforme suas notas em um apontamento semanal organizado.**

O LogWeek é um projeto acadêmico que ajuda a reunir o que foi feito durante a semana em um só lugar. Em vez de depender da memória para escrever o relatório no final do período, você pode registrar cada atividade enquanto trabalha e gerar um arquivo de texto com as notas salvas.

## Como o LogWeek facilita os apontamentos

- **Notas rápidas:** registre um título e, quando necessário, uma observação sobre a atividade.
- **Horários automáticos:** o início é marcado na primeira digitação e o término no primeiro salvamento. É possível corrigir a hora de início.
- **Organização por semana:** as atividades são agrupadas pela semana de início, de segunda-feira a domingo.
- **Histórico acessível:** selecione uma semana para consultar, editar ou excluir suas notas.
- **Relatório pronto para compartilhar:** gere um `.txt` com atividades agrupadas por dia, horários, quantidade de notas e soma do tempo registrado.
- **Dados por usuário:** cada conta acessa seus próprios apontamentos, que permanecem salvos ao encerrar normalmente a aplicação.

### Um exemplo de uso

1. Entre na sua conta e comece uma nota, como “Configuração da rede”.
2. Acrescente uma observação sobre o que fez e salve ao terminar.
3. Repita ao longo da semana.
4. Selecione a semana desejada e clique em **Gerar apontamento semanal**.

O arquivo reúne as notas já salvas. Rascunhos não entram no relatório; não há salvamento automático. Durações de atividades simultâneas são somadas sem descontar sobreposições.

## Tecnologias

| Parte | Tecnologias |
| --- | --- |
| Frontend | HTML, CSS e JavaScript vanilla, com eventos declarados no HTML |
| Servidor das páginas | Node.js e Express 5.2.1 |
| Backend | Java 21, Spring Boot 4.1.1 e JdbcTemplate |
| Banco de dados | H2 em arquivo, iniciado junto com o backend |
| Relatório | Texto simples em UTF-8 (`.txt`) |

## Executar localmente

Requisitos: **JDK 21**, **Node.js 22 ou superior** com npm e acesso à internet para baixar dependências na primeira execução. Os comandos devem ser executados na pasta do projeto baixado ou clonado.

### 1. Iniciar o frontend

Na raiz do projeto:

```sh
npm ci
npm start
```

### 2. Iniciar o backend

Em outro terminal, no Windows:

```powershell
cd backend/logweek-api
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```sh
cd backend/logweek-api
sh mvnw spring-boot:run
```

O Maven Wrapper acompanha o projeto; não é necessário instalar Maven separadamente.

### 3. Abrir a aplicação

Acesse [LogWeek](http://localhost:3000). A API atende em `http://localhost:8080`. Utilize **localhost** nas duas portas para manter a configuração de sessão e CORS.

Cadastre uma conta ou use a conta de demonstração:

- Email: `demo@email.com`
- Senha: `Logweek123!`

O banco e a conta demo são criados automaticamente quando necessários. Execute o backend dentro de `backend/logweek-api`: os dados ficam em `dados/logweek.mv.db`, relativo a esse diretório. No IntelliJ, importe o `pom.xml`, selecione JDK 21, use esse diretório de trabalho e execute `LogweekApiApplication`.

O banco local não vai para o GitHub. Não apague o arquivo de dados caso queira preservar suas notas. As portas 3000 e 8080 precisam estar disponíveis.

## Estrutura do repositório

```text
logweek/
├── README.md                      # Apresentação e execução
├── DOCUMENTACAO_TECNICA.md         # Fluxos, regras e endpoints
├── .gitignore                     # Arquivos locais fora do versionamento
├── .gitattributes                 # Finais de linha entre sistemas
├── package.json
├── package-lock.json
├── app.js                         # Servidor das páginas
├── public/
│   ├── index.html                 # Apresentação, acessível por /index.html
│   ├── login.html                 # Página atendida em /
│   ├── cadastro.html
│   ├── dashboard.html
│   ├── styles.css
│   └── js/
│       ├── usuarios.js
│       └── dashboard.js
└── backend/logweek-api/
    ├── pom.xml
    ├── mvnw / mvnw.cmd / .mvn/    # Maven Wrapper
    └── src/
        ├── main/java/             # Controllers, modelos e auxiliares
        ├── main/resources/        # Configuração, schema.sql e data.sql
        └── test/java/             # Testes automatizados do backend
```

A estrutura mantém o frontend simples e o SQL próximo dos controllers. A [Documentação Técnica](DOCUMENTACAO_TECNICA.md) explica o caminho de uma ação no HTML até o banco e a resposta exibida na tela.

## Verificações

Na raiz, verifique a sintaxe dos arquivos JavaScript:

```sh
npm run check
```

Esse comando verifica sintaxe; não executa testes funcionais do frontend. Atualmente não há suíte JavaScript versionada.

No backend, compile e execute os testes:

```powershell
cd backend/logweek-api
.\mvnw.cmd clean verify
```

No Linux/macOS, use `sh mvnw clean verify`. Os testes usam H2 em memória ou arquivo temporário isolado e não alteram suas notas. O comando gera o pacote `target/logweek-api-0.0.1-SNAPSHOT.jar`.

## Arquivos locais e configuração

`node_modules/`, `target/`, bancos H2, logs, configurações de IDE e arquivos `.env` são ignorados pelo Git. O lockfile npm, o Maven Wrapper e os scripts SQL fazem parte do repositório e permitem reconstruir a aplicação em outra máquina.

O projeto está configurado para estudo e execução local, com conta demo e [H2 Console](http://localhost:8080/h2-console) habilitados. No console: JDBC `jdbc:h2:file:./dados/logweek`, usuário `sa`, senha vazia. Hospedar a aplicação exige configurar ambiente, origem da API e acesso ao banco; o GitHub hospeda o código, não executa este backend automaticamente.
