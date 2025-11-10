package ar.edu.tpo.service;

import ar.edu.tpo.domain.RegistroConducta;
import ar.edu.tpo.domain.motivo.Motivo;
import ar.edu.tpo.domain.motivo.MotivoAbandono;
import ar.edu.tpo.domain.motivo.MotivoNoShow;

import java.time.Duration;

public class ConductaService {
    private final RegistroConducta registro = new RegistroConducta();
    private final UsuarioService usuarioService;

    public ConductaService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public void registrarAbandono(String email){ registrar(email, new MotivoAbandono()); }
    public void registrarNoShow(String email){ registrar(email, new MotivoNoShow()); }

    private void registrar(String email, Motivo motivo) {
        registro.registrar(email, motivo);
        if (motivo instanceof MotivoAbandono) {
            usuarioService.aplicarCooldown(email, motivo.nombre(), Duration.ofMinutes(30));
        } else if (motivo instanceof MotivoNoShow) {
            usuarioService.aplicarStrike(email, motivo.nombre());
        }
    }

    public void listarHistorial(){
        registro.getHistorial().forEach(e ->
                System.out.println(e.email() + " - " + e.motivo() + " - " + e.fecha())
        );
    }
}
