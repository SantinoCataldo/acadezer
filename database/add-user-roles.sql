-- Existing academic databases: preserves users, passwords and academic content.
-- Every existing user starts as an enabled STUDENT. No account becomes ADMIN.
BEGIN;
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
        CONSTRAINT ck_usuarios_role CHECK (role IN ('STUDENT', 'ADMIN')),
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
COMMIT;

-- After registering the chosen account, run separately with its real email:
-- UPDATE usuarios SET role = 'ADMIN' WHERE email = lower('admin@example.com');
