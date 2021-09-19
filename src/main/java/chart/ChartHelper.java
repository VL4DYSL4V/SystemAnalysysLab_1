package chart;

import dto.ChartDto;
import org.apache.commons.math3.linear.RealVector;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ChartHelper {

    private static final ChartHelper INSTANCE = new ChartHelper();

    private JFrame previousChart;

    private ChartHelper() {
    }

    public static ChartHelper getInstance() {
        return INSTANCE;
    }

    public void showNextChart(List<RealVector> sequenceY, double T) {
        if (previousChart != null) {
            previousChart.dispose();
        }
        ChartDto dto = getChartDto(sequenceY, T);
        XYChart chart = getChart(dto.getXData(), dto.getYData());
        this.previousChart = new SwingWrapper<>(chart).displayChart();
        this.previousChart.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private XYChart getChart(List<? extends Number> xData, List<? extends Number> yData) {
        XYChart chart = new XYChart(1600, 900);
        chart.setTitle("Chart");
        chart.setXAxisTitle("t");
        chart.setYAxisTitle("y(t)");
        XYSeries series = chart.addSeries("y(t)", xData, yData);
        series.setMarker(SeriesMarkers.NONE);
        return chart;
    }

    private ChartDto getChartDto(List<RealVector> sequenceY, double T) {
        int factor = 1000;
        int step = Math.max(sequenceY.size() / factor, 1);
        List<Double> yData = new ArrayList<>(factor);
        List<Double> xData = new ArrayList<>(factor);
        for (int i = 0; i < sequenceY.size(); i += step) {
            RealVector vector = sequenceY.get(i);
            if (vector.getDimension() == 1) {
                yData.add(vector.getEntry(0));
                xData.add(i * T);
            }
        }
        return new ChartDto(xData, yData);
    }
}
