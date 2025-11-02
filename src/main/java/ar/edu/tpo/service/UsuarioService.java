package ar.edu.tpo.service;

import ar.edu.tpo.domain.Rol;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

public class UsuarioService {
    private final UsuarioRepository repo;

    public UsuarioService(UsuarioRepository repo){
        this.repo = repo;
    }

    public void registrar(String email, String nickname, int mmr, int latenciaMs, Rol rol){
        repo.guardar(new Usuario(email, nickname, mmr, latenciaMs, rol));
    }

    public List<Usuario> listar(){
        return new ArrayList<>(repo.listar());
    }

    public Usuario buscar(String email){
        Usuario u = repo.buscar(email);
        if (u == null) throw new IllegalArgumentException("Usuario no encontrado");
        return u;
    }
}
