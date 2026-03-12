package com.uniquindio.financial;

import java.util.*;

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

    // --- SORTING ALGORITHMS SECTION ---

    // 1. Selection Sort
    public void selectionSort(double[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIdx]) minIdx = j;
            }
            double temp = arr[minIdx];
            arr[minIdx] = arr[i];
            arr[i] = temp;
        }
    }

    // 2. QuickSort
    public void quickSort(double[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }

    private int partition(double[] arr, int low, int high) {
        double pivot = arr[high];
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            if (arr[j] < pivot) {
                i++;
                double temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        double temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }

    // 3. HeapSort
    public void heapSort(double[] arr) {
        int n = arr.length;
        for (int i = n / 2 - 1; i >= 0; i--) heapify(arr, n, i);
        for (int i = n - 1; i > 0; i--) {
            double temp = arr[0];
            arr[0] = arr[i];
            arr[i] = temp;
            heapify(arr, i, 0);
        }
    }

    private void heapify(double[] arr, int n, int i) {
        int largest = i;
        int l = 2 * i + 1;
        int r = 2 * i + 2;
        if (l < n && arr[l] > arr[largest]) largest = l;
        if (r < n && arr[r] > arr[largest]) largest = r;
        if (largest != i) {
            double swap = arr[i];
            arr[i] = arr[largest];
            arr[largest] = swap;
            heapify(arr, n, largest);
        }
    }

    // 4. Gnome Sort
    public void gnomeSort(double[] arr) {
        int n = arr.length;
        int index = 0;
        while (index < n) {
            if (index == 0) index++;
            if (arr[index] >= arr[index - 1]) index++;
            else {
                double temp = arr[index];
                arr[index] = arr[index - 1];
                arr[index - 1] = temp;
                index--;
            }
        }
    }

    // 5. Comb Sort
    public void combSort(double[] arr) {
        int n = arr.length;
        int gap = n;
        boolean swapped = true;
        while (gap != 1 || swapped) {
            gap = (gap * 10) / 13;
            if (gap < 1) gap = 1;
            swapped = false;
            for (int i = 0; i < n - gap; i++) {
                if (arr[i] > arr[i + gap]) {
                    double temp = arr[i];
                    arr[i] = arr[i + gap];
                    arr[i + gap] = temp;
                    swapped = true;
                }
            }
        }
    }

    // 6. TimSort (Simplified)
    public void timSort(double[] arr) {
        int n = arr.length;
        int RUN = 32;
        for (int i = 0; i < n; i += RUN) {
            insertionSort(arr, i, Math.min((i + RUN - 1), (n - 1)));
        }
        for (int size = RUN; size < n; size = 2 * size) {
            for (int left = 0; left < n; left += 2 * size) {
                int mid = left + size - 1;
                int right = Math.min((left + 2 * size - 1), (n - 1));
                if (mid < right) merge(arr, left, mid, right);
            }
        }
    }

    private void insertionSort(double[] arr, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            double temp = arr[i];
            int j = i - 1;
            while (j >= left && arr[j] > temp) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = temp;
        }
    }

    private void merge(double[] arr, int l, int m, int r) {
        int len1 = m - l + 1, len2 = r - m;
        double[] left = new double[len1];
        double[] right = new double[len2];
        System.arraycopy(arr, l, left, 0, len1);
        System.arraycopy(arr, m + 1, right, 0, len2);
        int i = 0, j = 0, k = l;
        while (i < len1 && j < len2) {
            if (left[i] <= right[j]) arr[k++] = left[i++];
            else arr[k++] = right[j++];
        }
        while (i < len1) arr[k++] = left[i++];
        while (j < len2) arr[k++] = right[j++];
    }

    // 7. Tree Sort
    private static class Node {
        double key;
        Node left, right;
        Node(double item) { key = item; }
    }

    public void treeSort(double[] arr) {
        if (arr.length == 0) return;
        Node root = new Node(arr[0]);
        for (int i = 1; i < arr.length; i++) {
            insertNode(root, arr[i]);
        }
        
        int index = 0;
        Stack<Node> stack = new Stack<>();
        Node current = root;
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.push(current);
                current = current.left;
            }
            current = stack.pop();
            arr[index++] = current.key;
            current = current.right;
        }
    }

    private void insertNode(Node root, double key) {
        Node current = root;
        while (true) {
            if (key < current.key) {
                if (current.left != null) current = current.left;
                else {
                    current.left = new Node(key);
                    break;
                }
            } else {
                if (current.right != null) current = current.right;
                else {
                    current.right = new Node(key);
                    break;
                }
            }
        }
    }


    // 8. Pigeonhole Sort
    @SuppressWarnings("unchecked")
    public void pigeonholeSort(double[] arr) {
        if (arr.length == 0) return;
        double min = arr[0], max = arr[0];
        for (double x : arr) {
            if (x < min) min = x;
            if (x > max) max = x;
        }
        int range = (int)(max - min) + 1;
        List<Double>[] holes = new ArrayList[range];
        for (int i = 0; i < range; i++) holes[i] = new ArrayList<>();
        for (double x : arr) holes[(int)(x - min)].add(x);
        int index = 0;
        for (int i = 0; i < range; i++) {
            for (double x : holes[i]) arr[index++] = x;
        }
    }

    // 9. Bucket Sort
    @SuppressWarnings("unchecked")
    public void bucketSort(double[] arr) {
        int n = arr.length;
        if (n <= 0) return;
        List<Double>[] buckets = new ArrayList[n];
        for (int i = 0; i < n; i++) buckets[i] = new ArrayList<>();
        double min = arr[0], max = arr[0];
        for (double x : arr) {
            if (x < min) min = x;
            if (x > max) max = x;
        }
        for (double x : arr) {
            int bi = (int) ( (x - min) / (max - min + 0.0001) * (n - 1) );
            buckets[bi].add(x);
        }
        for (int i = 0; i < n; i++) Collections.sort(buckets[i]);
        int index = 0;
        for (int i = 0; i < n; i++) {
            for (double x : buckets[i]) arr[index++] = x;
        }
    }

    // 10. Bitonic Sort
    public void bitonicSort(double[] arr) {
        bitonicSortRecursive(arr, 0, arr.length, 1);
    }
    private void bitonicSortRecursive(double[] a, int low, int cnt, int dir) {
        if (cnt > 1) {
            int k = cnt / 2;
            bitonicSortRecursive(a, low, k, 1);
            bitonicSortRecursive(a, low + k, cnt - k, 0);
            bitonicMerge(a, low, cnt, dir);
        }
    }
    private void bitonicMerge(double[] a, int low, int cnt, int dir) {
        if (cnt > 1) {
            int k = cnt / 2;
            for (int i = low; i < low + cnt - k; i++) {
                if (dir == (a[i] > a[i + k] ? 1 : 0)) {
                    double temp = a[i]; a[i] = a[i + k]; a[i + k] = temp;
                }
            }
            bitonicMerge(a, low, k, dir);
            bitonicMerge(a, low + k, cnt - k, dir);
        }
    }

    // 11. Binary Insertion Sort
    public void binaryInsertionSort(double[] arr) {
        for (int i = 1; i < arr.length; i++) {
            double x = arr[i];
            int j = Math.abs(Arrays.binarySearch(arr, 0, i, x) + 1);
            System.arraycopy(arr, j, arr, j + 1, i - j);
            arr[j] = x;
        }
    }

    // 12. Radix Sort
    public void radixSort(double[] arr) {
        if (arr.length == 0) return;
        long[] scaled = new long[arr.length];
        long min = Long.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            scaled[i] = (long)(arr[i] * 100);
            if (scaled[i] < min) min = scaled[i];
        }
        for (int i = 0; i < arr.length; i++) scaled[i] -= min;
        long max = 0;
        for (long x : scaled) if (x > max) max = x;
        for (long exp = 1; max / exp > 0; exp *= 10) countSortRadix(scaled, exp);
        for (int i = 0; i < arr.length; i++) arr[i] = (double)(scaled[i] + min) / 100.0;
    }
    private void countSortRadix(long[] arr, long exp) {
        int n = arr.length;
        long[] output = new long[n];
        int[] count = new int[10];
        for (int i = 0; i < n; i++) count[(int)((arr[i] / exp) % 10)]++;
        for (int i = 1; i < 10; i++) count[i] += count[i - 1];
        for (int i = n - 1; i >= 0; i--) {
            output[count[(int)((arr[i] / exp) % 10)] - 1] = arr[i];
            count[(int)((arr[i] / exp) % 10)]--;
        }
        System.arraycopy(output, 0, arr, 0, n);
    }

    public Map<String, Long> benchmarkSorting(double[] original) {
        Map<String, Long> results = new LinkedHashMap<>();
        String[] algorithms = {
            "Selection Sort", "QuickSort", "HeapSort", "Gnome Sort", "Comb Sort", 
            "TimSort", "Tree Sort", "Pigeonhole Sort", "Bucket Sort", "Bitonic Sort", 
            "Binary Insertion Sort", "Radix Sort"
        };

        for (String algo : algorithms) {
            double[] copy = Arrays.copyOf(original, original.length);
            long start = System.nanoTime();
            switch (algo) {
                case "Selection Sort": selectionSort(copy); break;
                case "QuickSort": quickSort(copy, 0, copy.length - 1); break;
                case "HeapSort": heapSort(copy); break;
                case "Gnome Sort": gnomeSort(copy); break;
                case "Comb Sort": combSort(copy); break;
                case "TimSort": timSort(copy); break;
                case "Tree Sort": treeSort(copy); break;
                case "Pigeonhole Sort": pigeonholeSort(copy); break;
                case "Bucket Sort": bucketSort(copy); break;
                case "Bitonic Sort": bitonicSort(copy); break;
                case "Binary Insertion Sort": binaryInsertionSort(copy); break;
                case "Radix Sort": radixSort(copy); break;
            }
            long end = System.nanoTime();
            results.put(algo, (end - start) / 1000); // return in microseconds
        }
        return results;
    }
}
