package ar.edu.tpo;

import ar.edu.tpo.controller.ScrimController;
import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Organizador;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.regiones.StateRegion;
import ar.edu.tpo.domain.roles.StateRoles;
import ar.edu.tpo.repository.JsonScrimRepository;
import ar.edu.tpo.repository.JsonUsuarioRepository;
import ar.edu.tpo.service.ConductaService;
import ar.edu.tpo.service.MockUsuarioActualPort;
import ar.edu.tpo.service.UsuarioService;
import ar.edu.tpo.service.scrim.ScrimCicloDeVidaService;
import ar.edu.tpo.service.scrim.ScrimLobbyService;
import ar.edu.tpo.service.scrim.ScrimSchedulerService;
import ar.edu.tpo.service.scrim.ScrimStatsService;

import java.time.Duration;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static MockUsuarioActualPort usuarioActual;
    private static ScrimController scrimController;
    private static UsuarioService usuarioService;

    public static void main(String[] args) {
        JsonScrimRepository scrimRepo = new JsonScrimRepository("data/scrims.json");
        JsonUsuarioRepository usuarioRepo = new JsonUsuarioRepository("data/usuarios.json");

        usuarioService = new UsuarioService(usuarioRepo);
        ConductaService conductaService = new ConductaService(usuarioService);
        ScrimCicloDeVidaService scrimLifecycleService = new ScrimCicloDeVidaService(scrimRepo, usuarioService);
        ScrimLobbyService scrimLobbyService = new ScrimLobbyService(scrimRepo, usuarioService, conductaService);
        ScrimStatsService scrimStatsService = new ScrimStatsService(scrimRepo, usuarioService);

        usuarioActual = new MockUsuarioActualPort();
        scrimController = new ScrimController(scrimLifecycleService, scrimLobbyService, scrimStatsService, usuarioActual);

        ScrimSchedulerService schedulerService = ScrimSchedulerService.getInstance(scrimRepo, scrimLifecycleService);
        schedulerService.iniciar(30);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[sistema] Cerrando scheduler...");
            schedulerService.detener();
        }));

        boolean salir = false;
        while (!salir) {
            Usuario usuarioLogueado = usuarioActual.obtenerUsuarioActual();
            if (usuarioLogueado == null) {
                mostrarMenuLogin();
                int opcion = leerEntero();
                switch (opcion) {
                    case 1 -> hacerLogin();
                    case 2 -> registrarUsuario();
                    case 0 -> {
                        salir = true;
                        System.out.println("¡Hasta luego!");
                    }
                    default -> System.out.println("Opción inválida.");
                }
            } else {
                if (usuarioLogueado instanceof Jugador jugador) {
                    mostrarMenuJugador(jugador);
                } else if (usuarioLogueado instanceof Organizador organizador) {
                    mostrarMenuOrganizer(organizador);
                } else {
                    System.out.println("Tipo de usuario no soportado: " + usuarioLogueado.getTipo());
                    usuarioActual.limpiarUsuarioActual();
                    continue;
                }
                int opcion = leerEntero();
                salir = procesarOpcion(opcion, usuarioLogueado) || salir;
            }
        }
        scanner.close();
    }

    private static void mostrarMenuLogin() {
        System.out.println("\n=== LOGIN ===");
        System.out.println("1. Iniciar sesión");
        System.out.println("2. Registrarse");
        System.out.println("0. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private static void hacerLogin() {
        String email = leerNoVacio("Ingrese su email: ");
        String password = leerNoVacio("Ingrese su contraseña: ");

        try {
            Usuario usuario = usuarioService.login(email, password);
            usuarioActual.establecerUsuarioActual(usuario);
            System.out.println("¡Bienvenido, " + usuario.getEmail() + "! (Tipo: " + usuario.getTipo() + ")");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void mostrarMenuJugador(Jugador jugador) {
        System.out.println("\n=== MENÚ JUGADOR ===");
        System.out.println("Usuario: " + jugador.getEmail());
        System.out.println("Rango: " + jugador.getRangoNombre() +
                " | Rol preferido: " + jugador.getRolNombre() +
                " | Región: " + jugador.getRegionNombre());
        System.out.println("1. Listar Scrims");
        System.out.println("2. Unirse a Scrim");
        System.out.println("3. Salir de Scrim");
        System.out.println("4. Confirmar participación en Scrim");
        System.out.println("0. Cerrar sesión");
        System.out.print("Seleccione una opción: ");
    }

    private static void mostrarMenuOrganizer(Organizador organizador) {
        System.out.println("\n=== MENÚ ORGANIZER ===");
        System.out.println("Usuario: " + organizador.getEmail());
        System.out.println("1. Listar Scrims");
        System.out.println("2. Crear Scrim");
        System.out.println("3. Confirmar Jugador");
        System.out.println("4. Sacar Jugador");
        System.out.println("5. Programar Scrim");
        System.out.println("6. Eliminar programación (limpiar fechas)");
        System.out.println("7. Iniciar Scrim");
        System.out.println("8. Finalizar Scrim");
        System.out.println("9. Cancelar Scrim");
        System.out.println("10. Cargar Resultado");
        System.out.println("11. Agregar Suplente");
        System.out.println("12. Agregar sanción a jugador");
        System.out.println("0. Cerrar sesión");
        System.out.print("Seleccione una opción: ");
    }

    private static boolean procesarOpcion(int opcion, Usuario usuario) {
        try {
            if (usuario instanceof Jugador jugador) {
                return procesarOpcionJugador(opcion, jugador);
            } else if (usuario instanceof Organizador) {
                return procesarOpcionOrganizer(opcion);
            } else {
                throw new IllegalStateException("Tipo de usuario no soportado: " + usuario.getTipo());
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    private static boolean procesarOpcionJugador(int opcion, Jugador jugador) {
        switch (opcion) {
            case 0 -> {
                usuarioActual.limpiarUsuarioActual();
                System.out.println("Sesión cerrada.");
            }
            case 1 -> {
                System.out.println("\n=== SCRIMS DISPONIBLES ===");
                scrimController.listar();
            }
            case 2 -> {
                System.out.print("Ingrese ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                System.out.print("Nombre del equipo (o vacío para auto-asignar): ");
                String nombreEquipo = scanner.nextLine().trim();
                String emailJugador = jugador.getEmail();
                if (nombreEquipo.isBlank()) {
                    scrimController.unirse(idScrim, emailJugador);
                } else {
                    scrimController.unirseAEquipo(idScrim, emailJugador, nombreEquipo);
                }
            }
            case 3 -> {
                System.out.print("Ingrese ID del scrim del que desea salir: ");
                String idScrimSalir = scanner.nextLine().trim();
                scrimController.salir(idScrimSalir, jugador.getEmail());
            }
            case 4 -> {
                System.out.print("Ingrese ID del scrim a confirmar: ");
                String idScrimConfirmar = scanner.nextLine().trim();
                scrimController.confirmar(idScrimConfirmar, jugador.getEmail());
            }
            default -> System.out.println("Opción inválida.");
        }
        return false;
    }

    private static boolean procesarOpcionOrganizer(int opcion) {
        switch (opcion) {
            case 0 -> {
                usuarioActual.limpiarUsuarioActual();
                System.out.println("Sesión cerrada.");
            }
            case 1 -> {
                System.out.println("\n=== SCRIMS DISPONIBLES ===");
                scrimController.listar();
            }
            case 2 -> {
                System.out.print("Juego: ");
                String juego = scanner.nextLine().trim();
                String emailCreador = usuarioActual.obtenerEmailUsuarioActual();
                StateRangos rangoMinObj = pedirRango("mínimo");
                StateRangos rangoMaxObj = pedirRango("máximo");
                int rangoMin = rangoMinObj.getMinimo();
                int rangoMax = rangoMaxObj.getMaximo();
                if (rangoMax < rangoMin) {
                    int tmp = rangoMin;
                    rangoMin = rangoMax;
                    rangoMax = tmp;
                }
                int cupo = seleccionarCupo();
                String formato = seleccionarFormato(cupo);
                String region = seleccionarRegion();
                System.out.print("Latencia máxima (ms): ");
                int latenciaMax = leerEntero();
                String modalidad = seleccionarModalidad();
                System.out.print("Inicio (yyyy-MM-dd HH:mm, hora Argentina) o vacío: ");
                String inicioStr = scanner.nextLine().trim();
                System.out.print("Fin (yyyy-MM-dd HH:mm, hora Argentina) o vacío: ");
                String finStr = scanner.nextLine().trim();
                scrimController.crear(juego, emailCreador, rangoMin, rangoMax,
                        cupo, formato, region, latenciaMax, modalidad,
                        inicioStr, finStr);
            }
            case 3 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador: ");
                String emailJugador = scanner.nextLine().trim();
                scrimController.confirmar(idScrim, emailJugador);
            }
            case 4 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador a quitar: ");
                String emailJugador = scanner.nextLine().trim();
                scrimController.salir(idScrim, emailJugador);
            }
            case 5 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                System.out.print("Fecha inicio (yyyy-MM-dd HH:mm, hora Argentina): ");
                String inicio = scanner.nextLine().trim();
                System.out.print("Fecha fin (yyyy-MM-dd HH:mm, hora Argentina): ");
                String fin = scanner.nextLine().trim();
                scrimController.programar(idScrim, inicio, fin);
            }
            case 6 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                scrimController.limpiarAgenda(idScrim);
            }
            case 7 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                scrimController.iniciar(idScrim);
            }
            case 8 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                scrimController.finalizar(idScrim);
            }
            case 9 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                scrimController.cancelar(idScrim);
            }
            case 10 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador: ");
                String emailJugador = scanner.nextLine().trim();
                System.out.print("Kills: ");
                int kills = leerEntero();
                System.out.print("Assists: ");
                int assists = leerEntero();
                System.out.print("Deaths: ");
                int deaths = leerEntero();
                System.out.print("Rating: ");
                double rating = leerDouble();
                scrimController.cargarResultado(idScrim, emailJugador, kills, assists, deaths, rating);
            }
            case 11 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                System.out.print("Email del jugador: ");
                String emailJugador = scanner.nextLine().trim();
                scrimController.agregarSuplente(idScrim, emailJugador);
            }
            case 12 -> {
                String email = leerNoVacio("Email del jugador a sancionar: ");
                String motivo = seleccionarMotivoSancion();
                int minutos = leerEnteroConMensaje("Duración de la sanción en minutos: ");
                try {
                    usuarioService.agregarSancion(email, motivo, Duration.ofMinutes(minutos));
                    System.out.println("Sanción registrada para " + email + " con motivo " + motivo);
                } catch (Exception e) {
                    System.out.println("Error al registrar sanción: " + e.getMessage());
                }
            }
            default -> System.out.println("Opción inválida.");
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

    private static String leerNoVacio(String mensaje) {
        System.out.print(mensaje);
        String value = scanner.nextLine().trim();
        while (value.isEmpty()) {
            System.out.print("El valor no puede ser vacío. Intente nuevamente: ");
            value = scanner.nextLine().trim();
        }
        return value;
    }

    private static int leerEnteroConMensaje(String mensaje) {
        System.out.print(mensaje);
        return leerEntero();
    }

    private static StateRoles pedirRol() {
        while (true) {
            System.out.println("Roles disponibles:");
            for (StateRoles rol : StateRoles.disponibles()) {
                System.out.println("- " + rol.getNombre());
            }
            String rolStr = leerNoVacio("Rol preferido: ");
            StateRoles rol = StateRoles.fromNombre(rolStr);
            if (rol != null) {
                return rol;
            }
            System.out.println("Rol inválido. Intente nuevamente.");
        }
    }

    private static StateRegion pedirRegion() {
        while (true) {
            System.out.println("Regiones disponibles:");
            var disponibles = StateRegion.disponibles();
            for (int i = 0; i < disponibles.size(); i++) {
                System.out.println((i + 1) + ". " + disponibles.get(i).getNombre());
            }
            int opcion = leerEnteroConMensaje("Seleccione una región: ");
            if (opcion >= 1 && opcion <= disponibles.size()) {
                return disponibles.get(opcion - 1);
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }

    private static StateRangos pedirRango(String etiqueta) {
        while (true) {
            System.out.println("Rangos disponibles:");
            var disponibles = StateRangos.disponibles();
            for (int i = 0; i < disponibles.size(); i++) {
                StateRangos rango = disponibles.get(i);
                System.out.println((i + 1) + ". " + rango.getNombre() + " (" + rango.getMinimo() + " - " + rango.getMaximo() + ")");
            }
            int opcion = leerEnteroConMensaje("Seleccione rango " + etiqueta + ": ");
            if (opcion >= 1 && opcion <= disponibles.size()) {
                return disponibles.get(opcion - 1);
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }

    private static void registrarUsuario() {
        System.out.println("\n=== REGISTRO DE USUARIO ===");
        System.out.println("1. Registrar Organizador");
        System.out.println("2. Registrar Jugador");
        System.out.print("Seleccione una opción: ");
        int opcion = leerEntero();

        switch (opcion) {
            case 1 -> registrarOrganizadorFlow();
            case 2 -> registrarJugadorFlow();
            default -> System.out.println("Opción inválida.");
        }
    }

    private static void registrarOrganizadorFlow() {
        String email = leerNoVacio("Email del organizador: ");
        String password = leerNoVacio("Contraseña: ");
        try {
            usuarioService.registrarOrganizador(email, password);
            System.out.println("Organizador registrado exitosamente.");
        } catch (Exception e) {
            System.out.println("Error al registrar organizador: " + e.getMessage());
        }
    }

    private static void registrarJugadorFlow() {
        String email = leerNoVacio("Email del jugador: ");
        String password = leerNoVacio("Contraseña: ");
        System.out.println("Ingrese datos del perfil competitivo:");
        int mmr = leerEnteroConMensaje("MMR (número entero): ");
        int latencia = leerEnteroConMensaje("Latencia promedio (ms): ");

        StateRoles rolPreferido = pedirRol();
        StateRegion region = pedirRegion();
        StateRangos rango = StateRangos.asignarRangoSegunPuntos(mmr);

        try {
            usuarioService.registrarJugador(email, password, mmr, latencia, rango, rolPreferido, region);
            System.out.println("Jugador registrado exitosamente. Rango asignado: " + rango.getNombre());
        } catch (Exception e) {
            System.out.println("Error al registrar jugador: " + e.getMessage());
        }
    }

    private static String seleccionarMotivoSancion() {
        String[] motivos = {"Abandono", "NoShow", "Strike", "Cooldown"};
        System.out.println("Motivos disponibles:");
        for (int i = 0; i < motivos.length; i++) {
            System.out.println((i + 1) + ". " + motivos[i]);
        }
        while (true) {
            int opcion = leerEnteroConMensaje("Seleccione un motivo (1-" + motivos.length + "): ");
            if (opcion >= 1 && opcion <= motivos.length) {
                return motivos[opcion - 1];
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }

    private static int seleccionarCupo() {
        int[] opciones = {5, 3, 1};
        System.out.println("Jugadores por equipo:");
        System.out.println("1. 5 (5v5)");
        System.out.println("2. 3 (3v3)");
        System.out.println("3. 1 (1v1)");
        while (true) {
            int opcion = leerEnteroConMensaje("Seleccione una opción: ");
            if (opcion >= 1 && opcion <= opciones.length) {
                return opciones[opcion - 1];
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }

    private static String seleccionarFormato(int jugadoresPorEquipo) {
        return switch (jugadoresPorEquipo) {
            case 5 -> "5v5";
            case 3 -> "3v3";
            case 1 -> "1v1";
            default -> jugadoresPorEquipo + "v" + jugadoresPorEquipo;
        };
    }

    private static String seleccionarRegion() {
        return pedirRegion().getNombre();
    }

    private static String seleccionarModalidad() {
        String[] modalidades = {"ranked-like", "casual", "práctica"};
        System.out.println("Modalidades disponibles:");
        for (int i = 0; i < modalidades.length; i++) {
            System.out.println((i + 1) + ". " + modalidades[i]);
        }
        while (true) {
            int opcion = leerEnteroConMensaje("Seleccione modalidad: ");
            if (opcion >= 1 && opcion <= modalidades.length) {
                return modalidades[opcion - 1];
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }
}

