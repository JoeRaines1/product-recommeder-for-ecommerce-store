module coding.capstone.product_recommender {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens coding.capstone.product_recommender to javafx.fxml;
    exports coding.capstone.product_recommender;
}