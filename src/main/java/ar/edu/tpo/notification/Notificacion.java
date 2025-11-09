package ar.edu.tpo.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notificacion {
    private final UUID id;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
    private final String tipo;
    private final String canal;
    private final String payload;
    private final String destinatario;
    private String estado;

    public Notificacion(String tipo, String canal, String payload, String destinatario) {
        this.id = UUID.randomUUID();
        this.fechaCreacion = LocalDateTime.now();
        this.tipo = tipo;
        this.canal = canal;
        this.payload = payload;
        this.destinatario = destinatario;
        this.estado = "PENDIENTE";
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCanal() {
        return canal;
    }

    public String getPayload() {
        return payload;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getEstado() {
        return estado;
    }

    public void marcarEnviado() {
        this.estado = "ENVIADO";
        this.fechaEnvio = LocalDateTime.now();
    }

    public void marcarError() {
        this.estado = "ERROR";
        this.fechaEnvio = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Notificacion{" +
                "id=" + id +
                ", tipo='" + tipo + '\'' +
                ", canal='" + canal + '\'' +
                ", destinatario='" + destinatario + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}


