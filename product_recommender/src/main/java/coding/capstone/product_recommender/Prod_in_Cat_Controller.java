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
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class Prod_in_Cat_Controller implements Initializable {
    @FXML
    private TableView<Product> cart = new TableView<>();
    @FXML
    private TableColumn<Product, String> cat_col = new TableColumn<>();
    @FXML
    private TableColumn<Product, String>  brand_col = new TableColumn<>();
    @FXML
    private TableColumn<Product, Long>  id_col = new TableColumn<>();
    @FXML
    private TableColumn<Product, Double>  price_col = new TableColumn<>();
    @FXML
    private TableView<Product> prod_in_cat = new TableView<>();
    @FXML
    private TableColumn<Product, String>  prod_cat = new TableColumn<>();
    @FXML
    private TableColumn<Product, String>  prod_brand = new TableColumn<>();
    @FXML
    private TableColumn<Product, Long>  prod_id = new TableColumn<>();
    @FXML
    private TableColumn<Product, Double>  prod_price = new TableColumn<>();
    @FXML
    private ListView<Product> recommendations = new ListView<>();
    @FXML
    private TextField search_bar = new TextField();
    @FXML
    private TextField search_prod_bar = new TextField();
    private static ObservableList<Product> current_cart = FXCollections.observableArrayList();
    private static String searched_text = null;
    private FilteredList<Product> filteredProducts;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /** Retrieves the selected category from the main scene */
        String selected_cat = MainController.get_Selected_Cat();
        /** Determines if the user searched from the Prod_in_Cat scene, if so the searched text will not be null */
        if (searched_text == null) {
            /** If user searched from the main scene to searched text is retrieved */
            searched_text = MainController.get_Search_text();
        }
        /** Sets the value for each column in the products in category/search results table */
        prod_cat.setCellValueFactory(new PropertyValueFactory<>("category"));
        prod_brand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        prod_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        prod_price.setCellValueFactory(new PropertyValueFactory<>("price"));
        /** Sets the value for each column in the cart table */
        cat_col.setCellValueFactory(new PropertyValueFactory<>("category"));
        brand_col.setCellValueFactory(new PropertyValueFactory<>("brand"));
        id_col.setCellValueFactory(new PropertyValueFactory<>("id"));
        price_col.setCellValueFactory(new PropertyValueFactory<>("price"));
        ObservableList<Product> products = FXCollections.observableArrayList();
        try {
            /** Determines if the user selected a category or searched for a product */
            if (searched_text != null && !searched_text.isEmpty()) {
                /** If the user searched for a product, all products matching the search input are placed into the search
                 * results table */
                products.setAll(Get_data.get_search_results(searched_text));
            }
            else {
                /** If the user selected a category, all products belonging to the selected category are placed into the table */
                products.setAll(Get_data.get_products_in_category(selected_cat));
            }
            /** Retrieves the user's cart from the main scene and populates the cart with the retrieved products */
            cart.setItems(MainController.get_cart());
        } catch (NullPointerException ignored) {} catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            /** Determines if the user's current shopping cart contains any products */
            if (Prod_in_Cat_Controller.getCurrent_cart().isEmpty()) {
                /** If there are no products currently in the user's cart, the cart is populated with the top 10 most
                 *  purchased products */
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
        filteredProducts = new FilteredList<>(products, p -> true);
        prod_in_cat.setItems(filteredProducts);
        search_prod_bar.textProperty().addListener((observableValue, oldvalue, newvalue) -> {
            filteredProducts.setPredicate(product -> {
                /** Shows all categories if the search bar is blank */
                if (newvalue == null || newvalue.isEmpty()) {
                    return true;
                }
                /** Determines if the user's input text into the search products bar is characters or numerical */
                else if (newvalue.matches("[^0-9]*")) {
                    /** If the user's input was an alphabetic value, all products whose brand or category code contain the searched
                     *  value are returned */
                    if (product.getBrand().contains(newvalue.toLowerCase()) || product.getCategory().contains(newvalue.toLowerCase())) {
                        return true;
                    }
                    else return false;
                }
                /** If the user's input was a numeric value, all products whose product ID contains the searched value are returned */
                else if (newvalue.matches("[0-9]*") && String.valueOf(product.getId()).contains(newvalue) ) {
                    return true;
                }
                else return false;
            });
        });
    }

    /** @return the user's current cart */
    public static ObservableList<Product> getCurrent_cart() {
        return current_cart;
    }

    @FXML
    protected void add_prod() throws SQLException {
        Product selected_prod = prod_in_cat.getSelectionModel().getSelectedItem();
        /** Ignores the button press if no product is selected in the products table */
        if (selected_prod != null) {
            /** Finds the currently selected product in the products in category table and adds the product to the user's cart */
            cart.getItems().add(selected_prod);
            /** Recalculates the recommended products based on the new cart */
            recommendations.setItems(Get_data.get_recommendations(cart.getItems()));
            if (recommendations.getItems().size() < 10) {
                /** If there are less than 10 recommendations, products similar to ones in the user's cart are recommended
                 * so that there are always 10 recommended products provided to the user */
                recommendations.setItems(Get_data.get_like_prod(cart.getItems(), recommendations.getItems()));
            }
        }
    }

    @FXML
    protected void add_prod_from_rcmnd() throws SQLException {
        try {
            Product selected_item = recommendations.getSelectionModel().getSelectedItem();
            /** Ignores the button press if no product is selected in the list of recommended products */
            if (selected_item != null) {
                /** Adds the selected recommendation to the user's cart */
                cart.getItems().add(selected_item);
                /** Recalculates the recommended products based on the new cart */
                recommendations.setItems(Get_data.get_recommendations(cart.getItems()));
                if (recommendations.getItems().size() < 10) {
                    /** If there are less than 10 recommendations, products similar to ones in the user's cart are recommended
                     * so that there are always 10 recommended products provided to the user */
                    recommendations.setItems(Get_data.get_like_prod(cart.getItems(), recommendations.getItems()));
                }
            }
        } catch (NullPointerException ignored) {} catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    protected void home(ActionEvent actionEvent) throws IOException, SQLException {
        /** Retrieves the products currently in the user's cart */
        current_cart = cart.getItems();
        /** Sets the searched text to null, used for determining which scene the user searched from */
        searched_text = null;
        /** Loads the main scene */
        Parent root = FXMLLoader.load(getClass().getResource("main_view.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setTitle("Main View");
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    @FXML
    protected void remove_from_cart() throws SQLException {
        Product selected_item = cart.getSelectionModel().getSelectedItem();
        /** Ignores the button press if no product is selected in the user's cart */
        if (selected_item != null) {
            /** Removes the selected product from the user's cart */
            cart.getItems().remove(selected_item);
            /** Determines if there is at least one product in the user's cart */
            if (cart.getItems().size() > 0) {
                /** Recalculates the recommended products based on the new cart */
                recommendations.setItems(Get_data.get_recommendations(cart.getItems()));
                if (recommendations.getItems().size() < 10) {
                    /** If there are less than 10 recommendations, products similar to ones in the user's cart are recommended
                     * so that there are always 10 recommended products provided to the user */
                    recommendations.setItems(Get_data.get_like_prod(cart.getItems(), recommendations.getItems()));
                }
            } else {
                /** If there are no items currently in the user's cart, the list of recommendations is filed with the
                 * top 10 most purchased products */
                recommendations.getItems().setAll(Get_data.get_most_purchased());
            }
        }
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
    protected void search_btn_clck(ActionEvent actionEvent) throws SQLException, IOException {
        /** Ignores the search button press if there is no text in the search bar */
        if (!search_bar.getText().isBlank() && !search_bar.getText().isEmpty()) {
            current_cart = cart.getItems();
            searched_text = search_bar.getText();
            /** Loads the products in category scene */
            Parent root = FXMLLoader.load(getClass().getResource("prod_in_category.fxml"));
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setTitle("Search Results");
            stage.setScene(new Scene(root, 850, 600));
            stage.show();
            /** Sets the products in the search results/products in category table to all products matching the search bar input */
            //filteredProducts.setAll(Get_data.get_search_results(search_bar.getText()));
            //prod_in_cat.setItems(Get_data.get_search_results(search_bar.getText()));
        }
    }

    @FXML
    protected void search_prod_click() {
        try {
            /** If the default text is displayed in the products search bar, clicking the search bar makes the bar empty*/
            if (search_prod_bar.getText().equals("Search Products")) {
                search_prod_bar.setText(null);
            }
        } catch (NullPointerException ignored) {}
    }
}