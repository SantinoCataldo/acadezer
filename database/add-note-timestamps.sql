-- Migración aditiva: agrega fechas de creación y última modificación a los apuntes.
-- Los apuntes existentes reciben la fecha de aplicación del script como referencia;
-- el backend las mantiene a partir de ahí. Es idempotente: se puede ejecutar de nuevo.
BEGIN;
ALTER TABLE apuntes ADD COLUMN IF NOT EXISTS creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE apuntes ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
COMMIT;
