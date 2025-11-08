package ar.edu.tpo;

import ar.edu.tpo.controller.*;
import ar.edu.tpo.repository.*;
import ar.edu.tpo.service.*;
import ar.edu.tpo.service.estrategias.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Wiring (manual)
        ScrimRepository scrimRepo = new JsonScrimRepository("data/scrims.json");
        UsuarioRepository usuarioRepo = new JsonUsuarioRepository("data/usuarios.json");

        UsuarioService usuarioService = new UsuarioService(usuarioRepo);
        ConductaService conductaService = new ConductaService();

        EstrategiaEmparejamiento estrategia = new EstrategiaPorMMR();
        ScrimService scrimService = new ScrimService(scrimRepo, usuarioService, estrategia, conductaService);

        // Implementación temporal del puerto de usuario actual
        // En producción, este será proporcionado por el módulo de login/usuario
        MockUsuarioActualPort usuarioActualPort = new MockUsuarioActualPort();
        // Por defecto, establecer como ORGANIZER (a@a.com) para mantener compatibilidad
        // TODO: El módulo de login deberá proporcionar la implementación real de UsuarioActualPort
        usuarioActualPort.establecerUsuarioActual("a@a.com", ar.edu.tpo.domain.Rol.ORGANIZER);

        UsuarioController usuarioController = new UsuarioController(usuarioService);
        ScrimController scrimController = new ScrimController(scrimService, usuarioService, usuarioActualPort);
        ConductaController conductaController = new ConductaController(conductaService);

        // Iniciar scheduler para transiciones automáticas (revisa cada 30 segundos)
        ScrimSchedulerService schedulerService = new ScrimSchedulerService(scrimRepo, scrimService);
        schedulerService.iniciar(30);

        // Agregar shutdown hook para detener el scheduler al cerrar la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[sistema] Cerrando scheduler...");
            schedulerService.detener();
        }));

        Scanner sc = new Scanner(System.in);
        boolean seguir = true;

        while (seguir) {
            System.out.println("==============================");
            System.out.println("       SISTEMA DE SCRIMS");
            System.out.println("==============================");
            System.out.println("1) Usuarios");
            System.out.println("2) Scrims");
            System.out.println("3) Conducta");
            System.out.println("4) Estrategia de emparejamiento");
            System.out.println("0) Salir");
            System.out.print("> ");
            String op = sc.nextLine().trim();

            try {
                switch (op) {
                    case "1" -> usuarioMenu(usuarioController, sc);
                    case "2" -> scrimMenu(scrimController, sc);
                    case "3" -> conductaMenu(conductaController, sc);
                    case "4" -> estrategiaMenu(scrimService, sc);
                    case "0" -> seguir = false;
                    default -> System.out.println("Opción inválida.");
                }
            } catch (Exception e) {
                System.out.println("⚠ Error: " + e.getMessage());
            }
        }
        System.out.println("¡Chau!");
    }

    // ===== Usuarios =====
    private static void usuarioMenu(UsuarioController uc, Scanner sc) {
        System.out.println("1) Registrar usuario");
        System.out.println("2) Listar usuarios");
        System.out.println("3) Buscar usuario por email");
        System.out.println("0) Volver");
        System.out.print("> ");
        String op = sc.nextLine().trim();
        switch (op) {
            case "1" -> {
                System.out.print("Email: "); String email = sc.nextLine();
                System.out.print("Nickname: "); String nick = sc.nextLine();
                System.out.print("MMR: "); int mmr = Integer.parseInt(sc.nextLine());
                System.out.print("Latencia promedio (ms): "); int ping = Integer.parseInt(sc.nextLine());
                System.out.print("Rol (PLAYER/ORGANIZER): "); String rolStr = sc.nextLine().trim().toUpperCase();
                ar.edu.tpo.domain.Rol rol;
                try {
                    rol = ar.edu.tpo.domain.Rol.valueOf(rolStr);
                } catch (IllegalArgumentException e) {
                    System.out.println("Rol inválido. Usando PLAYER por defecto.");
                    rol = ar.edu.tpo.domain.Rol.PLAYER;
                }
                uc.registrar(email, nick, mmr, ping, rol);
            }
            case "2" -> uc.listar();
            case "3" -> {
                System.out.print("Email: "); String email = sc.nextLine();
                uc.buscar(email);
            }
            case "0" -> { return; }
            default -> System.out.println("Opción inválida.");
        }
    }

    // ===== Scrims (nuevo flujo con cupo/lobby/confirmaciones) =====
    private static void scrimMenu(ScrimController scCtrl, Scanner sc) {
        System.out.println("1) Crear scrim (cupo + fechas opcionales)");
        System.out.println("2) Listar scrims");
        System.out.println("3) Unirse a scrim");
        System.out.println("4) Quitar jugador");
        System.out.println("5) Confirmar jugador");
        System.out.println("6) Programar/Reprogramar fechas");
        System.out.println("7) Iniciar scrim (CONFIRMADO → EN_JUEGO)");
        System.out.println("8) Finalizar scrim (EN_JUEGO → FINALIZADO)");
        System.out.println("9) Cancelar scrim");
        System.out.println("10) Cargar resultado (K/A/D + rating)");
        System.out.println("11) Agregar suplente (lista de espera)");
        System.out.println("0) Volver");
        System.out.print("> ");
        String op = sc.nextLine().trim();

        switch (op) {
            case "1" -> {
                System.out.print("Juego: "); String juego = sc.nextLine();
                System.out.print("Creador (email): "); String creador = sc.nextLine();
                System.out.print("Rival (email): "); String rival = sc.nextLine();
                System.out.print("Rango mínimo MMR: "); int min = Integer.parseInt(sc.nextLine());
                System.out.print("Rango máximo MMR: "); int max = Integer.parseInt(sc.nextLine());
                System.out.print("Jugadores por equipo (p.ej. 5 para 5v5): "); int cupo = Integer.parseInt(sc.nextLine());
                System.out.print("Inicio (yyyy-MM-dd HH:mm, hora Argentina) o vacío: "); String inicio = sc.nextLine();
                System.out.print("Fin    (yyyy-MM-dd HH:mm, hora Argentina) o vacío: "); String fin = sc.nextLine();
                scCtrl.crear(juego, creador, rival, min, max, cupo, inicio, fin);
            }
            case "2" -> scCtrl.listar();
            case "3" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                System.out.print("Email jugador: "); String email = sc.nextLine();
                System.out.print("Nombre del equipo (o vacío para auto-asignar): "); String equipo = sc.nextLine().trim();
                if (equipo.isBlank()) {
                    scCtrl.unirse(id, email);
                } else {
                    scCtrl.unirseAEquipo(id, email, equipo);
                }
            }
            case "4" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                System.out.print("Email jugador: "); String email = sc.nextLine();
                scCtrl.salir(id, email);
            }
            case "5" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                System.out.print("Email jugador: "); String email = sc.nextLine();
                scCtrl.confirmar(id, email);
            }
            case "6" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                System.out.print("Inicio (yyyy-MM-dd HH:mm, hora Argentina): "); String ini = sc.nextLine();
                System.out.print("Fin    (yyyy-MM-dd HH:mm, hora Argentina): "); String fin = sc.nextLine();
                scCtrl.programar(id, ini, fin);
            }
            case "7" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                scCtrl.iniciar(id);
            }
            case "8" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                scCtrl.finalizar(id);
            }
            case "9" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                scCtrl.cancelar(id);
            }
            case "10" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                System.out.print("Email jugador: "); String email = sc.nextLine();
                System.out.print("Kills: "); int k = Integer.parseInt(sc.nextLine());
                System.out.print("Assists: "); int a = Integer.parseInt(sc.nextLine());
                System.out.print("Deaths: "); int d = Integer.parseInt(sc.nextLine());
                System.out.print("Rating (0-10): "); double r = Double.parseDouble(sc.nextLine());
                scCtrl.cargarResultado(id, email, k, a, d, r);
            }
            case "11" -> {
                System.out.print("ID scrim: "); String id = sc.nextLine();
                System.out.print("Email jugador suplente: "); String email = sc.nextLine();
                scCtrl.agregarSuplente(id, email);
            }
            case "0" -> { return; }
            default -> System.out.println("Opción inválida.");
        }
    }

    // ===== Conducta =====
    private static void conductaMenu(ConductaController cc, Scanner sc){
        System.out.println("1) Registrar Abandono");
        System.out.println("2) Registrar NoShow");
        System.out.println("3) Registrar Strike");
        System.out.println("4) Registrar Cooldown");
        System.out.println("5) Ver historial");
        System.out.println("0) Volver");
        System.out.print("> ");
        String op = sc.nextLine().trim();

        String email = null;
        if (!op.equals("5") && !op.equals("0")) {
            System.out.print("Email del jugador: ");
            email = sc.nextLine();
        }

        switch (op) {
            case "1" -> cc.registrarAbandono(email);
            case "2" -> cc.registrarNoShow(email);
            case "3" -> cc.registrarStrike(email);
            case "4" -> cc.registrarCooldown(email);
            case "5" -> cc.listarHistorial();
            case "0" -> { return; }
            default -> System.out.println("Opción inválida.");
        }
    }

    // ===== Estrategia =====
    private static void estrategiaMenu(ScrimService scrimService, Scanner sc) {
        System.out.println("Estrategia: 1) MMR  2) Latencia  3) KDA");
        System.out.println("0) Volver");
        System.out.print("> ");
        String op = sc.nextLine().trim();
        switch (op) {
            case "1" -> scrimService.cambiarEstrategia(new EstrategiaPorMMR());
            case "2" -> scrimService.cambiarEstrategia(new EstrategiaPorLatencia());
            case "3" -> scrimService.cambiarEstrategia(new EstrategiaPorKDA());
            case "0" -> { return; }
            default -> System.out.println("Opción inválida.");
        }
        if (!op.equals("0")) System.out.println("Estrategia actualizada.");
    }
}
