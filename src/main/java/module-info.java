module com.meltstakecommander {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.meltstakecommander to javafx.fxml;
    exports com.meltstakecommander;
}