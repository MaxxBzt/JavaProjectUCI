import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Vector;

public class GameVaultApp {
    private static JList<String> displayList;
    private static Vector<String> listData;
    private static DefaultListModel<String> listModel;

    private static final String DB_URL = "jdbc:mariadb://localhost/gamevault_db";
    private static final String DB_USER = "max";
    private static final String DB_PASSWORD = "";

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

        listModel = new DefaultListModel<>();
        displayList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(displayList);
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
                int selectedIndex = displayList.getSelectedIndex();
                displayList.getSelectedIndex();
                if (selectedIndex !=-1){
                    deleteGame(selectedIndex);
                }
                displayGamesList();
            }
        });

        displayList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int selectedIndex = displayList.getSelectedIndex();
                    displayList.getSelectedIndex();

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
                // if the user dont confirm the delete, return
                if (confirmDialog != JOptionPane.YES_OPTION) {
                    return;
                }
                // delete the game
                sql = "DELETE FROM game WHERE gameID = ?";
                try (PreparedStatement preparedStatement1 = connection.prepareStatement(sql)) {
                    preparedStatement1.setInt(1, resultSet.getInt("gameID"));
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
                int remainStock = resultSet.getInt("remain_stock");


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
                rightPanel.add(new JTextField(String.valueOf(remainStock)));

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
                            String updateSql = "UPDATE game SET price = ?, release_date = ?, remain_stock = ? WHERE game_title = ?";
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
                            return;
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
                    "countryorigin VARCHAR(255))";
            statement.executeUpdate(createTableSQL);
            createTableSQL = "CREATE TABLE IF NOT EXISTS game (" +
                    "gameID INT AUTO_INCREMENT PRIMARY KEY," +
                    "game_title VARCHAR(255)," +
                    "price INT," +
                    "release_date DATE," +
                    "exclusive BIT," +
                    "studio_name VARCHAR(255)," +
                    "remain_stock INT," +
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
            Statement statement = connection.createStatement();

            String title = JOptionPane.showInputDialog("Enter the title of the game :");
            String price = JOptionPane.showInputDialog("Enter the price :");
            String date = JOptionPane.showInputDialog("Enter the release date of the game (yyyy-mm-dd):");
            int exclusiveInt = Integer.parseInt(JOptionPane.showInputDialog("Is the game exclusive? 1 = yes / 0 = no :"));
            String exclusive = (exclusiveInt == 1) ? "1" : "0";
            //String exclusive = JOptionPane.showInputDialog("Is the game exclusive? 1 = yes / 0 = no :");
            String studio_name = JOptionPane.showInputDialog("Enter the name of the studio :");
            studio_name = studio_name.toUpperCase();
            String countryorigin;
            if (isStudioNameExists(connection, studio_name)) {
                ResultSet resultSet = statement.executeQuery("SELECT countryorigin FROM studio WHERE studio_name = '" + studio_name + "'");

                // Check if the result set has a valid row
                if (resultSet.next()) {
                    countryorigin = resultSet.getString("countryorigin");
                } else {
                    countryorigin = null;
                }
                resultSet.close();
            } else {
                countryorigin = JOptionPane.showInputDialog("Enter the name of the studio's origin country :");
            }

            String stock = JOptionPane.showInputDialog("Enter the remaining stock :");


            String sql;


            studio_name = studio_name.toUpperCase();

            if (!isStudioNameExists(connection, studio_name)) {
                sql = "INSERT INTO studio (studio_name, countryorigin) VALUES ('" + studio_name + "', '" + countryorigin + "')";
                statement.executeUpdate(sql);
            }

            sql = "INSERT INTO game (game_title, price, release_date, exclusive, studio_name, remain_stock) VALUES ('" + title + "', '" + price +"','"+ date +"', " + exclusive +", '" + studio_name +"','"+ stock +"')";
            statement.executeUpdate(sql);

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
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM game;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                listModel.clear();
                listData = new Vector<>();
                while (resultSet.next()) {
                    String title = resultSet.getString("game_title");
                    String price = resultSet.getString("price");
                    int isExclusive = resultSet.getInt("exclusive");
                    String studio_name = resultSet.getString("studio_name");
                    if (isExclusive == 1) {
                        title = title + " (Exclusive)";
                    }
                    String displayString = String.format("%s $%s %s", title, price,studio_name);

                    listModel.addElement(displayString);
                }

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
