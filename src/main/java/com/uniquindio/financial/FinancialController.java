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
    private final List<String> symbols = Arrays.asList(
            "ECOPETROL.CB", "ISA.CB", "GEB.CB", "PFBCOLOM.CB", "BCOLOMBIA.CB",
            "VOO", "CSPX", "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
            "META", "NVDA", "V", "MA", "PYPL", "NFLX", "ADBE", "CRM");

    public FinancialController() {
        this.etlService = new EtlService();
        this.algorithmService = new AlgorithmService();
        this.reportService = new ReportService();
        initializeData();
    }

    private void initializeData() {
        // In a real app, this would be an async task or triggered by an ETL endpoint
        for (String symbol : symbols) {
            try {
                // For demonstration, we'll try to download, but would normally use a cache
                String csv = etlService.downloadHistoricalData(symbol);
                portfolioData.put(symbol, etlService.parseCsv(csv));
            } catch (Exception e) {
                // Mock data if API fails or for symbols not found
                portfolioData.put(symbol, generateMockData(symbol));
            }
        }
        // Unify and clean
        Map<String, List<FinancialRecord>> cleaned = etlService.cleanAndUnifyData(portfolioData);
        portfolioData.clear();
        portfolioData.putAll(cleaned);
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
            assetInfo.put("risk", algorithmService.classifyRisk(volatility));
            assetInfo.put("volatility", volatility);
            result.add(assetInfo);
        }
        // Requirement 3: Sorted by risk (volatility descending)
        result.sort((a, b) -> Double.compare((double) b.get("volatility"), (double) a.get("volatility")));
        return result;
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

    @GetMapping("/patterns/{symbol}")
    public Map<String, Integer> getPatterns(@RequestParam String symbol) {
        List<FinancialRecord> records = portfolioData.get(symbol);
        double[] prices = getClosePrices(records);

        Map<String, Integer> patterns = new HashMap<>();
        patterns.put("consecutiveUp", algorithmService.countConsecutiveUp(prices, 3));
        patterns.put("bullishEngulfing", algorithmService.countBullishEngulfing(records));
        return patterns;
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
        for (int i = 0; i < 200; i++) {
            double open = lastPrice;
            double close = open * (1 + (r.nextDouble() - 0.5) * 0.05);
            double high = Math.max(open, close) * (1 + r.nextDouble() * 0.01);
            double low = Math.min(open, close) * (1 - r.nextDouble() * 0.01);
            mock.add(new FinancialRecord(LocalDate.now().minusDays(200 - i), open, high, low, close, 1000000, close));
            lastPrice = close;
        }
        return mock;
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        List<Map<String, Object>> assets = getAssets();
        byte[] pdfContent = reportService.generateTechnicalReport(assets);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=financial_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }
}
