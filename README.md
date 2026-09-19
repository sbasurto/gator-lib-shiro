# Gator Lib Shiro — retirado

El módulo dejó de formar parte de las aplicaciones Gator. La autenticación web
usa Spring Security y Keycloak en `gator-lib-web-security`.

- Los datos de sesión están en `gator.lib.session`, dentro de `gator-lib`.
- La generación compatible de claves AES-128 está en `gator.lib.sec.GappKeyMaker`.
- El hash SHA-512 con sal e iteraciones está en `gator.lib.sec.GappAuth`.
- La identidad web usa la identidad autenticada y la asociación local de HttpSession.

El código, pruebas y build de Shiro se retiraron. El historial Git conserva la
implementación anterior; no se debe reconstruir ni empaquetar el JAR antiguo.
La sustitución no requiere cambios de esquema, claves o contraseñas existentes.
