package com.rpg.model;

import java.util.Locale;

public class Heroe {
    private int id;
    private String nombre;
    private String tipo;
    private int vidaMaxima;
    private int nivel;

    public Heroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        setNombre(nombre);
        setTipo(tipo);
        setVidaMaxima(vidaMaxima);
        setNivel(nivel);
    }

    public Heroe(int id, String nombre, String tipo, int vidaMaxima, int nivel) {
        this(nombre, tipo, vidaMaxima, nivel);
        setId(id);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("El id debe ser mayor que cero.");
        }
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre debe tener entre 1 y 100 caracteres.");
        }
        this.nombre = nombre.trim();
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = validarTipo(tipo);
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima < 1) {
            throw new IllegalArgumentException("La vida maxima debe ser mayor que cero.");
        }
        this.vidaMaxima = vidaMaxima;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = validarNivel(nivel);
    }

    public static int validarNivel(int nivel) {
        if (nivel < 1) {
            throw new IllegalArgumentException("El nivel debe ser al menos 1.");
        }
        return nivel;
    }

    public static String validarTipo(String tipo) {
        String valor = tipo == null ? "" : tipo.trim().toLowerCase(Locale.ROOT);
        if (!valor.equals("guerrero") && !valor.equals("mago")) {
            throw new IllegalArgumentException("El tipo debe ser guerrero o mago.");
        }
        return valor;
    }
}
