package ar.edu.tpo.controller;

import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.regiones.StateRegion;
import ar.edu.tpo.domain.roles.StateRoles;
import ar.edu.tpo.service.UsuarioService;

public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    public void registrarOrganizador(String email, String password) {
        service.registrarOrganizador(email, password);
        System.out.println("Organizador registrado.");
    }

    public void registrarJugador(String email,
                                 String password,
                                 int mmr,
                                 int latenciaMs,
                                 StateRangos rango,
                                 StateRoles rolPreferido,
                                 StateRegion region) {
        service.registrarJugador(email, password, mmr, latenciaMs, rango, rolPreferido, region);
        System.out.println("Jugador registrado.");
    }

    public void registrar(Usuario usuario) {
        service.registrar(usuario);
        System.out.println("Usuario registrado.");
    }

    public void listar() {
        for (Usuario u : service.listar()) {
            System.out.println(u);
        }
    }

    public void buscar(String email) {
        System.out.println(service.buscar(email));
    }

    public Usuario login(String email, String password) {
        return service.login(email, password);
    }
}
