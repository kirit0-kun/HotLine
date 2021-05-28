package com.flowapp.HotLine.Controllers;

import com.flowapp.HotLine.HotLine;
import com.flowapp.HotLine.Models.HotLineResult;
import com.flowapp.HotLine.Models.Point;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {
    
    @FXML
    private TextField iDTextField;

    @FXML
    private TextField oDTextField;

    @FXML
    private CheckBox isReverseCheckBox;

    @FXML
    private TextField spGrTextField;

    @FXML
    private TextField vis100TextField;

    @FXML
    private TextField vis212TextField;

    @FXML
    private TextField flowRateTextField;

    @FXML
    private TextField maxTempTextField;

    @FXML
    private TextField minTempTextField;

    @FXML
    private TextField tsTextField;

    @FXML
    private TextField lambdaSTextField;

    @FXML
    private TextField tinTextField;

    @FXML
    private TextField hTextField;

    @FXML
    private TextField lambdaCTextField;

    @FXML
    private TextField alphaCTextField;

    @FXML
    private TextField tf1TextField;

    @FXML
    private TextField tf2TextField;

    @FXML
    private TextField maxPumpPressureTextField;

    @FXML
    private TextField noPumpsTextField;

    @FXML
    private TextField pumpInitialPressureTextField;

    @FXML
    private TextArea answerArea;

    @FXML
    private Button calculateBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DecimalFormat format = new DecimalFormat( "#.0" );
        final var decimalFormatter = new TextFormatter<>(c -> {
            if (c.getControlNewText().isEmpty() ) { return c; }
            ParsePosition parsePosition = new ParsePosition(0);
            Object object = format.parse(c.getControlNewText(), parsePosition);
            if (object == null || parsePosition.getIndex() < c.getControlNewText().length()) { return null; }
            else { return c; }
        });
        final TextField[] textFields = {
            iDTextField,oDTextField,spGrTextField,vis100TextField,
                vis212TextField,flowRateTextField,maxTempTextField,
                minTempTextField,tsTextField,lambdaSTextField,tinTextField,
                hTextField,lambdaCTextField,alphaCTextField,tf1TextField,
                tf2TextField,maxPumpPressureTextField,noPumpsTextField,
                pumpInitialPressureTextField
        };
        for (var field: textFields) {
            field.setTextFormatter(decimalFormatter);
        }
        calculateBtn.setOnAction(e -> calculate());
    }

    void calculate() {
        final Float iDmm = getFloat(iDTextField.getText());
        final Float oDmm = getFloat(oDTextField.getText());
        final float spGr = getFloat(spGrTextField.getText());
        final float visAt100F = getFloat(vis100TextField.getText());
        final float visAt212F = getFloat(vis212TextField.getText());
        final float flowRateM3H = getFloat(flowRateTextField.getText());
        final Float maxTempC = getFloat(maxTempTextField.getText());
        final Float minTempC = getFloat(minTempTextField.getText());
        final float tsC = getFloat(tsTextField.getText());
        final float lambdaS = getFloat(lambdaSTextField.getText());
        final float tinIn = getFloat(tinTextField.getText());
        final float h = getFloat(hTextField.getText());
        final float lambdaC = getFloat(lambdaCTextField.getText());
        final float alphaT = getFloat(alphaCTextField.getText());
        final float tf1 = getFloat(tf1TextField.getText());
        final float tf2 = getFloat(tf2TextField.getText());
        final float maxPumpPressure = getFloat(maxPumpPressureTextField.getText());
        final Float maxTotalPressure = getInteger(noPumpsTextField.getText()) * maxPumpPressure;
        final float pumpInitialIntakePressure = getFloat(pumpInitialPressureTextField.getText());
        final boolean reverse = isReverseCheckBox.isSelected();

        final Service<HotLineResult> calculationService = new Service<>() {
            @Override
            protected Task<HotLineResult> createTask() {
                return new Task<HotLineResult>() {
                    @Override
                    protected HotLineResult call() throws Exception {
                        final var hotline = new HotLine();
                        return hotline.hotLine(
                                iDmm,
                                oDmm,
                                spGr,
                                visAt100F,
                                visAt212F,
                                flowRateM3H,
                                maxTempC,
                                minTempC,
                                tsC,
                                lambdaS,
                                tinIn,
                                h,
                                lambdaC,
                                alphaT,
                                tf1,
                                tf2,
                                maxPumpPressure,
                                maxTotalPressure,
                                pumpInitialIntakePressure,
                                reverse);
                    }

                    @Override
                    public void run() {
                        final var loadingDialog = createProgressAlert((Stage) iDTextField.getScene().getWindow(), this);
                        super.run();
                        loadingDialog.show();
                    }
                };
            }
        };
        calculationService.setOnSucceeded(e -> {
            final var result = calculationService.getValue();
            showTraverse(result.getPressureTraverse(), result.getTemperatureTraverse(), reverse);
            setAnswer(result.getSteps());
        });
    }

    Float getFloat(String value) {
        try {
            return Float.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    Integer getInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    void setAnswer(String answer) {
        answerArea.setText(answer);
    }

    Alert createProgressAlert(Stage owner, Task<?> task) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initOwner(owner);
        alert.titleProperty().bind(task.titleProperty());
        alert.contentTextProperty().bind(task.messageProperty());

        ProgressIndicator pIndicator = new ProgressIndicator();
        pIndicator.progressProperty().bind(task.progressProperty());
        alert.setGraphic(pIndicator);

        alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
        alert.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty().bind(task.runningProperty());

        alert.getDialogPane().cursorProperty().bind(
                Bindings.when(task.runningProperty())
                        .then(Cursor.WAIT)
                        .otherwise(Cursor.DEFAULT)
        );
        return alert;
    }

    private void showTraverse(Point[] pressureTraverse, Point[] temperatureTraverse, boolean reverse) {
        XYChart.Series<Number, Number> series = new XYChart.Series();
        for (var p: pressureTraverse) {
            series.getData().add(new XYChart.Data(p.getX(), p.getY()));
        }
        XYChart.Series<Number, Number> series2 = new XYChart.Series();
        for (var p: temperatureTraverse) {
            series2.getData().add(new XYChart.Data(p.getX(), p.getY()));
        }
        //Defining the x an y axes
        NumberAxis pressureXAxis = new NumberAxis();
        NumberAxis tempXAxis = new NumberAxis();
        if (reverse) {
            final NumberAxis[] axes = {pressureXAxis, tempXAxis};
            for (var axis: axes) {
                axis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axis) {
                    @Override
                    public String toString(Number value) {
                        // note we are printing minus value
                        return String.format("%7.1f", -value.doubleValue());
                    }
                });
            }
        }
        final var first = pressureTraverse[0];
        final var last = pressureTraverse[pressureTraverse.length - 1];
        NumberAxis pressureAxis = new NumberAxis();
        NumberAxis tempAxis = new NumberAxis();
        //Setting labels for the axes
        pressureXAxis.setLabel("L(m)");
        tempXAxis.setLabel("L(m)");
        tempAxis.setLabel("T(°C)");
        pressureAxis.setLabel("P(psi)");
        LineChart<Number, Number> pressureChart = new LineChart<Number, Number>(pressureXAxis, pressureAxis);
        pressureChart.getData().addAll(series);
        LineChart<Number, Number> tempChart = new LineChart<Number, Number>(tempXAxis, tempAxis);
        tempChart.getData().addAll(series2);
        final List<XYChart.Series<Number, Number>> allSeries = List.of(series, series2);
        for (var item: allSeries) {
            for (XYChart.Data<Number, Number> entry : item.getData()) {
                Tooltip t = new Tooltip("(" + String.format("%.2f", Math.abs((float) entry.getXValue())) + " , " + entry.getYValue().toString() + ")");
                t.setShowDelay(new Duration(50));
                Tooltip.install(entry.getNode(), t);
            }
        }
        //Creating a stack pane to hold the chart
        VBox box = new VBox(tempChart, pressureChart);
        box.setPadding(new Insets(15, 15, 15, 15));
        box.setStyle("-fx-background-color: BEIGE");
        //Setting the Scene
        Scene scene = new Scene(box, 595, 650);
        Stage stage = new Stage();
        stage.setTitle("Line Chart");
        stage.setScene(scene);
        stage.show();
    }
}
