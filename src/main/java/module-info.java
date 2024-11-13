module com.meltstakecommander {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.jsobject;
    requires org.yaml.snakeyaml;
    requires java.desktop;


    opens com.meltstakecommander to javafx.fxml;
    exports com.meltstakecommander;
}