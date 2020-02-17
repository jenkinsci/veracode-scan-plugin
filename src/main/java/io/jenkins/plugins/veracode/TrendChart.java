package io.jenkins.plugins.veracode;

import java.awt.Color;
import java.awt.Paint;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import hudson.util.Graph;
import io.jenkins.plugins.veracode.data.BuildHistory;
import io.jenkins.plugins.veracode.data.ScanHistory;

public class TrendChart extends Graph {

    private final CategoryDataset dataset;

    public TrendChart(long timestamp, int defaultW, int defaultH,
            Collection<BuildHistory> buildHistoryList) {
        super(timestamp, defaultW, defaultH);
        dataset = createDataset(buildHistoryList);
    }

    private void populateDataset(DefaultCategoryDataset dataset, String dataType,
            List<Map<String, Long>> buildList) {
        if (null == dataset || (null == buildList || buildList.size() == 0)) {
            return;
        }

        for (Map<String, Long> currEntry : buildList) {
            Long value = currEntry.get(ScanHistory.BUILD_DATE);
            String buildDate = value != null ? formatDate(value) : null;
            if (null != buildDate) {
                value = currEntry.get(ScanHistory.FLAWS_COUNT);
                if (null != value) {
                    dataset.addValue(value, dataType, buildDate);
                }
            }
        }
    }

    private CategoryDataset createDataset(Collection<BuildHistory> buildHistoryList) {

        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (BuildHistory iter : buildHistoryList) {
            if ((iter.getBuildType() != null) && (iter.getBuildList() != null)) {
                populateDataset(dataset, iter.getBuildType(), iter.getBuildList());
            }
        }
        return dataset;
    }

    private String formatDate(Long value) {

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(value);
        String buildDate = String.format("%s/%s %02d:%02d",
                date.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.HOUR_OF_DAY),
                date.get(Calendar.MINUTE));
        return buildDate;
    }

    @Override
    protected JFreeChart createGraph() {
        final JFreeChart chart = ChartFactory.createStackedBarChart("", "", "", dataset,
                PlotOrientation.VERTICAL, true, // enable legend
                false, // tooltips
                false);
        chart.setBackgroundPaint(Color.white);
        final CategoryPlot plot = chart.getCategoryPlot();

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        CategoryAxis domainAxis = plot.getDomainAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);

        StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
        Paint p1 = new Color(117, 205, 235);
        Paint p2 = new Color(122, 96, 168);
        renderer.setSeriesPaint(0, p1); // series 0 for static
        renderer.setSeriesPaint(1, p2); // series 1 for SCA
        renderer.setMaximumBarWidth(0.125);

        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setRenderer(renderer);

        return chart;
    }
}