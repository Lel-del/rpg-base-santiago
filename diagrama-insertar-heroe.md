# Insertar heroe

```text
HeroeUI lee nombre, tipo, vida maxima y nivel
  |
  v
HeroeController valida y construye el objeto Heroe
  |
  v
HeroeDAO abre la conexion con ConexionDB, asigna los parametros
y llama a executeUpdate()
  |
  v
MySQL inserta la fila en rpg_grupal_db.heroes
```

El DAO devuelve las filas afectadas al controlador. El controlador devuelve
el mensaje y la vista lo imprime. Si falla la validacion, no se consulta la BD.

## Ejecutar

1. Ejecuta `schema.sql` en MySQL.
2. Copia `config.example.txt` como `config.txt` junto al `pom.xml` y escribe tus credenciales locales.
3. Con JDK 21, abre el proyecto Maven en IntelliJ, recarga las dependencias y ejecuta `com.rpg.Main`.
4. En el menu principal usa **Registro de heroes** para gestionar registros o **Cargar heroe desde BD** para seleccionar un heroe y combatir.
