package ar.edu.tpo.domain.motivo;
public class MotivoAbandono implements Motivo {
    public String nombre(){ return "Abandono"; }
    public void aplicarSancion(String emailJugador){
        System.out.println("[conducta] Abandono -> cooldown 30min para " + emailJugador);
    }
}
