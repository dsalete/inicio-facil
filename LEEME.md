# Inicio fácil — launcher para Android

Pantalla de inicio con la hora grande, la fecha («Lunes 16 nov») y botones grandes: Cámara, Fotos, WhatsApp, Juegos (web), Llamar, YouTube, TikTok y Alarmas. Abajo del todo hay un botón **Ajustes** que pide PIN.

## Compilar el APK (sin instalar nada en tu Mac)

1. En GitHub, crea un repositorio **privado** (por ejemplo `inicio-facil`).
2. Sube el contenido de esta carpeta con **Add file → Upload files**: arrastra la carpeta `app` y los archivos sueltos (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `LEEME.md`).
3. Crea el archivo del compilador a mano, porque Mac oculta las carpetas que empiezan por punto:
   **Add file → Create new file**, en el nombre escribe exactamente `.github/workflows/build.yml` y pega dentro el contenido de `build.yml` (viene en este zip). Pulsa **Commit**.
4. Ve a la pestaña **Actions**. Se ejecutará solo "Compilar APK" (si no, pulsa **Run workflow**). Tarda unos 3–5 minutos.
5. Entra en la ejecución terminada y, abajo en **Artifacts**, descarga `inicio-facil-apk`. Dentro del zip está `app-debug.apk`.

Para actualizar: sube los archivos nuevos (mismos nombres) y Actions genera otro APK. Se instala encima del anterior sin perder los ajustes.

## Instalar en el móvil de tu madre

1. Pasa `app-debug.apk` al móvil (Drive, WhatsApp, cable...).
2. Ábrelo y, si lo pide, activa **Instalar apps desconocidas** para esa app.
3. Pulsa el botón de Inicio del móvil → elige **Inicio fácil** → **Siempre**.

Una vez elegido como inicio **«Siempre»**, el móvil lo arranca solo al encenderlo o reiniciarlo; no hay que abrirlo a mano.

## Ajustes (botón de abajo, con PIN)

PIN inicial: **1234** (cámbialo en ajustes). Desde ahí:

- Mostrar u ocultar cada botón y **ordenarlos con ▲ ▼**.
- **Añadir app:** muestra todas las apps instaladas en el móvil; la que elijas aparece en Inicio con su icono real y un fondo del color de ese icono. Las apps añadidas se pueden ordenar con ▲ ▼ y quitar del todo con ✕ (los 8 botones de serie solo se ocultan con su casilla).
- **Páginas:** cada página de Inicio tiene 8 botones. Si hay más, aparecen **Anterior** y **Siguiente** debajo (con «1 de 2»). Al salir a otra app y volver, Inicio empieza otra vez en la primera página.
- **Contactos para llamar:** «Añadir contacto» abre la agenda del móvil; se pueden ordenar con ▲ ▼ y quitar con ✕. La primera vez, acepta los permisos de **Contactos** (para las fotos) y **Teléfono** (para llamar directamente).
- Tamaño del texto, ignorar toques repetidos y dirección web de los juegos.
- Para volver al launcher anterior: **Cambiar de launcher**.

## Pantalla Llamar

Muestra 6 contactos por página con su foto de la agenda (si no tienen, una inicial de color). Con más de 6 aparecen los botones **Anterior** y **Siguiente**. Al tocar uno sale «¿Quieres llamar a …?» y, al pulsar **Llamar**, llama directamente. Si no se aceptó el permiso de Teléfono, abre el marcador con el número ya puesto.

## Cambiar o añadir botones

Para añadir o quitar apps basta con **Añadir app** en Ajustes. Los 8 botones de serie están en la lista `all` de `app/src/main/java/com/familia/inicio/Tiles.kt`: cada uno es una línea con nombre, emoji, color y la app (por su nombre de paquete) o acción que abre.

## Límites

Un launcher no puede bloquear la barra de notificaciones, el gesto de «Atrás» ni la vista de apps recientes; solo controla la pantalla de inicio.
