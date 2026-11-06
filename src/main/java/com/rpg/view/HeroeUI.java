package com.rpg.view;

import com.rpg.controller.HeroeController;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.model.Heroe;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class HeroeUI {
    private final Consola consola = new Consola(new Scanner(System.in));
    private final MotorJuego motor = new MotorJuego(consola);
    private final HeroeController controlador = new HeroeController(motor);

    public void iniciar() {
        try {
            int opcion;
            do {
                mostrarMenuPrincipal();
                opcion = consola.leerEntero("Opcion: ", 0, 9);
                switch (opcion) {
                    case 1 -> motor.combateRapido();
                    case 2 -> motor.jugarTorneo();
                    case 3 -> motor.mostrarBestiario();
                    case 4 -> motor.cargarProgreso();
                    case 5 -> motor.mostrarHistorial();
                    case 6 -> motor.inspeccionarArchivos();
                    case 7 -> motor.borrarPartida();
                    case 8 -> registroHeroes();
                    case 9 -> cargarHeroe();
                    default -> System.out.println("Hasta luego.");
                }
            } while (opcion != 0);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    private void mostrarMenuPrincipal() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Cargar partida");
        System.out.println("5. Ver historial");
        System.out.println("6. Inspeccionar archivos");
        System.out.println("7. Borrar partida guardada");
        System.out.println("8. Registro de heroes");
        System.out.println("9. Cargar heroe desde BD");
        System.out.println("0. Salir");
    }

    private void registroHeroes() {
        int opcion;
        do {
            System.out.println("\n=== REGISTRO DE HEROES ===");
            System.out.println("1. Insertar heroe");
            System.out.println("2. Listar heroes");
            System.out.println("3. Buscar por tipo");
            System.out.println("4. Actualizar nivel");
            System.out.println("5. Eliminar heroe");
            System.out.println("0. Volver");
            opcion = consola.leerEntero("Opcion: ", 0, 5);
            try {
                switch (opcion) {
                    case 1 -> insertarHeroe();
                    case 2 -> mostrarHeroes(controlador.listar());
                    case 3 -> mostrarHeroes(controlador.buscarPorTipo(consola.leerLinea("Tipo (guerrero o mago): ")));
                    case 4 -> {
                        int id = consola.leerEntero("Id: ", 1, Integer.MAX_VALUE);
                        int nivel = consola.leerEntero("Nuevo nivel: ", Integer.MIN_VALUE, Integer.MAX_VALUE);
                        System.out.println(controlador.actualizarNivel(id, nivel));
                    }
                    case 5 -> System.out.println(controlador.eliminar(consola.leerEntero("Id: ", 1, Integer.MAX_VALUE)));
                }
            } catch (IOException | IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        } while (opcion != 0);
    }

    private void insertarHeroe() throws IOException {
        String nombre = consola.leerLinea("Nombre: ");
        String tipo = consola.leerLinea("Tipo (guerrero o mago): ");
        int vidaMaxima = consola.leerEntero("Vida maxima: ", Integer.MIN_VALUE, Integer.MAX_VALUE);
        int nivel = consola.leerEntero("Nivel: ", Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.println(controlador.insertar(nombre, tipo, vidaMaxima, nivel));
    }

    private void mostrarHeroes(List<Heroe> heroes) {
        if (heroes.isEmpty()) {
            System.out.println("No hay heroes para mostrar.");
        }
        for (Heroe heroe : heroes) {
            System.out.printf("%d. %s | %s | Vida maxima: %d | Nivel: %d%n",
                    heroe.getId(), heroe.getNombre(), heroe.getTipo(), heroe.getVidaMaxima(), heroe.getNivel());
        }
    }

    private void cargarHeroe() {
        try {
            List<Heroe> heroes = controlador.listar();
            mostrarHeroes(heroes);
            if (heroes.isEmpty()) {
                return;
            }
            int id = consola.leerEntero("Id del heroe (0 = cancelar): ", 0, Integer.MAX_VALUE);
            if (id != 0) {
                controlador.combatir(id, heroes);
            }
        } catch (IOException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}
