package ar.edu.tpo;

import ar.edu.tpo.controller.ScrimController;
import ar.edu.tpo.domain.Jugador;
import ar.edu.tpo.domain.Organizador;
import ar.edu.tpo.domain.SancionActiva;
import ar.edu.tpo.domain.SancionHistorica;
import ar.edu.tpo.domain.Usuario;
import ar.edu.tpo.domain.Scrim;
import ar.edu.tpo.domain.alerta.ScrimAlerta;
import ar.edu.tpo.domain.rangos.StateRangos;
import ar.edu.tpo.domain.regiones.StateRegion;
import ar.edu.tpo.domain.roles.StateRoles;
import ar.edu.tpo.notification.MailStrategy;
import ar.edu.tpo.notification.Notificador;
import ar.edu.tpo.notification.NotificationService;
import ar.edu.tpo.repository.JsonScrimRepository;
import ar.edu.tpo.repository.JsonUsuarioRepository;
import ar.edu.tpo.service.ConductaService;
import ar.edu.tpo.service.MockUsuarioActualPort;
import ar.edu.tpo.service.SancionSchedulerService;
import ar.edu.tpo.service.UsuarioService;
import ar.edu.tpo.service.scrim.ScrimCicloDeVidaService;
import ar.edu.tpo.service.scrim.ScrimLobbyService;
import ar.edu.tpo.service.scrim.ScrimSchedulerService;
import ar.edu.tpo.service.scrim.ScrimStatsService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static MockUsuarioActualPort usuarioActual;
    private static ScrimController scrimController;
    private static UsuarioService usuarioService;

    public static void main(String[] args) {
        JsonScrimRepository scrimRepo = new JsonScrimRepository("data/scrims.json");
        JsonUsuarioRepository usuarioRepo = new JsonUsuarioRepository("data/usuarios.json");

        String defaultRemitente = "no-reply@escrims.local";
        String defaultHost = "smtp.gmail.com";
        String defaultPortStr = "587";
        String defaultUser = "tposcrim@gmail.com"; // TODO: reemplazar con tu correo real
        String defaultPass = "pfjp sgbk fzhv ghin";      // TODO: reemplazar con contraseña o token de app
        String defaultFrom = "no-reply@escrims.local";
        String defaultStartTls = "true";

        String smtpHost = firstNonNull(System.getenv("SMTP_HOST"), defaultHost);
        String smtpPort = firstNonNull(System.getenv("SMTP_PORT"), defaultPortStr);
        String smtpUser = firstNonNull(System.getenv("SMTP_USER"), defaultUser);
        String smtpPass = firstNonNull(System.getenv("SMTP_PASS"), defaultPass);
        String smtpFrom = firstNonNull(System.getenv("SMTP_FROM"), defaultFrom);
        String smtpStartTls = firstNonNull(System.getenv("SMTP_STARTTLS"), defaultStartTls);

        MailStrategy mailStrategy;
        try {
            int port = Integer.parseInt(smtpPort);
            boolean usarStartTls = Boolean.parseBoolean(smtpStartTls);
            String remitente = (smtpFrom != null && !smtpFrom.isBlank()) ? smtpFrom : defaultRemitente;
            mailStrategy = MailStrategy.smtp(remitente, smtpHost, port, smtpUser, smtpPass, usarStartTls);
            System.out.println("[mail] SMTP configurado para host " + smtpHost + ":" + port);
        } catch (NumberFormatException e) {
            System.err.println("[mail] Puerto SMTP inválido (" + smtpPort + "). Se usará envío simulado.");
            mailStrategy = MailStrategy.consola(defaultRemitente);
        }
        Notificador notificador = new Notificador(mailStrategy);
        NotificationService notificationService = new NotificationService(notificador);

        usuarioService = new UsuarioService(usuarioRepo, notificationService);
        ConductaService conductaService = new ConductaService(usuarioService);
        ScrimCicloDeVidaService scrimLifecycleService = new ScrimCicloDeVidaService(scrimRepo, usuarioService, notificationService);
        ScrimLobbyService scrimLobbyService = new ScrimLobbyService(scrimRepo, usuarioService, conductaService, notificationService);
        ScrimStatsService scrimStatsService = new ScrimStatsService(scrimRepo, usuarioService);

        usuarioActual = new MockUsuarioActualPort();
        scrimController = new ScrimController(scrimLifecycleService, scrimLobbyService, scrimStatsService, usuarioActual);

        ScrimSchedulerService scrimSchedulerService = ScrimSchedulerService.getInstance(scrimRepo, scrimLifecycleService);
        scrimSchedulerService.iniciar(30);

        SancionSchedulerService sancionSchedulerService = SancionSchedulerService.getInstance(usuarioService);
        sancionSchedulerService.iniciar(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[sistema] Cerrando scheduler...");
            scrimSchedulerService.detener();
            sancionSchedulerService.detener();
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

    private static String firstNonNull(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
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
            System.out.println("¡Bienvenido, " + usuario.getNombre() + "! (Tipo: " + usuario.getTipo() + ")");
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
        System.out.println("1. Listar scrims");
        System.out.println("2. Unirse a scrim");
        System.out.println("3. Salir de scrim");
        System.out.println("4. Confirmar participación en scrim");
        System.out.println("5. Actualizar perfil");
        System.out.println("6. Guardar scrim favorita");
        System.out.println("7. Ver scrims favoritas");
        System.out.println("8. Configurar alerta de scrim");
        System.out.println("9. Ver alertas configuradas");
        System.out.println("0. Cerrar sesión");
        System.out.print("Seleccione una opción: ");
    }

    private static void mostrarMenuOrganizer(Organizador organizador) {
        System.out.println("\n=== MENÚ ORGANIZADOR ===");
        System.out.println("Usuario: " + organizador.getEmail());
        System.out.println("1. Listar Scrims");
        System.out.println("2. Crear Scrim");
        System.out.println("3. Confirmar Jugador");
        System.out.println("4. Sacar Jugador");
        System.out.println("5. Reprogramar fecha de Scrim");
        System.out.println("6. Iniciar Scrim");
        System.out.println("7. Finalizar Scrim");
        System.out.println("8. Cancelar Scrim");
        System.out.println("9. Cargar Resultado");
        System.out.println("10. Ver suplentes");
        System.out.println("11. Agregar sanción a jugador");
        System.out.println("12. Ver sanciones activas de un jugador");
        System.out.println("13. Ver sanciones históricas de un jugador");
        System.out.println("14. Levantar sanción de un jugador");
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
                String idScrim = leerIdScrimValido(false);
                if (idScrim != null) {
                    String emailJugador = jugador.getEmail();
                    boolean finalizado = false;
                    while (!finalizado) {
                        Scrim scrim;
                        try {
                            scrim = scrimController.buscar(idScrim);
                        } catch (Exception e) {
                            System.out.println("No se pudo encontrar el scrim: " + e.getMessage());
                            break;
                        }

                        String equipoSeleccionado = seleccionarEquipoNumerico();
                        String otroEquipo = equipoSeleccionado.equals("Equipo 1") ? "Equipo 2" : "Equipo 1";
                        boolean equipoSeleccionadoLleno = equipoLleno(scrim, equipoSeleccionado);
                        boolean otroEquipoLleno = equipoLleno(scrim, otroEquipo);

                        if (equipoSeleccionadoLleno) {
                            if (!otroEquipoLleno) {
                                boolean decisionTomada = false;
                                while (!decisionTomada) {
                                    System.out.println("El " + equipoSeleccionado + " está completo. ¿Deseás unirte al " + otroEquipo + "?");
                                    System.out.println("1. Sí");
                                    System.out.println("2. No, cancelar");
                                    int respuesta = leerEnteroConMensaje("Opción: ");
                                    if (respuesta == 1) {
                                        equipoSeleccionado = otroEquipo;
                                        decisionTomada = true;
                                    } else if (respuesta == 2) {
                                        System.out.println("Operación cancelada. No se unió a la scrim.");
                                        finalizado = true;
                                        decisionTomada = true;
                                    } else {
                                        System.out.println("Opción inválida. Intente nuevamente.");
                                    }
                                }
                                if (finalizado) {
                                    break;
                                }
                            } else {
                                boolean decisionTomada = false;
                                while (!decisionTomada) {
                                    System.out.println("Ambos equipos están completos. ¿Deseás ingresar a la lista de suplentes?");
                                    System.out.println("1. Sí, agregarme como suplente");
                                    System.out.println("2. No, cancelar");
                                    int respuesta = leerEnteroConMensaje("Opción: ");
                                    if (respuesta == 1) {
                                        try {
                                            scrimController.unirseAEquipo(idScrim, emailJugador, equipoSeleccionado);
                                        } catch (Exception e) {
                                            System.out.println("Error: " + e.getMessage());
                                        }
                                        finalizado = true;
                                        decisionTomada = true;
                                    } else if (respuesta == 2) {
                                        System.out.println("Operación cancelada. No se unió a la scrim.");
                                        finalizado = true;
                                        decisionTomada = true;
                                    } else {
                                        System.out.println("Opción inválida. Intente nuevamente.");
                                    }
                                }
                                break;
                            }
                        }

                        if (!finalizado) {
                            try {
                                scrimController.unirseAEquipo(idScrim, emailJugador, equipoSeleccionado);
                                finalizado = true;
                            } catch (Exception e) {
                                System.out.println("Error al unirse: " + e.getMessage());
                                finalizado = true;
                            }
                        }
                    }
                }
            }
            case 3 -> {
                String idScrimSalir = leerIdScrimValido();
                if (idScrimSalir == null) break;
                scrimController.salir(idScrimSalir, jugador.getEmail());
            }
            case 4 -> {
                String idScrimConfirmar = leerIdScrimValido();
                if (idScrimConfirmar == null) break;
                scrimController.confirmar(idScrimConfirmar, jugador.getEmail());
            }
            case 5 -> actualizarPerfilJugador(jugador);
            case 6 -> {
                String idScrimFavorito = leerNoVacio("Ingrese ID del scrim a guardar como favorito: ");
                try {
                    scrimController.buscar(idScrimFavorito);
                    boolean agregado = usuarioService.agregarScrimFavorita(jugador.getEmail(), idScrimFavorito);
                    if (agregado) {
                        usuarioActual.establecerUsuarioActual(usuarioService.buscar(jugador.getEmail()));
                        System.out.println("Scrim " + idScrimFavorito + " añadida a tus favoritas.");
                    } else {
                        System.out.println("La scrim ya estaba en tu lista de favoritas.");
                    }
                } catch (Exception e) {
                    System.out.println("No se pudo guardar como favorita: " + e.getMessage());
                }
            }
            case 7 -> {
                List<String> favoritas = usuarioService.obtenerScrimsFavoritas(jugador.getEmail());
                if (favoritas.isEmpty()) {
                    System.out.println("Aún no guardaste scrims favoritas.");
                } else {
                    System.out.println("\n=== SCRIMS FAVORITAS ===");
                    for (String id : favoritas) {
                        try {
                            Scrim scrim = scrimController.buscar(id);
                            System.out.println("* " + scrimController.formatearResumen(scrim));
                        } catch (Exception e) {
                            System.out.println("* " + id + " (scrim no encontrada)");
                        }
                    }
                }
            }
            case 8 -> {
                String juego = leerOpcional("Juego preferido (ENTER para cualquiera): ");
                String region = seleccionarRegionOpcionalLibre();
                Integer rangoMin = seleccionarRangoOpcional("Seleccione rango mínimo (0 = cualquiera): ", false);
                Integer rangoMax = seleccionarRangoOpcional("Seleccione rango máximo (0 = cualquiera): ", true);
                if (rangoMin != null && rangoMax != null && rangoMax < rangoMin) {
                    int tmp = rangoMin;
                    rangoMin = rangoMax;
                    rangoMax = tmp;
                }
                Integer latenciaMax = leerEnteroOpcionalNulo("Latencia máxima permitida (ENTER para omitir): ");
                String formato = seleccionarFormatoOpcional();
                ScrimAlerta alerta = new ScrimAlerta(juego, region, rangoMin, rangoMax, latenciaMax, formato);
                try {
                    usuarioService.agregarAlertaScrim(jugador.getEmail(), alerta);
                    usuarioActual.establecerUsuarioActual(usuarioService.buscar(jugador.getEmail()));
                    System.out.println("Alerta configurada correctamente.");
                } catch (Exception e) {
                    System.out.println("No se pudo configurar la alerta: " + e.getMessage());
                }
            }
            case 9 -> {
                List<ScrimAlerta> alertas = usuarioService.obtenerAlertasScrim(jugador.getEmail());
                if (alertas.isEmpty()) {
                    System.out.println("No tenés alertas configuradas.");
                } else {
                    System.out.println("\n=== ALERTAS CONFIGURADAS ===");
                    for (int i = 0; i < alertas.size(); i++) {
                        ScrimAlerta alerta = alertas.get(i);
                        System.out.println((i + 1) + ". Juego: " + valorOAny(alerta.getJuego()) +
                                " | Región: " + valorOAny(alerta.getRegion()) +
                                " | Formato: " + valorOAny(alerta.getFormato()) +
                                " | Rango: " + rangoTexto(alerta.getRangoMin(), alerta.getRangoMax()) +
                                " | Latencia máx: " + (alerta.getLatenciaMax() != null ? alerta.getLatenciaMax() + " ms" : "cualquiera"));
                    }
                }
            }
            default -> System.out.println("Opción inválida.");
        }
        return false;
    }

    private static void actualizarPerfilJugador(Jugador jugador) {
        System.out.println("\n=== ACTUALIZAR PERFIL ===");
        System.out.println("Deja el campo vacío para mantener el valor actual.");

        System.out.print("Nuevo password: ");
        String nuevoPassword = scanner.nextLine().trim();
        if (nuevoPassword.isBlank()) {
            nuevoPassword = jugador.getPasswordHash();
        }

        int nuevoMmr = leerEnteroOpcional("Nuevo MMR", jugador.getMmr());
        int nuevaLatencia = leerEnteroOpcional("Nueva latencia (ms)", jugador.getLatenciaMs());

        StateRoles rolActual = jugador.getRolPreferido();
        StateRoles nuevoRol = pedirRolOpcional(rolActual);

        StateRegion regionActual = jugador.getRegion();
        StateRegion nuevaRegion = pedirRegionOpcional(regionActual);

        StateRangos nuevoRango = StateRangos.asignarRangoSegunPuntos(nuevoMmr);

        Usuario actualizado = new Jugador(
                jugador.getId(),
                jugador.getNombre(),
                jugador.getEmail(),
                nuevoPassword,
                nuevoMmr,
                nuevaLatencia,
                nuevoRango.getNombre(),
                nuevoRol.getNombre(),
                nuevaRegion.getNombre(),
                new ArrayList<>(jugador.getSancionesActivasSinDepurar()),
                new ArrayList<>(jugador.getSancionesHistoricas()),
                jugador.getStrikeCount(),
                jugador.estaSuspendido(),
                new ArrayList<>(jugador.getScrimsFavoritas()),
                new ArrayList<>(jugador.getAlertasScrim())
        );

        usuarioService.actualizar(actualizado);
        usuarioActual.establecerUsuarioActual(actualizado);
        System.out.println("Perfil actualizado correctamente.");
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
                scrimController.iniciar(idScrim);
            }
            case 7 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                scrimController.finalizar(idScrim);
            }
            case 8 -> {
                System.out.print("ID del scrim: ");
                String idScrim = scanner.nextLine().trim();
                scrimController.cancelar(idScrim);
            }
            case 9 -> {
                String idScrim = leerIdScrimValido();
                if (idScrim == null) {
                    break;
                }
                Scrim scrim;
                try {
                    scrim = scrimController.buscar(idScrim);
                } catch (Exception e) {
                    System.out.println("No se pudo encontrar el scrim: " + e.getMessage());
                    break;
                }
                List<String> jugadores = new ArrayList<>(scrim.getJugadores());
                if (jugadores.isEmpty()) {
                    System.out.println("El scrim no tiene jugadores registrados.");
                    break;
                }
                boolean completado = false;
                while (!completado) {
                    List<RegistroResultado> resultados = new ArrayList<>();
                    System.out.println("\nCargando resultados para el scrim " + scrim.getId() +
                            " (" + scrim.getEstado().getNombre() + ").");
                    for (String email : jugadores) {
                        System.out.println("\nJugador: " + email);
                        int kills = leerEnteroConMensaje("  Kills: ");
                        int assists = leerEnteroConMensaje("  Assists: ");
                        int deaths = leerEnteroConMensaje("  Deaths: ");
                        double rating = leerDoubleConMensaje("  Rating: ");
                        resultados.add(new RegistroResultado(email, kills, assists, deaths, rating));
                    }

                    System.out.println("\nResumen ingresado:");
                    resultados.forEach(r -> System.out.println("  " + r.email() +
                            " -> K:" + r.kills() + " / A:" + r.assists() + " / D:" + r.deaths() +
                            " | Rating: " + r.rating()));

                    System.out.println("\n¿Desea guardar estos resultados?");
                    System.out.println("1. Guardar y salir");
                    System.out.println("2. Volver a cargar");
                    System.out.println("3. Cancelar sin guardar");
                    int decision = leerEnteroConMensaje("Opción: ");
                    if (decision == 1) {
                        boolean exito = true;
                        for (RegistroResultado r : resultados) {
                            try {
                                scrimController.cargarResultado(idScrim, r.email(), r.kills(), r.assists(), r.deaths(), r.rating());
                            } catch (Exception ex) {
                                System.out.println("Error al guardar resultados para " + r.email() + ": " + ex.getMessage());
                                exito = false;
                                break;
                            }
                        }
                        if (exito) {
                            System.out.println("Resultados guardados correctamente.");
                            completado = true;
                        } else {
                            System.out.println("No se pudieron guardar todos los resultados. Intente nuevamente.");
                        }
                    } else if (decision == 2) {
                        System.out.println("Reiniciando carga de resultados...");
                    } else if (decision == 3) {
                        System.out.println("Operación cancelada. No se guardaron resultados.");
                        completado = true;
                    } else {
                        System.out.println("Opción inválida. Se volverá a pedir la confirmación.");
                    }
                }
            }
            case 10 -> {
                String idScrim = leerIdScrimValido();
                if (idScrim == null) break;
                scrimController.mostrarSuplentes(idScrim);
            }
            case 11 -> {
                String email = leerNoVacio("Email del jugador a sancionar: ");
                String motivo = seleccionarMotivoSancion();
                String consecuencia = seleccionarConsecuenciaSancion();
                try {
                    if ("Strike".equalsIgnoreCase(consecuencia)) {
                        int strikes = usuarioService.aplicarStrike(email, motivo);
                        if (strikes >= 3) {
                            System.out.println("Se aplicó el tercer strike. La cuenta de " + email + " queda suspendida.");
                        } else {
                            String duracion = strikes == 1 ? "24 horas" : "1 semana";
                            System.out.println("Strike #" + strikes + " aplicado a " + email + " (" + duracion + ").");
                        }
                    } else {
                        int minutos = leerEnteroConMensaje("Duración del cooldown en minutos: ");
                        usuarioService.aplicarCooldown(email, motivo, Duration.ofMinutes(minutos));
                        System.out.println("Cooldown aplicado a " + email + " por " + minutos + " minutos.");
                    }
                } catch (Exception e) {
                    System.out.println("Error al registrar sanción: " + e.getMessage());
                }
            }
            case 12 -> {
                String email = leerNoVacio("Email del jugador: ");
                try {
                    List<SancionActiva> activas = usuarioService.obtenerSancionesActivas(email);
                    if (activas.isEmpty()) {
                        System.out.println("No hay sanciones activas para " + email);
                    } else {
                        System.out.println("Sanciones activas para " + email + ":");
                        for (int i = 0; i < activas.size(); i++) {
                            SancionActiva sancion = activas.get(i);
                            String expira = sancion.getExpiraEn() != null ? sancion.getExpiraEn().toString() : "sin fecha de expiración";
                            System.out.println((i + 1) + ". " + sancion.getMotivo() + " (expira: " + expira + ")");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error al listar sanciones: " + e.getMessage());
                }
            }
            case 13 -> {
                String email = leerNoVacio("Email del jugador: ");
                try {
                    List<SancionHistorica> historicas = usuarioService.obtenerSancionesHistoricas(email);
                    if (historicas.isEmpty()) {
                        System.out.println("No hay sanciones históricas para " + email);
                    } else {
                        System.out.println("Sanciones históricas para " + email + ":");
                        for (int i = 0; i < historicas.size(); i++) {
                            SancionHistorica sancion = historicas.get(i);
                            String expiro = sancion.getExpiraEn() != null ? sancion.getExpiraEn().toString() : "sin fecha de expiración";
                            System.out.println((i + 1) + ". " + sancion.getMotivo() + " (expiraba: " + expiro + ", levantada: " + sancion.getLevantadaEn() + ")");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error al listar sanciones: " + e.getMessage());
                }
            }
            case 14 -> {
                String email = leerNoVacio("Email del jugador: ");
                try {
                    List<SancionActiva> activas = usuarioService.obtenerSancionesActivas(email);
                    if (activas.isEmpty()) {
                        System.out.println("No hay sanciones activas para " + email);
                        break;
                    }
                    System.out.println("Sanciones activas para " + email + ":");
                    for (int i = 0; i < activas.size(); i++) {
                        SancionActiva sancion = activas.get(i);
                        String expira = sancion.getExpiraEn() != null ? sancion.getExpiraEn().toString() : "sin fecha de expiración";
                        System.out.println((i + 1) + ". " + sancion.getMotivo() + " (expira: " + expira + ")");
                    }
                    int seleccion = leerEnteroConMensaje("Seleccione la sanción a levantar (1-" + activas.size() + "): ");
                    if (seleccion < 1 || seleccion > activas.size()) {
                        System.out.println("Selección inválida.");
                        break;
                    }
                    var levantada = usuarioService.levantarSancion(email, seleccion - 1);
                    String expiro = levantada.getExpiraEn() != null ? levantada.getExpiraEn().toString() : "sin fecha de expiración";
                    System.out.println("Sanción '" + levantada.getMotivo() + "' levantada para " + email +
                            " (expiraba: " + expiro + ", levantada: " + levantada.getLevantadaEn() + ")");
                } catch (Exception e) {
                    System.out.println("Error al levantar sanción: " + e.getMessage());
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

    private static double leerDoubleConMensaje(String mensaje) {
        System.out.print(mensaje);
        return leerDouble();
    }

    private static boolean equipoLleno(Scrim scrim, String nombreEquipo) {
        if ("Equipo 1".equalsIgnoreCase(nombreEquipo)) {
            return scrim.getEquipo1().getCantidadJugadores() >= scrim.getCupo();
        }
        if ("Equipo 2".equalsIgnoreCase(nombreEquipo)) {
            return scrim.getEquipo2().getCantidadJugadores() >= scrim.getCupo();
        }
        return true;
    }

    private record RegistroResultado(String email, int kills, int assists, int deaths, double rating) {}

    private static int leerEnteroOpcional(String mensaje, int valorActual) {
        while (true) {
            System.out.print(mensaje + " (actual " + valorActual + "): ");
            String input = scanner.nextLine().trim();
            if (input.isBlank()) {
                return valorActual;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Por favor ingrese un número válido o deje vacío para mantener el actual.");
            }
        }
    }

    private static StateRoles pedirRol() {
        return seleccionarRolDesdeMenu(null);
    }

    private static StateRoles pedirRolOpcional(StateRoles actual) {
        return seleccionarRolDesdeMenu(actual);
    }

    private static StateRoles seleccionarRolDesdeMenu(StateRoles actual) {
        List<StateRoles> roles = StateRoles.disponibles();
        StateRoles actualNoNulo = actual != null ? actual : roles.get(0);
        while (true) {
            System.out.println("Roles disponibles" + (actual != null ? " (actual: " + actualNoNulo.getNombre() + ")" : "") + ":");
            for (int i = 0; i < roles.size(); i++) {
                System.out.println((i + 1) + ". " + roles.get(i).getNombre());
            }
            if (actual != null) {
                System.out.print("Seleccione un rol (ENTER para mantener): ");
                String input = scanner.nextLine().trim();
                if (input.isBlank()) {
                    return actualNoNulo;
                }
                try {
                    int opcion = Integer.parseInt(input);
                    if (opcion >= 1 && opcion <= roles.size()) {
                        return roles.get(opcion - 1);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else {
                int opcion = leerEnteroConMensaje("Seleccione un rol: ");
                if (opcion >= 1 && opcion <= roles.size()) {
                    return roles.get(opcion - 1);
                }
            }
            System.out.println("Rol inválido. Intente nuevamente.");
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
        String nombre = leerNoVacio("Nombre del organizador: ");
        String email = leerNoVacio("Email del organizador: ");
        String password = leerNoVacio("Contraseña: ");
        try {
            usuarioService.registrarOrganizador(nombre, email, password);
            System.out.println("Organizador registrado exitosamente.");
        } catch (Exception e) {
            System.out.println("Error al registrar organizador: " + e.getMessage());
        }
    }

    private static void registrarJugadorFlow() {
        String nombre = leerNoVacio("Nombre del jugador: ");
        String email = leerNoVacio("Email del jugador: ");
        String password = leerNoVacio("Contraseña: ");
        System.out.println("Ingrese datos del perfil competitivo:");
        int mmr = leerEnteroConMensaje("MMR (número entero): ");
        int latencia = leerEnteroConMensaje("Latencia promedio (ms): ");

        StateRoles rolPreferido = pedirRol();
        StateRegion region = pedirRegion();
        StateRangos rango = StateRangos.asignarRangoSegunPuntos(mmr);

        try {
            usuarioService.registrarJugador(nombre, email, password, mmr, latencia, rango, rolPreferido, region);
            System.out.println("Jugador registrado exitosamente. Rango asignado: " + rango.getNombre());
        } catch (Exception e) {
            System.out.println("Error al registrar jugador: " + e.getMessage());
        }
    }

    private static String seleccionarMotivoSancion() {
        String[] motivos = {"Abandono", "NoShow"};
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

    private static String seleccionarConsecuenciaSancion() {
        String[] consecuencias = {"Strike", "Cooldown"};
        System.out.println("Consecuencias disponibles:");
        for (int i = 0; i < consecuencias.length; i++) {
            System.out.println((i + 1) + ". " + consecuencias[i]);
        }
        while (true) {
            int opcion = leerEnteroConMensaje("Seleccione una consecuencia (1-" + consecuencias.length + "): ");
            if (opcion >= 1 && opcion <= consecuencias.length) {
                return consecuencias[opcion - 1];
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

    private static Integer leerEnteroOpcionalNulo(String mensaje) {
        System.out.print(mensaje);
        String input = scanner.nextLine().trim();
        if (input.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Valor inválido. Se ignorará este campo.");
            return null;
        }
    }

    private static String leerOpcional(String mensaje) {
        System.out.print(mensaje);
        String value = scanner.nextLine().trim();
        return value.isBlank() ? null : value;
    }

    private static String seleccionarEquipoNumerico() {
        while (true) {
            System.out.println("Seleccione equipo:");
            System.out.println("1. Equipo 1");
            System.out.println("2. Equipo 2");
            int opcion = leerEnteroConMensaje("Opción: ");
            if (opcion == 1) {
                return "Equipo 1";
            }
            if (opcion == 2) {
                return "Equipo 2";
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }

    private static String leerIdScrimValido() {
        return leerIdScrimValido(true);
    }

    private static String leerIdScrimValido(boolean requerido) {
        System.out.print("Ingrese ID del scrim: ");
        String idScrim = scanner.nextLine().trim();
        if (idScrim.isBlank()) {
            if (requerido) {
                System.out.println("Error: el ID no puede ser vacío. Volviendo al menú.");
            }
            return null;
        }
        try {
            scrimController.buscar(idScrim);
            return idScrim;
        } catch (Exception e) {
            System.out.println("Error: scrim no encontrado. Volviendo al menú.");
            return null;
        }
    }

    private static String seleccionarRegionOpcionalLibre() {
        var disponibles = StateRegion.disponibles();
        System.out.println("Regiones disponibles:");
        for (int i = 0; i < disponibles.size(); i++) {
            System.out.println((i + 1) + ". " + disponibles.get(i).getNombre());
        }
        System.out.println("0. Cualquiera");
        while (true) {
            int opcion = leerEnteroConMensaje("Seleccione una región: ");
            if (opcion == 0) {
                return null;
            }
            if (opcion >= 1 && opcion <= disponibles.size()) {
                return disponibles.get(opcion - 1).getNombre();
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }

    private static Integer seleccionarRangoOpcional(String mensaje, boolean devolverMaximo) {
        var rangos = StateRangos.disponibles();
        System.out.println("Rangos disponibles:");
        for (int i = 0; i < rangos.size(); i++) {
            StateRangos rango = rangos.get(i);
            System.out.println((i + 1) + ". " + rango.getNombre() + " (" + rango.getMinimo() + " - " + rango.getMaximo() + ")");
        }
        System.out.println("0. Cualquiera");
        while (true) {
            int opcion = leerEnteroConMensaje(mensaje);
            if (opcion == 0) {
                return null;
            }
            if (opcion >= 1 && opcion <= rangos.size()) {
                StateRangos rango = rangos.get(opcion - 1);
                return devolverMaximo ? rango.getMaximo() : rango.getMinimo();
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }

    private static String seleccionarFormatoOpcional() {
        String[] formatos = {"5v5", "3v3", "1v1"};
        System.out.println("Formatos disponibles:");
        for (int i = 0; i < formatos.length; i++) {
            System.out.println((i + 1) + ". " + formatos[i]);
        }
        System.out.println("0. Cualquiera");
        while (true) {
            int opcion = leerEnteroConMensaje("Seleccione un formato: ");
            if (opcion == 0) {
                return null;
            }
            if (opcion >= 1 && opcion <= formatos.length) {
                return formatos[opcion - 1];
            }
            System.out.println("Opción inválida. Intente nuevamente.");
        }
    }

    private static StateRegion seleccionarRegionDesdeMenu(StateRegion actual) {
        var disponibles = StateRegion.disponibles();
        StateRegion actualNoNulo = actual != null ? actual : disponibles.get(0);
        while (true) {
            System.out.println("Regiones disponibles" + (actual != null ? " (actual: " + actualNoNulo.getNombre() + ")" : "") + ":");
            for (int i = 0; i < disponibles.size(); i++) {
                System.out.println((i + 1) + ". " + disponibles.get(i).getNombre());
            }
            if (actual != null) {
                System.out.print("Seleccione una región (ENTER para mantener): ");
                String input = scanner.nextLine().trim();
                if (input.isBlank()) {
                    return actualNoNulo;
                }
                try {
                    int opcion = Integer.parseInt(input);
                    if (opcion >= 1 && opcion <= disponibles.size()) {
                        return disponibles.get(opcion - 1);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else {
                int opcion = leerEnteroConMensaje("Seleccione una región: ");
                if (opcion >= 1 && opcion <= disponibles.size()) {
                    return disponibles.get(opcion - 1);
                }
            }
            System.out.println("Región inválida. Intente nuevamente.");
        }
    }

    private static StateRegion pedirRegion() {
        return seleccionarRegionDesdeMenu(null);
    }

    private static StateRegion pedirRegionOpcional(StateRegion actual) {
        return seleccionarRegionDesdeMenu(actual);
    }

    private static String valorOAny(String valor) {
        return valor == null || valor.isBlank() ? "cualquiera" : valor;
    }

    private static String rangoTexto(Integer min, Integer max) {
        if (min == null && max == null) {
            return "cualquiera";
        }
        if (min == null) {
            return "<= " + max;
        }
        if (max == null) {
            return ">= " + min;
        }
        return min + " - " + max;
    }
}
