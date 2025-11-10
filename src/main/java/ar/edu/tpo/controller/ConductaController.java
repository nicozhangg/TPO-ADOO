package ar.edu.tpo.controller;

import ar.edu.tpo.service.ConductaService;

public class ConductaController {
    private final ConductaService service;
    public ConductaController(ConductaService s){ this.service = s; }

    public void registrarAbandono(String email){ service.registrarAbandono(email); }
    public void registrarNoShow(String email){ service.registrarNoShow(email); }
    public void listarHistorial(){ service.listarHistorial(); }
}
