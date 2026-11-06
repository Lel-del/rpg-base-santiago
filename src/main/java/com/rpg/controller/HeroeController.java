package com.rpg.controller;

import com.rpg.core.MotorJuego;
import com.rpg.dao.HeroeDAO;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;

import java.io.IOException;
import java.util.List;

public class HeroeController {
    private final HeroeDAO dao = new HeroeDAO();
    private final MotorJuego motor;

    public HeroeController(MotorJuego motor) {
        this.motor = motor;
    }

    public String insertar(String nombre, String tipo, int vidaMaxima, int nivel) throws IOException {
        Heroe heroe = new Heroe(nombre, tipo, vidaMaxima, nivel);
        return dao.insertar(heroe) > 0 ? "Heroe registrado." : "No se pudo registrar el heroe.";
    }

    public List<Heroe> listar() throws IOException {
        return dao.listar();
    }

    public List<Heroe> buscarPorTipo(String tipo) throws IOException {
        return dao.buscarPorTipo(Heroe.validarTipo(tipo));
    }

    public String actualizarNivel(int id, int nivel) throws IOException {
        validarId(id);
        return dao.actualizarNivel(id, Heroe.validarNivel(nivel)) > 0
                ? "Nivel actualizado." : "No existe un heroe con ese id.";
    }

    public String eliminar(int id) throws IOException {
        validarId(id);
        return dao.eliminar(id) > 0 ? "Heroe eliminado." : "No existe un heroe con ese id.";
    }

    private void validarId(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("El id debe ser mayor que cero.");
        }
    }

    public void combatir(int id, List<Heroe> heroes) {
        Heroe heroe = heroes.stream().filter(candidato -> candidato.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe un heroe con ese id."));
        Personaje jugador = heroe.getTipo().equals("guerrero")
                ? new Guerrero(heroe.getNombre()) : new Mago(heroe.getNombre());
        jugador.setVidaMaxima(heroe.getVidaMaxima());
        jugador.restaurarVida();
        motor.combateRapido(jugador);
    }
}
