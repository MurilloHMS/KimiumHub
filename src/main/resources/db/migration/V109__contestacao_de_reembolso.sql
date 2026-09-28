-- Contestação de reembolso recusado: uma só, até 30 dias depois da recusa.
--
-- O pedido é o MESMO: volta a "em análise" com um comprovante novo e um
-- comentário. O que a primeira análise decidiu vai para as colunas first_*, e
-- o comprovante anterior para original_receipt_* — nada é apagado, porque o
-- comprovante que a diretoria vê precisa ter a trilha inteira.
--
-- contested_at não nulo é o "já contestou": é ele que impede a segunda vez.
ALTER TABLE reimbursements
    ADD COLUMN contested_at                  TIMESTAMP,
    ADD COLUMN contest_comment               VARCHAR(500),
    ADD COLUMN original_receipt_filename     VARCHAR(255),
    ADD COLUMN original_receipt_storage_path VARCHAR(500),
    ADD COLUMN first_reviewed_by_id          UUID REFERENCES parceiros(id),
    ADD COLUMN first_reviewed_at             TIMESTAMP,
    ADD COLUMN first_review_notes            VARCHAR(500);
