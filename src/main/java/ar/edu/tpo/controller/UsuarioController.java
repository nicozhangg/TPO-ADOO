package ar.edu.tpo.controller;

import ar.edu.tpo.domain.Rol;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.service.UsuarioService;

public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService s){ this.service = s; }

    public void registrar(String email, String nickname, int mmr, int latenciaMs, Rol rol){
        service.registrar(email, nickname, mmr, latenciaMs, rol);
        System.out.println("Usuario registrado.");
    }
    public void listar(){
        for (Usuario u: service.listar()) System.out.println(u);
    }
    public void buscar(String email){
        System.out.println(service.buscar(email));
    }
}
