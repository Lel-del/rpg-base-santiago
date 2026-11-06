package com.rpg.persistencia;

import com.rpg.entities.Enemigo;
import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;

import java.io.Serializable;
import java.util.Map;

public record Partida(PerfilJugador perfil, Gremio gremio, Personaje heroe, Enemigo rival,
                      Map<String, Integer> derrotados, String historial) implements Serializable {
    private static final long serialVersionUID = 1L;
}
