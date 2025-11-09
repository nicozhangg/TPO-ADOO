package ar.edu.tpo.service;

import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Organizador;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.regiones.StateRegion;
import ar.edu.tpo.domain.roles.StateRoles;
import ar.edu.tpo.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

public class UsuarioService {
    private final UsuarioRepository repo;

    public UsuarioService(UsuarioRepository repo){
        this.repo = repo;
    }

    public void registrar(Usuario usuario){
        repo.guardar(usuario);
    }

    public void registrarOrganizador(String email, String password){
        registrar(new Organizador(email, password));
    }

    public void registrarJugador(String email,
                                 String password,
                                 int mmr,
                                 int latenciaMs,
                                 StateRangos rango,
                                 StateRoles rolPreferido,
                                 StateRegion region){
        registrar(new Jugador(email, password, mmr, latenciaMs, rango, rolPreferido, region));
    }

    public List<Usuario> listar(){
        return new ArrayList<>(repo.listar());
    }

    public Usuario buscar(String email){
        Usuario u = repo.buscar(email);
        if (u == null) throw new IllegalArgumentException("Usuario no encontrado");
        return u;
    }

    public void agregarSancion(String email, String motivo) {
        Usuario usuario = buscar(email);
        usuario.agregarSancion(motivo);
        repo.actualizar(usuario);
    }

    public Usuario login(String email, String password){
        Usuario usuario = repo.buscar(email);
        if (usuario == null || !usuario.getPasswordHash().equals(password)) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        return usuario;
    }
}
