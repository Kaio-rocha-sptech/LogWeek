-- Conta de demonstracao: demo@email.com / Logweek123!
-- Guarda se a conta ainda nao existe, antes de inserir qualquer dado.
SET @criar_demo = (SELECT COUNT(*) FROM usuarios WHERE email = 'demo@email.com') = 0;

INSERT INTO usuarios (nome, email, senha)
SELECT 'Usuario Demo', 'demo@email.com', 'logweek-demo-2026:U5/ZbEr50OvHIRczdHG1P0snAcpDbs75oaZkiYag7po='
WHERE @criar_demo;

INSERT INTO apontamento (email_usuario, semana, titulo, conteudo, criado_em, inicio_em, fim_em)
SELECT 'demo@email.com', '31/08/2026 - 06/09/2026', 'Configuração inicial da VPC',
'Defini as sub-redes que serão utilizadas pela aplicação.',
TIMESTAMP '2026-08-31 10:02:00', TIMESTAMP '2026-08-31 09:14:00', TIMESTAMP '2026-08-31 10:02:00'
WHERE @criar_demo;

