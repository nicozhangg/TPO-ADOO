package ar.edu.tpo.service;

import ar.edu.tpo.domain.RegistroConducta;
import ar.edu.tpo.domain.motivo.Motivo;
import ar.edu.tpo.domain.motivo.MotivoAbandono;
import ar.edu.tpo.domain.motivo.MotivoCooldown;
import ar.edu.tpo.domain.motivo.MotivoNoShow;
import ar.edu.tpo.domain.motivo.MotivoStrike;

public class ConductaService {
    private final RegistroConducta registro = new RegistroConducta();
    private final UsuarioService usuarioService;

    public ConductaService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public void registrarAbandono(String email){ registrar(email, new MotivoAbandono()); }
    public void registrarNoShow(String email){ registrar(email, new MotivoNoShow()); }
    public void registrarStrike(String email){ registrar(email, new MotivoStrike()); }
    public void registrarCooldown(String email){ registrar(email, new MotivoCooldown()); }

    private void registrar(String email, Motivo motivo) {
        registro.registrar(email, motivo);
        usuarioService.agregarSancion(email, motivo.nombre(), null);
    }

    public void listarHistorial(){
        registro.getHistorial().forEach(e ->
                System.out.println(e.email() + " - " + e.motivo() + " - " + e.fecha())
        );
    }
}
