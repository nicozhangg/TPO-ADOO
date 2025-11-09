package ar.edu.tpo.notification;

public class Notificador {
    private NotificacionStrategy estrategia;

    public Notificador(NotificacionStrategy estrategia) {
        this.estrategia = estrategia;
    }

    public void setEstrategia(NotificacionStrategy estrategia) {
        this.estrategia = estrategia;
    }

    public String getTipo() {
        return estrategia != null ? estrategia.getTipo() : "SIN_CANAL";
    }

    public void enviar(Notificacion notificacion) {
        if (estrategia == null) {
            System.out.println("[notificador] No hay estrategia configurada, se descarta notificación: " + notificacion);
            return;
        }
        estrategia.enviar(notificacion);
    }
}


