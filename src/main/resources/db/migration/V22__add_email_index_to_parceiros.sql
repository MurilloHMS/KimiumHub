CREATE INDEX idx_parceiros_email
    ON parceiros(email);

CREATE INDEX idx_registros_ponto_employee_data
    ON registros_ponto(employee_id, data);

CREATE INDEX idx_registros_ponto_employee_mesano
    ON registros_ponto(employee_id, mes_ano);

CREATE INDEX idx_registros_ponto_data
    ON registros_ponto(data);

CREATE INDEX idx_registros_ponto_mes_ano
    ON registros_ponto(mes_ano);