# Diseño: reparto de bytes del registro binario

## Registro

Cada registro de `lecturas.dat` se escribe en este orden, sin relleno ni separadores:

| Orden | Campo    | Escritura   | Bytes | Desplazamiento en el registro |
|-------|----------|-------------|-------|-------------------------------|
| 1     | Contador | `writeInt`  | 4     | 0 – 3                         |
| 2     | Fecha    | `writeLong` | 8     | 4 – 11                        |
| 3     | Lectura  | `writeDouble` | 8   | 12 – 19                       |
| 4     | Abonado  | `writeUTF`  | 2 + n | 20 – (20 + 2 + n − 1)         |

- `writeUTF` escribe primero 2 bytes con la longitud `n` en bytes (UTF-8 modificado) y después los `n` bytes del texto.
- La fecha se guarda como `long` en formato AAAAMMDD (p. ej. `20261019`), no como texto.
- La parte fija del registro ocupa siempre 20 bytes. El abonado es el único campo de longitud variable.

## Por qué ese orden

1. **Campos de tamaño fijo primero.** Con `int`, `long` y `double` el lector sabe exactamente cuántos bytes ocupa cada uno sin leer nada más. Así el contador queda en la posición 0 y los datos numéricos en desplazamientos conocidos.
2. **El abonado al final.** Es el único campo cuya longitud no es fija. Al ir en último lugar, no hace falta ningún campo que indique dónde empieza el siguiente registro: el siguiente empieza justo después del texto. Además `writeUTF` ya lleva su propia longitud delante, así que el lector no necesita contar caracteres.
3. **El contador delante.** Es la clave de agrupación del resumen. Puede leerse sin decodificar el resto del registro.

## Ejemplo

Registro de `00123;Muñoz Galianez, Oskar;2026-10-19;1234,500`:

| Campo    | Valor                      | Bytes | Acumulado |
|----------|----------------------------|-------|-----------|
| Contador | `00123` → 123              | 4     | 4         |
| Fecha    | `20261019`                 | 8     | 12        |
| Lectura  | `1234.5`                   | 8     | 20        |
| Longitud | 22 bytes de texto          | 2     | 22        |
| Abonado  | `Muñoz Galianez, Oskar` (la `ñ` ocupa 2 bytes) | 22 | 44 |

Tamaño total del registro: **44 bytes**.
