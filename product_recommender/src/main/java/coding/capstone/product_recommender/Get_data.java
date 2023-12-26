package coding.capstone.product_recommender;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class Get_data {
    static Connection connection;

    static {
        /** Establishes connection with the MySQL database */
        try {
            connection = DriverManager.getConnection("jdbc:mysql://hostname/purchasesdb", "username", "password");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    static PreparedStatement stmnt;

    /** @return the list of all categories*/
    public static ObservableList<String> get_all_categories() throws SQLException {
        ObservableList<String> all_categories = FXCollections.observableArrayList();
        Connection conn = connection;
        /** Selects all category codes that are not blank */
        String sql = "Select distinct category_code from purchases where not category_code = \"\" order by category_code;";
        stmnt = conn.prepareStatement(sql);
        ResultSet rs = stmnt.executeQuery();
        while (rs.next()) {
            /** Loops over the results of the SQL query */
            all_categories.add(rs.getString("category_code"));
        }
        return all_categories;
    }

    /** @return the list of similar products
     * @param cart, the items currently in the user's cart
     * @param current_recommendations, the items currently being recommended to the user*/
    public static ObservableList<Product> get_like_prod (ObservableList<Product> cart, ObservableList<Product> current_recommendations) throws SQLException {
        /** Selects the product ID, brand, category code, and price for the nearest neighbors of the current product */
        String sql = "SELECT distinct productID, brand, category_code, price, distance_between FROM purchases " +
                "inner join nearest_neighbors on productID = productID2 where ";
        int x = 0;
        for (Product product : cart) {
            /** Adds the product id of each product in the user's cart to the SQL query */
            if (x == 0) {
                sql += String.format("(productID1 = %s", product.getId());
            } else {
                sql += String.format(" or productID1 = %s", product.getId());
            }
            x++;
        }
        int count_neighbors = 10 - current_recommendations.size();
        /** Determines how many products are currently recommended to ensure that there are only 10 products recommended to the user */
        sql += String.format(") and not category_code =\"\" and not price = 0 order by distance_between limit %s;", count_neighbors);
        /** Establishes a connection with the MySQL server */
        Connection conn = connection;
        stmnt = conn.prepareStatement(sql);
        ResultSet rs = stmnt.executeQuery();
        while (rs.next()) {
            /** Loops over the results of the SQL query and adds the values to the list of recommendations */
            String item_brand = rs.getString("brand");
            String item_cat = rs.getString("category_code");
            double item_price = Double.parseDouble(rs.getString("price"));
            long item_id = Long.parseLong(rs.getString("productID"));
            item_cat = item_cat.substring(item_cat.lastIndexOf(".") + 1);
            Product tempProduct = new Product(item_id, item_brand, item_cat, item_price);
            boolean prod_in_rcmnd = false;
            boolean prod_in_cart = false;
            for (Product product : current_recommendations) {
                /** Determines if a product is already in the current list of recommendations */
                if (item_id == product.getId()) {
                    /** If product is already in the list it is skipped */
                    prod_in_rcmnd = true;
                    break;
                }
            }
            /** Determines if a recommended product is in the current user's cart */
            for (Product cart_prod : cart) {
                /** Does not recommend the product if it is already in the user's cart */
                if (item_id == cart_prod.getId()) {
                    prod_in_cart = true;
                    break;
                }
            }
            /** If the product is not already recommended and is not in the user's cart,
             *  it is added to the list of recommendations */
            if (!prod_in_cart && !prod_in_rcmnd) {
                current_recommendations.add(tempProduct);
            }
        }
        ObservableList<Product> most_purchased = get_most_purchased();
        for (Product product : most_purchased) {
            /** Adds the most frequently purchased items to the list of recommended products if there are less than 10 recommendations */
            if (current_recommendations.size() < 10) {
                current_recommendations.add(product);
            }
            else break;
        }
        return current_recommendations;
    }

    /** @return the top 10 products that are purchased the most */
    public static ObservableList<Product> get_most_purchased() throws SQLException {
        ObservableList<Product> recommendations = FXCollections.observableArrayList();
        Connection conn = connection;
        /** Selects the productID, price, category, and brand of the top 10 most sold product's */
        String sql = "SELECT distinct productID, price, category_code, brand, count(distinct orderID) FROM purchasesdb.purchases " +
                "where not category_code = '' and not price = 0 group by productID, price, category_code, brand order by count(distinct orderID) desc limit 10;";
        stmnt = conn.prepareStatement(sql);
        ResultSet rs = stmnt.executeQuery();
        while (rs.next()) {
            /** Loops over the results of the SQL query and adds the values to the list of top products */
            String item_brand = rs.getString("brand");
            String item_cat = rs.getString("category_code");
            double item_price = Double.parseDouble(rs.getString("price"));
            long item_id = Long.parseLong(rs.getString("productID"));
            item_cat = item_cat.substring(item_cat.lastIndexOf(".") + 1);
            Product tempProduct = new Product(item_id, item_brand, item_cat, item_price);
            recommendations.add(tempProduct);
        }
        return recommendations;
    }

    /** @return the list of products that are in a given category
     * @param category, the category that was selected by the user, all products displayed belong to given category */
    public static ObservableList<Product> get_products_in_category(String category) throws SQLException {
        ObservableList<Product> products_in_category = FXCollections.observableArrayList();
        /** Selects all distinct products, their brand, category code and price that have the matching category code */
        String sql = String.format("Select distinct productID, brand, category_code, price from purchasesdb.purchases" +
                " where category_code = \"%s\" and not price = 0;", category);
        stmnt = connection.prepareStatement(sql);
        ResultSet rs = stmnt.executeQuery();
        while (rs.next()) {
            /** Loops over the results of the SQL query and adds each value to the products in category table */
            String temp_brand = rs.getString("brand");
            String temp_category = rs.getString("category_code");
            temp_category = temp_category.substring(temp_category.lastIndexOf('.') + 1);
            double temp_price = Double.parseDouble(rs.getString("price"));
            long temp_id = Long.parseLong(rs.getString("productID"));
            Product temp_product = new Product(temp_id, temp_brand, temp_category, temp_price);
            products_in_category.add(temp_product);
        }
        return products_in_category;
    }

    /** @return the list of recommended products for the products currently in the user's cart
     *  @param cart, the items currently in the user's cart */
    public static ObservableList<Product> get_recommendations(ObservableList<Product> cart) throws SQLException {
        ObservableList<Product> recommendations = FXCollections.observableArrayList();
        /** Finds the most frequently purchased products with any product in the user's current cart */
        String sql = "SELECT distinct productID, brand, price, category_code, conviction from purchases inner join " +
                "frequently_purchased on consequents = productID where";
        int x = 0;
        for (Product product : cart) {
            /** Adds the product id of each product in the user's cart to the SQL query */
            if (x == 0) {
                sql += String.format("( antecedents = \"%s\"", product.getId());
            }
            else {
                sql += String.format(" or antecedents = \"%s\"", product.getId());
            }
            x++;
        }
        /** Sets the limit depending on cart size to account for possible duplicates in the results */
        int limit = 10 * (cart.size() + 1);
        /** Ignores products with no category code, orders the results by conviction
         *  and shows only the top 10 products */
        sql += String.format(") and not category_code =\"\" order by conviction desc limit %s;", limit);
        Connection conn = connection;
        stmnt = conn.prepareStatement(sql);
        ResultSet rs = stmnt.executeQuery();
        while (rs.next()) {
            /** Loops over the results of the SQL query and adds the values to the list of recommendations */
            String item_brand = rs.getString("brand");
            String item_cat = rs.getString("category_code");
            double item_price = Double.parseDouble(rs.getString("price"));
            long item_id = Long.parseLong(rs.getString("productID"));
            item_cat = item_cat.substring(item_cat.lastIndexOf(".") + 1);
            Product tempProduct = new Product(item_id, item_brand, item_cat, item_price);
            boolean prod_in_rcmnd = false;
            boolean prod_in_cart = false;
            if (recommendations.size() < 10) {
                for (Product product : recommendations) {
                    /** Determines if a product is already in the current list of recommendations */
                    if (item_id == product.getId()) {
                        /** If product is already in the list it is skipped */
                        prod_in_rcmnd = true;
                        break;
                    }
                }
                /** Determines if a recommended product is in the current user's cart */
                for (Product cart_prod : cart) {
                    /** Does not recommend the product if it is already in the user's cart */
                    if (item_id == cart_prod.getId()) {
                        prod_in_cart = true;
                        break;
                    }
                }
                /** If the product is not already recommended and is not in the user's cart,
                 *  it is added to the list of recommendations */
                if (!prod_in_cart && !prod_in_rcmnd) {
                    recommendations.add(tempProduct);
                }
            }
            else break;
        }
        return recommendations;
    }

    /** @return all products that match the user's search results
     * @param searched_text, the user's input into the search bar */
    public static ObservableList<Product> get_search_results(String searched_text) throws SQLException {
        ObservableList<Product> search_results = FXCollections.observableArrayList();
        /** Determines if the user's input was characters or numerical */
        if (searched_text.matches("[^0-9]*")) {
            /** If the user's input was text, the SQL query returns all products with a similar category code or brand,
             * ignores products with no brand or category code */
            String sql = String.format("Select distinct productID, brand, category_code, price from " +
                            "purchasesdb.purchases where (category_code like \"%%%s\" or category_code like \"%s%%\"" +
                            " or brand like \"%%%s\" or brand like \"%s%%\") and not price = 0 and not category_code = '';",
                    searched_text, searched_text, searched_text, searched_text);
            Connection conn = connection;
            stmnt =conn.prepareStatement(sql);
            ResultSet rs = stmnt.executeQuery();
            while (rs.next()) {
                /** Loops over the results of the SQL query and adds each value to the search results table */
                String temp_brand = rs.getString("brand");
                String temp_category = rs.getString("category_code");
                temp_category = temp_category.substring(temp_category.lastIndexOf('.') + 1);
                double temp_price = Double.parseDouble(rs.getString("price"));
                long temp_id = Long.parseLong(rs.getString("productID"));
                Product temp_product = new Product(temp_id, temp_brand, temp_category, temp_price);
                search_results.add(temp_product);
            }
        }
        else if (searched_text.matches("[0-9]*")) {
            /** If the user's input was numerical, the SQL query returns all products with a product ID containing the
             * user's search input */
            String sql = String.format("Select distinct productID, brand, category_code, price " +
                    "from purchasesdb.purchases where (productID like \"%%%s\" or productID like \"%s%%\") " +
                    "and not price = 0 and not category_code = '';", searched_text, searched_text);
            stmnt.execute(sql);
            ResultSet rs = stmnt.getResultSet();
            while (rs.next()) {
                /** Loops over the results of the SQL query and adds each value to the search results table */
                String temp_brand = rs.getString("brand");
                String temp_category = rs.getString("category_code");
                temp_category = temp_category.substring(temp_category.lastIndexOf('.') + 1);
                double temp_price = Double.parseDouble(rs.getString("price"));
                long temp_id = Long.parseLong(rs.getString("productID"));
                Product temp_product = new Product(temp_id, temp_brand, temp_category, temp_price);
                search_results.add(temp_product);
            }
        }
        return search_results;
    }
}
