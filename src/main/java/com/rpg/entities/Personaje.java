package com.rpg.entities;

import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.function.Consumer;

import com.rpg.concurrency.EfectoContinuoTask;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Personaje implements ICombatiente, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;

    private String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    private int vida;
    private final int ataqueBase;
    private final int defensaBase;
    private EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;                  // asociacion 0..1
    private Gremio gremio;              // asociacion bidireccional 0..1
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO); // composicion
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();

    private transient Map<String, Thread> hilosEfectos;
    private transient Consumer<String> registroEfectos;

    private synchronized void writeObject(ObjectOutputStream salida) throws IOException {
        salida.defaultWriteObject();
    }

    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    // ---- combate

    /**
     * Ataque especial propio de cada clase.
     * @return descripcion de lo ocurrido, para el registro de combate
     */
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    /** Hook que se ejecuta al comenzar el turno (por ejemplo, el mago regenera mana). */
    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        // Con enteros, 100 / 123 daria 0: por eso se divide con un double
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida

    // El mismo monitor protege vida, estado y efectos. AtomicInteger.addAndGet
    // no acota la vida ni actualiza el estado MUERTO en la misma operacion.
    public synchronized void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        setVida(vida - cantidad);
    }

    /** @return vida realmente recuperada */
    public synchronized int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        if (!estaVivo()) {
            return 0;
        }
        int vidaAntes = vida;
        setVida(vida + Math.min(cantidad, vidaMaxima - vida));
        return vida - vidaAntes;
    }

    public synchronized void restaurarVida() {
        cancelarEfectos();
        estado = EstadoPersonaje.VIVO;
        vida = vidaMaxima;
    }

    @Override
    public synchronized boolean estaVivo() {
        return estado != EstadoPersonaje.MUERTO;
    }

    // ---- efectos DoT / HoT

    public synchronized void iniciarEfectos(Consumer<String> registro) {
        registroEfectos = registro;
        for (EstadoEfecto efecto : efectos.values()) {
            iniciarHiloEfecto(efecto);
        }
    }

    public synchronized void aplicarEfecto(EstadoEfecto efecto) {
        if (!estaVivo() || efecto.haTerminado()) {
            return;
        }
        quitarEfecto(efecto.getNombre());
        EstadoEfecto activo = efecto.copia();
        efectos.put(activo.getNombre(), activo);
        if (activo.esDanio() && estado == EstadoPersonaje.VIVO) {
            estado = EstadoPersonaje.ENVENENADO;
        }
        iniciarHiloEfecto(activo);
    }

    private void iniciarHiloEfecto(EstadoEfecto efecto) {
        if (hilosEfectos == null) {
            hilosEfectos = new LinkedHashMap<>();
        }
        if (!hilosEfectos.containsKey(efecto.getNombre())) {
            Thread hilo = new Thread(new EfectoContinuoTask(this, efecto, 1000), efecto.getNombre());
            hilosEfectos.put(efecto.getNombre(), hilo);
            hilo.start();
        }
    }

    public synchronized boolean quitarEfecto(String nombreEfecto) {
        if (efectos.remove(nombreEfecto) == null) {
            return false;
        }
        Thread hilo = hilosEfectos == null ? null : hilosEfectos.remove(nombreEfecto);
        if (hilo != null && hilo != Thread.currentThread()) {
            hilo.interrupt();
        }
        normalizarEstado();
        return true;
    }

    public synchronized void quitarEfectosDanio() {
        for (EstadoEfecto efecto : new ArrayList<>(efectos.values())) {
            if (efecto.esDanio()) {
                quitarEfecto(efecto.getNombre());
            }
        }
    }

    public synchronized void cancelarEfectos() {
        for (String nombreEfecto : new ArrayList<>(efectos.keySet())) {
            quitarEfecto(nombreEfecto);
        }
        registroEfectos = null;
    }

    public synchronized boolean aplicarTickContinuo(EstadoEfecto efecto) {
        if (efectos.get(efecto.getNombre()) != efecto || !estaVivo()) {
            return false;
        }
        String mensaje = aplicarTick(efecto);
        efecto.consumirTurno();
        if (registroEfectos != null) {
            registroEfectos.accept(mensaje);
        }
        return !efecto.haTerminado() && estaVivo();
    }

    public synchronized void finalizarEfecto(EstadoEfecto efecto) {
        if (efectos.get(efecto.getNombre()) == efecto) {
            quitarEfecto(efecto.getNombre());
        }
    }

    private String aplicarTick(EstadoEfecto efecto) {
        if (efecto.esDanio()) {
            recibirDanio(efecto.getValorPorTick());
            return String.format("%s sufre %d de dano por %s", nombre, efecto.getValorPorTick(), efecto.getNombre());
        }
        int curado = curar(efecto.getValorPorTick());
        return String.format("%s recupera %d de vida por %s", nombre, curado, efecto.getNombre());
    }

    private boolean tieneEfectoDanio() {
        for (EstadoEfecto efecto : efectos.values()) {
            if (efecto.esDanio()) {
                return true;
            }
        }
        return false;
    }

    /** Si ya no queda ningun DoT, un personaje ENVENENADO vuelve a estar VIVO. */
    private void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    // ---- aturdimiento

    public synchronized void aturdir() {
        if (estaVivo()) {
            estado = EstadoPersonaje.ATURDIDO;
        }
    }

    /** @return true si estaba aturdido (y por tanto pierde este turno) */
    public synchronized boolean consumirAturdimiento() {
        boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
        if (estabaAturdido) {
            estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
        }
        return estabaAturdido;
    }

    // ---- inventario, arma, gremio, habilidades

    public String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public void desequiparArma() {
        this.arma = null;
    }

    public void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    /** @return false si la habilidad ya estaba aprendida (el Set rechaza duplicados) */
    public boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    public synchronized void setVida(int nuevaVida) {
        vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
        if (vida == 0) {
            estado = EstadoPersonaje.MUERTO;
        }
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public synchronized void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public synchronized int getVida() {
        return vida;
    }

    public synchronized int getVidaMaxima() {
        return vidaMaxima;
    }

    public synchronized EstadoPersonaje getEstado() {
        return estado;
    }

    public Arma getArma() {
        return arma;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public synchronized Collection<EstadoEfecto> getEfectos() {
        return efectos.values().stream().map(EstadoEfecto::copia).toList();
    }
}
