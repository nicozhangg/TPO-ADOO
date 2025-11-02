package ar.edu.tpo.domain;

public enum EstadoScrim {
    BUSCANDO_JUGADORES,   // faltan plazas
    LOBBY_ARMADO,         // cupo completo, falta confirmar
    CONFIRMADO,           // todos confirmaron, listo para iniciar
    EN_JUEGO,             // en curso
    FINALIZADO,           // terminó, se pueden cargar stats
    CANCELADO             // cancelado antes de iniciar
}
