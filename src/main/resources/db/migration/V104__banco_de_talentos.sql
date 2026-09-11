-- Banco de talentos: consentimento, prazo, e o token de acesso do candidato.
--
-- O banco de talentos NAO e tabela nova. Uma inscricao espontanea e um
-- `candidato` sem `candidatura` -- o `CandidaturaService` ja fazia
-- find-or-create por e-mail, e o curriculo ja e gravado como <id>.<ext> com
-- REPLACE_EXISTING. O que faltava era consentimento, prazo, e um jeito de a
-- pessoa voltar.
--
-- As duas alternativas foram recusadas com motivo:
--   * `vaga_id` anulavel em `candidaturas` quebraria o join fetch de
--     findCandidaturasByVagaId, o existsByCandidatoAndVaga, e a maquina de
--     estado inteira -- avancarEtapa levaria um candidato "aprovado" para
--     vaga nenhuma, e o kanban ganharia uma coluna fantasma.
--   * uma `Vaga` sentinela apareceria em /api/vaga/publicadas, seria encerrada
--     pelo VagaScheduler, e recusaria a segunda inscricao espontanea com
--     "Candidato ja se candidatou para essa vaga".

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. O candidato passa a carregar consentimento e prazo
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE candidatos ADD COLUMN area_interesse   VARCHAR(100);
ALTER TABLE candidatos ADD COLUMN consentimento_em TIMESTAMP;
ALTER TABLE candidatos ADD COLUMN expira_em        TIMESTAMP;
ALTER TABLE candidatos ADD COLUMN anonimizado_em   TIMESTAMP;
ALTER TABLE candidatos ADD COLUMN atualizado_em    TIMESTAMP;

-- Nao ha backfill de consentimento, e isso e decisao, nao esquecimento:
-- consentimento nao se inventa retroativamente. As 21 linhas que ja existem
-- ficam com consentimento_em NULL, que e um TERCEIRO ESTADO -- nem sim, nem
-- nao, mas "nao registrado". Elas chegaram por candidatura a vaga, finalidade
-- legitima por si so; o que falta e o aceite de PERMANECER para vagas futuras.
--
-- Consequencia obrigatoria no codigo: toda query de expurgo tem que escrever
--     WHERE expira_em IS NOT NULL AND expira_em < :agora
-- e nao so a comparacao. Hoje da no mesmo (NULL nunca satisfaz `<`), mas o dia
-- em que alguem "melhorar" com COALESCE(expira_em, criado_em + interval '2
-- years') apaga exatamente as linhas cujo consentimento nos nao temos.
--
-- `expira_em` guarda a DATA RESOLVIDA, e nao o periodo. Se guardasse o periodo,
-- mudar a retencao de 24 para 12 meses expiraria metade do banco numa
-- madrugada sem ninguem pedir. E a mesma regra do CareerHistory: snapshot
-- congelado, nunca referencia dinamica.

COMMENT ON COLUMN candidatos.consentimento_em IS
    'Quando a pessoa autorizou a permanencia no banco de talentos. NULL = nao registrado (linhas anteriores a 2026-09-11), e o expurgo nunca pode pega-las.';
COMMENT ON COLUMN candidatos.expira_em IS
    'Data resolvida, nao periodo: mudar a retencao nao pode reescrever o passado.';
COMMENT ON COLUMN candidatos.anonimizado_em IS
    'Lapide do "apagar meus dados" quando ha candidatura apontando para a linha.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. O token que deixa a pessoa voltar
-- ─────────────────────────────────────────────────────────────────────────────
--
-- Tabela nova, e nao `first_access_token`, porque aquela tem FK para
-- `parceiros` e candidato nao e Partner. Anular a FK e somar candidato_id
-- criaria uma tabela com duas chaves exclusivas e um "qual das duas e?" em todo
-- caminho de leitura -- a mesma forma de discriminador de que a Machine levou
-- uma migration inteira para sair.
--
-- Guarda o HASH, nao o token, como `public_secrets` ja faz. Este token abre o
-- dossie pessoal completo de alguem: um dump da tabela, um SELECT * num
-- plantao ou uma linha de log viraria chave-mestra de todos os candidatos.
-- (`first_access_token` guarda o token cru -- e o precedente pior dos dois.)
--
-- `revoked_at`, e nao `used`: a pessoa abre o link, le, procura o arquivo,
-- sobe, salva, e volta para corrigir o telefone. Token de uso unico morreria no
-- primeiro GET e o PUT responderia 410 -- erro que so aparece com gente de
-- verdade, porque quem testa le e fecha.

CREATE TABLE talent_bank_access_token (
    id           UUID PRIMARY KEY,
    token_hash   VARCHAR(64) NOT NULL UNIQUE,
    candidato_id UUID NOT NULL REFERENCES candidatos(id),
    expires_at   TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    revoked_at   TIMESTAMP
);

-- Serve as tres consultas quentes: revogar os anteriores ao emitir um novo,
-- o cooldown de 60s, e a limpeza.
CREATE INDEX ix_talent_token_candidato ON talent_bank_access_token (candidato_id);
