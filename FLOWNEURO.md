# FlowNeuro en Metrolist

Esta integración adapta ideas del motor FlowNeuro de [Flow Android Client](https://github.com/A-EDev/Flow) al reproductor musical de Metrolist. Se mantiene bajo GPL-3.0 y conserva la atribución al proyecto original.

## Comportamiento

FlowNeuro analiza canciones relacionadas locales y online, filtra duplicados y versiones no deseadas, respeta la red seleccionada y evita canciones FLOW recientes. En cada cálculo genera un bloque de hasta **20 canciones nuevas y diversas**, no una sola recomendación segura. La cola normal de YouTube Music se conserva intacta y no se usa como fuente de recomendaciones: el bloque FLOW se inserta inmediatamente después de la canción actual, por delante de las canciones normales pendientes. En cada transición se vuelve a calcular otro bloque cuando sea necesario, priorizando descubrimiento sin borrar ni modificar la cola original. Las canciones añadidas por el motor llevan `FLOW` en la metadata visible de la cola.

“Local” significa canciones que ya están disponibles en la base de datos de Metrolist, por ejemplo contenido guardado o ya conocido por el reproductor; no significa archivos MP3 del almacenamiento del teléfono. “Online” significa que, cuando la red lo permite, FlowNeuro consulta a YouTube/InnerTube por canciones relacionadas. El selector de red permite usar Wi‑Fi y datos móviles o solo Wi‑Fi. Si no hay red, se utilizan únicamente los candidatos locales.

La confirmación de escucha controla la inyección, no solamente el contador de aprendizaje: `Inmediata` permite insertar tras un breve retardo de seguridad, `Tras 60%` espera a que la pista actual alcance el 60% de su duración y `Tras 80%` espera el 80%. Si se cambia de canción antes del umbral, la tarea pendiente se cancela y nunca inserta la recomendación atrasada.

El aprendizaje registra reproducciones suficientes según el umbral elegido, saltos tempranos como señales negativas, nivel de aprendizaje, total de inyecciones e historial reciente. El perfil versionado añade afinidades de artista y álbum, contexto horario, actualización acotada, diversidad y una razón visible de la última señal. Una vez al día se aplica mantenimiento con decaimiento suave de afinidades, sin inventar reproducciones ni señales. El motor completo nunca bloquea la reproducción: los errores de DataStore, red, base de datos o conversión devuelven un resultado vacío.

## Backup y restauración

Desde Ajustes → FlowNeuro se puede crear un archivo JSON local con las preferencias, progreso, señales, contador de inyecciones e historial reciente. El archivo puede copiarse a otro teléfono y restaurarse desde la misma pantalla. No se sube a ningún servidor.

## Validación

El workflow `.github/workflows/flowneuro.yml` compila el APK FOSS debug en GitHub Actions, genera el keystore temporal de CI y publica tanto el APK como un ZIP del código fuente.
