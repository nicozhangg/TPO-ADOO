package ar.edu.tpo.notification;

import ar.edu.tpo.domain.SancionActiva;
import ar.edu.tpo.domain.SancionHistorica;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.WaitlistEntry;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.service.ArgentinaTimeZone;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

public class NotificationService {
    private static final DateTimeFormatter FECHA_ARG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Notificador notificador;

    public NotificationService(Notificador notificador) {
        this.notificador = notificador;
    }

    public void notificarRegistro(Usuario usuario) {
        enviar(usuario.getEmail(), "Registro exitoso",
                "Hola %s,\n\nTu cuenta en eScrims fue creada correctamente. ¡Bienvenido!".formatted(nombreMostrar(usuario)));
    }

    public void notificarInicioSesion(Usuario usuario) {
        enviar(usuario.getEmail(), "Inicio de sesión detectado",
                "Hola %s,\n\nDetectamos un nuevo inicio de sesión en tu cuenta eScrims a las %s (hora Argentina)."
                        .formatted(nombreMostrar(usuario), ahoraArgentina()));
    }

    public void notificarUnionScrim(Scrim scrim, String emailJugador) {
        String mensaje = """
                Hola %s,

                Te uniste al scrim %s (%s). Recordá confirmar tu participación cuanto antes.
                Cupo por equipo: %d | Latencia máxima: %d ms.

                Si tienes agenda asignada, revísala para no perderte el inicio.
                """.formatted(emailJugador, scrim.getId(), scrim.getJuego(), scrim.getCupo(), scrim.getLatenciaMaxMs());
        enviar(emailJugador, "Unión a scrim " + scrim.getJuego(), mensaje);
    }

    public void notificarIngresoListaEspera(Scrim scrim, String emailJugador) {
        int orden = scrim.getListaEspera().stream()
                .filter(entry -> entry.emailJugador().equalsIgnoreCase(emailJugador))
                .map(WaitlistEntry::orden)
                .findFirst()
                .orElse(-1);
        String ordenTexto = orden > 0 ? (" con orden #" + orden) : "";
        String mensaje = """
                Hola %s,

                El scrim %s (%s) está completo por ahora. Te agregamos a la lista de suplentes%s.

                Te avisaremos apenas se libere un lugar para que puedas ingresar.
                """.formatted(
                emailJugador,
                scrim.getId(),
                scrim.getJuego(),
                ordenTexto
        );
        enviar(emailJugador, "Quedaste en la lista de suplentes", mensaje);
    }

    public void notificarCupoDisponible(Scrim scrim) {
        if (scrim.getListaEspera().isEmpty()) {
            return;
        }
        String mensaje = """
                Se liberó un cupo en el scrim %s (%s).

                Ingresá lo antes posible para asegurar tu lugar.
                """.formatted(
                scrim.getId(),
                scrim.getJuego()
        );
        scrim.getListaEspera().stream()
                .map(WaitlistEntry::emailJugador)
                .filter(email -> email != null && !email.isBlank())
                .forEach(email -> enviar(email, "Hay un lugar disponible en el scrim", mensaje));
    }

    public void notificarSancionAplicada(Usuario usuario, SancionActiva sancion) {
        String expira = sancion.getExpiraEn() != null
                ? sancion.getExpiraEn().format(FECHA_ARG)
                : "sin fecha de expiración";
        String mensaje = """
                Hola %s,

                Se aplicó una sanción por '%s'.
                Expira: %s.

                Mientras esté activa no podrás unirte a nuevos scrims.
                """.formatted(nombreMostrar(usuario), sancion.getMotivo(), expira);
        enviar(usuario.getEmail(), "Se registró una sanción en tu cuenta", mensaje);
    }

    public void notificarSancionLevantada(Usuario usuario, SancionHistorica sancion) {
        String expira = sancion.getExpiraEn() != null
                ? sancion.getExpiraEn().format(FECHA_ARG)
                : "sin fecha de expiración";
        String mensaje = """
                Hola %s,

                La sanción '%s' fue levantada.
                Expiración original: %s.

                Ya podés volver a participar normalmente.
                """.formatted(nombreMostrar(usuario), sancion.getMotivo(), expira);
        enviar(usuario.getEmail(), "Sanción levantada", mensaje);
    }

    public void notificarScrimEstado(Scrim scrim, String nuevoEstado) {
        String mensaje = """
                Actualización del scrim %s (%s):
                Estado actual: %s.
                Inicio programado: %s
                Fin programado: %s
                """.formatted(
                scrim.getId(),
                scrim.getJuego(),
                nuevoEstado,
                formatear(scrim.getInicio()),
                formatear(scrim.getFin())
        );
        enviarATodos(scrim, "Actualización de scrim " + scrim.getJuego(), mensaje);
    }

    public void notificarScrimProgramado(Scrim scrim) {
        String mensaje = """
                El scrim %s (%s) fue programado.
                Inicio: %s
                Fin: %s
                """.formatted(
                scrim.getId(),
                scrim.getJuego(),
                formatear(scrim.getInicio()),
                formatear(scrim.getFin())
        );
        enviarATodos(scrim, "Scrim programado", mensaje);
    }

    public void notificarScrimRecomendada(Scrim scrim, Usuario usuario) {
        String mensaje = """
                Hola %s,

                Encontramos una scrim que coincide con tus preferencias:
                - Juego: %s
                - Región: %s
                - Formato: %s
                - Rango requerido: %d - %d
                - Latencia máxima: %d ms
                - Inicio programado: %s

                ID del scrim: %s

                Ingresá a eScrims para sumarte mientras haya lugar.
                """.formatted(
                nombreMostrar(usuario),
                scrim.getJuego(),
                scrim.getRegion(),
                scrim.getFormato(),
                scrim.getRangoMin(),
                scrim.getRangoMax(),
                scrim.getLatenciaMaxMs(),
                formatear(scrim.getInicio()),
                scrim.getId()
        );
        enviar(usuario.getEmail(), "Nueva scrim compatible disponible", mensaje);
    }

    private void enviarATodos(Scrim scrim, String tipo, String mensaje) {
        Set<String> destinatarios = new LinkedHashSet<>();
        destinatarios.add(scrim.getEmailCreador());
        destinatarios.addAll(scrim.getEquipo1().getJugadores());
        destinatarios.addAll(scrim.getEquipo2().getJugadores());
        scrim.getListaEspera().forEach(entry -> destinatarios.add(entry.emailJugador()));

        destinatarios.stream()
                .filter(email -> email != null && !email.isBlank())
                .forEach(email -> enviar(email, tipo, mensaje));
    }

    private void enviar(String destinatario, String tipo, String mensaje) {
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        if (notificador == null) {
            return;
        }
        Notificacion notificacion = new Notificacion(tipo, notificador.getTipo(), mensaje, destinatario);
        notificador.enviar(notificacion);
    }

    private String nombreMostrar(Usuario usuario) {
        String nombre = usuario.getNombre();
        if (nombre != null && !nombre.isBlank()) {
            return nombre;
        }
        return usuario.getEmail();
    }

    private static String ahoraArgentina() {
        return ArgentinaTimeZone.ahora().format(FECHA_ARG);
    }

    private static String formatear(LocalDateTime fecha) {
        if (fecha == null) {
            return "sin asignar";
        }
        return fecha.format(FECHA_ARG);
    }
}


