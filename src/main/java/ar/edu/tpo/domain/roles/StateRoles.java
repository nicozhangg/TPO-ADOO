package ar.edu.tpo.domain.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public interface StateRoles {
    String getNombre();

    static List<StateRoles> disponibles() {
        List<StateRoles> roles = new ArrayList<>();
        roles.add(new Duelista());
        roles.add(new Iniciador());
        roles.add(new Controlador());
        roles.add(new Centinela());
        return roles;
    }

    @SuppressWarnings("resource")
    static StateRoles seleccionarRol() {
        List<StateRoles> rolesDisponibles = disponibles();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("=== ROLES DISPONIBLES ===");
            for (int i = 0; i < rolesDisponibles.size(); i++) {
                StateRoles rol = rolesDisponibles.get(i);
                System.out.println((i + 1) + ". " + rol.getNombre());
            }
            System.out.println("========================");
            System.out.print("Elige un rol (1-" + rolesDisponibles.size() + "): ");

            try {
                int opcion = scanner.nextInt();
                if (opcion >= 1 && opcion <= rolesDisponibles.size()) {
                    StateRoles rolSeleccionado = rolesDisponibles.get(opcion - 1);
                    System.out.println("¡Has seleccionado: " + rolSeleccionado.getNombre() + "!");
                    return rolSeleccionado;
                } else {
                    System.out.println("Opción no válida. Intenta de nuevo.");
                }
            } catch (Exception e) {
                System.out.println("Por favor, ingresa un número válido.");
                scanner.nextLine();
            }
        }
    }

    static StateRoles fromNombre(String nombre) {
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

