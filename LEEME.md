# Inicio fácil — launcher para Android

Pantalla de inicio con la hora grande, la fecha («Lunes 16 nov») y botones grandes: Cámara, Fotos, WhatsApp, Juegos (web), Llamar, YouTube, TikTok y Alarmas. **Ajustes** es el último botón de todos (pide PIN); con 8 aplicaciones queda en la segunda página.

## Cómo se compila y se publica (automático)

Cada vez que subes cambios a la rama `main`, GitHub Actions compila el APK, lo firma con tu clave fija y lo publica como **Release** de este mismo repositorio (`inicio-facil.apk`). El launcher consulta esas Releases para actualizarse.

### Preparación (una sola vez)

1. **Hacer público el repositorio:** **Settings → General →** baja hasta **Danger Zone → Change repository visibility → Make public** y confirma escribiendo el nombre. El código no contiene contraseñas ni datos personales; la clave de firma va en secretos, que no se ven aunque el repositorio sea público.
2. **Crear dos secretos:** **Settings → Secrets and variables → Actions → New repository secret**:
   - `KEYSTORE_BASE64` → el texto largo del archivo `inicio-facil-clave.txt`.
   - `KEYSTORE_PASSWORD` → la contraseña de ese mismo archivo.
3. **Subir los archivos** de este zip con **Add file → Upload files**: la carpeta `app` y los archivos sueltos (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `LEEME.md`).
4. **Editar el compilador:** abre `.github/workflows/build.yml` (si no existe, **Add file → Create new file** y escribe exactamente ese nombre), pega el contenido de `build.yml` de este zip y pulsa **Commit**. Mac oculta las carpetas que empiezan por punto, por eso va suelto en el zip.
5. En la pestaña **Actions** se ejecutará «Compilar APK» (3–5 minutos). Al acabar en verde, en la página principal del repositorio, a la derecha, aparece **Releases** con la versión nueva y el archivo `inicio-facil.apk`.

**Guarda `inicio-facil-clave.txt` en un sitio seguro y NO lo subas a GitHub.** Si se pierde la clave, los móviles no podrán actualizarse y habría que desinstalar la app.

### Primera instalación con la clave nueva

Desinstala la versión anterior del móvil e instala la nueva (la de la clave fija). Descárgala desde la página de **Releases** en el propio móvil: `github.com/dsalete/inicio-facil/releases/latest`. Desde entonces las actualizaciones se instalan encima, sin perder ajustes.

### Actualizar el launcher

Subes los archivos cambiados (o editas en GitHub) y esperas a que Actions termine. En el móvil: **Ajustes → Actualizaciones → Buscar actualización ahora**. Además, Inicio comprueba solo cada 12 horas y, si hay versión nueva, pregunta «¿Instalar ahora?». La primera vez Android pedirá permitir «instalar apps desconocidas» a Inicio fácil. Android no permite instalar en silencio: puede pedir pulsar «Instalar» o «Actualizar».

## Instalar en el móvil de tu madre

1. Descarga `inicio-facil.apk` desde Releases en el móvil (o pásalo por Drive o cable).
2. Ábrelo desde la app **Archivos** y, si lo pide, activa **Instalar apps desconocidas** para esa app.
3. Pulsa el botón de Inicio del móvil → elige **Inicio fácil** → **Siempre**.

Una vez elegido como inicio **«Siempre»**, el móvil lo arranca solo al encenderlo o reiniciarlo; no hay que abrirlo a mano.

## Ajustes (último botón, con PIN)

PIN inicial: **1234** (cámbialo en ajustes). Desde ahí:

- Mostrar u ocultar cada botón y **ordenarlos con ▲ ▼**.
- **Añadir app:** muestra todas las apps instaladas en el móvil; la que elijas aparece en Inicio con su icono real y un fondo del color de ese icono. Las apps añadidas se pueden ordenar con ▲ ▼ y quitar del todo con ✕ (los 8 botones de serie solo se ocultan con su casilla).
- **Páginas:** cada página de Inicio tiene 8 botones. Si hay más, aparecen **Anterior** y **Siguiente** debajo (con «1 de 2»). Al salir a otra app y volver, Inicio empieza otra vez en la primera página.
- **Contactos para llamar:** «Añadir contacto» abre la agenda del móvil; se pueden ordenar con ▲ ▼ y quitar con ✕. La primera vez, acepta los permisos de **Contactos** (para las fotos) y **Teléfono** (para llamar directamente).
- Tamaño del texto, ignorar toques repetidos y dirección web de los juegos.
- **Actualizar la aplicación:** botón al principio de Ajustes; busca la última versión publicada y la instala. Debajo, la versión instalada y si Inicio avisa cuando hay una nueva.
- Para volver al launcher anterior: **Cambiar de launcher**.

## Tarjeta del Tiempo

Es un botón más de Inicio (el primero por defecto): emoji del estado, temperatura grande y el estado («Despejado», «Lluvia»…). Al tocarlo se abre una pantalla con el tiempo de ahora, hoy y mañana (máxima, mínima y probabilidad de lluvia). Usa Open-Meteo (gratis, sin clave) y la ubicación aproximada del móvil, y se actualiza cada 30 minutos. Pide el permiso de **Ubicación** (en Ajustes o al tocar la tarjeta la primera vez). Sin internet enseña el último dato guardado. Se puede ocultar o mover desde Ajustes como los demás botones. Con 9 botones de serie, Alarmas y Ajustes pasan a la segunda página; si no la quieres, oculta alguno.

## Pantalla Llamar

Muestra 6 contactos por página con su foto de la agenda (si no tienen, una inicial de color). Con más de 6 aparecen los botones **Anterior** y **Siguiente**. Al tocar uno sale «¿Quieres llamar a …?» con dos botones grandes: **Llamar** (llamada normal por teléfono, directa) y **Videollamada** (abre WhatsApp en la videollamada con ese contacto). Si no se aceptó el permiso de Teléfono, **Llamar** abre el marcador con el número ya puesto. La videollamada solo funciona con contactos que tengan WhatsApp y que WhatsApp tenga sincronizados con la agenda; si no, avisa de que no encuentra al contacto.

## Cambiar o añadir botones

Para añadir o quitar apps basta con **Añadir app** en Ajustes. Los 8 botones de serie están en la lista `all` de `app/src/main/java/com/familia/inicio/Tiles.kt`: cada uno es una línea con nombre, emoji, color y la app (por su nombre de paquete) o acción que abre.

## Límites

Un launcher no puede bloquear la barra de notificaciones, el gesto de «Atrás» ni la vista de apps recientes; solo controla la pantalla de inicio.
