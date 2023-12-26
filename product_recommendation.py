from mlxtend.frequent_patterns import fpgrowth
from mlxtend.frequent_patterns import association_rules
from mlxtend.preprocessing import TransactionEncoder
from sklearn.neighbors import NearestNeighbors
from sqlalchemy import create_engine
import numpy as np
import mysql.connector
import pandas as pd

uncalculated_categories = []

# Finds products that are frequently purchased together
def find_frequent_patterns():
    # Finds all products that are on in order with atleast one other product and also has a price and category code
    sql = '''Select Distinct purchasesdb.purchases.orderID, purchasesdb.purchases.productID
    from purchasesdb.prodinorder inner join purchasesdb.purchases
    on purchasesdb.prodinorder.OrderID = purchasesdb.purchases.orderID where ProdPerOrder > 1
    and not purchases.price = 0 and not purchases.category_code = '';'''
    order_cursor.execute(sql)
    # Retrieves the results from the SQL query
    purchase_data = order_cursor.fetchall()
    # Creates an empty list to store the orders and their products
    prod_by_order = []
    # Rearranges the purchase data into a list of order where each order contains the products purchased in the order
    for product in purchase_data:
        # Retrieves the previous product in the list of products
        prev_product = purchase_data[purchase_data.index(product)-1]
        # Checks if the previous product in the list was on the same order as the current product
        if product[0] == prev_product[0]:
            # If products are in the same order, the current product is added to the list containing the previous product
            prod_by_order[-1].append(product[1])
        else:
            # If the products are not on the same order, a list is made for the new order's products
            prod_by_order.append([product[1]])
    
    # Transforms the data into the needed format to perform the frequently purchased analysis
    te = TransactionEncoder()
    te_ary = te.fit(prod_by_order).transform(prod_by_order)
    df = pd.DataFrame(te_ary, columns=te.columns_)
    # Sets restrictions on what product pairs are returned, must occur in at least .01% of the orders
    # and must have a high correlation between the two products, measured by conviction
    freq_purchases = fpgrowth(df, min_support = .0001, use_colnames = True)
    rules = association_rules(freq_purchases, metric= 'conviction', min_threshold = 1.25)
    rules['antecedents']= rules['antecedents'].apply(lambda x:list(x)[0]).astype("unicode")
    rules['consequents']= rules['consequents'].apply(lambda x:list(x)[0]).astype("unicode")
    # Sorts the retured pairs of products based on their conviction level, the higher the conviction the earlier it is listed
    rules = rules.sort_values(by=['conviction'], ascending=False)
    rules = rules.replace([np.inf, -np.inf], np.nan)
    rules = rules.fillna('inf')
    engine = create_engine(url= "mysql+pymysql://username:password@hostname:port/purchasesdb")
    rules.to_sql(con= engine, name= 'frequently_purchased', if_exists= 'replace')

# Finds the nearest neighboring points for each point in the list
def find_neighbors(plt_points, all_prod_in_ctgry):
    # Determines if there are at least 11 products within a given category
    if len(plt_points) > 10:
        # Finds the 11 nearest neighbors to each product, n is set to 11 to find the nearest 10
        # because the nearest neighbor to a point will always be it self
        neighbors = NearestNeighbors(n_neighbors=11, algorithm='auto').fit(plt_points)
        for point in plt_points:
            # Stores the nearest 10 points and the distance to them
            dist, ind = neighbors.kneighbors(np.array(point).reshape(1, -1))
            # The base product id that all neighbors are related to is always the first index returned in the list
            base_prod = all_prod_in_ctgry[ind[0][0]][0]
            # Determines if the nearest neighbors of the current product have been calculated
            for index in range(len(ind[0])):
                # Ignores the first point in the list
                if index != 0:
                    # Inserts the two product ID's and the distance between them into the nearest neighbors table
                    sql = f'''Insert into purchasesdb.nearest_neighbors values({base_prod}, {all_prod_in_ctgry[ind[0][index]][0]}, {dist[0][index]});'''
                    order_cursor.execute(sql)
                    # Commits the insert statement 
                    mydb.commit()

    else:
        # Finds the category_code for the base product in the current category
        sql = f'''Select distinct purchasesdb.purchases.category_code from purchasesdb.purchases 
        where purchasesdb.purchases.productID = {all_prod_in_ctgry[0][0]};'''
        order_cursor.execute(sql)
        category = order_cursor.fetchall()[0][0].rsplit('.', 1)[0]
        if category not in uncalculated_categories:
            uncalculated_categories.append(category)
        neighbors = NearestNeighbors(n_neighbors=len(plt_points), algorithm='auto').fit(plt_points)
        for point in plt_points:
            # Stores the nearest 10 points and the distance to them
            dist, ind = neighbors.kneighbors(np.array(point).reshape(1, -1))
            # The base product id that all neighbors are related to is always the first index returned in the list
            base_prod = all_prod_in_ctgry[ind[0][0]][0]
            # Determines if the nearest neighbors of the current product have been calculated
            for index in range(len(ind[0])):
                # Ignores the first point in the list
                if index != 0:
                    # Inserts the two product ID's and the distance between them into the nearest neighbors table
                    sql = f'''Insert into purchasesdb.nearest_neighbors 
                    values({base_prod}, {all_prod_in_ctgry[ind[0][index]][0]}, {dist[0][index]});'''
                    order_cursor.execute(sql)
                    # Commits the insert statement 
                    mydb.commit()

