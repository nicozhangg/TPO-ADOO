package ar.edu.tpo.service;

import ar.edu.tpo.domain.Rol;

/**
 * Implementación temporal/mock del UsuarioActualPort.
 * Esta implementación es solo para desarrollo/testing.
 * El módulo de login/usuario de otro equipo deberá proporcionar la implementación real.
 */
public class MockUsuarioActualPort implements UsuarioActualPort {
    private String emailActual;
    private Rol rolActual;

    /**
     * Permite configurar el usuario actual para testing.
     * En producción, esto será manejado por el módulo de login.
     */
    public void establecerUsuarioActual(String email, Rol rol) {
        this.emailActual = email;
        this.rolActual = rol;
    }

    /**
     * Limpia el usuario actual (logout simulado).
     */
    public void limpiarUsuarioActual() {
        this.emailActual = null;
        this.rolActual = null;
    }

    @Override
    public String obtenerEmailUsuarioActual() {
        return emailActual;
    }

    @Override
    public Rol obtenerRolUsuarioActual() {
        return rolActual;
    }
}

