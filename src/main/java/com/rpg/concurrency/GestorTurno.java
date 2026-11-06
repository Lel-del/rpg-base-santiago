package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class GestorTurno {
    public Thread iniciar(int segundos, TurnoCallback callback) {
        Thread hilo = new Thread(new TemporizadorTurnoTask(segundos, callback), "temporizador-turno");
        hilo.start();
        return hilo;
    }
}
