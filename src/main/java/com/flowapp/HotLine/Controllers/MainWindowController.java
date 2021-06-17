package com.flowapp.HotLine.Controllers;

import com.flowapp.HotLine.HotLine;
import com.flowapp.HotLine.Models.HotLineResult;
import com.flowapp.HotLine.Models.Point;
import com.flowapp.HotLine.Models.PressureTraverse;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.regex.Pattern;

public class MainWindowController implements Initializable {

    @FXML
    private TextField iDTextField;

    @FXML
    private TextField oDTextField;

    @FXML
    private CheckBox isReverseCheckBox;

    @FXML
    private CheckBox isSimplifiedOnlyCheckBox;

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
    private TextField firstDtAssumptionTextBox;

    @FXML
    private TextField dTAllowedErrorTextBox;

    @FXML
    private TextArea answerArea;

    @FXML
    private Button calculateBtn;

    @FXML
    private ImageView facebookIcon;

    @FXML
    private ImageView linkedInIcon;

    @FXML
    private ImageView emailIcon;

    private Stage chartsWindow;

    private final Application application;

    public MainWindowController(Application application) {
        this.application = application;
    }

    Stage getStage() {
        return (Stage) iDTextField.getScene().getWindow();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        final TextField[] textFields = {
            iDTextField,oDTextField,spGrTextField,vis100TextField,
                vis212TextField,flowRateTextField,maxTempTextField,
                minTempTextField,tsTextField,lambdaSTextField,tinTextField,
                hTextField,lambdaCTextField,alphaCTextField,tf1TextField,
                tf2TextField,maxPumpPressureTextField,noPumpsTextField, firstDtAssumptionTextBox,dTAllowedErrorTextBox,
                pumpInitialPressureTextField
        };
        for (var field: textFields) {
            field.setTextFormatter(createDecimalFormatter());
        }
        var packagePath = getClass().getPackageName().split("\\.");
        packagePath[packagePath.length-1] = "Fonts";
        String fontPath = Arrays.stream(packagePath).reduce("", (s, s2) -> s + "/" + s2);
        Font font = Font.loadFont(getClass().getResourceAsStream(fontPath + "/FiraCode-Retina.ttf"), answerArea.getFont().getSize());
        answerArea.setFont(font);
        calculateBtn.setOnAction(e -> {
            try {
                calculate();
            } catch (Exception ex) {
                ex.printStackTrace();
                final var errorDialog = createErrorDialog(getStage(), ex);
                errorDialog.show();
            }
        });
        setUpIcons();
    }

    private void setUpIcons() {
        var packagePath = getClass().getPackageName().split("\\.");
        packagePath[packagePath.length-1] = "Images";
        String fontPath = Arrays.stream(packagePath).reduce("", (s, s2) -> s + "/" + s2);
        final var facebookImage = getClass().getResource(fontPath + "/facebook.png");
        final var linkedInImage = getClass().getResource(fontPath + "/linkedin.png");
        final var emailImage = getClass().getResource(fontPath + "/email.png");
        facebookIcon.setImage(new Image(Objects.requireNonNull(facebookImage).toString()));
        linkedInIcon.setImage(new Image(Objects.requireNonNull(linkedInImage).toString()));
        emailIcon.setImage(new Image(Objects.requireNonNull(emailImage).toString()));
        facebookIcon.setPickOnBounds(true);
        linkedInIcon.setPickOnBounds(true);
        emailIcon.setPickOnBounds(true);
        facebookIcon.setOnMouseClicked(e -> {
            openBrowser("https://www.facebook.com/Moustafa.essam.hpp");
        });
        linkedInIcon.setOnMouseClicked(e -> {
            openBrowser("https://www.linkedin.com/in/moustafa-essam-726262174");
        });
        emailIcon.setOnMouseClicked(e -> {
            final var email = "mailto:essam.moustafa15@gmail.com";
            openBrowser(email);
            copyToClipboard(email);
        });
    }

    void openBrowser(String url) {
        application.getHostServices().showDocument(url);
    }