def retrieve_points():
    # Lists the name of every category, excludes the category's with no name
    order_cursor.execute('''SELECT distinct category_code FROM purchasesdb.purchases where not category_code = '' 
                        order by category_code;''')
    categories = order_cursor.fetchall()
    # Loops over the list of all categories
    for category in categories:
        points = []
        # Finds the price and the total number sold for each product in the given category
        sql = f'''Select distinct purchasesdb.purchases.productID, purchasesdb.purchases.price, 
        count(purchasesdb.purchases.orderID) from purchasesdb.purchases 
        where purchasesdb.purchases.category_code = '{category[0]}' and not purchasesdb.purchases.price = 0 
        group by purchasesdb.purchases.productID, purchasesdb.purchases.price;'''
        order_cursor.execute(sql)
        # Retrieves the results of the SQL query
        prods_in_ctgry = order_cursor.fetchall()
        # Loops over each product in a given category
        for product in prods_in_ctgry:
            # Creates a plot point for the k nearest neighbor algorithm based on price and total sales
            points.append([product[1], product[2]])
        # Restructures the list of points into an array so it can be used in the nearest neighbor function
        points = np.array(points)
        # Finds the neares neighbors for the given products and category
        find_neighbors(points, prods_in_ctgry)
    
    # Loops over each parent category that contained a child category with less than 11 products
    for category in uncalculated_categories:
        points = []
        # Finds every product in a given parent category
        sql = f'''Select distinct purchasesdb.purchases.productID, purchasesdb.purchases.price, 
        count(purchasesdb.purchases.orderID) from purchasesdb.purchases 
        where purchasesdb.purchases.category_code like '{category}%' and not purchasesdb.purchases.price = 0 
        group by purchasesdb.purchases.productID, purchasesdb.purchases.price;'''
        order_cursor.execute(sql)
        # Retrieves the results of the SQL query
        uncalculated_products = order_cursor.fetchall()
        # Loops over every product in the parent category
        for product in uncalculated_products:
            # Creates a plot point for the k nearest neighbor algorithm based on price and total sales
            points.append([product[1], product[2]])
        points = np.array(points)
        # Determines if there are at least 11 products within a given parent category
        if len(points) > 10:
            # Finds the 11 nearest neighbors to each product, n is set to 11 to find the nearest 10
            # because the nearest neighbor to a point will always be it self
            neighbors = NearestNeighbors(n_neighbors=11, algorithm='auto').fit(points)
            for point in points:
                # Stores the nearest 10 points and the distance to them
                dist, ind = neighbors.kneighbors(np.array(point).reshape(1, -1))
                # The base product id that all neighbors are related to is always the first index returned in the list
                base_prod = uncalculated_products[ind[0][0]][0]
                # Finds the nearest neighbors of the base product, if any
                sql = f'''Select * from purchasesdb.nearest_neighbors where productID1 = {base_prod};'''
                order_cursor.execute(sql)
                # Retrieves the results of the SQL statement
                current_neighbors = order_cursor.fetchall()
                # Determines if the nearest neighbors of the current product have been calculated
                # If the neares neighbors have been calculated the current product is skipped
                if len(current_neighbors) == 0:
                    for index in range(len(ind[0])):
                        # Ignores the first point in the list
                        if index != 0:
                            # Inserts the two product ID's and the distance between them into the nearest neighbors table
                            sql = f'''Insert into purchasesdb.nearest_neighbors 
                            values({base_prod}, {uncalculated_products[ind[0][index]][0]}, {dist[0][index]});'''
                            order_cursor.execute(sql)
                            # Commits the insert statement 
                            mydb.commit()

# Connects the current python file to the MySQL database
mydb = mysql.connector.connect(host="hostname", user="username", password="password")
order_cursor = mydb.cursor(buffered = True)
find_frequent_patterns()
retrieve_points()