package com.rpg.core;

import com.rpg.concurrency.GestorTurno;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Entrada por teclado con validacion y reintento. */
public class Consola {

    private static final int LARGO_MAXIMO_NOMBRE = 15;
    private static final String PATRON_NOMBRE = "[\\p{L}0-9 ]+";

    private static final Object FIN = new Object();
    private final BlockingQueue<Object> entrada = new LinkedBlockingQueue<>();

    public Consola(Scanner scanner) {
        Thread lector = new Thread(() -> {
            try (scanner) {
                while (scanner.hasNextLine()) {
                    entrada.add(scanner.nextLine().trim());
                }
            } finally {
                entrada.add(FIN);
            }
        }, "lector-consola");
        lector.setDaemon(true);
        lector.start();
    }

    public String leerLinea(String mensaje) {
        System.out.print(mensaje);
        return leerEntrada(null);
    }

    private String leerEntrada(Object vencimiento) {
        try {
            while (true) {
                Object dato = entrada.take();
                if (dato == FIN) {
                    entrada.add(FIN);
                    throw new NoSuchElementException();
                }
                if (dato == vencimiento) {
                    return null;
                }
                if (dato instanceof String texto) {
                    return texto;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Lectura interrumpida");
        }
    }

    public Integer leerEnteroConTiempo(String mensaje, int minimo, int maximo, int segundos) {
        Object vencimiento = new Object();
        Thread temporizador = new GestorTurno().iniciar(segundos, () -> entrada.add(vencimiento));
        try {
            return leerEntero(mensaje, minimo, maximo, vencimiento);
        } finally {
            temporizador.interrupt();
        }
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        return leerEntero(mensaje, minimo, maximo, null);
    }

    private Integer leerEntero(String mensaje, int minimo, int maximo, Object vencimiento) {
        while (true) {
            System.out.print(mensaje);
            String texto = leerEntrada(vencimiento);
            if (texto == null) {
                return null;
            }
            if (texto.isEmpty()) {
                System.out.println("No escribiste nada.");
                continue;
            }
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" no es un numero.");
            }
        }
    }

    public String leerNombre(String mensaje) {
        while (true) {
            String nombre = leerLinea(mensaje);
            if (nombre.isEmpty()) {
                System.out.println("El nombre no puede estar vacio.");
            } else if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
                System.out.println("Maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.");
            } else if (!nombre.matches(PATRON_NOMBRE)) {
                System.out.println("Usa solo letras, numeros y espacios.");
            } else {
                return nombre;
            }
        }
    }
}
