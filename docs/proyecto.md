# Proyecto TPO eScrims

## 1. Elevator pitch
eScrims es una plataforma de consola que permite organizar y vivir scrims competitivas con la misma rigurosidad que un torneo formal: agenda, lobby, confirmaciones, estadísticas, sanciones y notificaciones en un solo flujo. Ideal para presentar cómo una aplicación CLI puede orquestar múltiples reglas de negocio manteniendo persistencia y automatización.

## 2. Objetivos del proyecto
- Simplificar la coordinación entre organizadores y jugadores.
- Registrar métricas competitivas (KDA, rating) y comportamiento (sanciones) de cada usuario.
- Automatizar procesos repetitivos: cambios de estado en scrims, limpieza de sanciones, avisos por correo.
- Ofrecer un diseño extensible (strategies, servicios independientes, schedulers) que pueda evolucionar hacia otras interfaces.

## 3. Arquitectura en una mirada
- **Presentación (controller)**: menús en consola, captura de input y delegación a servicios.
- **Servicios (service)**: núcleo de negocio, dividido en usuarios, scrims (lobby, ciclo de vida, estadísticas, scheduler) y notificaciones.
- **Persistencia (repository)**: lectura/escritura JSON con Gson, caché en memoria y adaptadores especializados.
- **Notificaciones**: estrategia plug&play (`NotificacionStrategy`) con soporte para envío real vía SMTP o modo demo por consola.
- **Schedulers**: hilos daemon que ejecutan tareas periódicas (iniciar scrims, depurar sanciones).

Todo el proyecto está modularizado por paquetes (`ar.edu.tpo.domain`, `service.scrim`, `notification`, etc.), lo que facilita explicarlo y navegarlo durante una presentación.

## 4. Roles y experiencia de usuario
### Organizadores
- Crear scrims definiendo juego, cupos, formato, región, latencia, modalidad y agenda.
- Confirmar equipos, gestionar lobby, asignar suplentes, programar y actualizar estados (iniciar, finalizar, cancelar).
- Registrar sanciones, consultarlas (activas/históricas) y levantarlas manualmente.
- Cargar estadísticas al terminar una partida.

### Jugadores
- Registrarse, iniciar sesión y actualizar su perfil (MMR, latencia, rol, región).
- Unirse a scrims (respetando validaciones de rango, latencia y región), confirmarse o darse de baja.
- Recibir notificaciones cuando se unen, son sancionados o se actualiza un scrim donde participan.

## 5. Automatizaciones destacadas
- **Scheduler de scrims**: revisa periódicamente si llegó el horario programado y cambia el estado a EN_JUEGO; también avisa por correo.
- **Scheduler de sanciones**: elimina sanciones vencidas, las mueve a historial y notifica al jugador.
- **Notificaciones**: registro e inicio de sesión, unión a scrim, sanción aplicada o levantada, cambios de estado importantes.

## 6. Persistencia y datos
- JSON plano (`data/usuarios.json`, `data/scrims.json`) para facilitar demos y revisiones en vivo.
- Repositorios en memoria con Gson para mapear dominos complejos (jugadores, scrims con equipos, waitlist, historial de sanciones).
- Actualización inmediata tras cada operación relevante (evita comandos de “guardar” manual).

## 7. Integración con correo
- `MailStrategy` permite dos modos:
  - **SMTP real**: configurar `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM` y `SMTP_STARTTLS`.
  - **Simulado**: imprime en consola el contenido del correo (ideal para demos sin credenciales).
- Eventos notificados: registros, logins, union a scrim, sanciones nuevas o levantadas, scrims programados / en juego / finalizados / cancelados.

## 8. Flujo de demo recomendado
1. Ejecutar `mvn -q exec:java` (Java 17+).
2. Mostrar el menú principal: registrar organizador y/o jugador.
3. Como organizador:
   - Crear scrim, programarla y ver cómo el scheduler la inicia automáticamente (log + email).
   - Agregar sanción a un jugador y consultar lista activa/histórica.
4. Como jugador:
   - Actualizar perfil (cambia MMR/rol/región), unirse a scrim válida e intentar una no válida (mensaje de región/latencia).
5. Verificar notificaciones en consola o correos reales.

## 9. Extensiones futuras para comentar
- UI web o mobile, manteniendo el mismo núcleo de servicios.
- Integración con un motor de emparejamiento más complejo o métricas de rendimiento adicionales.
- Persistencia en base de datos relacional / no relacional según necesidad de escalabilidad.
- Otros canales de notificación (push, SMS) reutilizando el patrón Strategy.

## 10. Archivos clave para mostrar en la exposición
- `Main.java`: loop principal, configuración de notificaciones y schedulers.
- `ScrimCicloDeVidaService` / `ScrimLobbyService`: reglas de negocio para scrims.
- `UsuarioService`: sanciones, perfil y notificaciones.
- `MailStrategy`: switch entre SMTP real y simulación.
- `SancionSchedulerService` y `ScrimSchedulerService`: ejemplo de automatización en background.
- `docs/proyecto.md`: este guion para exponer la solución de principio a fin.


