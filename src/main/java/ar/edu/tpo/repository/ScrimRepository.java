package ar.edu.tpo.repository;

import ar.edu.tpo.domain.Scrim;
import java.util.List;

public interface ScrimRepository {
    void guardar(Scrim scrim);
    Scrim buscarPorId(String id);
    List<Scrim> listar();
}
