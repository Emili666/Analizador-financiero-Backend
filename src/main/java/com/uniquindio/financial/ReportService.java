package com.uniquindio.financial;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ReportService {

    // -------------------------------------------------------------------------
    // REPORTE TÉCNICO GLOBAL
    // -------------------------------------------------------------------------

    public byte[] generateTechnicalReport(
            List<Map<String, Object>> assets,
            List<String> assetNames,
            double[][] correlationMatrix) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            DeviceRgb primaryColor = new DeviceRgb(0, 150, 60);
            DeviceRgb darkColor    = new DeviceRgb(20, 30, 48);
            DeviceRgb lightGray    = new DeviceRgb(245, 245, 245);
            DeviceRgb white        = new DeviceRgb(255, 255, 255);

            // --- Encabezado ---
            addHeader(document, primaryColor, darkColor,
                    "Análisis de Algoritmos — Reporte del Portafolio Bursátil");

            // --- Sección 1: Metodología de volatilidad ---
            document.add(new Paragraph("1. Metodología de Clasificación de Riesgo\n")
                    .setBold().setFontSize(12).setFontColor(primaryColor));
            document.add(new Paragraph(
                    "La volatilidad histórica se calcula como la desviación estándar anualizada de los retornos " +
                    "diarios del precio de cierre (σ × √252). Clasifica cada activo en:\n" +
                    "  • Conservador:  Volatilidad < 15%\n" +
                    "  • Moderado:     15% ≤ Volatilidad < 30%\n" +
                    "  • Agresivo:     Volatilidad ≥ 30%\n\n")
                    .setTextAlignment(TextAlignment.JUSTIFIED));

            // --- Gráfica de volatilidad ---
            try {
                byte[] chartBytes = generateVolatilityChart(assets);
                Image img = new Image(ImageDataFactory.create(chartBytes));
                img.setAutoScale(true);
                document.add(img);
                document.add(new Paragraph("\n"));
            } catch (Exception e) {
                System.err.println("[PDF] Error generando gráfica de volatilidad: " + e.getMessage());
            }

            // --- Sección 2: Tabla de portafolio ---
            document.add(new Paragraph("2. Análisis del Portafolio\n")
                    .setBold().setFontSize(12).setFontColor(primaryColor));

            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 40, 30}))
                    .useAllAvailableWidth();
            table.addHeaderCell(createHeaderCell("Activo (Símbolo)", primaryColor));
            table.addHeaderCell(createHeaderCell("Perfil de Riesgo",  primaryColor));
            table.addHeaderCell(createHeaderCell("Volatilidad Anualizada", primaryColor));

            boolean alt = false;
            for (Map<String, Object> asset : assets) {
                DeviceRgb bg   = alt ? lightGray : white;
                String risk    = String.valueOf(asset.get("risk"));
                DeviceRgb riskColor = getRiskColor(risk);

                table.addCell(createCell(String.valueOf(asset.get("symbol")), bg, darkColor).setBold());
                table.addCell(createCell(risk, bg, riskColor).setBold());
                table.addCell(createCell(String.format("%.4f", (double) asset.get("volatility")), bg, darkColor));
                alt = !alt;
            }
            document.add(table);
            document.add(new Paragraph("\n"));

            // --- Sección 3: Matriz de correlación ---
            document.add(new Paragraph("3. Matriz de Correlación de Pearson\n")
                    .setBold().setFontSize(12).setFontColor(primaryColor));
            document.add(new Paragraph(
                    "La matriz muestra la correlación de Pearson entre los retornos diarios de todos los activos " +
                    "del portafolio. Valores cercanos a +1 indican movimiento conjunto (alta correlación positiva), " +
                    "valores cercanos a -1 indican movimiento opuesto, y valores cercanos a 0 indican independencia. " +
                    "El mapa de calor usa verde para correlaciones positivas y rojo para negativas.\n\n")
                    .setTextAlignment(TextAlignment.JUSTIFIED));

            // Imagen del heatmap generada algorítmicamente con Java2D
            try {
                byte[] heatmapBytes = generateCorrelationHeatmap(assetNames, correlationMatrix);
                Image heatmapImg = new Image(ImageDataFactory.create(heatmapBytes));
                heatmapImg.setAutoScale(true);
                document.add(heatmapImg);
                document.add(new Paragraph("\n"));
            } catch (Exception e) {
                System.err.println("[PDF] Error generando heatmap: " + e.getMessage());
            }

            // Tabla numérica de correlación (primeros 10 activos para legibilidad)
            int n = Math.min(assetNames.size(), 10);
            if (n > 0 && correlationMatrix != null && correlationMatrix.length >= n) {
                document.add(new Paragraph("Valores numéricos de correlación (primeros " + n + " activos):\n")
                        .setFontSize(10).setFontColor(darkColor));

                float[] colWidths = new float[n + 1];
                colWidths[0] = 15;
                for (int i = 1; i <= n; i++) colWidths[i] = (float)(85.0 / n);

                Table corrTable = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();
                corrTable.addHeaderCell(createHeaderCell("", primaryColor));
                for (int i = 0; i < n; i++) {
                    corrTable.addHeaderCell(createHeaderCell(shortName(assetNames.get(i)), primaryColor));
                }

                for (int i = 0; i < n; i++) {
                    DeviceRgb bg = (i % 2 == 0) ? lightGray : white;
                    corrTable.addCell(createCell(shortName(assetNames.get(i)), bg, darkColor).setBold());
                    for (int j = 0; j < n; j++) {
                        double val = correlationMatrix[i][j];
                        DeviceRgb cellColor = getCorrColor(val);
                        corrTable.addCell(
                            new Cell().add(new Paragraph(String.format("%.2f", val)).setFontSize(7))
                                .setBackgroundColor(cellColor)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(2)
                        );
                    }
                }
                document.add(corrTable);
            }

            document.add(new Paragraph(
                    "\n\n* Reporte generado con fines educativos — Análisis de Algoritmos, Universidad del Quindío.")
                    .setFontSize(9).setItalic());

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    // -------------------------------------------------------------------------
    // REPORTE COMPARATIVO
    // -------------------------------------------------------------------------

    public byte[] generateComparativeReport(
            String sym1, String sym2,
            Map<String, Double> similarityMetrics,
            List<FinancialRecord> hist1,
            List<FinancialRecord> hist2) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            DeviceRgb primaryColor = new DeviceRgb(41, 121, 255);
            DeviceRgb darkColor    = new DeviceRgb(20, 30, 48);
            DeviceRgb lightGray    = new DeviceRgb(245, 245, 245);
            DeviceRgb white        = new DeviceRgb(255, 255, 255);

            addHeader(document, primaryColor, darkColor,
                    "Análisis de Similitud — Reporte Comparativo: " + sym1 + " vs " + sym2);

            document.add(new Paragraph(String.format(
                    "Análisis matemático y algorítmico de la similitud histórica entre %s y %s. " +
                    "Se emplean cuatro métricas de distancia y correlación para determinar el grado " +
                    "de dependencia entre ambas series de precios.\n\n", sym1, sym2))
                    .setTextAlignment(TextAlignment.JUSTIFIED));

            // --- Gráfica de series temporales ---
            try {
                byte[] tsBytes = generateTimeSeriesChart(sym1, sym2, hist1, hist2);
                Image tsImg = new Image(ImageDataFactory.create(tsBytes));
                tsImg.setAutoScale(true);
                document.add(tsImg);
                document.add(new Paragraph("\n"));
            } catch (Exception e) {
                System.err.println("[PDF] Error generando gráfica de series: " + e.getMessage());
            }

            // --- Sección 1: Descripción de algoritmos ---
            document.add(new Paragraph("1. Algoritmos de Similitud Aplicados\n")
                    .setBold().setFontSize(12).setFontColor(primaryColor));

            com.itextpdf.layout.element.List bulletList = new com.itextpdf.layout.element.List();
            bulletList.add(new com.itextpdf.layout.element.ListItem(
                    "Distancia Euclidiana — O(n): Distancia directa en el espacio n-dimensional. " +
                    "Valores bajos indican mayor similitud en precios absolutos."));
            bulletList.add(new com.itextpdf.layout.element.ListItem(
                    "Correlación de Pearson — O(n): Mide la relación lineal entre series (-1 a +1). " +
                    "Cercano a +1: mismo movimiento. Cercano a -1: movimiento opuesto."));
            bulletList.add(new com.itextpdf.layout.element.ListItem(
                    "Similitud Coseno — O(n): Mide el ángulo entre vectores de retornos. " +
                    "Ignora magnitud, enfocándose en la dirección del movimiento."));
            bulletList.add(new com.itextpdf.layout.element.ListItem(
                    "Dynamic Time Warping (DTW) — O(n×m): Alineación no lineal de secuencias. " +
                    "Detecta similitud aunque existan desfasajes temporales entre activos."));
            document.add(bulletList);
            document.add(new Paragraph("\n"));

            // --- Sección 2: Resultados ---
            document.add(new Paragraph("2. Resultados de Similitud\n")
                    .setBold().setFontSize(12).setFontColor(primaryColor));

            Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth();
            table.addHeaderCell(createHeaderCell("Métrica", primaryColor));
            table.addHeaderCell(createHeaderCell("Valor Calculado", primaryColor));

            boolean alt = false;
            for (Map.Entry<String, Double> entry : similarityMetrics.entrySet()) {
                DeviceRgb bg = alt ? lightGray : white;
                table.addCell(createCell(entry.getKey().toUpperCase(), bg, darkColor).setBold());
                table.addCell(createCell(String.format("%.6f", entry.getValue()), bg, darkColor));
                alt = !alt;
            }
            document.add(table);

            document.add(new Paragraph(
                    "\n\n* Reporte generado con fines educativos — Análisis de Algoritmos, Universidad del Quindío.")
                    .setFontSize(9).setItalic());

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    // -------------------------------------------------------------------------
    // GENERACIÓN DE IMÁGENES
    // -------------------------------------------------------------------------

    /**
     * Genera el mapa de calor de correlación usando Java2D (sin librerías externas).
     * Cada celda se colorea interpolando entre rojo (correlación -1), blanco (0)
     * y verde (correlación +1). Los valores se imprimen dentro de cada celda.
     */
    private byte[] generateCorrelationHeatmap(List<String> names, double[][] matrix) throws Exception {
        int n = Math.min(names.size(), 20);
        int cellSize  = 36;
        int labelSize = 60;
        int padding   = 10;
        int width  = labelSize + n * cellSize + padding;
        int height = labelSize + n * cellSize + padding;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        Font labelFont = new Font("SansSerif", Font.BOLD, 9);
        Font valueFont = new Font("SansSerif", Font.PLAIN, 7);
        g.setFont(labelFont);

        // Etiquetas columnas (rotadas 45°)
        for (int j = 0; j < n; j++) {
            String label = shortName(names.get(j));
            g.setColor(new Color(20, 30, 48));
            int x = labelSize + j * cellSize + cellSize / 2;
            int y = labelSize - 5;
            // Dibujar texto rotado
            java.awt.geom.AffineTransform orig = g.getTransform();
            g.translate(x, y);
            g.rotate(-Math.PI / 4);
            g.drawString(label, 0, 0);
            g.setTransform(orig);
        }

        // Etiquetas filas + celdas
        for (int i = 0; i < n; i++) {
            // Etiqueta fila
            g.setFont(labelFont);
            g.setColor(new Color(20, 30, 48));
            String rowLabel = shortName(names.get(i));
            FontMetrics fm = g.getFontMetrics();
            int labelY = labelSize + i * cellSize + cellSize / 2 + fm.getAscent() / 2;
            g.drawString(rowLabel, padding, labelY);

            for (int j = 0; j < n; j++) {
                double val = (matrix != null && i < matrix.length && j < matrix[i].length)
                        ? matrix[i][j] : 0.0;

                // Color de la celda: rojo (-1) → blanco (0) → verde (+1)
                Color cellColor = interpolateColor(val);
                int cx = labelSize + j * cellSize;
                int cy = labelSize + i * cellSize;

                g.setColor(cellColor);
                g.fillRect(cx, cy, cellSize - 1, cellSize - 1);

                // Borde
                g.setColor(new Color(200, 200, 200));
                g.drawRect(cx, cy, cellSize - 1, cellSize - 1);

                // Valor numérico dentro de la celda
                g.setFont(valueFont);
                String valStr = String.format("%.2f", val);
                FontMetrics vfm = g.getFontMetrics();
                int tx = cx + (cellSize - vfm.stringWidth(valStr)) / 2;
                int ty = cy + (cellSize + vfm.getAscent()) / 2 - 2;
                // Texto oscuro si fondo claro, blanco si fondo oscuro
                float brightness = cellColor.getRed() * 0.299f + cellColor.getGreen() * 0.587f + cellColor.getBlue() * 0.114f;
                g.setColor(brightness > 140 ? new Color(20, 30, 48) : Color.WHITE);
                g.drawString(valStr, tx, ty);
            }
        }

        // Leyenda
        int legendY = height - padding - 12;
        g.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g.setColor(new Color(20, 30, 48));
        g.drawString("■ Verde: correlación positiva   ■ Rojo: correlación negativa   ■ Blanco: sin correlación", padding, legendY);

        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /**
     * Interpola el color de una celda del heatmap según el valor de correlación.
     * -1.0 → rojo puro, 0.0 → blanco, +1.0 → verde puro.
     */
    private Color interpolateColor(double val) {
        val = Math.max(-1.0, Math.min(1.0, val));
        if (val >= 0) {
            // Blanco → Verde
            int r = (int)(255 * (1 - val));
            int g = 255;
            int b = (int)(255 * (1 - val));
            return new Color(r, g, b);
        } else {
            // Blanco → Rojo
            int r = 255;
            int g = (int)(255 * (1 + val));
            int b = (int)(255 * (1 + val));
            return new Color(r, g, b);
        }
    }

    private byte[] generateVolatilityChart(List<Map<String, Object>> assets) throws Exception {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int limit = Math.min(10, assets.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> a = assets.get(i);
            dataset.addValue((Double) a.get("volatility"), "Volatilidad", (String) a.get("symbol"));
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Top Activos por Volatilidad Anualizada",
                "Activo", "Volatilidad",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BufferedImage bi = chart.createBufferedImage(650, 300);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", out);
        return out.toByteArray();
    }

    private byte[] generateTimeSeriesChart(
            String sym1, String sym2,
            List<FinancialRecord> hist1, List<FinancialRecord> hist2) throws Exception {

        XYSeries s1 = new XYSeries(sym1);
        XYSeries s2 = new XYSeries(sym2);

        int points = Math.min(Math.min(120, hist1.size()), hist2.size());
        for (int i = 0; i < points; i++) {
            s1.add(points - i, hist1.get(hist1.size() - 1 - i).close());
            s2.add(points - i, hist2.get(hist2.size() - 1 - i).close());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(s1);
        dataset.addSeries(s2);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Comparación de Precios de Cierre (últimos 120 días)",
                "Días", "Precio",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(41, 121, 255));
        renderer.setSeriesPaint(1, new Color(255, 61, 0));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        BufferedImage bi = chart.createBufferedImage(650, 320);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", out);
        return out.toByteArray();
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private void addHeader(Document doc, DeviceRgb primary, DeviceRgb dark, String title) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        doc.add(new Paragraph("UNIVERSIDAD DEL QUINDÍO")
                .setBold().setFontSize(18).setFontColor(primary).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Facultad de Ingeniería — Ingeniería de Sistemas y Computación")
                .setFontSize(11).setTextAlignment(TextAlignment.CENTER).setFontColor(dark));
        doc.add(new Paragraph("\n" + title + "\n")
                .setBold().setFontSize(13).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Generado: " + date + "\n\n")
                .setFontSize(9).setTextAlignment(TextAlignment.RIGHT));
    }

    private Cell createHeaderCell(String text, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(new DeviceRgb(255, 255, 255)).setFontSize(9))
                .setBackgroundColor(bg)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
    }

    private Cell createCell(String text, DeviceRgb bg, DeviceRgb fontColor) {
        return new Cell()
                .add(new Paragraph(text).setFontColor(fontColor).setFontSize(9))
                .setBackgroundColor(bg)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(4);
    }

    /** Color de celda en la tabla numérica de correlación */
    private DeviceRgb getCorrColor(double val) {
        val = Math.max(-1.0, Math.min(1.0, val));
        if (val >= 0) {
            int r = (int)(255 * (1 - val * 0.6));
            int g = (int)(200 + 55 * val);
            int b = (int)(255 * (1 - val * 0.6));
            return new DeviceRgb(r, g, b);
        } else {
            int r = (int)(200 + 55 * (-val));
            int g = (int)(255 * (1 + val * 0.6));
            int b = (int)(255 * (1 + val * 0.6));
            return new DeviceRgb(r, g, b);
        }
    }

    private DeviceRgb getRiskColor(String risk) {
        if (risk.equals("Conservador") || risk.equals("CONSERVATIVE")) return new DeviceRgb(46, 125, 50);
        if (risk.equals("Moderado")    || risk.equals("MODERATE"))     return new DeviceRgb(230, 81, 0);
        return new DeviceRgb(198, 40, 40); // Agresivo
    }

    /** Acorta el nombre del activo para que quepa en la celda del heatmap */
    private String shortName(String name) {
        return name.length() > 7 ? name.substring(0, 7) : name;
    }
}
