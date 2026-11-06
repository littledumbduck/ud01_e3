import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Lee el parte nocturno de lecturas, guarda los registros validos en lecturas.dat
 * (binario, solo se anade), registra incidencias en incidencias.log y regenera
 * resumen.txt a partir de todo el historico.
 *
 * Uso: java -cp out LectorContadores datos/lecturas_AAAAMMDD.txt
 */
public class LectorContadores {

    private static final Pattern CONTADOR = Pattern.compile("\\d{5}");
    private static final Pattern FECHA = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern LECTURA = Pattern.compile("\\d{1,5},\\d{3}");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.LENIENT);
    private static final Locale ES = Locale.forLanguageTag("es-ES");

    /** Un registro del binario: contador, fecha AAAAMMDD, lectura y abonado. */
    private record Registro(int contador, long fecha, double lectura, String abonado) {}

    /** Datos acumulados de un contador para generar resumen.txt. */
    private static final class Historial {
        String abonado;
        int numLecturas;
        double ultima;
        double anterior;

        void anadir(Registro r) {
            anterior = ultima;
            ultima = r.lectura();
            abonado = r.abonado();
            numLecturas++;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java -cp out LectorContadores <fichero_parte.txt>");
            System.exit(2);
        }
        File entrada = new File(args[0]);
        if (!entrada.isFile() || entrada.length() == 0) {
            System.out.println("Error: el fichero no existe o esta vacio: " + entrada.getPath());
            System.exit(2);
        }

        File carpeta = entrada.getAbsoluteFile().getParentFile();
        File dat = new File(carpeta, "lecturas.dat");
        File log = new File(carpeta, "incidencias.log");
        File resumen = new File(carpeta, "resumen.txt");

        try {
            int previos = recorrer(dat, r -> { });
            System.out.println("Registros ya existentes en lecturas.dat: " + previos);

            procesarParte(entrada, dat, log);
            regenerarResumen(dat, resumen);
            System.out.println("Resumen regenerado en: " + resumen.getPath());
        } catch (IOException e) {
            System.out.println("Error de E/S: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Procesa el parte linea a linea, sin cargarlo entero en memoria. */
    private static void procesarParte(File entrada, File dat, File log) throws IOException {
        Set<Integer> aceptados = new HashSet<>();
        int numLinea = 0;
        int validas = 0;
        int incidencias = 0;

        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                        new FileInputStream(entrada), StandardCharsets.ISO_8859_1));
             DataOutputStream salida = new DataOutputStream(new BufferedOutputStream(
                        new FileOutputStream(dat)));
             BufferedWriter incidenciasLog = Files.newBufferedWriter(log.toPath(),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            String linea;
            while ((linea = lector.readLine()) != null) {
                numLinea++;
                if (linea.isBlank()) {
                    continue;
                }

                String[] campos = linea.split(";", -1);
                String motivo = validar(campos, aceptados);
                if (motivo != null) {
                    incidencias++;
                    continue;
                }

                int contador = Integer.parseInt(campos[0]);
                long fecha = Long.parseLong(campos[2].replace("-", ""));
                double lectura = Double.parseDouble(campos[3].replace(',', '.'));
                String abonado = campos[1].trim();

                salida.writeInt(contador);
                salida.writeLong(fecha);
                salida.writeDouble(lectura);
                salida.writeUTF(abonado);
                aceptados.add(contador);
                validas++;
            }
        }

        System.out.println("Lineas leidas: " + numLinea
                + " | validas: " + validas
                + " | incidencias: " + incidencias);
    }

    /** Devuelve el primer motivo de incidencia que se cumple, o null si la linea es valida. */
    private static String validar(String[] c, Set<Integer> aceptados) {
        if (c.length != 4) {
            return "campos incompletos";
        }
        if (!CONTADOR.matcher(c[0]).matches() || Integer.parseInt(c[0]) == 0) {
            return "contador no válido";
        }
        if (c[1].isBlank()) {
            return "abonado vacío";
        }
        if (!FECHA.matcher(c[2]).matches() || !fechaExiste(c[2])) {
            return "fecha no válida";
        }
        if (!LECTURA.matcher(c[3]).matches()) {
            return "lectura no válida";
        }
        if (aceptados.contains(Integer.parseInt(c[0]))) {
            return "lectura duplicada";
        }
        return null;
    }

    private static boolean fechaExiste(String texto) {
        try {
            LocalDate.parse(texto, FORMATO_FECHA);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** Regenera resumen.txt con todo el contenido de lecturas.dat, ordenado por contador. */
    private static void regenerarResumen(File dat, File resumen) throws IOException {
        Map<Integer, Historial> porContador = new TreeMap<>();
        recorrer(dat, r -> porContador.computeIfAbsent(r.contador(), k -> new Historial()).anadir(r));

        try (BufferedWriter w = Files.newBufferedWriter(resumen.toPath(), StandardCharsets.UTF_8)) {
            for (Map.Entry<Integer, Historial> e : porContador.entrySet()) {
                Historial h = e.getValue();
                String consumo;
                String estado;
                if (h.numLecturas == 1) {
                    consumo = "-";
                    estado = "SIN_HISTORICO";
                } else {
                    double diferencia = h.ultima - h.anterior;
                    long milesimas = Math.round(diferencia * 1000);
                    consumo = String.format(ES, "%.3f", diferencia);
                    if (milesimas < 0) {
                        estado = "POSSIBLE_SUBSTITUCION";
                    } else if (milesimas == 0) {
                        estado = "SIN_CONSUMO";
                    } else {
                        estado = "OK";
                    }
                }
                w.write(String.format("%05d;%s;%d;%s;%s\n",
                        e.getKey(), h.abonado, h.numLecturas, consumo, estado));
            }
        }
    }

    /**
     * Recorre lecturas.dat registro a registro y aplica la accion a cada uno.
     * Devuelve cuantos registros ha leido. Si el fichero no existe, devuelve 0.
     */
    private static int recorrer(File dat, Consumer<Registro> accion) throws IOException {
        if (!dat.exists()) {
            return 0;
        }
        int n = 0;
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(dat)))) {
            while (true) {
                accion.accept(leerRegistro(in));
                n++;
            }
        } catch (EOFException fin) {
            return n;
        }
    }

    /** Lee un registro en el mismo orden en que se escribio. */
    private static Registro leerRegistro(DataInputStream in) throws IOException {
        int contador = in.readInt();
        long fecha = in.readLong();
        double lectura = in.readDouble();
        String abonado = in.readUTF();
        return new Registro(contador, fecha, lectura, abonado);
    }
}
