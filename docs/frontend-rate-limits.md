# Acadezer: cambios para Angular

El backend incorpora límites de login/registro y generación de preguntas.
Las rutas y las respuestas exitosas no cambian. No hace falta cambiar los DTO.

## Manejar HTTP 429

Aplica a `POST /auth`, `POST /auth/register` y
`POST /api/apuntes/{id}/preguntas`.

- Leer `Retry-After` de las cabeceras de la respuesta: son segundos enteros positivos.
- Mostrar `error.error.message` y deshabilitar el botón durante ese tiempo.
- Si falta la cabecera o no es válida, usar una espera mínima de un segundo.
- No borrar el JWT ni cerrar sesión por 429. Mantener el manejo existente de 401/403.
- No configurar reintentos automáticos para estas peticiones, especialmente IA.
- Evitar dobles clics: deshabilitar el botón mientras hay una petición en curso.
- CORS ya expone `Retry-After` al navegador.

El JSON del error conserva `ErrorResponse`:

```json
{
  "status": 429,
  "message": "Se alcanzó el límite de solicitudes. Intentá nuevamente más tarde",
  "path": "/api/apuntes/1/preguntas",
  "timestamp": "2026-11-09T15:00:00Z"
}
```

`Retry-After` indica el resto de la ventana de cuota; en concurrencia es `1` y no
garantiza que al segundo siguiente haya lugar. Volver a intentar queda a decisión
del usuario al terminar la espera, sin enviar peticiones repetidas en segundo plano.

## Límites predeterminados, configurables en el backend

- Login: 10 intentos por cuenta y 100 por IP cada 15 minutos.
- Registro: 5 intentos por cuenta y 30 por IP cada hora.
- IA: 20 intentos admitidos por usuario cada hora, compartidos entre sus apuntes.
- IA simultánea: 1 solicitud por usuario y 2 por instancia.

Un apunte ajeno/inexistente sigue devolviendo 404, incluso si se agotó la cuota.
Si falta autenticación se mantiene 401. Los límites de IA se verifican antes de
llamar al proveedor: los 429 no generan preguntas ni llamadas costosas.

Los rechazos por concurrencia no consumen cuota. Las llamadas admitidas que fallan
o vencen por timeout sí cuentan. Los errores locales por IA sin configurar o texto
insuficiente no consumen cuota. Las cuentas no se deshabilitan por estos límites.

Los contadores son temporales y viven en memoria: se reinician con el proceso y
son independientes entre instancias. Configuración completa e IPs de proxies:
[README.md](../README.md#límites-de-autenticación-y-de-ia).

Estos cambios estarán disponibles en la instancia de Render cuando termine el
despliegue del commit que los contiene.
