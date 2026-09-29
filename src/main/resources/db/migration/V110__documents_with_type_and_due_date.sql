-- Documentos do funcionário: tipo, vencimento, substituição e alertas.
--
-- O tipo é uma TABELA, e não um enum: NR-10, NR-35, ASO periódico… o RH cria
-- tipo novo sem deploy. E é no tipo que moram os dias de aviso e quem recebe,
-- porque ASO e contrato têm prazos e responsáveis diferentes.

CREATE TABLE employee_document_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    notify_on_expiry BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE employee_document_type_alert_days (
    type_id UUID NOT NULL REFERENCES employee_document_types(id) ON DELETE CASCADE,
    days_before INT NOT NULL CHECK(days_before > 0),
    PRIMARY KEY (type_id, days_before)
);

CREATE TABLE employee_document_type_recipients (
  type_id UUID NOT NULL REFERENCES employee_document_types(id) ON DELETE CASCADE,
  employee_id UUID NOT NULL REFERENCES parceiros(id) ON DELETE CASCADE,
  PRIMARY KEY (type_id, employee_id)
);

-- Os quatro que ele citou. Sem dias e sem responsáveis: quem sabe o prazo de
-- cada um é o RH, e um padrão inventado aqui mandaria aviso para ninguém.
INSERT INTO employee_document_types(name) VALUES
    ('Contrato de trabalho'),
    ('Documento pessoal'),
    ('ASO'),
    ('NR');

-- Tipo nulo nos documentos que já existem (V59): eles aparecem como "Sem tipo"
-- e nunca geram alerta, que é o comportamento de hoje.
ALTER TABLE employee_documents
    ADD COLUMN type_id UUID REFERENCES employee_document_types(id),
    ADD COLUMN due_date DATE,
    ADD COLUMN replaced_by_id UUID REFERENCES employee_documents(id) ON DELETE SET NULL,
    ADD COLUMN replaced_at TIMESTAMP,
    ADD COLUMN uploaded_by VARCHAR(100),
    ADD COLUMN content_type VARCHAR(100),
    ADD COLUMN size_bytes BIGINT;

CREATE INDEX idx_employee_documents_type_id ON employee_documents(type_id);

-- O alerta que roda todo dia só olha quem tem vencimento e não foi substituído.
CREATE INDEX idx_employee_documents_due_date ON employee_documents(due_date)
    WHERE due_date IS NOT NULL AND replaced_by_id IS NULL;

-- Um aviso por documento, por vencimento e por marco. `due_date` entra na chave
-- de propósito: se o RH corrigir a data, os avisos voltam a valer para a data
-- nova. `days_before = -1` é o aviso do dia do vencimento.
CREATE TABLE employee_document_alerts_sent (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  document_id UUID NOT NULL REFERENCES employee_documents(id) ON DELETE CASCADE,
  due_date DATE NOT NULL,
  days_before INT NOT NULL,
  sent_at TIMESTAMP NOT NULL,
  UNIQUE (document_id, due_date, days_before)
);

-- A tela do RH. Aberta para os modelos RH e ADMIN e para quem já os tem, como
-- na V87: as células são criadas AQUI porque o Flyway roda antes da
-- sincronização do boot, e sem elas os UPDATEs abririam zero telas.

INSERT INTO screens (code, label, module, sort_order) VALUES
    ('rh/employee-documents', 'Documentos dos funcionários', 'Recursos Humanos', 55);

INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, 'rh/employee-documents', p.permission, FALSE
FROM permission_templates t
         CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                            ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

INSERT INTO user_permissions (user_id, screen_code, permission, allowed)
SELECT u.id, 'rh/employee-documents', p.permission, FALSE
FROM users u
         CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                            ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission)
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'CLIENTE'
);

UPDATE template_permissions SET allowed = TRUE
WHERE screen_code = 'rh/employee-documents'
  AND template_id IN (SELECT id FROM permission_templates WHERE name IN ('RH', 'ADMIN'));

UPDATE user_permissions SET allowed = TRUE
WHERE screen_code = 'rh/employee-documents'
  AND user_id IN (SELECT user_id FROM user_roles WHERE role IN ('RH', 'ADMIN'));