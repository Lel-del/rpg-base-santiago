package com.rpg.dao;

import com.rpg.model.Heroe;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HeroeDAO {
    private final ConexionDB conexion = new ConexionDB();

    public int insertar(Heroe heroe) throws IOException {
        String sql = "INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)";
        try (Connection bd = conexion.conectar();
             PreparedStatement consulta = bd.prepareStatement(sql)) {
            consulta.setString(1, heroe.getNombre());
            consulta.setString(2, heroe.getTipo());
            consulta.setInt(3, heroe.getVidaMaxima());
            consulta.setInt(4, heroe.getNivel());
            return consulta.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new IOException("Ya existe un heroe con ese nombre.", e);
            }
            throw new IOException("No se pudo insertar el heroe en MySQL.", e);
        }
    }

    public List<Heroe> listar() throws IOException {
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes ORDER BY id";
        try (Connection bd = conexion.conectar();
             PreparedStatement consulta = bd.prepareStatement(sql);
             ResultSet filas = consulta.executeQuery()) {
            return leerHeroes(filas);
        } catch (SQLException e) {
            throw new IOException("No se pudo listar los heroes en MySQL.", e);
        }
    }

    public List<Heroe> buscarPorTipo(String tipo) throws IOException {
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE tipo = ? ORDER BY id";
        try (Connection bd = conexion.conectar();
             PreparedStatement consulta = bd.prepareStatement(sql)) {
            consulta.setString(1, tipo);
            try (ResultSet filas = consulta.executeQuery()) {
                return leerHeroes(filas);
            }
        } catch (SQLException e) {
            throw new IOException("No se pudo buscar los heroes en MySQL.", e);
        }
    }

    public int actualizarNivel(int id, int nivel) throws IOException {
        String sql = "UPDATE heroes SET nivel = ? WHERE id = ?";
        try (Connection bd = conexion.conectar();
             PreparedStatement consulta = bd.prepareStatement(sql)) {
            consulta.setInt(1, nivel);
            consulta.setInt(2, id);
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("No se pudo actualizar el nivel en MySQL.", e);
        }
    }

    public int eliminar(int id) throws IOException {
        String sql = "DELETE FROM heroes WHERE id = ?";
        try (Connection bd = conexion.conectar();
             PreparedStatement consulta = bd.prepareStatement(sql)) {
            consulta.setInt(1, id);
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("No se pudo eliminar el heroe en MySQL.", e);
        }
    }

    private List<Heroe> leerHeroes(ResultSet filas) throws SQLException {
        List<Heroe> heroes = new ArrayList<>();
        while (filas.next()) {
            heroes.add(new Heroe(filas.getInt("id"), filas.getString("nombre"), filas.getString("tipo"),
                    filas.getInt("vida_maxima"), filas.getInt("nivel")));
        }
        return heroes;
    }
}
