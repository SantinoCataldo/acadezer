# Requisitos — Acadezer

1. Registro y login de estudiantes con email único y contraseña BCrypt.
2. JWT y roles STUDENT/ADMIN; propietario obtenido del usuario autenticado.
3. CRUD de materias y sus apuntes, pendientes, evaluaciones y asistencias mediante DTO.
4. Listados por materia; consultas por ID con verificación de propiedad.
5. Pendientes inicialmente incompletos, marcado/desmarcado y consulta por rango de fechas.
6. Evaluaciones con fecha obligatoria y calificación opcional, de 0 a 10 con dos decimales.
7. Asistencia PRESENTE/AUSENTE/JUSTIFICADA, única por materia y fecha.
8. Borrado de materia y su contenido en una transacción, documentado para confirmación del frontend.
9. Cinco preguntas y respuestas desde un apunte propio guardado, una llamada de texto a IA.
10. Sin credenciales, el CRUD funciona y la IA informa que no está configurada; sin simulación.
11. Entradas validadas, errores JSON consistentes, timeout y sin reintentos automáticos de aplicación.
12. Pruebas de aislamiento, validaciones, duplicados, permisos de administración y fallos de IA sin proveedor real.
13. ADMIN consulta usuarios y estadísticas generales y habilita/deshabilita estudiantes.
14. El registro siempre crea STUDENT; el rol ADMIN se asigna por SQL. No habilita acceso a apuntes privados ajenos.
15. Los request se declaran como record, con validaciones por campo; entidades, DTO, servicios y mappers usan archivos en inglés.
16. Despliegue preparado para Render con Docker/Java 21, PostgreSQL de Neon y variables de entorno. GET /health es público y no expone datos privados.

Arquitectura sencilla de controlador, servicio y repositorio, con DTO y JPA.
Base de desarrollo separada del ATS y credenciales externas al repositorio.
Sin reglas de aprobación/promoción/asistencia mínima, PDF, imágenes,
audio, video, chat, grupos, docentes, notificaciones ni integraciones de calendarios.
No se implementan resúmenes de apuntes, acceso con Google ni correos con Resend
en esta entrega. La IA genera únicamente preguntas de práctica con respuestas.
Contratos, ejemplos y limitaciones detallados en [README.md](README.md).
