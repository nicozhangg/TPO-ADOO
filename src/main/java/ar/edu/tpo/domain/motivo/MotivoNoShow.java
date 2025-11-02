package ar.edu.tpo.domain.motivo;
public class MotivoNoShow implements Motivo {
    public String nombre(){ return "NoShow"; }
    public void aplicarSancion(String emailJugador){
        System.out.println("[conducta] NoShow -> strike +1 para " + emailJugador);
    }
}
