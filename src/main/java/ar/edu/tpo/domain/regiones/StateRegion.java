package ar.edu.tpo.domain.regiones;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public interface StateRegion {
    String getNombre();
    int getPing();

    static List<StateRegion> disponibles() {
        List<StateRegion> regiones = new ArrayList<>();
        regiones.add(new America());
        regiones.add(new Europa());
        regiones.add(new Asia());
        return regiones;
    }

    @SuppressWarnings("resource")
    static StateRegion seleccionarRegion() {
        List<StateRegion> regionesDisponibles = disponibles();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("=== REGIONES DISPONIBLES ===");
            for (int i = 0; i < regionesDisponibles.size(); i++) {
                StateRegion region = regionesDisponibles.get(i);
                System.out.println((i + 1) + ". " + region.getNombre() + " - Ping: " + region.getPing() + "ms");
            }
            System.out.println("============================");
            System.out.print("Elige una región (1-" + regionesDisponibles.size() + "): ");

            try {
                int opcion = scanner.nextInt();
                if (opcion >= 1 && opcion <= regionesDisponibles.size()) {
                    StateRegion regionSeleccionada = regionesDisponibles.get(opcion - 1);
                    System.out.println("¡Has seleccionado: " + regionSeleccionada.getNombre() + "!");
                    return regionSeleccionada;
                } else {
                    System.out.println("Opción no válida. Intenta de nuevo.");
                }
            } catch (Exception e) {
                System.out.println("Por favor, ingresa un número válido.");
                scanner.nextLine();
            }
        }
    }

    static StateRegion fromNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        String buscado = nombre.trim().toLowerCase(Locale.ROOT);
        return disponibles().stream()
                .filter(r -> r.getNombre().toLowerCase(Locale.ROOT).equals(buscado))
                .findFirst()
                .orElse(null);
    }
}
