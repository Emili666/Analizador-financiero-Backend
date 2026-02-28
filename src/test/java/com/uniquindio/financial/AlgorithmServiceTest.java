package com.uniquindio.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmServiceTest {

    private AlgorithmService algorithmService;

    @BeforeEach
    void setUp() {
        algorithmService = new AlgorithmService();
    }

    @Test
    void testEuclideanDistance() {
        double[] s1 = { 1.0, 2.0, 3.0 };
        double[] s2 = { 4.0, 5.0, 6.0 };
        double result = algorithmService.euclideanDistance(s1, s2);
        assertEquals(Math.sqrt(27.0), result, 0.001);
    }

    @Test
    void testPearsonCorrelation() {
        double[] s1 = { 1, 2, 3 };
        double[] s2 = { 2, 4, 6 }; // perfectly correlated
        double result = algorithmService.pearsonCorrelation(s1, s2);
        assertEquals(1.0, result, 0.001);

        double[] s3 = { 3, 2, 1 }; // negative correlated
        result = algorithmService.pearsonCorrelation(s1, s3);
        assertEquals(-1.0, result, 0.001);
    }

    @Test
    void testCosineSimilarity() {
        double[] s1 = { 1, 0 };
        double[] s2 = { 0, 1 }; // orthogonal
        assertEquals(0.0, algorithmService.cosineSimilarity(s1, s2), 0.001);

        double[] s3 = { 1, 1 };
        double[] s4 = { 2, 2 }; // same direction
        assertEquals(1.0, algorithmService.cosineSimilarity(s3, s4), 0.001);
    }

    @Test
    void testCountConsecutiveUp() {
        double[] prices = { 1.0, 2.0, 3.0, 2.0, 1.0, 2.0, 3.0, 4.0 };
        // Expecting 2 windows of size 3: (1,2,3) at index 0 and (2,3,4) at index 5.
        // Wait, windowSize = 3 means 3 days total: p[i], p[i+1], p[i+2]
        // Prices:
        // i=0: 1,2,3 -> up
        // i=1: 2,3,2 -> not up
        // i=2: 3,2,1 -> not up
        // i=3: 2,1,2 -> not up
        // i=4: 1,2,3 -> up
        // i=5: 2,3,4 -> up
        int result = algorithmService.countConsecutiveUp(prices, 3);
        assertEquals(3, result);
    }

    @Test
    void testCountBullishEngulfing() {
        FinancialRecord day1 = new FinancialRecord(LocalDate.now().minusDays(2), 10.0, 11.0, 9.0, 8.0, 100, 8.0); // Red:
                                                                                                                  // close(8)<open(10)
        FinancialRecord day2 = new FinancialRecord(LocalDate.now().minusDays(1), 7.0, 12.0, 6.0, 11.0, 100, 11.0); // Green:
                                                                                                                   // close(11)>open(7)
                                                                                                                   // engulfs
                                                                                                                   // day1
                                                                                                                   // body
                                                                                                                   // (8-10)
        FinancialRecord day3 = new FinancialRecord(LocalDate.now(), 11.0, 11.0, 10.0, 10.0, 100, 10.0); // Not engulfing

        List<FinancialRecord> records = Arrays.asList(day1, day2, day3);
        int count = algorithmService.countBullishEngulfing(records);
        assertEquals(1, count);
    }

    @Test
    void testCalculateVolatilityAndRisk() {
        double[] returns = { 0.01, 0.02, -0.01, 0.005, -0.02 };
        double vol = algorithmService.calculateVolatility(returns);
        assertNotEquals(0.0, vol);
        String risk = algorithmService.classifyRisk(vol);
        assertNotNull(risk);
    }

    @Test
    void testCalculateCorrelationMatrix() {
        double[] s1 = { 1, 2, 3 };
        double[] s2 = { 2, 4, 6 };
        double[] s3 = { 3, 2, 1 };
        double[][] allSeries = { s1, s2, s3 };

        double[][] matrix = algorithmService.calculateCorrelationMatrix(allSeries);

        assertEquals(3, matrix.length);
        assertEquals(3, matrix[0].length);
        assertEquals(1.0, matrix[0][0], 0.001);
        assertEquals(1.0, matrix[1][1], 0.001);
        assertEquals(1.0, matrix[2][2], 0.001);

        assertEquals(1.0, matrix[0][1], 0.001);
        assertEquals(-1.0, matrix[0][2], 0.001);
    }
}
