CREATE TABLE registros_ponto (
                                 id UUID NOT NULL PRIMARY KEY,
                                 employee_id UUID NOT NULL,
                                 data DATE NOT NULL,
                                 entrada TIME,
                                 almoco_saida TIME,
                                 almoco_retorno TIME,
                                 saida TIME,
                                 mes_ano VARCHAR(7) NOT NULL,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_registros_ponto_employee
                                     FOREIGN KEY (employee_id)
                                         REFERENCES parceiros(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT uk_employee_data
                                     UNIQUE (employee_id, data)
);

CREATE INDEX idx_registros_ponto_employee_data
    ON registros_ponto(employee_id, data);

CREATE INDEX idx_registros_ponto_employee_mesano
    ON registros_ponto(employee_id, mes_ano);

CREATE INDEX idx_registros_ponto_data
    ON registros_ponto(data);

CREATE INDEX idx_registros_ponto_mes_ano
    ON registros_ponto(mes_ano);