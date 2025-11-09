package ar.edu.tpo.service;

import ar.edu.tpo.domain.Usuario;

/**
 * Implementación temporal/mock del UsuarioActualPort.
 * Esta implementación es solo para desarrollo/testing.
 * El módulo de login/usuario de otro equipo deberá proporcionar la implementación real.
 */
public class MockUsuarioActualPort implements UsuarioActualPort {
    private Usuario usuarioActual;

    /**
     * Permite configurar el usuario actual para testing.
     * En producción, esto será manejado por el módulo de login.
     */
    public void establecerUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    /**
     * Limpia el usuario actual (logout simulado).
     */
    public void limpiarUsuarioActual() {
        this.usuarioActual = null;
    }

    @Override
    public String obtenerEmailUsuarioActual() {
        return usuarioActual != null ? usuarioActual.getEmail() : null;
    }

    @Override
    public Usuario obtenerUsuarioActual() {
        return usuarioActual;
    }
}
