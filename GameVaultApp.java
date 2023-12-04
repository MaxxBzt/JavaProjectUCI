import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.List;

public class GameVaultApp {
    private static final Vector<String> columnNames = new Vector<>(List.of(new String[]{"Id", "Title", "Price", "Release Date", "Exclusive", "Studio", "Rating", "Platforms", "Stock"}));
    private static DefaultTableModel tableModel;
    private static JTable displayTable;
    private static Vector<Vector<String>> listData;
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

        JButton addGame = new JButton("Add a game");
        panel.add(addGame, BorderLayout.NORTH);

        JButton deleteGame = new JButton("Delete game");
        deleteGame.setForeground(Color.RED);
        panel.add(deleteGame, BorderLayout.SOUTH);

        displayTable = new JTable();
        tableModel = new DefaultTableModel();
        JScrollPane scrollPane = new JScrollPane(displayTable);
        panel.add(scrollPane, BorderLayout.CENTER);

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
                displayGamesList();
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
            String sql = "SELECT * FROM game";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                // Move to the selected game's position in the ResultSet
                for (int i = 0; i <= selectedIndex; i++) {
                    if (!resultSet.next()) {
                        // Handle if the index is out of bounds
                        JOptionPane.showMessageDialog(null, "Invalid index selected.");
                        return;
                    }
                }
                // ask the user if they want to delete the game or not
                int confirmDialog = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete : "+ resultSet.getString("game_title"));
                // if the user doesn't confirm to delete, return
                if (confirmDialog != JOptionPane.YES_OPTION) {
                    return;
                }
                // delete the game
                sql = "DELETE FROM game WHERE game_id = ?";
                try (PreparedStatement preparedStatement1 = connection.prepareStatement(sql)) {
                    preparedStatement1.setInt(1, resultSet.getInt("game_id"));
                    preparedStatement1.executeUpdate();
                }
            }
            JOptionPane.showMessageDialog(null, "Game deleted successfully.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching game details.");
        }
    }

    private static void openNewFrame(int selectedGameIndex) {
        JFrame gameDetails = new JFrame("Game Details");
        gameDetails.setSize(600, 300);
        gameDetails.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        gameDetails.add(panel);

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM game";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                // Move to the selected game's position in the ResultSet
                for (int i = 0; i <= selectedGameIndex; i++) {
                    if (!resultSet.next()) {
                        // Handle if the index is out of bounds
                        JOptionPane.showMessageDialog(null, "Invalid index selected.");
                        gameDetails.dispose(); // Close the frame if invalid index
                        return;
                    }
                }

                // Retrieve details from the ResultSet
                String gameTitle = resultSet.getString("game_title");
                int price = resultSet.getInt("price");
                Date releaseDate = resultSet.getDate("release_date");
                String studioName = resultSet.getString("studio_name");
                int remainingStock = resultSet.getInt("remaining_stock");


                // Create and set big title label
                JTextField titleField = new JTextField(gameTitle);
                titleField.setFont(new Font("Arial", Font.BOLD, 20));
                titleField.setHorizontalAlignment(JLabel.CENTER);
                panel.add(titleField, BorderLayout.NORTH);

                // Create left panel for price and date
                JPanel leftPanel = new JPanel(new GridLayout(3, 2)); // Change to 3 rows
                leftPanel.add(new JLabel("Price $"));
                leftPanel.add(new JTextField(String.valueOf(price)));
                leftPanel.add(new JLabel("Release Date"));
                leftPanel.add(new JTextField(String.valueOf(releaseDate)));
                leftPanel.add(new JLabel("(YYYY-MM-DD)")); // Add label below Release Date
                panel.add(leftPanel, BorderLayout.WEST);


                // Create right panel for studio name and remaining stock
                JPanel rightPanel = new JPanel(new GridLayout(2, 2));
                rightPanel.add(new JLabel("Studio Name"));
                ArrayList<String> studioNames = fetchStudioNames();
                JComboBox<String> studioComboBox = new JComboBox<>(studioNames.toArray(new String[0]));
                studioComboBox.setSelectedItem(studioName);
                rightPanel.add(studioComboBox);
                rightPanel.add(new JLabel("Remaining Stock"));
                rightPanel.add(new JTextField(String.valueOf(remainingStock)));

                panel.add(rightPanel, BorderLayout.EAST);

                JPanel buttonPanel = new JPanel();
                JButton updateButton = new JButton("Update Game");
                buttonPanel.add(updateButton);
                panel.add(buttonPanel, BorderLayout.SOUTH);


                updateButton.addActionListener(e -> {
                    try (Connection updateConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        if (studioName.equals( ((String) studioComboBox.getSelectedItem()))) {
                            // Studio name is not changed
                            // Update the game details
                            String updateSql = "UPDATE game SET price = ?, release_date = ?, remaining_stock = ? WHERE game_title = ?";
                            try (PreparedStatement updateStatement = updateConnection.prepareStatement(updateSql)) {
                                // Get updated values from text fields
                                int updatedPrice = Integer.parseInt(((JTextField) leftPanel.getComponent(1)).getText());
                                String updatedReleaseDate = ((JTextField) leftPanel.getComponent(3)).getText();
                                int updatedRemainStock = Integer.parseInt(((JTextField) rightPanel.getComponent(3)).getText());

                                // Set parameters for the update statement
                                updateStatement.setInt(1, updatedPrice);
                                updateStatement.setString(2, updatedReleaseDate);
                                updateStatement.setInt(3, updatedRemainStock);
                                updateStatement.setString(4, gameTitle);

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
                        }
                        else {
                            String updatedStudioName = (String) studioComboBox.getSelectedItem();
                            // Change the studio of the game
                            String updateSql = "UPDATE game SET studio_name = ? WHERE game_title = ?";
                            try (PreparedStatement updateStatement = updateConnection.prepareStatement(updateSql)) {
                                // Set parameters for the update statement
                                updateStatement.setString(1, updatedStudioName);
                                updateStatement.setString(2, gameTitle);

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

                        }

                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error updating game details.");
                    }
                });

                gameDetails.setVisible(true);
            }
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
            System.out.println(date);

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
                    intStock = Integer.parseInt(price);
                } catch (NumberFormatException e){
                    JOptionPane.showMessageDialog(null, "Error: an integer was expected", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }


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

            sql = "INSERT INTO game (game_title, price, release_date, exclusive, studio_name, remaining_stock) VALUES (?, ?, ?, ? ,?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, title);
            preparedStatement.setInt(2, intPrice);
            preparedStatement.setDate(3, new java.sql.Date(date.getTime()));
            preparedStatement.setBoolean(4, exclusive);
            preparedStatement.setString(5, studioName);
            preparedStatement.setInt(6, intStock);
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
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)){
            String sql = "SELECT * FROM game;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            listData = new Vector<>();
            while (resultSet.next()) {
                listData.add(new Game(resultSet).toVector());
            }
            tableModel = new DefaultTableModel(listData, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    //all cells false
                    return false;
                }
            };
            displayTable.setModel(tableModel);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateGameTable(ArrayList<Game> gameList){

    }

    static class GameRankingManagement {
        public static ArrayList<Game> getGameRanking() {
            ArrayList<Game> ranking = new ArrayList<>();
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement statement = connection.createStatement()) {
                String sql = "SELECT * FROM game ORDER BY rating DESC";
                ResultSet resultSet = statement.executeQuery(sql);
                while (resultSet.next()){
                    ranking.add(new Game(resultSet));
                }
            }catch (SQLException ex){
                ex.printStackTrace();
            }
            return ranking;
        }
    }
}
