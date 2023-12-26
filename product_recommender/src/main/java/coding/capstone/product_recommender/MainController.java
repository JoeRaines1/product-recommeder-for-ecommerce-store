package coding.capstone.product_recommender;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    private TextField search_bar;
    @FXML
    private TextField search_ctgrs_bar;
    @FXML
    private TableView<Product> cart = new TableView<>();
    @FXML
    private TableColumn<Product, String> cat_col = new TableColumn<>();
    @FXML
    private TableColumn<Product, String> brand_col = new TableColumn<>();
    @FXML
    private TableColumn<Product, Long> id_col = new TableColumn<>();
    @FXML
    private TableColumn<Product, Double> price_col = new TableColumn<>();
    @FXML
    private ListView<String> categories = new ListView<>();
    @FXML
    private ListView<Product> recommendations = new ListView<>();
    private static String search_text;
    private static String selected_cat;
    private ObservableList<String> all_categories = FXCollections.observableArrayList();
    private static ObservableList<Product> temp_cart = FXCollections.observableArrayList();

    public void initialize(URL location, ResourceBundle resources) {
        search_text = null;
        /** Sets the value for each column in the cart table */
        cat_col.setCellValueFactory(new PropertyValueFactory<Product, String>("category"));
        brand_col.setCellValueFactory(new PropertyValueFactory<Product, String>("brand"));
        id_col.setCellValueFactory(new PropertyValueFactory<Product, Long>("id"));
        price_col.setCellValueFactory(new PropertyValueFactory<Product, Double>("price"));
        /** Retrieves the user's current shopping cart from the product's in category scene */
        cart.setItems(Prod_in_Cat_Controller.getCurrent_cart());
        try {
            all_categories.setAll(Get_data.get_all_categories());
            if (Prod_in_Cat_Controller.getCurrent_cart().isEmpty()) {
                /** If there are no items currently in the user's cart, the list of recommendations is filed with the
                 * top 10 most purchased products */
                recommendations.getItems().setAll(Get_data.get_most_purchased());
            }
            else {
                /** Finds recommendations for the items currently in the user's cart */
                recommendations.setItems(Get_data.get_recommendations(cart.getItems()));
                if (recommendations.getItems().size() < 10) {
                    /** If there are less than 10 recommendations, products similar to ones in the user's cart are recommended
                     * so that there are always 10 recommended products provided to the user */
                    recommendations.setItems(Get_data.get_like_prod(cart.getItems(), recommendations.getItems()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        /** Creates a filterable list for the list of all categories, so it can be searched */
        FilteredList<String> filteredCtgrs = new FilteredList<>(all_categories, p -> true);
        categories.setItems(filteredCtgrs);
        search_ctgrs_bar.textProperty().addListener((observableValue, oldvalue, newvalue) -> {
            filteredCtgrs.setPredicate(cat -> {
                /** Shows all categories if the search bar is blank */
                if (newvalue == null || newvalue.isEmpty()) {
                    return true;
                }
                /** Shows all categories that contain the searched text */
                else return newvalue.matches(".*") && cat.contains(newvalue.toLowerCase());
            });
        });
    }

    /** @return the list of items currently in the user's cart so it can be transferred to the next scene */
    public static ObservableList<Product> get_cart() {
        return temp_cart;
    }

    /** @return the text that is in the search bar when the search button is clicked */
    public static String get_Search_text() {
        return search_text;
    }

    /** @return the selected category in the list of all categories when the show products in category button is clicked */
    public static String get_Selected_Cat() {
        return selected_cat;
    }

    @FXML
    protected void add_btn_clck() throws SQLException {
        Product selected_item = recommendations.getSelectionModel().getSelectedItem();
        /** Ignores the button press if no recommended product is selected */
        if (selected_item != null) {
            /** Retrieves the information from the selected product in the recommendation list
             *  and adds the product to the user's cart*/
            cart.getItems().add(selected_item);
            /** Recalculates the recommended products based on the new cart */
            recommendations.setItems(Get_data.get_recommendations(cart.getItems()));
            /** If there are less than 10 recommendations, products similar to ones in the user's cart are recommended
             * so that there are always 10 recommended products provided to the user */
            if (recommendations.getItems().size() < 10) {
                recommendations.setItems(Get_data.get_like_prod(cart.getItems(), recommendations.getItems()));}
        }
    }

    @FXML
    protected void prod_in_cat_btn_clck(ActionEvent actionEvent) throws IOException {
        try {
            /** Stores the current cart, so it can be retrieved in other scenes */
            temp_cart = cart.getItems();
            /** Finds the currently selected category in the list of categories,
             * ignores the input if no category is selected */
            selected_cat = categories.getSelectionModel().getSelectedItem();
            if (!selected_cat.isEmpty()) {
                /** Loads the products in category scene */
                Parent root = FXMLLoader.load(getClass().getResource("prod_in_category.fxml"));
                Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                stage.setTitle("Products In Category");
                stage.setScene(new Scene(root, 850, 600));
                stage.show();
            }
        } catch(NullPointerException ignored){}
    }

    @FXML
    protected void remove_item_btn() throws SQLException {
        /** Removes the selected product from the user's cart */
        cart.getItems().remove(cart.getSelectionModel().getSelectedItem());
        /** Determines if there is at least one product in the user's cart */
        if (cart.getItems().size() > 0) {
            /** Recalculates the recommended products based on the new cart */
            recommendations.setItems(Get_data.get_recommendations(cart.getItems()));
            if (recommendations.getItems().size() < 10) {
                /** If there are less than 10 recommendations, products similar to ones in the user's cart are recommended
                 * so that there are always 10 recommended products provided to the user */
                recommendations.setItems(Get_data.get_like_prod(cart.getItems(), recommendations.getItems()));
            }
        }
        /** If there are no items currently in the user's cart, the list of recommendations is filed with the
         * top 10 most purchased products */
        else {recommendations.getItems().setAll(Get_data.get_most_purchased());}
    }

    @FXML
    protected void search_bar_click() {
        try {
            /** If the default text is displayed in the search bar, clicking the bar makes it empty */
            if (search_bar.getText().equals("Search")) {
                search_bar.setText(null);
            }
        } catch (NullPointerException ignored) {}
    }

    @FXML
    protected void search_btn_click(ActionEvent actionEvent) throws IOException {
        try {
            /** Ignores the search button press if the search bar is empty */
            if (!search_bar.getText().isBlank() && !search_bar.getText().isEmpty()) {
                /** Saves the user's searched text and cart to be retrieved from other scenes */
                search_text = search_bar.getText();
                temp_cart = cart.getItems();
                /** Loads the products in category scene */
                Parent root = FXMLLoader.load(getClass().getResource("prod_in_category.fxml"));
                Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                stage.setTitle("Search Results");
                stage.setScene(new Scene(root, 850, 600));
                stage.show();
            }
        } catch (NullPointerException ignored) {}
    }

    @FXML
    protected void search_cat_clck() {
        try {
            /** If the default text is displayed in the categories search bar, clicking the search bar makes the bar empty*/
            if (search_ctgrs_bar.getText().equals("Search Categories")) {
                search_ctgrs_bar.setText(null);
            }
        } catch (NullPointerException ignored) {}
    }
}