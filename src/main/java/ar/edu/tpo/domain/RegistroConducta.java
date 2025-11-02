package ar.edu.tpo.domain;

import ar.edu.tpo.domain.motivo.Motivo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Una sola clase con motivos por interfaz (Abandono/NoShow/Strike/Cooldown)
public class RegistroConducta {
    public static record Entrada(String email, String motivo, LocalDateTime fecha) {}
    private final List<Entrada> historial = new ArrayList<>();
    public void registrar(String emailJugador, Motivo motivo){
        historial.add(new Entrada(emailJugador, motivo.nombre(), LocalDateTime.now()));
        motivo.aplicarSancion(emailJugador);
    }
    public List<Entrada> getHistorial(){ return List.copyOf(historial); }
}
