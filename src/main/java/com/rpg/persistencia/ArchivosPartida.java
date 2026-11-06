package com.rpg.persistencia;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ArchivosPartida {
    private final File carpeta = new File("partidas");
    private final File partida = new File(carpeta, "partida.dat");
    private final File temporal = new File(carpeta, "partida.tmp");
    private final File historial = new File(carpeta, "combates.txt");

    private void prepararCarpeta() throws IOException {
        if (!carpeta.isDirectory() && !carpeta.mkdirs() && !carpeta.isDirectory()) {
            throw new IOException("No se pudo crear la carpeta partidas.");
        }
    }

    public synchronized boolean guardar(Partida estado) {
        try {
            prepararCarpeta();
            // El archivo anterior sigue disponible hasta que el nuevo este completo.
            try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(temporal))) {
                salida.writeObject(estado);
            }
            Files.move(temporal.toPath(), partida.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (NotSerializableException e) {
            System.out.println("Objeto de la partida sin soporte de serializacion: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("No se pudo guardar la partida: " + e.getMessage());
        } finally {
            if (temporal.exists() && !temporal.delete()) {
                System.out.println("No se pudo borrar el archivo temporal.");
            }
        }
        return false;
    }

    public synchronized Partida cargar() {
        // readObject puede ejecutar codigo o consumir recursos al reconstruir datos manipulados.
        // Solo se cargan guardados propios. JSON o texto propio permiten validar los datos primero.
        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(partida))) {
            entrada.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "maxdepth=50;maxrefs=100000;maxbytes=10000000;com.rpg.**;java.base/java.lang.*;java.base/java.util.*;!*"));
            Object contenido = entrada.readObject();
            if (contenido instanceof Partida estado && estado.perfil() != null && estado.gremio() != null
                    && estado.derrotados() != null && estado.historial() != null) {
                return estado;
            }
            System.out.println("El archivo no contiene una partida valida.");
        } catch (FileNotFoundException e) {
            System.out.println("No se encontro una partida guardada.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No se pudo cargar la partida: " + e.getMessage());
        }
        return null;
    }

    public synchronized void guardarHistorial(String texto) {
        try {
            prepararCarpeta();
            historial.createNewFile();
            // BufferedWriter con FileWriter permite escribir linea por linea y agregar al historial.
            // Files.writeString sirve para escribir un texto completo de una vez con NIO.
            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(historial, StandardCharsets.UTF_8, true))) {
                for (String linea : texto.lines().toList()) {
                    escritor.write(linea);
                    escritor.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo escribir el historial: " + e.getMessage());
        }
    }

    public synchronized void mostrarHistorial() {
        try (BufferedReader lector = new BufferedReader(new FileReader(historial, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                System.out.println(linea);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Aun no hay historial de combate.");
        } catch (IOException e) {
            System.out.println("No se pudo leer el historial: " + e.getMessage());
        }
    }

    public synchronized void inspeccionar() {
        for (File relativo : new File[]{partida, historial}) {
            File absoluto = new File(relativo.getAbsolutePath());
            // La ruta relativa parte del directorio de ejecucion. La absoluta indica la ubicacion completa.
            System.out.printf("%s | %s | %s | existe: %s | %d bytes%n",
                    relativo.getName(), relativo.getPath(), absoluto.getPath(), absoluto.exists(), absoluto.length());
        }
    }

    public synchronized void borrarPartida() {
        // File.delete devuelve false ante ausencia o fallo. NIO devuelve false por ausencia y lanza IOException ante fallo.
        try {
            System.out.println(Files.deleteIfExists(partida.toPath())
                    ? "Partida borrada." : "No hay partida guardada.");
        } catch (IOException e) {
            System.out.println("No se pudo borrar la partida: " + e.getMessage());
        }
    }
}
