package ar.edu.tpo.service;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Utilidades para trabajar con la zona horaria de Argentina.
 */
public class ArgentinaTimeZone {
    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    /**
     * Obtiene la zona horaria de Argentina.
     */
    public static ZoneId getZoneId() {
        return ZONA_ARGENTINA;
    }

    /**
     * Obtiene la fecha/hora actual en Argentina.
     */
    public static ZonedDateTime ahora() {
        return ZonedDateTime.now(ZONA_ARGENTINA);
    }

    /**
     * Convierte una fecha/hora ingresada como string
     * (acepta formatos: "yyyy-MM-dd HH:mm" o "yyyy-MM-ddTHH:mm")
     * asumiendo que está en zona horaria de Argentina.
     */
    public static ZonedDateTime parsear(String fechaHoraStr) {
        if (fechaHoraStr == null || fechaHoraStr.isBlank()) {
            return null;
        }
        
        LocalDateTime localDateTime;
        // Intentar parsear con formato ISO primero (con 'T'), luego con espacio
        try {
            // Formato ISO: "2025-02-11T18:40"
            localDateTime = LocalDateTime.parse(fechaHoraStr);
        } catch (Exception e) {
            try {
                // Formato con espacio: "2025-02-11 18:40"
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                localDateTime = LocalDateTime.parse(fechaHoraStr, formatter);
            } catch (Exception e2) {
                throw new IllegalArgumentException("Formato de fecha inválido: " + fechaHoraStr + 
                    ". Use formato: yyyy-MM-dd HH:mm o yyyy-MM-ddTHH:mm");
            }
        }
        
        return localDateTime.atZone(ZONA_ARGENTINA);
    }

    /**
     * Convierte un LocalDateTime a ZonedDateTime en zona horaria de Argentina.
     */
    public static ZonedDateTime aZonaArgentina(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZONA_ARGENTINA);
    }

    /**
     * Convierte un ZonedDateTime en zona Argentina a LocalDateTime
     * (para mantener compatibilidad con el modelo existente).
     */
    public static LocalDateTime aLocalDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.withZoneSameInstant(ZONA_ARGENTINA).toLocalDateTime();
    }
}

