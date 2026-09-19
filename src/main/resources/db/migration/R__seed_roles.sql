INSERT INTO roles (nombre)
VALUES ('CLIENTE'),
       ('ADMIN')
ON CONFLICT (nombre) DO NOTHING;
