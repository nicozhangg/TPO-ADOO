# TPO eScrims — Maven (Consola, 3 capas, JSON, Strategy)

**Qué trae**
- 3 capas: controller / service / repository (sin DTOs)
- Persistencia JSON con **Gson** (Scrims y Usuarios)
- **Strategy** en service (MMR / Latencia / KDA)
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
