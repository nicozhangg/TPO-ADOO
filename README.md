# TPO eScrims

Ecosistema para gestionar scrims competitivas desde consola. Se apoya en Maven, una arquitectura en capas y persistencia JSON. Incluye notificaciones por correo y automatizaciones para cuidar la integridad de los datos.

## Características principales
- Consola en español con menús diferenciados para jugadores y organizadores.
- Arquitectura en tres capas (`controller` / `service` / `repository`) y paquetes dedicados para scrims, usuarios y notificaciones.
- Persistencia en `data/scrims.json` y `data/usuarios.json` mediante Gson.
- Estrategias de emparejamiento pluggables (MMR, latencia, KDA) y cálculo de estadísticas por partida.
- Scheduler de scrims (transición automática CONFIRMADO → EN_JUEGO) y scheduler de sanciones (limpia sanciones vencidas y mantiene historial).
- Sistema de sanciones con motivos estandarizados, historial, levantamiento manual y automático.
- Notificaciones: registro, login, unión a scrim, sanciones y cambios de estado; soporta SMTP real o modo simulado por consola.
- Edición de perfil para jugadores (MMR, latencia, rol, región) y validaciones de región/latencia al unirse a scrims.

## Puesta en marcha
```bash
mvn -q exec:java
```
Requisitos: Java 17+, Maven. Los datos iniciales se encuentran en `data/`.

## SMTP real (opcional)
Configura estas variables antes de ejecutar:
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`
- Opcionales: `SMTP_FROM`, `SMTP_STARTTLS=true|false`

Si no se definen, el sistema imprime los correos en consola.

