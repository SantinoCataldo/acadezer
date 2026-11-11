# Materias y asistencia para Angular

Las rutas de materias siguen siendo `/api/materias`, pero cambia el JSON. Eliminar
`descripcion` del formulario y enviar `fechaInicio`, `fechaFin` y `horarios`.
`fechaInicio` es obligatoria; `fechaFin` acepta `null` para indicar que la materia
sigue activa. El porcentaje mínimo ya no forma parte del contrato.

Trimestral, cuatrimestral y anual son botones del formulario: calculan una fecha
de fin aproximada a partir del inicio. No enviar `periodo` al backend; ambas fechas
pueden editarse antes de guardar. Para una materia en curso, enviar `fechaFin: null`.
En una materia anterior la fecha de inicio puede venir en `null`; hay que completarla
antes de usar los controles nuevos.

```json
{"nombre":"Análisis","fechaInicio":"2026-03-01","fechaFin":null,"horarios":[{"diaSemana":"LUNES","horaInicio":"08:00","horaFin":"12:00","controlesAsistencia":2}]}
```

Para finalizar una materia en curso, llamar `PATCH /api/materias/{materiaId}/finalizar`.
Guarda la fecha de hoy (zona `America/Argentina/Buenos_Aires`) en `fechaFin` y
devuelve la materia actualizada. La acción responde 409 si ya estaba finalizada,
si no tiene fecha de inicio o si existen controles fuera del rango resultante.

Cada horario recibido tiene `id`. En PUT incluir el `id` de los que se mantienen;
omitirlo en horarios nuevos. El servidor responde 409 si se intenta eliminar o
modificar un horario con controles registrados, o reducir las fechas dejando
controles afuera. Los errores y propiedades de usuarios ajenos siguen devolviendo
el formato habitual y 404. Los tipos están en [angular-contracts.ts](angular-contracts.ts).

Usar `GET /api/materias/{materiaId}/controles-asistencia?desde=YYYY-MM-DD&hasta=YYYY-MM-DD`
para mostrar bloques y resumen. Cada bloque trae `horarioId`, `fecha`, `numero`,
`horaInicio`, `horaFin`, `id` y `estado`; un bloque no marcado tiene `id: null` y
estado `PENDIENTE`. El rango es inclusivo y no puede superar 366 días. Para materias
activas, la respuesta no genera controles posteriores a hoy.

Para marcar o cambiar un bloque, usar
`PUT /api/materias/{materiaId}/horarios/{horarioId}/controles/{fecha}/{numero}`
con `{"estado":"PRESENTE"}`, `AUSENTE` o `JUSTIFICADA`. DELETE sobre la misma
ruta vuelve el bloque a pendiente. Dos controles dividen la duración del horario
en dos bloques iguales y cada bloque cuenta como una unidad.

El porcentaje considera controles marcados: presentes sobre presentes + ausentes
+ justificadas. Las justificadas cuentan como falta; pendientes se muestran pero
no se incluyen en el denominador. Sin marcas, el porcentaje es `null`. Los días
sin horario y las fechas indicadas como sin cursada se excluyen.

Feriados/cancelaciones: `PUT /api/materias/{materiaId}/fechas-sin-cursada/{fecha}`
excluye toda la fecha, DELETE la vuelve a incluir y GET sobre `/fechas-sin-cursada`
recibe `desde` y `hasta`. Las marcas anteriores de esa fecha se conservan.

Las asistencias anteriores siguen en su CRUD histórico y no se convierten en bloques
automáticamente. Se necesita ejecutar manualmente la migración aditiva indicada en
el README antes de arrancar contra Neon. El frontend debe usar una versión del backend que incluya estos cambios.
