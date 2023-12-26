This program uses data from an ecommerce store to provide the user with recommendations for products they maybe interested in purchasing. The recommendations are determined in Python by using mlxtend and Scikit Learn, mlxtend was used to determine which products frequently occured in the same purchase and then Scikit was used to calculate the conviction between products that were frequently purchased together. The conviction between two prodcuts is found by calculating the chance that one product is in an order given that another product has already been found in the order. The 10 products with the highest conviction are recommended to the user, any items that were already in the user's cart are not recommended. 

Install Guide:
1.	Ensure that the following prerequisites are installed:
•	Java 11, found at https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html. While Java 11.0.2 was used to develop the product, any version of Java 11 SDK should run the program.
•	JavaFX 17, found at https://gluonhq.com/products/javafx/ , to download the correct version, the user must change the drop-down menu to select version 17.0.7 and the download the appropriate SDK depending on their operating system.
•	MySQL 8.0, windows version is found at https://dev.mysql.com/downloads/installer/ , using the developer default has all of the required aspects needed to run the Java product.
•	Microsoft Visual C++ Redistributable, found at https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170 , is also needed to ensure that MySQL can function properly.

2.	Download the Capstone folder from the Dropbox link and unzip to retrieve all files.
3.	Create a MySQL connection, if one does not exist yet, and create a database named “purchasesdb”, this must be the database name or else the SQL statements within the code will not function.
4.	After logging in to the connection with an account the has administrator privileges, within the navigator, open the “Administration” tab and click on “Data Import/Restore”. Then select “Import from Self-Contained File” and navigate to through the directory to the location where the Capstone file is stored, open it and import the 4 .sql files with the names: “purachsesdb_frequently_purchased.sql”. “purachsesdb_nearest_neighbors.sql”, “purachsesdb_prodinorder.sql”, and “purachsesdb_purchases.sql”.
5.	After importing the tables, the user must open the “Get_data” file, located within the Capstone project at, “\Capstone\product_recommender\src\main\java\coding\capstone\product_recommender”, and change the connection statement, at line 13. The user must change the connection statement and make the provided URL leads to the database connection that was established and contains the purchasesdb database. The user must fill in the appropriate hostname, username, and password for the connection URL: “jdbc:mysql//hostname/purchasesdb”, “username”, “password”

6.	To execute the “product_recommender.jar” file the user must find the path to the JavaFX lib file, the last section should look like “\javafx1707\javafx-sdk-17.0.7\lib” and the path to the “product_recommender.jar” file. Then inside of the command prompt the user must type: “java --module-path path\to\javafx\lib --add-modules=javafx.controls,javafx.fxml -jar path\to\jar”. Where path\to\javafx\lib is the path to the javafx lib file and path\to\jar is the path to the location of the “product_recommender.jar” file in the user’s system.

7.	To add one of the recommended products to the cart, the user simply selects the desired product, then clicks the “Add to Cart” button in the bottom right corner, it may take a several seconds for the scene to refresh and display the additional product and to refresh the recommendations bar. Adding a recommended product to the cart can be done using the same method in any scene.

8.	 After a product has been added to the cart the user can remove an item, by selecting the desired item and then clicking the “Remove Item” button. The “Remove Item” button functions the same way in all scenes.

9.	 The “Search Categories” bar allows users to search for specific categories by typing in the bar, this results in the list of the categories being narrowed down to show only categories that match the user’s input.

10.	Selecting one of the categories then clicking the “Show Products in Category” button will load a new scene displaying all the products that belong to the selected category. Any items currently in the user’s cart will also be in the next scene’s cart, the recommended products should also be the same.

11.	In the new scene the user can add recommended products to their cart using the same method as the first scene, or by selecting one of the products that is displayed in the table showing all products in the selected category and then clicking the “Add Product to Cart” button.

12.	The “Search Products” bar functions similarly to the “Search Categories” bar in the main scene. Typing in the bar narrows down the list of products to only show ones whose ID, category or brand contain the user’s input.

13.	The “Home Screen” button returns the user to the home screen. The user’s current cart and recommendations are transferred as well.

14.	From any scene the user can use the “Search” bar at the top center of the screen to search for specific products. The user simply types their input into the bar, clicks the search button and in the next scene all products whose ID, category or brand contain the searched text by the user are displayed in the left-hand table. All buttons in this scene function the same a in the “Products in Category” scene.

The “product_recommendation.py” file generates the data in the “nearest_neighbors” and “frequently_purchased” tables. Because the user is loading the prebuilt data into the database it does not need to be run. If the user wishes to run the code to confirm its functionality that can be done easily.
1.	Ensure Python 3.11 is installed along with the following Python external libraries: Mlxtend, SQLAlchemy, numpy, scikit-learn, pandas, mysql.

2.	Next the user must go into “product_recommendation.py” file and edit two lines. The first line is at line 50 and the user must edit the url in the create_engine functions arguments to allow python to connect to their MySQL connection. The user must change the username, password, hostname, and port to match the values that exist in the existing connection.


3.	The next line that must be changed is at line 170, in this case the user must again change the host, user, and password values to match the values that exist in their MySQL connection containing the purchasesdb database.

4.	Once both lines have been altered the user can double click on the application and it will run automatically. Warning, this will take several hours to run and will not have any visible output to the command line, all output will go directly to the MySQL database and the “nearest_neighbors” and “frequently_purchased” tables. Also running this file will cause duplicates to appear in the “nearest_neighbors” table in the MySQL database, which may affect the execution of the “product_recommendation.jar” file by causing duplicate recommendations to appear.
