# TPO eScrims — Maven (Consola, 3 capas, JSON, Strategy)

**Qué trae**
- 3 capas: controller / service / repository (sin DTOs)
- Servicios de scrim modularizados en `ar.edu.tpo.service.scrim` (ciclo de vida, lobby, estadísticas y scheduler)
- Persistencia JSON con **Gson**; adaptador dedicado (`ScrimJsonAdapter`) en `repository/json`
- Estrategias disponibles (MMR / Latencia / KDA) listas para enchufar en el flujo de emparejamiento
- **KDA** = (kills + assists) / deaths (double) y **rating** por partida
- Registro de conducta (una clase) + motivos por interfaz (Abandono, NoShow, Strike, Cooldown)
- Lista de espera (suplentes) con **composición** dentro de `Scrim`
- Menús y funciones en español
- Ejecutable con `mvn exec:java`

## Cómo correr
```bash
mvn -q exec:java
```
Los datos se guardan en `data/scrims.json` y `data/usuarios.json`.

> Si querés un jar “fat” luego, puedo agregarte el plugin `maven-shade-plugin`.
