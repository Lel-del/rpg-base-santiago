package com.rpg.concurrency;

public class AutoGuardadoDaemon implements Runnable {
    private final long intervaloMs;
    private final Runnable guardar;

    public AutoGuardadoDaemon(long intervaloMs, Runnable guardar) {
        this.intervaloMs = intervaloMs;
        this.guardar = guardar;
    }

    public void iniciar() {
        Thread hilo = new Thread(this, "autoguardado");
        // La JVM espera a los hilos de usuario. Puede cerrar con hilos daemon activos.
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(intervaloMs);
                guardar.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
