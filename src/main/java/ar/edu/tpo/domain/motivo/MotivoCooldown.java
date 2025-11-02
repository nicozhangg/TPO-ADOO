package ar.edu.tpo.domain.motivo;
public class MotivoCooldown implements Motivo {
    public String nombre(){ return "Cooldown"; }
    public void aplicarSancion(String emailJugador){
        System.out.println("[conducta] Cooldown iniciado para " + emailJugador);
    }
}
