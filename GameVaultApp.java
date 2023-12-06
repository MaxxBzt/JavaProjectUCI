import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.List;
import javax.swing.border.EmptyBorder;

public class GameVaultApp {
    private static final Vector<String> columnNames = new Vector<>(List.of(new String[]{"Id", "Title", "Price", "Release Date", "Exclusive", "Studio", "Rating", "Platforms", "Stock"}));
    private static DefaultTableModel tableModel;
    private static JTable displayTable;
    private static Vector<Vector<String>> listData;
    private static String sort;
    private static String filter;
    private static String order;
    private static final String DB_URL = "jdbc:mariadb://localhost:3316/gamevault_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234";

    public static void main(String[] args) {
        createTableIfNotExists();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Gamevault Management System");
            frame.setSize(400, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel panel = new JPanel();
            frame.add(panel);
            placeComponents(panel);

            frame.setVisible(true);
        });
    }

    private static void placeComponents(JPanel panel) {
        panel.setLayout(new BorderLayout());

        // Define pale colors
        Color paleBackground = new Color(230, 230, 240);
        Color paleForeground = new Color(60, 60, 60);

        // top toolbar

        JPanel topPanel = new JPanel(new GridLayout(1,2));

        // Add a game button style
        JButton addGame = new JButton("Add a game");
        addGame.setBackground(paleBackground);
        addGame.setForeground(paleForeground);
        addGame.setFocusPainted(false);
        topPanel.add(addGame);

        // Delete a game button style
        JButton deleteGame = new JButton("Delete game");
        deleteGame.setBackground(paleBackground);
        deleteGame.setForeground(Color.RED);
        topPanel.add(deleteGame);

        panel.add(topPanel, BorderLayout.NORTH);

        // Game Table
        displayTable = new JTable();
        tableModel = new DefaultTableModel();
        JScrollPane scrollPane = new JScrollPane(displayTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom toolbar
        JPanel bottomPanel = new JPanel(new GridLayout(1, 5));

        // Search text field
        JLabel searchLabel = new JLabel("Search: ");
        JTextField searchTextField = new JTextField();
        searchTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = searchTextField.getText();
                if(title == null || title.equals("")) {
                    displayGamesList();
                }
                else {
                    filter = title;
                    displayGamesList(GameManagement.FILTER_BY);
                }
            }
        });

        // Sorting
        JLabel sortLabel = new JLabel("Sort by: ");
        String[] orderItems = new String[] {"ascending", "descending"};
        JComboBox<String> orderComboBox = new JComboBox<>(orderItems);

        orderComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(Objects.equals(orderComboBox.getSelectedItem(), "descending")){
                    order = "descending";
                }
                else {
                    order = "ascending";
                }
                displayGamesList(GameManagement.FILTER_AND_ORDER_BY);
            }
        });

        String[] sortItems = new String[] {"ID", "Title", "Price", "Release date", "Exclusivity", "Studio", "Rating", "Stock"};
        JComboBox<String> sortComboBox = new JComboBox<>(sortItems);
        sortComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = 0;
                while(index< sortItems.length && !Objects.requireNonNull(sortComboBox.getSelectedItem()).toString().equals(sortItems[index])) {
                    index++;
                }
                sort = (new String[] {"game_id", "game_title", "price", "release_date", "exclusive", "studio_name", "rating", "remaining_stock"})[index];
                order = orderComboBox.getSelectedItem() == null ? "ascending" : orderComboBox.getSelectedItem().toString();
                displayGamesList(GameManagement.FILTER_AND_ORDER_BY);
            }
        });

        bottomPanel.add(searchLabel);
        bottomPanel.add(searchTextField);
        bottomPanel.add(sortLabel);
        bottomPanel.add(sortComboBox);
        bottomPanel.add(orderComboBox);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        addGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addGame();
                displayGamesList();
            }
        });

        deleteGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = displayTable.getSelectedRow();
                if (selectedIndex !=-1){
                    deleteGame(selectedIndex);
                }
            }
        });

        displayTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int selectedIndex = displayTable.getSelectedRow();

                    if (selectedIndex != -1) {
                        openNewFrame(selectedIndex);
                    }
                }
            }
        });

        displayGamesList();
    }

    private static void deleteGame(int selectedIndex) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            int gameId = Integer.parseInt(listData.elementAt(selectedIndex).elementAt(0));
            String gameTitle = listData.elementAt(selectedIndex).elementAt(1);
            // ask the user if they want to delete the game or not
            int confirmDialog = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete : "+ gameTitle);
            // if the user doesn't confirm to delete, return
            if (confirmDialog != JOptionPane.YES_OPTION) {
                return;
            }
            // delete the game
            String sql = "DELETE FROM game WHERE game_id = ?";
            try (PreparedStatement preparedStatement1 = connection.prepareStatement(sql)) {
                preparedStatement1.setInt(1, gameId);
                int affectedRows = preparedStatement1.executeUpdate();
                if(affectedRows == 1) {
                    JOptionPane.showMessageDialog(null, "Game deleted successfully.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                else{
                    JOptionPane.showMessageDialog(null, "Game doesn't exist.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "SQL Error while deleting game :" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void openNewFrame(int selectedGameIndex) {
        int gameId = Integer.parseInt(listData.get(selectedGameIndex).get(0));
        JFrame gameDetails = new JFrame("Game Details");
        gameDetails.setSize(700, 350);
        gameDetails.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        gameDetails.add(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Define pale colors
        Color paleBackground = new Color(230, 230, 240);
        Color paleForeground = new Color(60, 60, 60);

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM game WHERE game_id = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, gameId);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();

            // Retrieve details from the ResultSet
            String gameTitle = resultSet.getString("game_title");
            int price = resultSet.getInt("price");
            Date releaseDate = resultSet.getDate("release_date");
            String studioName = resultSet.getString("studio_name");
            int remainingStock = resultSet.getInt("remaining_stock");
            float rating = resultSet.getFloat("rating");
            String platforms = resultSet.getString("platforms");

            // Create and set a big title label
            JTextField titleField = new JTextField(gameTitle);
            titleField.setFont(new Font("Arial", Font.BOLD, 20));
            titleField.setHorizontalAlignment(JLabel.CENTER);
            titleField.setBackground(paleBackground);
            titleField.setForeground(paleForeground);
            panel.add(titleField, BorderLayout.NORTH);

            // Create a panel for the game details
            JPanel infoPanel = new JPanel(new GridLayout(3, 2));
            infoPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

            // Price area
            JPanel topLeftPanel = new JPanel(new GridLayout(1, 2));
            topLeftPanel.add(new JLabel("Price $"));
            topLeftPanel.add(new JTextField(String.valueOf(price)));
            topLeftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Release date area
            JPanel middleLeftPanel = new JPanel(new GridLayout(1, 2));
            middleLeftPanel.add(new JLabel("Release Date"));
            middleLeftPanel.add(new JDateChooser(releaseDate));
            middleLeftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Studio name area
            JPanel topRightPanel = new JPanel(new GridLayout(1, 2));
            topRightPanel.add(new JLabel("Studio Name"));
            ArrayList<String> studioNames = fetchStudioNames();
            JComboBox<String> studioComboBox = new JComboBox<>(studioNames.toArray(new String[0]));
            studioComboBox.setSelectedItem(studioName);
            topRightPanel.add(studioComboBox);
            topRightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Remaining stock area
            JPanel middleRightPanel = new JPanel(new GridLayout(1, 2));
            middleRightPanel.add(new JLabel("Remaining Stock"));
            middleRightPanel.add(new JTextField(String.valueOf(remainingStock)));
            middleRightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Rating area
            JPanel bottomLeftPanel = new JPanel(new GridLayout(1, 2));
            bottomLeftPanel.add(new JLabel("Rating"));
            bottomLeftPanel.add(new JTextField(String.valueOf(rating)));
            bottomLeftPanel.setBorder(new EmptyBorder(10,10,10,10));

            // Platforms area
            JPanel bottomRightPanel = new JPanel(new GridLayout(1, 2));
            bottomRightPanel.add(new JLabel("Platforms"));
            bottomRightPanel.add(new JTextField(platforms));
            bottomRightPanel.setBorder(new EmptyBorder(10,10,10,10));

            // Exclusive area
            JPanel bottomPanel = new JPanel(new GridLayout(1,2));
            Vector<String> exclusiveItems = new Vector<>(List.of(new String[] {"no", "yes"}));
            JComboBox<String> exclusiveComboBox = new JComboBox<>(exclusiveItems);
            exclusiveComboBox.setSelectedItem(resultSet.getBoolean("exclusive") ? 1 : 0);
            bottomPanel.add(new JLabel("Exclusive"));
            bottomPanel.add(exclusiveComboBox);

            // Add the panels to the info panel
            infoPanel.add(topLeftPanel);
            infoPanel.add(topRightPanel);
            infoPanel.add(middleLeftPanel);
            infoPanel.add(middleRightPanel);
            infoPanel.add(bottomLeftPanel);
            infoPanel.add(bottomRightPanel);
            infoPanel.add(bottomPanel);
            panel.add(infoPanel, BorderLayout.CENTER);

            // Add an update button
            JButton updateButton = new JButton("Update Game");
            updateButton.setBackground(paleBackground);
            panel.add(updateButton, BorderLayout.SOUTH);




            updateButton.addActionListener(e -> {
                try (Connection updateConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    // Studio name is not changed
                    // Update the game details
                    String updateSql = "UPDATE game SET price = ?, release_date = ?, remaining_stock = ?, game_title = ?, rating = ?, platforms = ?, studio_name = ?, exclusive = ? WHERE game_id = ?";
                    try (PreparedStatement updateStatement = updateConnection.prepareStatement(updateSql)) {
                        // Get updated values from text fields
                        int updatedPrice = Integer.parseInt(((JTextField) topLeftPanel.getComponent(1)).getText());
                        Date updatedReleaseDate = new Date(((JDateChooser) middleLeftPanel.getComponent(1)).getDate().getTime());
                        int updatedRemainStock = Integer.parseInt(((JTextField) middleRightPanel.getComponent(1)).getText());
                        String updatedGameTitle = titleField.getText();
                        float updatedRating = Float.parseFloat(((JTextField) bottomLeftPanel.getComponent(1)).getText());
                        String updatedPlatforms = ((JTextField) bottomRightPanel.getComponent(1)).getText();

                        // Set parameters for the update statement
                        updateStatement.setInt(1, updatedPrice);
                        updateStatement.setDate(2, updatedReleaseDate);
                        updateStatement.setInt(3, updatedRemainStock);
                        updateStatement.setString(4, updatedGameTitle);
                        updateStatement.setFloat(5, updatedRating);
                        updateStatement.setString(6, updatedPlatforms);
                        updateStatement.setString(7, studioComboBox.getSelectedItem() == null ? studioName : studioComboBox.getSelectedItem().toString());
                        updateStatement.setBoolean(8, exclusiveComboBox.getSelectedItem().equals("yes"));
                        updateStatement.setInt(9, gameId);

                        // Execute the update statement
                        int rowsAffected = updateStatement.executeUpdate();

                        if (rowsAffected > 0) {
                            JOptionPane.showMessageDialog(null, "Game updated successfully!");
                            displayGamesList();
                            gameDetails.dispose(); // Close the frame after successful update

                        } else {
                            JOptionPane.showMessageDialog(null, "Failed to update game. Please check your input.");
                        }
                    }
                }catch (NumberFormatException ex){
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Failed to update game. Please check your input.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error updating game details.");
                }
            });

            gameDetails.setVisible(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching game details.");
            gameDetails.dispose(); // Close the frame in case of an error
        }
    }

    private static void createTableIfNotExists() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS studio (" +
                    "studio_name VARCHAR(255) PRIMARY KEY," +
                    "country_of_origin VARCHAR(255))";
            statement.executeUpdate(createTableSQL);
            createTableSQL = "CREATE TABLE IF NOT EXISTS game (" +
                    "game_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "game_title VARCHAR(255)," +
                    "price INT," +
                    "release_date DATE," +
                    "exclusive BIT," +
                    "studio_name VARCHAR(255)," +
                    "remaining_stock INT," +
                    "rating DOUBLE," +
                    "platforms VARCHAR(255)," +
                    "FOREIGN KEY(studio_name) REFERENCES studio(studio_name))";
            statement.executeUpdate(createTableSQL);
            System.out.println("Tables created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addGame() {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String title = JOptionPane.showInputDialog("Enter the title of the game :");
            if(title == null) {
                return;
            }

            String price = JOptionPane.showInputDialog("Enter the price :");
            int intPrice;
            if(price == null) {
                return;
            }
            else{
                try{
                    intPrice = Integer.parseInt(price);
                } catch (NumberFormatException e){
                    JOptionPane.showMessageDialog(null, "Error: an integer was expected", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }


            JDateChooser dateChooser = new JDateChooser();
            JPanel datePanel = new JPanel(new BorderLayout());
            datePanel.add(new JLabel("Enter the release date"), BorderLayout.NORTH);
            datePanel.add(dateChooser);
            JOptionPane.showMessageDialog(null, datePanel);

            java.util.Date date = dateChooser.getDate();
            if(date == null){
                return;
            }

            Object[] options = {"yes", "no"};
            Object selectionObject = JOptionPane.showInputDialog(null, "Is the game exclusive ?", "Menu", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            Boolean exclusive = null;

            if(Objects.equals(selectionObject.toString(), "yes")){
                exclusive = true;
            }
            else if(Objects.equals(selectionObject.toString(), "no")){
                exclusive = false;
            }
            if(exclusive == null){
                return;
            }

            String studioName = JOptionPane.showInputDialog("Enter the name of the studio :");
            if(studioName == null){
                return;
            }

            int intStock;
            String stock = JOptionPane.showInputDialog("Enter the remaining stock :");
            if(stock == null){
                return;
            }
            else{
                try{
                    intStock = Integer.parseInt(stock);
                } catch (NumberFormatException e){
                    JOptionPane.showMessageDialog(null, "Error: an integer was expected", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            float floatRating;
            String rating = JOptionPane.showInputDialog("Enter the rating of the game (out of 5) :");
            if(rating == null){
                return;
            }
            else{
                try{
                    floatRating = Float.parseFloat(rating);
                    if(floatRating > 5 || floatRating < 0){
                        JOptionPane.showMessageDialog(null, "Error: a float number between 0 and 5 was expected", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException e){
                    JOptionPane.showMessageDialog(null, "Error: a float number was expected", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String platforms =  JOptionPane.showInputDialog("Enter the platforms that support the game :");

            String countryOfOrigin;

            if (isStudioNameExists(connection, studioName)) {
                String sql = "SELECT country_of_origin FROM studio WHERE studio_name = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, studioName);
                ResultSet resultSet = preparedStatement.executeQuery();

                // Check if the result set has a valid row
                if (resultSet.next()) {
                    countryOfOrigin = resultSet.getString("country_of_origin");
                } else {
                    countryOfOrigin = null;
                }
                resultSet.close();
            } else {
                countryOfOrigin = JOptionPane.showInputDialog("Enter the name of the studio's origin country :");
            }
            String sql;

            if (!isStudioNameExists(connection, studioName)) {
                sql = "INSERT INTO studio (studio_name, country_of_origin) VALUES (?, ?);";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, studioName);
                preparedStatement.setString(2, countryOfOrigin);
                preparedStatement.executeUpdate();
            }

            sql = "INSERT INTO game (game_title, price, release_date, exclusive, studio_name, remaining_stock, rating, platforms) VALUES (?, ?, ?, ? ,?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, title);
            preparedStatement.setInt(2, intPrice);
            preparedStatement.setDate(3, new java.sql.Date(date.getTime()));
            preparedStatement.setBoolean(4, exclusive);
            preparedStatement.setString(5, studioName);
            preparedStatement.setInt(6, intStock);
            preparedStatement.setFloat(7, floatRating);
            preparedStatement.setString(8, platforms);
            preparedStatement.executeUpdate();

            connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println("Error: " + ex.getMessage() + "\n");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Unexpected error: " + ex.getMessage() + "\n");
        }
    }

    private static ArrayList<String> fetchStudioNames(){
        // Fetch all studio names from the database
        ArrayList<String> studioNames = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT studio_name FROM studio";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    String studioName = resultSet.getString("studio_name");
                    studioNames.add(studioName);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Handle the exception as needed
        }

        return studioNames;
    }

    private static boolean isStudioNameExists(Connection connection, String studioName) throws SQLException {
        studioName = studioName.toUpperCase();
        String query = "SELECT COUNT(*) FROM studio WHERE studio_name = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, studioName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0; // If count > 0, studio name exists
                }
            }
        }
        return false;
    }

    private static void displayGamesList() {
        GameManagement.updateGameTable(GameManagement.FILTER_AND_ORDER_BY, filter, sort, order);
    }

    private static void displayGamesList(int selectionMode){
       GameManagement.updateGameTable(selectionMode, filter, sort, order);
    }

    static class GameManagement {
        static final int FILTER_BY = 1;
        static final int ORDER_BY = 2;
        static final int FILTER_AND_ORDER_BY = 3;

        public static ArrayList<Game> fetchGamesList(int selectionMode, String filter, String sort, String order) {
            String sql;
            filter =  filter==null ? "" : filter;
            sort =  sort==null ? "game_id" : sort;
            order = order==null ? "ascending" : order;
            ArrayList<Game> gameArrayList = new ArrayList<>();

            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

                // default
                PreparedStatement preparedStatement;
                ResultSet resultSet;
                switch (selectionMode) {
                    case FILTER_BY -> {
                        // filter by name
                        sql = "SELECT * FROM game WHERE game_title LIKE ?;";
                        preparedStatement = connection.prepareStatement(sql);
                        preparedStatement.setString(1, "%" + filter + "%");
                        resultSet = preparedStatement.executeQuery();
                    }
                    case ORDER_BY -> {
                        // order by
                        if(order.equals("ascending")){
                            sql = "SELECT * FROM game ORDER BY ? ASC;";
                        }
                        else {
                            sql = "SELECT * FROM game ORDER BY ? DESC;";
                        }
                        preparedStatement = connection.prepareStatement(sql);
                        preparedStatement.setString(1, sort);
                        resultSet = preparedStatement.executeQuery();
                    }
                    case FILTER_AND_ORDER_BY -> {
                        if(order.equals("descending")){
                            sql = "SELECT * FROM game WHERE game_title LIKE '%" + filter + "%' ORDER BY " + sort + " DESC";
                        }
                        else {
                            sql = "SELECT * FROM game WHERE game_title LIKE '%" + filter + "%' ORDER BY " + sort +" ASC;";
                        }
                        Statement statement = connection.createStatement();
                        resultSet = statement.executeQuery(sql);
                    }
                    default -> {
                        sql = "SELECT * FROM game ORDER BY game_id;";
                        preparedStatement = connection.prepareStatement(sql);
                        resultSet = preparedStatement.executeQuery();
                    }
                }

                while (resultSet.next()){
                    gameArrayList.add(new Game(resultSet));
                }
            }catch (SQLException ex){
                ex.printStackTrace();
            }
            return gameArrayList;
        }

        private static void updateGameTable(int selectionMode, String filter, String sort, String order) {
            listData = new Vector<>();
            for(Game game : GameManagement.fetchGamesList(selectionMode, filter, sort, order)) {
                listData.add(game.toVector());
            }
            tableModel = new DefaultTableModel(listData, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    //all cells false
                    return false;
                }
            };
            displayTable.setModel(tableModel);
        }
    }
}
