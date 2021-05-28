module com.flowaap.HotLine {
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires org.jetbrains.annotations;

    exports com.flowapp.HotLine;
    exports com.flowapp.HotLine.Controllers to javafx.fxml;
    opens com.flowapp.HotLine;
    opens com.flowapp.HotLine.Controllers to javafx.fxml;
}