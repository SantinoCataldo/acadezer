-- Migración aditiva: las evaluaciones pasan a tener tipo (Parcial 1, Parcial 2,
-- Final, Recuperatorio, TP) y estado (Aprobado, Desaprobado, Pendiente).
-- Las filas existentes reciben valores por defecto: TP y PENDIENTE, para no
-- dejar la columna vacía. Es idempotente: se puede ejecutar de nuevo.
BEGIN;
ALTER TABLE evaluaciones ADD COLUMN IF NOT EXISTS tipo VARCHAR(15) NOT NULL DEFAULT 'TP';
ALTER TABLE evaluaciones ADD COLUMN IF NOT EXISTS estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE';
COMMIT;
