package ar.edu.tpo.service;

import ar.edu.tpo.domain.RegistroConducta;
import ar.edu.tpo.domain.motivo.*;

public class ConductaService {
    private final RegistroConducta registro = new RegistroConducta();

    public void registrarAbandono(String email){ registro.registrar(email, new MotivoAbandono()); }
    public void registrarNoShow(String email){ registro.registrar(email, new MotivoNoShow()); }
    public void registrarStrike(String email){ registro.registrar(email, new MotivoStrike()); }
    public void registrarCooldown(String email){ registro.registrar(email, new MotivoCooldown()); }

    public void listarHistorial(){
        registro.getHistorial().forEach(e ->
                System.out.println(e.email() + " - " + e.motivo() + " - " + e.fecha())
        );
    }
}
