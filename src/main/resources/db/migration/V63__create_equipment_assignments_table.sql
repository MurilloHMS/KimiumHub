CREATE TABLE equipment_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    equipment_type VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    delivered_at DATE NOT NULL,
    returned_at DATE,
    notes VARCHAR(500)
);

CREATE INDEX idx_equipment_assignments_employee_id ON equipment_assignments(employee_id);
