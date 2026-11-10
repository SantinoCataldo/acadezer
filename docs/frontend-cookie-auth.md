# Angular: sesión con cookie HttpOnly

El backend devuelve `accessToken`, `tokenType`, `expiresIn` y `usuario` en JSON.
El refresh token llega únicamente en la cookie `acadezer_refresh` (`HttpOnly`,
`Secure`, `SameSite=None`, `Path=/auth`); JavaScript no debe intentar leerla.

```typescript
// Login: guardar el access token en memoria para usarlo como Bearer.
http.post<AuthResponse>(`${apiUrl}/auth`, credentials, { withCredentials: true });

// Renovación: sin body ni Authorization; serializar las llamadas concurrentes.
http.post<AuthResponse>(`${apiUrl}/auth/refresh`, null, {
  withCredentials: true,
  headers: { 'X-CSRF-Protection': '1' }
});

// Logout: revoca la sesión y elimina la cookie en el navegador.
http.post<void>(`${apiUrl}/auth/logout`, null, {
  withCredentials: true,
  headers: { 'X-CSRF-Protection': '1' }
});
```

En logout descartar también el access token local. El access token emitido permanece
válido en el servidor hasta su vencimiento. La renovación invalida la cookie previa;
si varias peticiones reciben 401 simultáneamente, compartir una sola renovación y
reanudar las peticiones solo cuando termine. Evitar bucles de renovación: excluir
login, refresh y logout del interceptor que reintenta ante 401. Mantener el manejo
de 429 y `Retry-After` descrito en [frontend-rate-limits.md](frontend-rate-limits.md).

El servidor exige en refresh/logout un `Origin` explícito permitido y la cabecera
`X-CSRF-Protection: 1`. Configurar `CORS_ORIGIN` con el origen exacto de Angular;
admite varios separados por comas. En producción se necesita HTTPS. Para HTTP local,
usar `REFRESH_COOKIE_SECURE=false` y el mismo hostname en Angular y backend; la
cookie usa `SameSite=Lax`. Con frontend y backend en sitios diferentes, algunos
navegadores bloquean cookies de terceros: probar el despliegue elegido.

Esta modificación cambia el contrato anterior: el frontend y el backend deben
desplegarse coordinadamente. El proyecto Angular no está incluido en este repo.
