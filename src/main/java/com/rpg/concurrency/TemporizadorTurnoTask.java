package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class TemporizadorTurnoTask implements Runnable {
    private final int segundos;
    private final TurnoCallback callback;

    public TemporizadorTurnoTask(int segundos, TurnoCallback callback) {
        this.segundos = segundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            for (int restantes = segundos; restantes > 0; restantes--) {
                Thread.sleep(1000);
            }
            callback.onTiempoAgotado();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
