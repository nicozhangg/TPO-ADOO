package ar.edu.tpo.repository;

import ar.edu.tpo.domain.Usuario;
import java.util.Collection;

public interface UsuarioRepository {
    void guardar(Usuario u);
    Usuario buscar(String email);
    Collection<Usuario> listar();
    void actualizar(Usuario u);
}
