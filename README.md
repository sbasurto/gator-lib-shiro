# Gator Lib Shiro

Integración ligera de Apache Shiro 3 con el ecosistema Gator. Incluye el realm
JNDI, el filtro de autenticación, carga de configuración Shiro desde PostgreSQL
y sesiones persistentes mediante `GappSessionDAO`.

## Requisitos

- JDK 21
- `gator-lib` y `gator-lib-utils` en directorios hermanos
- Acceso a Maven Central durante la primera compilación

## Compilar y probar

```bash
./gradlew clean check jar
```

Puede indicar otra ubicación de `gator-lib`:

```bash
./gradlew clean check jar -PgatorLibDir=/ruta/gator-lib
```

El JAR se genera en `dist/gator-lib-shiro.jar`.

## Sesiones

`GappSessionDAO` conserva una caché local limitada con
`MemoryConstrainedCacheManager` y persiste mediante `app_fn_admon_session`.
También permite eliminar una sesión o todas las sesiones tanto de memoria como
de PostgreSQL.

## Seguridad

La configuración puede contener contraseñas, tokens y llaves. Esos valores se
enmascaran al registrar el `shiro.ini`; no deben agregarse al repositorio ni a
los logs. `./gradlew check` incluye una validación contra logs sensibles y una
prueba ejecutable del ciclo de vida de las sesiones.

## Licencia

GPL-3.0-or-later. Consulte [LICENSE](LICENSE) y [NOTICE](NOTICE).

Los objetos de datos de sesión se trasladaron a `gator.lib.session` en
`gator-lib`. Recompilar los consumidores y empaquetar las bibliotecas juntas;
las clases ya no están disponibles en `gator.lib.shiro.sessions`.
El JSON de sesión conserva el mismo formato.
