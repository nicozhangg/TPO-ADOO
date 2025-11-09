package ar.edu.tpo.service;

import ar.edu.tpo.domain.Usuario;

/**
 * Puerto para consultar el usuario actual logueado.
 * Este puerto será implementado por el módulo de login/usuario de otro equipo.
 */
public interface UsuarioActualPort {
    /**
     * Obtiene el email del usuario actualmente logueado.
     * @return el email del usuario logueado, o null si no hay usuario logueado
     */
    String obtenerEmailUsuarioActual();

    /**
     * Obtiene el usuario actualmente logueado.
     * @return el usuario logueado, o null si no hay usuario logueado
     */
    Usuario obtenerUsuarioActual();
}
