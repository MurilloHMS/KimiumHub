ALTER TABLE profile ADD COLUMN employee_id UUID;

ALTER TABLE profile ADD CONSTRAINT fk_profile_employee
    FOREIGN KEY (employee_id) REFERENCES parceiros(id);

ALTER TABLE profile ADD CONSTRAINT uq_profile_employee
    UNIQUE (employee_id);
