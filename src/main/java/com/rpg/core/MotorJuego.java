package com.rpg.core;

import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.persistencia.ArchivosPartida;
import com.rpg.persistencia.Partida;

import java.util.List;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private PerfilJugador perfil = new PerfilJugador();
    private Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final ArchivosPartida archivos = new ArchivosPartida();
    private String ultimoGuardado = "";
    private Personaje heroe;
    private Enemigo rival;
    private boolean combateActivo;

    public MotorJuego(Consola consola) {
        this.consola = consola;
        new AutoGuardadoDaemon(5000, this::autoGuardar).iniciar();
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        Personaje jugador = heroe != null && (rival != null || heroe.estaVivo()) ? heroe : crearJugador();
        Enemigo enemigo = rival != null ? rival : elegirEnemigo();
        ejecutarCombateRapido(jugador, enemigo, rival != null);
    }

    public void combateRapido(Personaje jugador) {
        entregarKitInicial(jugador);
        ejecutarCombateRapido(jugador, elegirEnemigo(), false);
    }

    private void ejecutarCombateRapido(Personaje jugador, Enemigo enemigo, boolean reanudar) {
        synchronized (this) {
            if (!reanudar) {
                registro.limpiar();
                registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());
            }
            if (jugador.getGremio() != gremio) {
                jugador.unirseAGremio(gremio);
            }
            heroe = jugador;
            rival = enemigo;
            combateActivo = true;
        }
        arena.prepararCombate(jugador, enemigo);
        boolean huyo;
        try {
            huyo = combatir(jugador, enemigo);
        } finally {
            synchronized (this) {
                arena.cerrarEfectos(jugador, enemigo);
                combateActivo = false;
            }
        }
        synchronized (this) {
            cerrarCombate(jugador, enemigo, huyo);
            rival = null;
            archivos.guardarHistorial(registro.getTexto());
            guardarProgreso();
        }
    }

    public void jugarTorneo() {
        registro.limpiar();
        synchronized (this) {
            combateActivo = true;
        }
        Personaje campeon;
        try {
            campeon = torneo.jugarTorneo();
        } finally {
            synchronized (this) {
                combateActivo = false;
            }
        }
        if (campeon == null) {
            return;
        }
        synchronized (this) {
            registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
            perfil.desbloquearLogro("Campeon del Torneo");
            archivos.guardarHistorial(registro.getTexto());
            guardarProgreso();
        }
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);

        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            boolean puedeActuar;
            synchronized (this) {
                puedeActuar = arena.iniciarTurno(jugador);
            }
            if (puedeActuar && turnoJugador(jugador, enemigo)) {
                return true;
            }
            synchronized (this) {
                arena.turnoAutomatico(enemigo, jugador);
            }
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (!jugador.estaVivo() || !enemigo.estaVivo()) {
                return false;
            }
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    synchronized (this) {
                        arena.atacar(jugador, enemigo);
                    }
                    yield true;
                }
                case ESPECIAL -> {
                    synchronized (this) {
                        yield arena.intentarEspecial(jugador, enemigo);
                    }
                }
                case TIEMPO_AGOTADO -> {
                    registro.agregar("Tiempo agotado. Pierdes el turno.");
                    yield true;
                }
                case POCION -> usarPocion(jugador);
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && jugador.estaVivo() && enemigo.estaVivo());
        return false;
    }

    private Accion pedirAccion() {
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");
        Integer opcion = consola.leerEnteroConTiempo("Accion (10 segundos): ", 0, 4, 10);
        if (opcion == null) {
            return Accion.TIEMPO_AGOTADO;
        }
        return switch (opcion) {
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        Integer eleccion = consola.leerEnteroConTiempo("Pocion (0 = cancelar, 10 segundos): ",
                0, inventario.tamanio(), 10);
        if (eleccion == null) {
            registro.agregar("Tiempo agotado. Pierdes el turno.");
            return true;
        }
        if (eleccion == 0) {
            return false;
        }
        synchronized (this) {
            if (jugador.estaVivo()) {
                registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
            }
        }
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d pulsos)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    private synchronized void autoGuardar() {
        if (combateActivo) {
            guardarProgreso();
        }
    }

    public synchronized void guardarProgreso() {
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();
        if (heroe != null) {
            ultimoGuardado += System.lineSeparator() + FormatoCombate.resumen(heroe);
        }
        if (rival != null) {
            ultimoGuardado += System.lineSeparator() + FormatoCombate.resumen(rival);
        }
        if (archivos.guardar(new Partida(perfil, gremio, heroe, rival, bestiario.copiarDerrotados(),
                registro.getTexto()))) {
            System.out.println("[Progreso guardado]");
        }
    }

    public synchronized void cargarProgreso() {
        Partida partida = archivos.cargar();
        if (partida != null) {
            perfil = partida.perfil();
            gremio = partida.gremio();
            heroe = partida.heroe();
            rival = partida.rival();
            bestiario.restaurarDerrotados(partida.derrotados());
            registro.restaurar(partida.historial());
            System.out.println("Partida cargada. Elige Combate rapido para continuar.");
        }
    }

    public void mostrarHistorial() {
        archivos.mostrarHistorial();
    }

    public void inspeccionarArchivos() {
        archivos.inspeccionar();
    }

    public void borrarPartida() {
        if (consola.leerEntero("Borrar partida guardada? (1. Si, 0. No): ", 0, 1) == 1) {
            archivos.borrarPartida();
        }
    }

    public synchronized String getUltimoGuardado() {
        return ultimoGuardado;
    }
}
