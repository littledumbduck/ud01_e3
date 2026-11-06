# Decisiones de diseño

## 1. Línea incompleta: qué hago y por qué

Una línea que no tiene exactamente cuatro campos se registra en `incidencias.log` con el motivo `campos incompletos`. No se inserta nada en `lecturas.dat`, y el programa sigue con la línea siguiente.

No intento reparar la línea. Si le faltan campos, no sé si falta el abonado, la fecha o la lectura, así que cualquier «arreglo» sería inventar datos de un contador. Además, un registro mal reconstruido en el binario no se puede corregir después, porque el binario no se borra.

Hay un detalle importante: el número de campos se cuenta con `split(";", -1)`. Con el límite `-1`, Java conserva los campos vacíos del final. Una línea como `00130;Díaz, Elena;2026-10-19;` tiene cuatro campos, y el cuarto está vacío, así que se registra como `lectura no válida` y no como `campos incompletos`. Con `split(";")` sin límite, el campo vacío final desaparecería y la línea se clasificaría mal.

## 2. Cómo distingo un contador sustituido de una lectura errónea

Con los datos del parte no se puede saber con certeza. El programa marca el caso como `POSSIBLE_SUBSTITUCION` cuando el consumo es negativo: la última lectura es menor que la anterior. Esa situación tiene dos causas posibles:

- **Contador sustituido**: el contador físico se cambió por uno nuevo, que empieza con una lectura baja.
- **Lectura errónea**: el operario o la PDA anotó mal la lectura, por ejemplo un dígito de menos.

Para distinguirlas miraría el historial del mismo contador:

- Si la lectura siguiente vuelve a estar en el nivel de antes, fue un error puntual de lectura. Es un valor atípico, no un cambio de contador.
- Si la lectura siguiente continúa desde el nuevo nivel bajo, es una sustitución. El consumo vuelve a tener sentido a partir de ahí.
- Si el abonado también cambia, la sustitución es más probable.

El programa no toma esa decisión: deja el caso marcado para que una persona lo revise.

## 3. Qué pasaría si el binario se abriera en modo sobrescribir

Si `lecturas.dat` se abriera con `new FileOutputStream(dat)` en lugar de `new FileOutputStream(dat, true)`, cada ejecución borraría el fichero antes de escribir. Pasarían tres cosas:

1. El histórico se perdería cada día. El binario tendría solo las lecturas del último parte.
2. El resumen saldría mal, porque se generaría a partir de un único día. Todos los contadores aparecerían como `SIN_HISTORICO`.
3. La línea «Registros ya existentes en lecturas.dat» siempre mostraría 0.

El requisito de que el binario sea histórico y de que nunca se borre se cumple precisamente por usar el modo añadir.
