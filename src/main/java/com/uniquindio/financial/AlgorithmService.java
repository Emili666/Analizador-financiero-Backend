package com.uniquindio.financial;

import java.util.List;

public class AlgorithmService {

    /**
     * Requirement: Manual implementation of Euclidean Distance.
     * Complexity Analysis: O(n) where n is the length of the series.
     * We iterate through the arrays once to calculate the sum of squared
     * differences.
     */
    public double euclideanDistance(double[] series1, double[] series2) {
        if (series1.length != series2.length) {
            throw new IllegalArgumentException("Series must have same length");
        }
        double sum = 0;
        for (int i = 0; i < series1.length; i++) {
            sum += Math.pow(series1[i] - series2[i], 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * Requirement: Manual implementation of Pearson Correlation.
     * Complexity Analysis: O(n) where n is the length of the series.
     * We iterate through the arrays once to accumulate sums for the correlation
     * formula.
     */
    public double pearsonCorrelation(double[] series1, double[] series2) {
        if (series1.length != series2.length) {
            throw new IllegalArgumentException("Series must have same length");
        }
        int n = series1.length;
        double sum1 = 0, sum2 = 0, sum1Sq = 0, sum2Sq = 0, pSum = 0;
        for (int i = 0; i < n; i++) {
            sum1 += series1[i];
            sum2 += series2[i];
            sum1Sq += Math.pow(series1[i], 2);
            sum2Sq += Math.pow(series2[i], 2);
            pSum += series1[i] * series2[i];
        }
        double num = pSum - (sum1 * sum2 / n);
        double den = Math.sqrt((sum1Sq - Math.pow(sum1, 2) / n) * (sum2Sq - Math.pow(sum2, 2) / n));
        if (den == 0)
            return 0;
        return num / den;
    }

    /**
     * Requirement: Manual implementation of Dynamic Time Warping (DTW).
     * Complexity Analysis: O(n*m) where n and m are the lengths of the two series.
     * We use a dynamic programming table of size (n+1) x (m+1).
     */
    public double computeDTW(double[] series1, double[] series2) {
        int n = series1.length;
        int m = series2.length;
        double[][] dtw = new double[n + 1][m + 1];

        for (int i = 1; i <= n; i++)
            dtw[i][0] = Double.POSITIVE_INFINITY;
        for (int j = 1; j <= m; j++)
            dtw[0][j] = Double.POSITIVE_INFINITY;
        dtw[0][0] = 0;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                double cost = Math.abs(series1[i - 1] - series2[j - 1]);
                dtw[i][j] = cost + Math.min(dtw[i - 1][j], Math.min(dtw[i][j - 1], dtw[i - 1][j - 1]));
            }
        }
        return dtw[n][m];
    }

    /**
     * Requirement: Manual implementation of Cosine Similarity.
     * Complexity Analysis: O(n) where n is the length of the series.
     * Single pass to calculate dot product and magnitudes.
     */
    public double cosineSimilarity(double[] series1, double[] series2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < series1.length; i++) {
            dotProduct += series1[i] * series2[i];
            normA += Math.pow(series1[i], 2);
            normB += Math.pow(series2[i], 2);
        }
        if (normA == 0 || normB == 0)
            return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Requirement: Volatility (historical) calculation.
     * Complexity Analysis: O(n) where n is the number of returns.
     * Two passes (one for mean, one for variance).
     */
    public double calculateVolatility(double[] returns) {
        if (returns.length < 2)
            return 0;
        double mean = 0;
        for (double r : returns)
            mean += r;
        mean /= returns.length;

        double variance = 0;
        for (double r : returns)
            variance += Math.pow(r - mean, 2);
        variance /= (returns.length - 1);

        return Math.sqrt(variance) * Math.sqrt(252); // Annualized
    }

    /**
     * Requirement: Sliding window pattern detection.
     * Pattern: Consecutive Up (prices increasing for 'windowSize' days).
     * Complexity Analysis: O(n * windowSize) where n is the prices length.
     */
    public int countConsecutiveUp(double[] prices, int windowSize) {
        int count = 0;
        for (int i = 0; i <= prices.length - windowSize; i++) {
            boolean isUp = true;
            for (int j = 1; j < windowSize; j++) {
                if (prices[i + j] <= prices[i + j - 1]) {
                    isUp = false;
                    break;
                }
            }
            if (isUp)
                count++;
        }
        return count;
    }

    /**
     * Requirement: Additional pattern detection (Bullish Engulfing).
     * Rule: Previous day is red (Close < Open), current day is green (Close >
     * Open),
     * and current body engulfs previous body.
     * Complexity Analysis: O(n) where n is the number of records.
     */
    public int countBullishEngulfing(List<FinancialRecord> records) {
        int count = 0;
        for (int i = 1; i < records.size(); i++) {
            FinancialRecord prev = records.get(i - 1);
            FinancialRecord curr = records.get(i);

            boolean prevRed = prev.close() < prev.open();
            boolean currGreen = curr.close() > curr.open();

            if (prevRed && currGreen) {
                if (curr.open() <= prev.close() && curr.close() >= prev.open()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Requirement: Risk Classification.
     * Complexity Analysis: O(1).
     */
    public String classifyRisk(double volatility) {
        if (volatility < 0.15)
            return "CONSERVATIVE";
        if (volatility < 0.30)
            return "MODERATE";
        return "AGGRESSIVE";
    }

    /**
     * Requirement: Calculate simple moving average (SMA).
     * Complexity Analysis: O(n * period).
     */
    public double[] calculateSMA(double[] prices, int period) {
        if (prices.length < period)
            return new double[0];
        double[] sma = new double[prices.length - period + 1];
        for (int i = 0; i <= prices.length - period; i++) {
            double sum = 0;
            for (int j = 0; j < period; j++) {
                sum += prices[i + j];
            }
            sma[i] = sum / period;
        }
        return sma;
    }

    /**
     * Requirement: Calculate Correlation Matrix.
     * Complexity Analysis: O(k * n^2) where k is the length of the series and n is
     * the number of assets.
     */
    public double[][] calculateCorrelationMatrix(double[][] allSeries) {
        int n = allSeries.length;
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 1.0;
                } else {
                    double corr = pearsonCorrelation(allSeries[i], allSeries[j]);
                    matrix[i][j] = corr;
                    matrix[j][i] = corr;
                }
            }
        }
        return matrix;
    }
}
