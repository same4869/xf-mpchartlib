package com.github.mikephil.charting.stockChart;

import android.content.Context;
import android.graphics.Paint;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.github.mikephil.charting.R;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.VolFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.stockChart.markerView.BarBottomMarkerView;
import com.github.mikephil.charting.stockChart.renderer.ColorContentYAxisRenderer;
import com.github.mikephil.charting.stockChart.charts.CoupleChartGestureListener;
import com.github.mikephil.charting.stockChart.markerView.LeftMarkerView;
import com.github.mikephil.charting.stockChart.charts.TimeBarChart;
import com.github.mikephil.charting.stockChart.charts.TimeLineChart;
import com.github.mikephil.charting.stockChart.markerView.TimeRightMarkerView;
import com.github.mikephil.charting.stockChart.charts.TimeXAxis;
import com.github.mikephil.charting.stockChart.dataManage.TimeDataManage;
import com.github.mikephil.charting.stockChart.enums.ChartType;
import com.github.mikephil.charting.stockChart.event.BaseEvent;
import com.github.mikephil.charting.stockChart.model.CirclePositionTime;
import com.github.mikephil.charting.stockChart.model.TimeDataModel;
import com.github.mikephil.charting.utils.CommonUtil;
import com.github.mikephil.charting.utils.NumberUtils;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * 当日分时图
 */
public class OneDayChart extends BaseChart {

    private Context mContext;
    TimeLineChart lineChart;
    FrameLayout cirCleView;

    private LineDataSet d1;

    TimeXAxis xAxisLine;
    YAxis axisRightLine;
    YAxis axisLeftLine;

    private int maxCount = ChartType.HK_ONE_DAY.getPointNum();//最大可见数量，即分时一天最大数据点数
    private SparseArray<String> xLabels = new SparseArray<>();//X轴刻度label
    private TimeDataManage mData;
    private int[] colorArray;

    public OneDayChart(Context context) {
        this(context, null);
    }

