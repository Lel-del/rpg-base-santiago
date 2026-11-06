package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();

        Personaje[] ganadores = new Personaje[2];
        Thread semifinalA = new Thread(() ->
                ganadores[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1)), "semifinal-A");
        Thread semifinalB = new Thread(() ->
                ganadores[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3)), "semifinal-B");

        mostrarEstados(semifinalA, semifinalB);
        semifinalA.start();
        semifinalB.start();
        mostrarEstados(semifinalA, semifinalB);
        try {
            semifinalA.join();
            semifinalB.join();
        } catch (InterruptedException e) {
            semifinalA.interrupt();
            semifinalB.interrupt();
            Thread.currentThread().interrupt();
            registro.agregar("Torneo interrumpido.");
            return null;
        }
        mostrarEstados(semifinalA, semifinalB);
        return ganadores[0] == null || ganadores[1] == null ? null : simularFinal(ganadores[0], ganadores[1]);
    }

    // NEW antes de start, RUNNABLE al ejecutar, TIMED_WAITING al dormir y TERMINATED tras join.
    private void mostrarEstados(Thread a, Thread b) {
        registro.agregar("%s: %s | %s: %s", a.getName(), a.getState(), b.getName(), b.getState());
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        if (ganador != null) {
            registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        }
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
