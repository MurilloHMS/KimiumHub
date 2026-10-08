-- Quem registrou a resposta no lugar do funcionário (2026-10-08): o login do RH
-- que recebeu o documento em papel ou anotou a resposta, para quem não tem
-- acesso ao portal. NULO = o próprio funcionário respondeu pelo portal.
--
-- Sem NOT NULL de propósito: o nulo tem significado, e as respostas que já
-- existem foram todas dadas pelo portal — ficam nulas, que é a verdade.
ALTER TABLE document_request_recipients
    ADD COLUMN registered_by VARCHAR(100);
