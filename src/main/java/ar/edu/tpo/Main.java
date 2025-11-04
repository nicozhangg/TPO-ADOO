package ar.edu.tpo;

import ar.edu.tpo.controller.ScrimController;
import ar.edu.tpo.domain.Rol;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.repository.JsonScrimRepository;
import ar.edu.tpo.repository.JsonUsuarioRepository;
import ar.edu.tpo.service.ConductaService;
import ar.edu.tpo.service.MockUsuarioActualPort;
import ar.edu.tpo.service.ScrimService;
import ar.edu.tpo.service.UsuarioService;
import ar.edu.tpo.service.estrategias.EstrategiaPorMMR;

import java.util.Scanner;

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static MockUsuarioActualPort usuarioActual;
    private static ScrimController scrimController;
    private static UsuarioService usuarioService;

    public static void main(String[] args) {
        // Inicializar repositorios y servicios
        JsonScrimRepository scrimRepo = new JsonScrimRepository("data/scrims.json");
        JsonUsuarioRepository usuarioRepo = new JsonUsuarioRepository("data/usuarios.json");
        
        usuarioService = new UsuarioService(usuarioRepo);
        ConductaService conductaService = new ConductaService();
        ScrimService scrimService = new ScrimService(
            scrimRepo, 
            usuarioService, 
            new EstrategiaPorMMR(), 
            conductaService
        );
        
        usuarioActual = new MockUsuarioActualPort();
        scrimController = new ScrimController(scrimService, usuarioService, usuarioActual);

        // Bucle principal
        boolean salir = false;
        while (!salir) {
            if (usuarioActual.obtenerEmailUsuarioActual() == null) {
                // No hay usuario logueado - mostrar menú de login
                mostrarMenuLogin();
                int opcion = leerEntero();
                switch (opcion) {
                    case 1:
                        hacerLogin();
                        break;
                    case 2:
                        salir = true;
                        System.out.println("¡Hasta luego!");
                        break;
                    default:
                        System.out.println("Opción inválida.");
                }
            } else {
                // Usuario logueado - mostrar menú según rol
                Rol rol = usuarioActual.obtenerRolUsuarioActual();
                if (rol == Rol.PLAYER) {
                    mostrarMenuJugador();
                } else {
                    mostrarMenuOrganizer();
                }
                int opcion = leerEntero();
                salir = procesarOpcion(opcion, rol);
            }
        }
        scanner.close();
    }

    private static void mostrarMenuLogin() {
        System.out.println("\n=== LOGIN ===");
        System.out.println("1. Iniciar sesión");
        System.out.println("2. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private static void hacerLogin() {
        System.out.print("Ingrese su email: ");
        String email = scanner.nextLine().trim();
        
        try {
            Usuario usuario = usuarioService.buscar(email);
            usuarioActual.establecerUsuarioActual(usuario.getEmail(), usuario.getRol());
            System.out.println("¡Bienvenido, " + usuario.getNickname() + "! (Rol: " + usuario.getRol() + ")");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void mostrarMenuJugador() {
        System.out.println("\n=== MENÚ JUGADOR ===");
        System.out.println("Usuario: " + usuarioActual.obtenerEmailUsuarioActual());
        System.out.println("1. Listar Scrims");
        System.out.println("2. Unirse a Scrim");
        System.out.println("3. Cerrar sesión");
        System.out.print("Seleccione una opción: ");
    }

    private static void mostrarMenuOrganizer() {
        System.out.println("\n=== MENÚ ORGANIZER ===");
        System.out.println("Usuario: " + usuarioActual.obtenerEmailUsuarioActual());
        System.out.println("1. Listar Scrims");
        System.out.println("2. Crear Scrim");
        System.out.println("3. Unirse a Scrim");
        System.out.println("4. Salir de Scrim");
        System.out.println("5. Confirmar Jugador");
        System.out.println("6. Programar Scrim");
        System.out.println("7. Limpiar Agenda");
        System.out.println("8. Iniciar Scrim");
        System.out.println("9. Finalizar Scrim");
        System.out.println("10. Cancelar Scrim");
        System.out.println("11. Cargar Resultado");
        System.out.println("12. Agregar Suplente");
        System.out.println("13. Cerrar sesión");
        System.out.print("Seleccione una opción: ");
    }

    private static boolean procesarOpcion(int opcion, Rol rol) {
        try {
            if (rol == Rol.PLAYER) {
                return procesarOpcionJugador(opcion);
            } else {
                return procesarOpcionOrganizer(opcion);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    private static boolean procesarOpcionJugador(int opcion) {
        switch (opcion) {
            case 1:
                // Listar Scrims
                System.out.println("\n=== SCRIMS DISPONIBLES ===");
                scrimController.listar();
                break;
            case 2:
                // Unirse a Scrim
                System.out.print("Ingrese ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                String emailJugador = usuarioActual.obtenerEmailUsuarioActual();
                scrimController.unirse(idScrim, emailJugador);
                break;
            case 3:
                // Cerrar sesión
                usuarioActual.limpiarUsuarioActual();
                System.out.println("Sesión cerrada.");
                return false;
            default:
                System.out.println("Opción inválida.");
        }
        return false;
    }

    private static boolean procesarOpcionOrganizer(int opcion) {
        switch (opcion) {
            case 1:
                // Listar Scrims
                System.out.println("\n=== SCRIMS DISPONIBLES ===");
                scrimController.listar();
                break;
            case 2:
                // Crear Scrim
                System.out.print("Juego: ");
                String juego = scanner.nextLine().trim();
                System.out.print("Email creador: ");
                String emailCreador = scanner.nextLine().trim();
                System.out.print("Email rival: ");
                String emailRival = scanner.nextLine().trim();
                System.out.print("Rango mínimo: ");
                int rangoMin = leerEntero();
                System.out.print("Rango máximo: ");
                int rangoMax = leerEntero();
                scrimController.crear(juego, emailCreador, emailRival, rangoMin, rangoMax);
                break;
            case 3:
                // Unirse a Scrim
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador: ");
                String emailJugador = scanner.nextLine().trim();
                scrimController.unirse(idScrim, emailJugador);
                break;
            case 4:
                // Salir de Scrim
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador: ");
                emailJugador = scanner.nextLine().trim();
                scrimController.salir(idScrim, emailJugador);
                break;
            case 5:
                // Confirmar Jugador
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador: ");
                emailJugador = scanner.nextLine().trim();
                scrimController.confirmar(idScrim, emailJugador);
                break;
            case 6:
                // Programar Scrim
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                System.out.print("Fecha inicio (yyyy-MM-dd HH:mm): ");
                String inicioStr = scanner.nextLine().trim();
                System.out.print("Fecha fin (yyyy-MM-dd HH:mm): ");
                String finStr = scanner.nextLine().trim();
                scrimController.programar(idScrim, inicioStr, finStr);
                break;
            case 7:
                // Limpiar Agenda
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                scrimController.limpiarAgenda(idScrim);
                break;
            case 8:
                // Iniciar Scrim
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                scrimController.iniciar(idScrim);
                break;
            case 9:
                // Finalizar Scrim
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                scrimController.finalizar(idScrim);
                break;
            case 10:
                // Cancelar Scrim
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                scrimController.cancelar(idScrim);
                break;
            case 11:
                // Cargar Resultado
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador: ");
                emailJugador = scanner.nextLine().trim();
                System.out.print("Kills: ");
                int kills = leerEntero();
                System.out.print("Assists: ");
                int assists = leerEntero();
                System.out.print("Deaths: ");
                int deaths = leerEntero();
                System.out.print("Rating: ");
                double rating = leerDouble();
                scrimController.cargarResultado(idScrim, emailJugador, kills, assists, deaths, rating);
                break;
            case 12:
                // Agregar Suplente
                System.out.print("ID del scrim: ");
                idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador: ");
                emailJugador = scanner.nextLine().trim();
                scrimController.agregarSuplente(idScrim, emailJugador);
                break;
            case 13:
                // Cerrar sesión
                usuarioActual.limpiarUsuarioActual();
                System.out.println("Sesión cerrada.");
                return false;
            default:
                System.out.println("Opción inválida.");
        }
        return false;
    }

    private static int leerEntero() {
        try {
            String input = scanner.nextLine().trim();
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.print("Por favor ingrese un número válido: ");
            return leerEntero();
        }
    }

    private static double leerDouble() {
        try {
            String input = scanner.nextLine().trim();
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            System.out.print("Por favor ingrese un número válido: ");
            return leerDouble();
        }
    }
}
