DECLARE @CODPARC INT=0;
-- Um parceiro do ERP, pelo código.
--
-- Serve ao formulário de funcionário: a pessoa digita o CODPARC e a tela
-- preenche nome, documento e e-mail. Empresa, setor, cargo, nível, tipo de
-- contrato e data de admissão continuam sendo dela — o ERP não sabe nada disso.
--
-- SEM FILTRO DE CLIENTE OU DE ATIVO. Funcionário não é cliente, e um parceiro
-- inativo no ERP ainda pode estar sendo cadastrado aqui. Quem decide é quem
-- está preenchendo o formulário.
--
-- RTRIM em todo campo de texto: as colunas do TGFPAR são CHAR e vêm com espaço
-- à direita. Sem aparar, nenhum e-mail casa com o regex do value object Email.
--
-- O @CODPARC é montado em Java a partir de um int já convertido — nunca de
-- texto vindo da URL.
--
-- Esta consulta foi rodada contra a produção em 2026-09-09 e voltou o parceiro
-- certo: as sete colunas existem e os formatos são os esperados.
--
-- Só colunas já medidas contra a base em 2026-09-09. RAZAOSOCIAL ficou de fora
-- de propósito: nunca foi confirmada, e coluna inexistente derruba a consulta
-- inteira com um erro que fala de sintaxe.
SELECT PAR.CODPARC                    AS codigo,
       RTRIM(PAR.NOMEPARC)            AS nome,
       RTRIM(ISNULL(PAR.EMAIL, ''))   AS email,
       RTRIM(ISNULL(PAR.CGC_CPF, '')) AS documento,
       PAR.ATIVO                      AS ativo,
       PAR.CLIENTE                    AS cliente,
       PAR.TIPPESSOA                  AS tipo_pessoa
  FROM TGFPAR PAR
 WHERE PAR.CODPARC = @CODPARC