    private void copyToClipboard(String answer) {
        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, answer));
    }

    private final Pattern numbersExpr = Pattern.compile("[-]?[\\d]*[.]?[\\d]*");
    TextFormatter<?> createDecimalFormatter() {
        final var pattern = numbersExpr.pattern();
        return new TextFormatter<>(c -> {
            if (c.getControlNewText().isEmpty()) { return c; }
            final var isGood = c.getControlNewText().matches(pattern);
            if (isGood) { return c; }
            else { return null; }
        });
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
        final float firstDtAssumption = getFloat(firstDtAssumptionTextBox.getText());
        final float dTAllowedError = getFloat(dTAllowedErrorTextBox.getText());
        final float maxPumpPressure = getFloat(maxPumpPressureTextField.getText());
        final Integer pumpsNum = getInteger(noPumpsTextField.getText());
        final Float maxTotalPressure = pumpsNum == null ? null : pumpsNum * maxPumpPressure;
        final float pumpInitialIntakePressure = getFloat(pumpInitialPressureTextField.getText());
        final boolean reverse = isReverseCheckBox.isSelected();
        final boolean simplifiedOnly = isSimplifiedOnlyCheckBox.isSelected();

        final var task = new Task<HotLineResult>() {
            Alert loadingDialog;

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
                        tf2,firstDtAssumption,dTAllowedError,
                        maxPumpPressure,
                        maxTotalPressure,
                        pumpInitialIntakePressure,
                        reverse, simplifiedOnly);
            }

            @Override
            public void run() {
                loadingDialog = createProgressAlert((Stage) iDTextField.getScene().getWindow(), this);
                super.run();
                loadingDialog.show();
            }

            protected void closeDialog() {
                if (loadingDialog != null) {
                    loadingDialog.close();
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                closeDialog();
            }

            @Override
            protected void failed() {
                super.failed();
                closeDialog();
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                closeDialog();
            }
        };
        task.setOnSucceeded(e -> {
            final var result = task.getValue();
            showTraverse(result.getPressureTraverse(), result.getTemperatureTraverse(), reverse);
            setAnswer(result.getSteps());
        });
        task.setOnFailed(e -> {
            final var error = e.getSource().getException();
            final var errorDialog = createErrorDialog(getStage(), error);
            errorDialog.show();
            setAnswer(error.getMessage());
        });
        task.run();
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

    Alert createErrorDialog(Stage owner, Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Error");
        alert.setContentText(e.getMessage());
        return alert;
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

    private void showTraverse(PressureTraverse pressureTraverse, Point[] temperatureTraverse, boolean reverse) {
        final String workingLineTitle = "Work Line";
        XYChart.Series<Number, Number> pressureSeries = new XYChart.Series();
        pressureSeries.setName("Pressure Plot");
        for (var p: pressureTraverse.getPressureTraverse()) {
            pressureSeries.getData().add(new XYChart.Data(p.getX(), p.getY()));
        }
        List<XYChart.Series<Number, Number>> pressureWorkingLinesSeries = new ArrayList<>();
        for (var p: pressureTraverse.getWorkLines()) {
            XYChart.Series<Number, Number> workingLine = new XYChart.Series();
            workingLine.getData().add(new XYChart.Data(p.getFirst().getX(), p.getFirst().getY()));
            workingLine.getData().add(new XYChart.Data(p.getSecond().getX(), p.getSecond().getY()));
            workingLine.setName(workingLineTitle);
            pressureWorkingLinesSeries.add(workingLine);
        }

        XYChart.Series<Number, Number> tempSeries = new XYChart.Series();
        tempSeries.setName("Temp Plot");
        for (var p: temperatureTraverse) {
            tempSeries.getData().add(new XYChart.Data(p.getX(), p.getY()));
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
        NumberAxis pressureAxis = new NumberAxis();
        NumberAxis tempAxis = new NumberAxis();
        //Setting labels for the axes
        pressureXAxis.setLabel("L(m)");
        tempXAxis.setLabel("L(m)");
        tempAxis.setLabel("T(°C)");
        pressureAxis.setLabel("P(psi)");
        LineChart<Number, Number> pressureChart = new LineChart<Number, Number>(pressureXAxis, pressureAxis);
        pressureChart.getData().addAll(pressureWorkingLinesSeries);
        for (var series: pressureChart.getData()) {
            series.getNode().setStyle("-fx-stroke-dash-array: 2 8 8 2; -fx-stroke: #00ff00");
        }
        pressureChart.getData().add(pressureSeries);
        LineChart<Number, Number> tempChart = new LineChart<Number, Number>(tempXAxis, tempAxis);
        tempChart.getData().add(tempSeries);
        final List<XYChart.Series<Number, Number>> allSeries = new ArrayList<>(List.of(pressureSeries, tempSeries));
        allSeries.addAll(pressureWorkingLinesSeries);
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
        var x = getStage().getX() + 200;
        var y = getStage().getY();
        if (chartsWindow != null) {
            x = chartsWindow.getX();
            y = chartsWindow.getY();
            chartsWindow.close();
        }
        chartsWindow = new Stage();
        chartsWindow.initOwner(getStage());
        chartsWindow.setX(x);
        chartsWindow.setY(y);
        chartsWindow.setTitle("Line Chart");
        chartsWindow.setScene(scene);
        chartsWindow.show();
    }
}
