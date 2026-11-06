# Solución del examen de rotura (NO MIRAR ANTES DE LA SIMULACIÓN)

Los tres fallos salen del banco de diez de la guía de la actividad (apartado 2.5). Se prueban con el parte de la guía (`examen/datos/lecturas_20261019.txt`) más una línea extra con fecha imposible, que la guía no trae, para el fallo 3.

## Los tres fallos introducidos

### Fallo 1 · Incidencia perdida (banco #8)
- **Dónde:** `examen/src/LectorContadores.java`, dentro del `if (motivo != null)`, la línea que escribía `numero;motivo` en `incidencias.log` ha desaparecido.
- **Síntoma:** el programa dice `incidencias: 4`, pero `incidencias.log` está vacío (0 bytes). Las líneas malas se descartan sin dejar rastro.
- **Por qué:** el log se escribe después de `continue` (o se omite), así que la línea mala se salta sin apuntarse.
- **Arreglo:** volver a escribir `incidenciasLog.write(numLinea + ";" + motivo + "\n");` antes de `incidencias++; continue;`.
- **Cómo se localiza:** contrastar el número de incidencias por pantalla con el contenido del log. Si no coinciden, falta el `write`.

### Fallo 2 · Modo de apertura del binario (banco #2)
- **Dónde:** `new FileOutputStream(dat)` en `procesarParte`, sin el `true`.
- **Síntoma:** cada ejecución trunca `lecturas.dat`. Con un parte de otro día, los registros del día anterior desaparecen. Aunque ejecutes dos veces el mismo fichero, la prueba no lo delata: la segunda vez siempre dice `Registros ya existentes: 8`, pero el contenido es igual. La prueba correcta es ejecutar con un parte distinto y comprobar que el recuento previo se mantiene.
- **Por qué:** sin el segundo argumento `true`, `FileOutputStream` abre el fichero en modo sobrescribir.
- **Arreglo:** `new FileOutputStream(dat, true)`.
- **Cómo se localiza:** ejecutar con dos partes distintos (o simular un segundo día) y mirar si el recuento previo crece.

### Fallo 3 · Fecha imposible aceptada (banco #9)
- **Dónde:** `DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.LENIENT)`, debe ser `STRICT`.
- **Síntoma:** la línea `00300;Prueba, Fecha;2026-02-30;10,000` se acepta como válida (`validas: 9`) y entra en `lecturas.dat`, cuando debería ser `fecha no válida`.
- **Por qué:** con `LENIENT`, el 30 de febrero se "corrige" a otra fecha en lugar de rechazarse. El enunciado exige que la fecha exista en el calendario.
- **Arreglo:** `ResolverStyle.STRICT`.
- **Cómo se localiza:** buscar en el parte una fecha imposible y comprobar que sale como incidencia.

## Criterio de corrección (rúbrica de la sesión)
- 2,0 si lo arreglas, lo verificas con el parte y explicas la causa.
- 1,5 si lo arreglas y pasa, pero explicas la causa a medias.
- 1,0 si lo arreglas con una pista.
- 0,5 si localizas dónde está.
- 0 si no lo encuentras.

---

## Banco de diez fallos (guía de la actividad, apartado 2.5)

1. Charset equivocado: lectura en UTF-8 de un fichero Latin-1.
2. Modo de apertura: binario abierto sin añadir. **(usado: fallo 2)**
3. Comparación de textos con `==` en lugar de `.equals`.
4. Consumo calculado con la fecha o el contador como referencia en vez de la lectura anterior.
5. `while` mal: se actualiza la variable antes de comparar, y se salta una vuelta.
6. `break` de más: corta el bucle entero cuando solo quería saltarse una vuelta (`continue`).
7. Contador mal formateado: `123` en vez de `00123`.
8. Incidencia perdida: `continue` antes de escribir en el log. **(usado: fallo 1)**
9. Fecha no estricta: acepta `2026-02-30`. **(usado: fallo 3)**
10. Resumen que se añade: `resumen.txt` abierto en modo añadir.

Nota: la guía dice que el resumen no lleva cabecera. Si tu versión escribe una línea `contador;abonado;...` arriba, es un error de formato aunque no esté en el banco.
