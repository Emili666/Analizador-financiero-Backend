package com.uniquindio.financial;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ETL Service — Extracción, Transformación y Carga de datos financieros.
 *
 * Fuente de datos: Yahoo Finance API no oficial (query1.finance.yahoo.com).
 * No requiere token ni registro. Se accede mediante peticiones HTTP directas
 * con parsing manual del JSON de respuesta.
 *
 * Restricciones cumplidas:
 *  - Sin yfinance, pandas_datareader ni equivalentes.
 *  - Peticiones HTTP explícitas con HttpClient de Java estándar.
 *  - Parsing manual del JSON sin librerías de alto nivel de finanzas.
 */
public class EtlService {

    private final HttpClient httpClient;

    // Pausa entre requests para no saturar la API (ms)
    private static final int REQUEST_DELAY_MS = 300;

    public EtlService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Descarga datos históricos diarios de Yahoo Finance para un símbolo dado.
     * Horizonte: 5 años hacia atrás desde hoy.
     *
     * Endpoint: https://query1.finance.yahoo.com/v8/finance/chart/{symbol}
     * Parámetros: interval=1d, range=5y
     *
     * No requiere autenticación. Se agrega User-Agent de navegador para evitar
     * bloqueos por parte del servidor.
     *
     * @param symbol Ticker de Yahoo Finance (ej: "ECOPETROL.CL", "VOO")
     * @return JSON crudo de la respuesta
     */
    public String downloadHistoricalData(String symbol) throws IOException, InterruptedException {
        String url = String.format(
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=5y",
            symbol
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            // User-Agent requerido: Yahoo bloquea requests sin cabecera de navegador
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(
                "Error descargando " + symbol + " desde Yahoo Finance. HTTP Status: " + response.statusCode()
            );
        }

        // Pausa para no saturar la API
        Thread.sleep(REQUEST_DELAY_MS);

        return response.body();
    }

    /**
     * Parsea el JSON de Yahoo Finance v8 de forma manual.
     *
     * Estructura del JSON relevante:
     * {
     *   "chart": {
     *     "result": [{
     *       "timestamp": [unix_epoch, ...],
     *       "indicators": {
     *         "quote": [{
     *           "open": [...], "high": [...], "low": [...],
     *           "close": [...], "volume": [...]
     *         }],
     *         "adjclose": [{ "adjclose": [...] }]
     *       }
     *     }]
     *   }
     * }
     *
     * El parsing se realiza con búsqueda de subcadenas para evitar dependencias
     * de librerías JSON externas, manteniendo el comportamiento algorítmico
     * completamente transparente.
     *
     * @param jsonData JSON crudo de Yahoo Finance
     * @return Lista de FinancialRecord ordenada por fecha ascendente
     */
    public List<FinancialRecord> parseYahooJson(String jsonData) {
        List<FinancialRecord> records = new ArrayList<>();

        try {
            // Verificar que hay resultado válido
            if (jsonData.contains("\"result\":null") || jsonData.contains("\"code\":\"Not Found\"")) {
                return records;
            }

            // Extraer array de timestamps (Unix epoch en segundos)
            long[] timestamps = extractLongArray(jsonData, "\"timestamp\":[");

            // Extraer arrays OHLCV del bloque "quote"
            int quoteStart = jsonData.indexOf("\"quote\":[{");
            if (quoteStart == -1) return records;
            String quoteBlock = jsonData.substring(quoteStart);

            double[] opens   = extractDoubleArray(quoteBlock, "\"open\":[");
            double[] highs   = extractDoubleArray(quoteBlock, "\"high\":[");
            double[] lows    = extractDoubleArray(quoteBlock, "\"low\":[");
            double[] closes  = extractDoubleArray(quoteBlock, "\"close\":[");
            double[] volumes = extractDoubleArray(quoteBlock, "\"volume\":[");

            // Extraer adjclose (puede estar en bloque separado)
            double[] adjCloses = extractDoubleArray(jsonData, "\"adjclose\":[");
            if (adjCloses.length == 0) adjCloses = closes; // fallback a close

            int n = timestamps.length;
            for (int i = 0; i < n; i++) {
                // Saltar registros con datos nulos (días sin cotización)
                if (i >= closes.length || Double.isNaN(closes[i])) continue;

                LocalDate date = Instant.ofEpochSecond(timestamps[i])
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();

                double open     = i < opens.length   ? opens[i]     : closes[i];
                double high     = i < highs.length   ? highs[i]     : closes[i];
                double low      = i < lows.length    ? lows[i]      : closes[i];
                double close    = closes[i];
                double volume   = i < volumes.length ? volumes[i]   : 0;
                double adjClose = i < adjCloses.length ? adjCloses[i] : close;

                // Validación básica: descartar registros con precios negativos o cero
                if (close <= 0 || open <= 0) continue;

                records.add(new FinancialRecord(date, open, high, low, close, volume, adjClose));
            }

            // Ordenar por fecha ascendente
            records.sort(Comparator.comparing(FinancialRecord::date));

        } catch (Exception e) {
            System.err.println("Error parseando JSON de Yahoo Finance: " + e.getMessage());
        }

        return records;
    }

    /**
     * Extrae un array de long (timestamps) desde un JSON crudo buscando la clave dada.
     * Parsing manual sin librerías externas.
     */
    private long[] extractLongArray(String json, String key) {
        int start = json.indexOf(key);
        if (start == -1) return new long[0];
        start += key.length();
        int end = json.indexOf("]", start);
        if (end == -1) return new long[0];

        String[] parts = json.substring(start, end).split(",");
        List<Long> values = new ArrayList<>();
        for (String p : parts) {
            try {
                String trimmed = p.trim();
                if (!trimmed.isEmpty() && !trimmed.equals("null")) {
                    values.add(Long.parseLong(trimmed));
                }
            } catch (NumberFormatException ignored) {}
        }
        return values.stream().mapToLong(Long::longValue).toArray();
    }

