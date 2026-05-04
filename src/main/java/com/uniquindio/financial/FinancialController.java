package com.uniquindio.financial;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FinancialController {

    private final EtlService etlService;
    private final AlgorithmService algorithmService;
    private final ReportService reportService;
    private final Map<String, List<FinancialRecord>> portfolioData = new HashMap<>();

    // Tickers de Yahoo Finance:
    // Activos colombianos (BVC) usan sufijo .CL en Yahoo Finance
    // ETFs globales usan su ticker directo (mercado NYSE/NASDAQ)
    private final List<String> symbols = Arrays.asList(
            "ECOPETROL.CL", "ISA.CL", "GEB.CL", "BOGOTA.CL", "GRUPOAVAL.CL",
            "NUTRESA.CL", "GRUPOSURA.CL", "CEMARGOS.CL", "PFAVAL.CL", "CELSIA.CL",
            "VOO", "SPY", "QQQ", "IVV", "VTI", "EFA", "IWM", "DIA", "XLK", "XLF");

    // Nombre legible para mostrar en el frontend (sin sufijo .CL)
    private String displaySymbol(String symbol) {
        return symbol.replace(".CL", "");
    }

    public FinancialController() {
        this.etlService = new EtlService();
        this.algorithmService = new AlgorithmService();
        this.reportService = new ReportService();
        initializeData();
    }

    private void initializeData() {
        System.out.println("[ETL] Iniciando descarga de datos desde Yahoo Finance...");
        for (String symbol : symbols) {
            try {
                String json = etlService.downloadHistoricalData(symbol);
                List<FinancialRecord> records = etlService.parseYahooJson(json);
                if (records.isEmpty()) {
                    System.err.println("[ETL] Sin datos para " + symbol + " — usando datos simulados.");
                    portfolioData.put(displaySymbol(symbol), generateMockData(symbol));
                } else {
                    System.out.println("[ETL] " + symbol + " → " + records.size() + " registros descargados.");
                    portfolioData.put(displaySymbol(symbol), records);
                }
            } catch (Exception e) {
                System.err.println("[ETL] Error en " + symbol + ": " + e.getMessage() + " — usando datos simulados.");
                portfolioData.put(displaySymbol(symbol), generateMockData(symbol));
            }
        }
        // Unificar y limpiar: alinear calendarios, interpolar NaN, forward-fill
        Map<String, List<FinancialRecord>> cleaned = etlService.cleanAndUnifyData(portfolioData);
        portfolioData.clear();
        portfolioData.putAll(cleaned);
        System.out.println("[ETL] Proceso completado. Activos cargados: " + portfolioData.size());
    }

    @GetMapping("/assets")
    public List<Map<String, Object>> getAssets() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String symbol : portfolioData.keySet()) {
            List<FinancialRecord> records = portfolioData.get(symbol);
            double[] returns = calculateReturns(records);
            double volatility = algorithmService.calculateVolatility(returns);

            Map<String, Object> assetInfo = new HashMap<>();
            assetInfo.put("symbol", symbol);
            assetInfo.put("name", symbol);
            assetInfo.put("risk", translateRisk(algorithmService.classifyRisk(volatility)));
            assetInfo.put("volatility", volatility);
            assetInfo.put("history", records);
            result.add(assetInfo);
        }
        // Requerimiento 3: ordenar por volatilidad descendente
        result.sort((a, b) -> Double.compare((double) b.get("volatility"), (double) a.get("volatility")));
        return result;
    }

    /** Traduce la clasificación interna al español para el frontend */
    private String translateRisk(String risk) {
        switch (risk) {
            case "CONSERVATIVE": return "Conservador";
            case "MODERATE":     return "Moderado";
            case "AGGRESSIVE":   return "Agresivo";
            default:             return risk;
        }
    }

    @GetMapping("/similarity")
    public Map<String, Double> getSimilarity(@RequestParam String sym1, @RequestParam String sym2) {
        double[] s1 = getClosePrices(portfolioData.get(sym1));
        double[] s2 = getClosePrices(portfolioData.get(sym2));

        Map<String, Double> similarity = new HashMap<>();
        similarity.put("euclidean", algorithmService.euclideanDistance(s1, s2));
        similarity.put("pearson", algorithmService.pearsonCorrelation(s1, s2));
        similarity.put("cosine", algorithmService.cosineSimilarity(s1, s2));
        similarity.put("dtw", algorithmService.computeDTW(s1, s2));
        return similarity;
    }

    /**
     * Requerimiento 3: Detección de patrones con ventana deslizante configurable.
     *
     * @param symbol     Ticker del activo
     * @param windowSize Tamaño de ventana para patrón "consecutivos al alza" (default: 3)
     *                   Valores recomendados: 3 (corto plazo), 5 (semanal), 10 (quincenal)
     */
    @GetMapping("/patterns/{symbol}")
    public Map<String, Object> getPatterns(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "3") int windowSize) {

        // Validar rango permitido para la ventana
        if (windowSize < 2) windowSize = 2;
        if (windowSize > 20) windowSize = 20;

        List<FinancialRecord> records = portfolioData.get(symbol);
        if (records == null) return Map.of("error", "Activo no encontrado: " + symbol);

        double[] prices = getClosePrices(records);
        double[] returns = calculateReturns(records);
        double volatility = algorithmService.calculateVolatility(returns);

        Map<String, Object> result = new HashMap<>();
        result.put("consecutiveUp", algorithmService.countConsecutiveUp(prices, windowSize));
        result.put("bullishEngulfing", algorithmService.countBullishEngulfing(records));
        result.put("windowSize", windowSize);          // Devolver ventana usada para transparencia
        result.put("totalDays", prices.length);        // Total de días analizados
        result.put("volatility", volatility);          // Volatilidad anualizada
        result.put("risk", translateRisk(algorithmService.classifyRisk(volatility)));
        return result;
    }

    @GetMapping("/correlation-matrix")
    public Map<String, Object> getCorrelationMatrix() {
        List<String> assetNames = new ArrayList<>(portfolioData.keySet());
        double[][] allReturns = new double[assetNames.size()][];

        for (int i = 0; i < assetNames.size(); i++) {
            List<FinancialRecord> records = portfolioData.get(assetNames.get(i));
            allReturns[i] = calculateReturns(records);
        }

        double[][] matrix = algorithmService.calculateCorrelationMatrix(allReturns);

        Map<String, Object> result = new HashMap<>();
        result.put("assets", assetNames);
        result.put("matrix", matrix);
        return result;
    }

    @GetMapping("/chart/{symbol}")
    public Map<String, Object> getChartData(@PathVariable String symbol) {
        List<FinancialRecord> records = portfolioData.get(symbol);
        double[] prices = getClosePrices(records);

        Map<String, Object> data = new HashMap<>();
        data.put("history", records);
        data.put("sma20", algorithmService.calculateSMA(prices, 20));
        return data;
    }

    private double[] getClosePrices(List<FinancialRecord> records) {
        return records.stream().mapToDouble(FinancialRecord::close).toArray();
    }

    private double[] calculateReturns(List<FinancialRecord> records) {
        if (records.size() < 2)
            return new double[0];
        double[] returns = new double[records.size() - 1];
        for (int i = 1; i < records.size(); i++) {
            returns[i - 1] = (records.get(i).close() - records.get(i - 1).close()) / records.get(i - 1).close();
        }
        return returns;
    }

    private List<FinancialRecord> generateMockData(String symbol) {
        List<FinancialRecord> mock = new ArrayList<>();
        double lastPrice = 100.0;
        Random r = new Random();
        for (int i = 0; i < 1260; i++) { // Approx 5 years of daily data
            double open = lastPrice;
            double close = open * (1 + (r.nextDouble() - 0.5) * 0.04);
            double high = Math.max(open, close) * (1 + r.nextDouble() * 0.01);
            double low = Math.min(open, close) * (1 - r.nextDouble() * 0.01);
            mock.add(new FinancialRecord(LocalDate.now().minusDays(1260 - i), open, high, low, close, 1000000, close));
            lastPrice = close;
        }
        return mock;
    }

    @GetMapping("/benchmark")
    public Map<String, Long> getBenchmark() {
        try {
            List<Double> allPrices = new ArrayList<>();
            for (List<FinancialRecord> records : portfolioData.values()) {
                for (FinancialRecord r : records) {
                    allPrices.add(r.close());
                }
            }
            if (allPrices.isEmpty()) {
                for (int i = 0; i < 500; i++) allPrices.add(Math.random() * 500);
            }
            int limit = Math.min(allPrices.size(), 500);
            double[] priceArray = new double[limit];
            for (int i = 0; i < limit; i++) priceArray[i] = allPrices.get(i);
            return algorithmService.benchmarkSorting(priceArray);
        } catch (Throwable t) {
            Map<String, Long> error = new LinkedHashMap<>();
            error.put("Error: " + t.getClass().getSimpleName(), 0L);
            return error;
        }
    }

    /**
     * Benchmark de los 4 algoritmos de similitud sobre series de tiempo reales.
     * Usa los retornos diarios de dos activos del portafolio.
     * Cada algoritmo se ejecuta 10 veces y se reporta el promedio en microsegundos.
     *
     * @param sym1 Primer activo (default: primer activo del portafolio)
     * @param sym2 Segundo activo (default: segundo activo del portafolio)
     */
    @GetMapping("/benchmark/similarity")
    public List<Map<String, Object>> getBenchmarkSimilarity(
            @RequestParam(required = false) String sym1,
            @RequestParam(required = false) String sym2) {

        List<String> keys = new ArrayList<>(portfolioData.keySet());
        String s1 = (sym1 != null && portfolioData.containsKey(sym1)) ? sym1 : keys.get(0);
        String s2 = (sym2 != null && portfolioData.containsKey(sym2)) ? sym2 : keys.get(Math.min(1, keys.size() - 1));

        double[] returns1 = calculateReturns(portfolioData.get(s1));
        double[] returns2 = calculateReturns(portfolioData.get(s2));

        return algorithmService.benchmarkSimilarity(returns1, returns2);
    }
    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        List<Map<String, Object>> assets = getAssets();

        // Calcular matriz de correlación para incluirla en el PDF
        List<String> assetNames = new ArrayList<>(portfolioData.keySet());
        double[][] allReturns = new double[assetNames.size()][];
        for (int i = 0; i < assetNames.size(); i++) {
            allReturns[i] = calculateReturns(portfolioData.get(assetNames.get(i)));
        }
        double[][] corrMatrix = algorithmService.calculateCorrelationMatrix(allReturns);

        byte[] pdfContent = reportService.generateTechnicalReport(assets, assetNames, corrMatrix);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=financial_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    @GetMapping("/report/comparative/pdf")
    public ResponseEntity<byte[]> exportComparativePdf(@RequestParam String sym1, @RequestParam String sym2) {
        Map<String, Double> similarities = getSimilarity(sym1, sym2);
        List<FinancialRecord> hist1 = portfolioData.get(sym1);
        List<FinancialRecord> hist2 = portfolioData.get(sym2);
        byte[] pdfContent = reportService.generateComparativeReport(sym1, sym2, similarities, hist1, hist2);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=comparative_report_" + sym1 + "_vs_" + sym2 + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }
}
