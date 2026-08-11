CREATE TABLE machine_alert_config (
    id UUID PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    alert_when_late BOOLEAN NOT NULL DEFAULT TRUE,
    send_at TIME NOT NULL DEFAULT '08:00'
);

CREATE TABLE machine_alert_days (
  config_id UUID NOT NULL REFERENCES machine_alert_config(id) ON DELETE CASCADE,
  days_before INT NOT NULL
);

CREATE TABLE machine_alert_recipients (
    config_id UUID NOT NULL REFERENCES machine_alert_config(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL
);

CREATE TABLE machine_alert_sent (
  id UUID PRIMARY KEY,
  register_id UUID NOT NULL REFERENCES machine_registers(id) ON DELETE CASCADE,
  alert_date DATE NOT NULL,
  days_before INT NOT NULL,
  UNIQUE(register_id, alert_date, days_before)
);