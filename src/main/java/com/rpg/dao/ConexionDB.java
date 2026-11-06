package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConexionDB {

    public Connection conectar() throws IOException, SQLException {
        File archivo = new File("config.txt");
        if (!archivo.exists()) {
            throw new FileNotFoundException("Falta config.txt. Usa config.example.txt como ejemplo.");
        }

        Map<String, String> datos = new HashMap<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] partes = linea.split("=", 2);
                if (partes.length != 2) {
                    throw new IOException("Formato invalido en config.txt. Usa clave=valor.");
                }
                datos.put(partes[0].trim(), partes[1].trim());
            }
        }

        for (String campo : new String[]{"url", "usuario", "clave"}) {
            if (!datos.containsKey(campo) || datos.get(campo).isEmpty()) {
                throw new IOException("Falta el valor de " + campo + " en config.txt.");
            }
        }

        // En la URL, jdbc:mysql indica el protocolo, localhost es el host,
        // 3306 es el puerto y rpg_grupal_db es el esquema de la base de datos.
        return DriverManager.getConnection(datos.get("url"), datos.get("usuario"), datos.get("clave"));
    }
}