    /**
     * Extrae un array de double (precios/volúmenes) desde un JSON crudo buscando la clave dada.
     * Los valores "null" del JSON (días sin datos) se convierten a Double.NaN para
     * ser manejados posteriormente en la etapa de limpieza.
     */
    private double[] extractDoubleArray(String json, String key) {
        int start = json.indexOf(key);
        if (start == -1) return new double[0];
        start += key.length();
        int end = json.indexOf("]", start);
        if (end == -1) return new double[0];

        String[] parts = json.substring(start, end).split(",");
        List<Double> values = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.equals("null") || trimmed.isEmpty()) {
                values.add(Double.NaN); // Valor faltante — se maneja en cleanAndUnifyData
            } else {
                try {
                    values.add(Double.parseDouble(trimmed));
                } catch (NumberFormatException ignored) {
                    values.add(Double.NaN);
                }
            }
        }
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * Limpieza y unificación de datos (Requerimiento 1).
     *
     * Estrategia de manejo de valores faltantes:
     *
     * 1. FORWARD-FILL (propagación hacia adelante): Para fechas donde un activo
     *    no tiene cotización (festivos, diferencias de calendario bursátil entre
     *    mercados colombiano y estadounidense), se replica el último registro conocido
     *    con volumen=0. Esto preserva la continuidad de la serie sin introducir
     *    información artificial. Es la técnica estándar en análisis de series
     *    financieras para alinear calendarios.
     *
     * 2. INTERPOLACIÓN LINEAL: Para valores NaN internos en campos numéricos
     *    (open, high, low, close) dentro de un registro existente, se usa el
     *    último valor conocido del mismo campo. Esto es preferible a eliminar
     *    el registro porque mantiene la longitud de la serie constante, lo cual
     *    es requisito para los algoritmos de similitud (DTW, Euclidiana, etc.)
     *    que operan sobre vectores de igual dimensión.
     *
     * 3. NO SE ELIMINAN REGISTROS: La eliminación rompería la alineación temporal
     *    entre activos, invalidando la matriz de correlación y los cálculos de
     *    similitud cruzada.
     *
     * Complejidad: O(D × A) donde D = número de fechas únicas, A = número de activos.
     *
     * @param rawData Mapa símbolo → lista de registros sin unificar
     * @return Mapa símbolo → lista de registros alineados a calendario común
     */
    public Map<String, List<FinancialRecord>> cleanAndUnifyData(Map<String, List<FinancialRecord>> rawData) {
        // Paso 1: Construir conjunto de todas las fechas únicas (unión de calendarios)
        Set<LocalDate> allDates = new TreeSet<>();
        rawData.values().forEach(list -> list.forEach(r -> allDates.add(r.date())));

        Map<String, List<FinancialRecord>> unifiedData = new HashMap<>();

        for (Map.Entry<String, List<FinancialRecord>> entry : rawData.entrySet()) {
            String symbol = entry.getKey();
            List<FinancialRecord> original = entry.getValue();

            // Índice por fecha para búsqueda O(1)
            Map<LocalDate, FinancialRecord> dateMap = original.stream()
                .collect(Collectors.toMap(FinancialRecord::date, r -> r, (r1, r2) -> r1));

            List<FinancialRecord> unified = new ArrayList<>();
            FinancialRecord lastKnown = null;

            // Obtener primer registro disponible para backward-fill inicial
            FinancialRecord firstKnown = original.isEmpty()
                ? new FinancialRecord(LocalDate.now(), 100, 100, 100, 100, 0, 100)
                : original.get(0);

            for (LocalDate date : allDates) {
                if (dateMap.containsKey(date)) {
                    FinancialRecord rec = dateMap.get(date);

                    // Interpolación: reemplazar NaN internos con último valor conocido
                    // Justificación: mantiene longitud de serie para algoritmos de similitud
                    if (Double.isNaN(rec.close()) && lastKnown != null) {
                        rec = interpolate(rec, lastKnown);
                    }
                    unified.add(rec);
                    lastKnown = rec;
                } else {
                    // Forward-fill: fecha sin cotización (festivo / diferencia de calendario)
                    // Justificación: preserva continuidad sin introducir datos artificiales
                    FinancialRecord filler = lastKnown != null ? lastKnown : firstKnown;
                    unified.add(new FinancialRecord(
                        date,
                        filler.open(), filler.high(), filler.low(), filler.close(),
                        0.0, // volumen 0 indica que no hubo negociación ese día
                        filler.adjClose()
                    ));
                }
            }

            unifiedData.put(symbol, unified);
        }

        return unifiedData;
    }

    /**
     * Interpolación de campos NaN usando el último valor conocido (forward-fill por campo).
     * Complejidad: O(1).
     */
    private FinancialRecord interpolate(FinancialRecord current, FinancialRecord last) {
        return new FinancialRecord(
            current.date(),
            Double.isNaN(current.open())     ? last.close()    : current.open(),
            Double.isNaN(current.high())     ? last.high()     : current.high(),
            Double.isNaN(current.low())      ? last.low()      : current.low(),
            Double.isNaN(current.close())    ? last.close()    : current.close(),
            Double.isNaN(current.volume())   ? 0               : current.volume(),
            Double.isNaN(current.adjClose()) ? last.adjClose() : current.adjClose()
        );
    }
}
