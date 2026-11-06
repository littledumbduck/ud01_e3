# Sesión de rotura (simulacro)

Esta copia de `LectorContadores` tiene **tres fallos**. Tu objetivo es dejarla funcionando y verificarla con el parte de prueba.

## Cómo compilar y ejecutar

Desde esta carpeta (`examen/`), con JDK 17 o superior:

```bash
javac -encoding UTF-8 -d out src/LectorContadores.java
java -cp out LectorContadores datos/lecturas_20261019.txt
```

## Qué mirar

- Los números de línea de `incidencias.log` deben coincidir con el fichero de entrada.
- Ejecuta el programa **dos veces** y comprueba que el histórico crece.
- Revisa cómo clasifica las líneas que terminan en `;`.

## Reglas del simulacro

- 40 minutos, sin conexión y sin asistentes de IA.
- Al terminar, explica cada fallo: ¿por qué falla?
- Pon el cronómetro en marcha y no mires `_SOLUCION_PROFESOR/`.
