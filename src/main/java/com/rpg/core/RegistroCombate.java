package com.rpg.core;

/** Imprime cada evento del combate y lo guarda para incluirlo en el guardado de la partida. */
public class RegistroCombate {

    private final StringBuilder texto = new StringBuilder();

    public synchronized void agregar(String linea) {
        System.out.println(linea);
        texto.append(linea).append(System.lineSeparator());
    }

    public void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        texto.setLength(0);
    }

    public synchronized void restaurar(String historial) {
        texto.setLength(0);
        texto.append(historial);
    }

    public synchronized String getTexto() {
        return texto.toString();
    }
}