    public OneDayChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.view_time, this);
        lineChart = (TimeLineChart) findViewById(R.id.line_chart);
        cirCleView = (FrameLayout) findViewById(R.id.circle_frame_time);

        EventBus.getDefault().register(this);

        colorArray = new int[]{ContextCompat.getColor(mContext, R.color.up_color), ContextCompat.getColor(mContext, R.color.equal_color), ContextCompat.getColor(mContext, R.color.down_color)};

        playHeartbeatAnimation(cirCleView.findViewById(R.id.anim_view));

    }

    /**
     * 初始化图表属性
     */
    public void initChart(boolean landscape) {
        this.landscape = landscape;
        //主图
        lineChart.setScaleEnabled(false);//是否可以缩放
        lineChart.setDrawBorders(false);//是否画外框线
        lineChart.setBorderColor(ContextCompat.getColor(mContext, R.color.border_color));
        lineChart.setBorderWidth(0.7f);
        lineChart.setNoDataText(getResources().getString(R.string.loading));
        Legend lineChartLegend = lineChart.getLegend();
        lineChartLegend.setEnabled(false);
        lineChart.setDescription(null);

        //主图X轴
        xAxisLine = (TimeXAxis) lineChart.getXAxis();
        xAxisLine.setDrawAxisLine(false);
        xAxisLine.setTextColor(ContextCompat.getColor(mContext, R.color.label_text));
        xAxisLine.setPosition(XAxis.XAxisPosition.BOTTOM);//x轴刻度值显示在底部
        xAxisLine.setAvoidFirstLastClipping(false);
        xAxisLine.setGridColor(ContextCompat.getColor(mContext, R.color.grid_color));
        xAxisLine.setGridLineWidth(0.7f);

//        // 获取图表的 X 轴
//        XAxis xAxis = lineChart.getXAxis();
//        // 计算中央位置的 X 坐标
//        float centerX = maxCount / 2;
//        // 在中央位置画一条竖线
//        LimitLine centerLine = new LimitLine(centerX, "");
//        centerLine.setLineColor(ContextCompat.getColor(mContext, R.color.border_color)); // 设置线的颜色
//        centerLine.setLineWidth(0.5f); // 设置线的宽度
////            centerLine.enableDashedLine(10f, 10f, 0f); // 设置为虚线，可以根据需要调整虚线的间隔和线段长度
//        xAxis.addLimitLine(centerLine);

        //主图左Y轴
        axisLeftLine = lineChart.getAxisLeft();
        axisLeftLine.setLabelCount(5, true);//Y轴左边分多少个刻度
        axisLeftLine.setDrawGridLines(false);
        axisLeftLine.setValueLineInside(true);
        axisLeftLine.setDrawTopBottomGridLine(false);
        axisLeftLine.setDrawAxisLine(false);
        axisLeftLine.setDrawZeroLine(false);
        //刻度值在图形里面还是外面
        axisLeftLine.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        axisLeftLine.setYOffset(-10f);
        axisLeftLine.setTextColor(ContextCompat.getColor(mContext, R.color.axis_text));
        axisLeftLine.setValueFormatter(new YAxisValueFormatter());

        //主图右Y轴
        axisRightLine = lineChart.getAxisRight();
        axisRightLine.setLabelCount(5, true);//Y轴右边分多少个刻度
        axisRightLine.setDrawTopBottomGridLine(true);
        axisRightLine.setDrawGridLines(true);
        axisRightLine.setGridLineWidth(0.7f);
        axisRightLine.setDrawLabels(false);
        //虚线
        axisRightLine.enableGridDashedLine(CommonUtil.dip2px(mContext, 4), CommonUtil.dip2px(mContext, 3), 0);
        axisRightLine.setDrawAxisLine(false);
        axisRightLine.setValueLineInside(true);
        axisRightLine.setPosition(landscape ? YAxis.YAxisLabelPosition.OUTSIDE_CHART : YAxis.YAxisLabelPosition.INSIDE_CHART);
        axisRightLine.setGridColor(ContextCompat.getColor(mContext, R.color.grid_color));
        axisRightLine.setTextColor(ContextCompat.getColor(mContext, R.color.axis_text));
        axisRightLine.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                DecimalFormat mFormat = new DecimalFormat("#0.00%");
                return mFormat.format(value);
            }
        });
    }

    static class YAxisValueFormatter extends ValueFormatter {
        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            if (value >= 100000000) { // 大于等于1亿
                return String.format(Locale.getDefault(), "%.0f亿", value / 100000000);
            } else if (value <= -100000000) { // 小于等于-1亿
                return String.format(Locale.getDefault(), "%.0f亿", value / 100000000);
            } else {
                return super.getAxisLabel(value, axis);
            }
        }
    }

    /**
     * 是否显示坐标轴label
     *
     * @param isShow
     */
    private void setShowLabels(boolean isShow) {
        lineChart.getAxisLeft().setDrawLabels(isShow);
        lineChart.getAxisRight().setDrawLabels(false);
        lineChart.getXAxis().setDrawLabels(isShow);
    }

    /**
     * 设置分时数据
     *
     * @param mData
     */
    public void setDataToChart(TimeDataManage mData) {
        this.mData = mData;
//        cirCleView.setVisibility(View.VISIBLE);
        if (mData.getDatas().size() == 0) {
            cirCleView.setVisibility(View.GONE);
            lineChart.setNoDataText(getResources().getString(R.string.no_data));
            lineChart.invalidate();
            return;
        }

        ArrayList<Entry> lineCJEntries = new ArrayList<>();
        ArrayList<Entry> lineJJEntries = new ArrayList<>();
        for (int i = 0, j = 0; i < mData.getDatas().size(); i++, j++) {
            TimeDataModel t = mData.getDatas().get(j);
            if (t == null) {
                lineCJEntries.add(new Entry(i, Float.NaN));
                lineJJEntries.add(new Entry(i, Float.NaN));
                continue;
            }
            lineCJEntries.add(new Entry(i, (float) mData.getDatas().get(i).getNowPrice()));
            lineJJEntries.add(new Entry(i, (float) mData.getDatas().get(i).getAveragePrice()));
        }

        if (lineChart.getData() != null && lineChart.getData().getDataSetCount() > 0) {
            d1 = (LineDataSet) lineChart.getData().getDataSetByIndex(0);
            d1.setValues(lineCJEntries);
            d1.notifyDataSetChanged();
            lineChart.getData().notifyDataChanged();
            lineChart.notifyDataSetChanged();
        } else {
            if (mData.getAssetId().endsWith(".HK")) {
                setPrecision(mData.getAssetId().contains("IDX") ? 2 : 3);
                setMaxCount(ChartType.HK_ONE_DAY.getPointNum());
            } else {
                setPrecision(2);
                setMaxCount(ChartType.ONE_DAY.getPointNum());
            }
            setXLabels(mData.getOneDayXLabels(landscape));
            setShowLabels(true);
            setMarkerView(mData);
            setBottomMarkerView(mData);

            //左Y轴label渲染颜色
            Transformer leftYTransformer = lineChart.getRendererLeftYAxis().getTransformer();
            ColorContentYAxisRenderer leftColorContentYAxisRenderer = new ColorContentYAxisRenderer(lineChart.getViewPortHandler(), axisLeftLine, leftYTransformer);
            leftColorContentYAxisRenderer.setLabelColor(colorArray);
            leftColorContentYAxisRenderer.setClosePrice(mData.getPreClose());
            leftColorContentYAxisRenderer.setLandscape(landscape);
            lineChart.setRendererLeftYAxis(leftColorContentYAxisRenderer);
            //右Y轴label渲染颜色
            Transformer rightYTransformer = lineChart.getRendererRightYAxis().getTransformer();
            ColorContentYAxisRenderer rightColorContentYAxisRenderer = new ColorContentYAxisRenderer(lineChart.getViewPortHandler(), axisRightLine, rightYTransformer);
            rightColorContentYAxisRenderer.setLabelColor(colorArray);
            rightColorContentYAxisRenderer.setClosePrice(mData.getPreClose());
            rightColorContentYAxisRenderer.setLandscape(landscape);
            lineChart.setRendererRightYAxis(rightColorContentYAxisRenderer);

            // 获取图表的 X 轴
            XAxis xAxis = lineChart.getXAxis();
            // 计算中央位置的 X 坐标
            float centerX = maxCount / 2;
            // 在中央位置画一条竖线
            LimitLine centerLine = new LimitLine(centerX, "");
            centerLine.setLineColor(ContextCompat.getColor(mContext, R.color.border_color)); // 设置线的颜色
            centerLine.setLineWidth(0.5f); // 设置线的宽度
            xAxis.addLimitLine(centerLine);

            if (Float.isNaN(mData.getPercentMax()) || Float.isNaN(mData.getPercentMin()) || Float.isNaN(mData.getVolMaxTime())) {
                axisRightLine.setAxisMinimum(-0.01f);
                axisRightLine.setAxisMaximum(0.01f);
            } else {
                axisRightLine.setAxisMinimum(mData.getPercentMin());
                axisRightLine.setAxisMaximum(mData.getPercentMax());
            }

            d1 = new LineDataSet(lineCJEntries, "分时线");
            d1.setDrawCircleDashMarker(landscape);
            d1.setDrawValues(false);
            d1.setLineWidth(0.7f);
            d1.setColor(ContextCompat.getColor(mContext, R.color.minute_blue));
            d1.setDrawFilled(false);
            d1.setHighLightColor(ContextCompat.getColor(mContext, R.color.highLight_Color));
            d1.setHighlightEnabled(false);//是否显示高亮十字线
            d1.setDrawCircles(false);
            d1.setAxisDependency(YAxis.AxisDependency.LEFT);
            d1.setPrecision(precision);
            d1.setTimeDayType(1);//设置分时图类型
            ArrayList<ILineDataSet> sets = new ArrayList<>();
            sets.add(d1);
            LineData cd = new LineData(sets);
            lineChart.setData(cd);

            //请注意，修改视口的所有方法需要在为Chart设置数据之后调用。
            //设置当前视图四周的偏移量。 设置这个，将阻止图表自动计算它的偏移量。使用 resetViewPortOffsets()撤消此设置。
            if (landscape) {
                float volwidth = Utils.calcTextWidthForVol(mPaint, mData.getVolMaxTime());
                float pricewidth = Utils.calcTextWidth(mPaint, NumberUtils.keepPrecision(Float.isNaN(mData.getMax()) ? "0" : mData.getMax() + "", precision) + "#");
                float left = CommonUtil.dip2px(mContext, pricewidth > volwidth ? pricewidth : volwidth);
                float right = CommonUtil.dip2px(mContext, Utils.calcTextWidth(mPaint, "-10.00%"));
                lineChart.setViewPortOffsets(left, CommonUtil.dip2px(mContext, 5), right, CommonUtil.dip2px(mContext, 15));
            } else {
                lineChart.setViewPortOffsets(CommonUtil.dip2px(mContext, 30), CommonUtil.dip2px(mContext, 5), CommonUtil.dip2px(mContext, 5), CommonUtil.dip2px(mContext, 15));
            }

            axisLeftLine.setAxisMinimum(mData.getMin());
            axisLeftLine.setAxisMaximum(mData.getMax());
            //下面方法需在填充数据后调用
            xAxisLine.setXLabels(getXLabels());
            xAxisLine.setLabelCount(getXLabels().size(), false);
            xAxisLine.setDrawGridLines(false);
//            xAxisLine.enableGridDashedLine(CommonUtil.dip2px(mContext, 4), CommonUtil.dip2px(mContext, 3), 0);
            lineChart.setVisibleXRange(maxCount, maxCount);
            //moveViewTo(...) 方法会自动调用 invalidate()
            lineChart.moveViewToX(mData.getDatas().size() - 1);

        }
    }

    /**
     * 动态增加一个点数据
     *
     * @param timeDatamodel
     * @param length
     */
    public void dynamicsAddOne(TimeDataModel timeDatamodel, int length) {
        int index = length - 1;
        LineData lineData = lineChart.getData();
        ILineDataSet d1 = lineData.getDataSetByIndex(0);
        d1.addEntry(new Entry(index, (float) timeDatamodel.getNowPrice()));
        ILineDataSet d2 = lineData.getDataSetByIndex(1);
        d2.addEntry(new Entry(index, (float) timeDatamodel.getAveragePrice()));

        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRange(maxCount, maxCount);
        //动态添加或移除数据后， 调用invalidate()刷新图表之前 必须调用 notifyDataSetChanged() .
        lineChart.moveViewToX(index);
    }

    /**
     * 动态更新最后一点数据
     *
     * @param timeDatamodel
     * @param length
     */
    public void dynamicsUpdateOne(TimeDataModel timeDatamodel, int length) {
        int index = length - 1;
        LineData lineData = lineChart.getData();
        ILineDataSet d1 = lineData.getDataSetByIndex(0);
        Entry e = d1.getEntryForIndex(index);
        d1.removeEntry(e);
        d1.addEntry(new Entry(index, (float) timeDatamodel.getNowPrice()));

        ILineDataSet d2 = lineData.getDataSetByIndex(1);
        Entry e2 = d2.getEntryForIndex(index);
        d2.removeEntry(e2);
        d2.addEntry(new Entry(index, (float) timeDatamodel.getAveragePrice()));

        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.moveViewToX(index);
    }

    public void cleanData() {
        if (lineChart != null && lineChart.getLineData() != null) {
            setShowLabels(false);
            lineChart.clearValues();
        }
        if (cirCleView != null) {
            cirCleView.setVisibility(View.GONE);
        }
    }

    private void setMarkerView(TimeDataManage mData) {
        LeftMarkerView leftMarkerView = new LeftMarkerView(mContext, R.layout.my_markerview, precision);
        TimeRightMarkerView rightMarkerView = new TimeRightMarkerView(mContext, R.layout.my_markerview);
        lineChart.setMarker(leftMarkerView, rightMarkerView, mData);
    }

    private void setBottomMarkerView(TimeDataManage kDatas) {
        BarBottomMarkerView bottomMarkerView = new BarBottomMarkerView(mContext, R.layout.my_markerview);
    }


    @Override
    public void onEventMainThread(BaseEvent event) {
        if (event.method == 1) {
            CirclePositionTime position = (CirclePositionTime) event.obj;
            cirCleView.setX(position.cx - cirCleView.getWidth() / 2);
            cirCleView.setY(position.cy - cirCleView.getHeight() / 2);
        }
    }

    public void setXLabels(SparseArray<String> xLabels) {
        this.xLabels = xLabels;
    }

    public SparseArray<String> getXLabels() {
        if (xLabels.size() == 0) {
            setMaxCount(ChartType.HK_ONE_DAY.getPointNum());
            xLabels.put(0, "09:30");
            xLabels.put(60, "10:30");
            xLabels.put(120, "11:30");
            xLabels.put(180, "13:30");
            xLabels.put(240, "14:30");
            xLabels.put(300, "15:30");
            xLabels.put(330, "16:00");
        }
        return xLabels;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public void eventBusUnregister() {
        EventBus.getDefault().unregister(this);
    }


}
