package ar.edu.tpo.domain.motivo;
public class MotivoStrike implements Motivo {
    public String nombre(){ return "Strike"; }
    public void aplicarSancion(String emailJugador){
        System.out.println("[conducta] Strike aplicado a " + emailJugador);
    }
}
