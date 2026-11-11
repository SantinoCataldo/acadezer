# Materias y asistencia para Angular

Las rutas de materias siguen siendo `/api/materias`, pero cambia el JSON. Eliminar
`descripcion` del formulario y enviar `fechaInicio`, `fechaFin` y `horarios`.
`fechaInicio` es obligatoria; `fechaFin` acepta `null` para indicar que la materia
sigue activa. El porcentaje mínimo ya no forma parte del contrato.

Trimestral, cuatrimestral y anual son botones del formulario: calculan una fecha
de fin aproximada a partir del inicio. No enviar `periodo` al backend; ambas fechas
pueden editarse antes de guardar. Para una materia en curso, enviar `fechaFin: null`.
Las materias antiguas pueden tener `fechaInicio: null`; esa fecha no limita consultas
ni registros retroactivos. Se pueden consultar y marcar clases anteriores a la fecha
de inicio o a la creación de la materia si coinciden con el horario.

```json
{"nombre":"Análisis","fechaInicio":"2026-03-01","fechaFin":null,"horarios":[{"diaSemana":"LUNES","horaInicio":"08:00","horaFin":"12:00","controlesAsistencia":2}]}
```

Para finalizar una materia en curso, llamar `PATCH /api/materias/{materiaId}/finalizar`.
Guarda la fecha de hoy (zona `America/Argentina/Buenos_Aires`) en `fechaFin` y
devuelve la materia actualizada. Se generan y aceptan controles hasta esa fecha
inclusive; desde el día siguiente se rechazan. La acción responde 409 si ya estaba
finalizada o si existen controles registrados posteriores a hoy.

Cada horario recibido tiene `id`. En PUT incluir el `id` de los que se mantienen;
omitirlo en horarios nuevos. El servidor responde 409 si se intenta eliminar o
modificar un horario con controles registrados, o cambiar `fechaFin` dejando
controles posteriores al fin. `fechaInicio` no limita el historial. Los errores y
propiedades de usuarios ajenos siguen devolviendo
el formato habitual y 404. Los tipos están en [angular-contracts.ts](angular-contracts.ts).

Usar `GET /api/materias/{materiaId}/controles-asistencia?desde=YYYY-MM-DD&hasta=YYYY-MM-DD`
para mostrar bloques y resumen. Cada bloque trae `horarioId`, `fecha`, `numero`,
`horaInicio`, `horaFin`, `id` y `estado`; un bloque no marcado tiene `id: null` y
estado `PENDIENTE`. El rango es inclusivo y no puede superar 366 días. Los controles
se generan para cualquier fecha del rango que coincida con el día semanal del horario,
sin limitar por `fechaInicio` ni por la fecha de creación de la materia. En materias
activas, no se generan controles posteriores a hoy; en finalizadas, no se pasa de
`fechaFin`.

Para marcar o cambiar un bloque, usar
`PUT /api/materias/{materiaId}/horarios/{horarioId}/controles/{fecha}/{numero}`
con `{"estado":"PRESENTE"}`, `AUSENTE` o `JUSTIFICADA`. Se permite marcar cualquier
fecha pasada que corresponda al día del horario, aunque sea anterior a `fechaInicio`.
No se aceptan marcas posteriores a `fechaFin`; en materias activas, el límite es hoy.
DELETE sobre la misma ruta vuelve el bloque a pendiente. Dos controles dividen la duración del horario
en dos bloques iguales y cada bloque cuenta como una unidad.

El porcentaje considera controles marcados: presentes sobre presentes + ausentes
+ justificadas. Las justificadas cuentan como falta; pendientes se muestran pero
no se incluyen en el denominador. Sin marcas, el porcentaje es `null`. Los días
sin horario y las fechas indicadas como sin cursada se excluyen.

Feriados/cancelaciones: `PUT /api/materias/{materiaId}/fechas-sin-cursada/{fecha}`
excluye toda la fecha, DELETE la vuelve a incluir y GET sobre `/fechas-sin-cursada`
recibe `desde` y `hasta`. Las marcas anteriores de esa fecha se conservan.

Para las cursadas activas, Angular puede filtrar `/api/materias` por `fechaFin == null`
o `fechaFin >= hoy` (fecha actual de Argentina). El listado general conserva materias
finalizadas para mostrar sus horarios y controles históricos hasta su `fechaFin`.

Las asistencias anteriores siguen en su CRUD histórico y no se convierten en bloques
automáticamente. Se necesita ejecutar manualmente la migración aditiva indicada en
el README antes de arrancar contra Neon. El frontend debe usar una versión del backend que incluya estos cambios.
