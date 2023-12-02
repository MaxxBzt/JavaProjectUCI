import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
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
                JPanel leftPanel = new JPanel(new GridLayout(2, 2));
                leftPanel.add(new JLabel("Price $"));
                leftPanel.add(new JTextField(String.valueOf(price)));
                leftPanel.add(new JLabel("Release Date (yyyy-mm-dd)"));
                leftPanel.add(new JTextField(String.valueOf(releaseDate)));
                panel.add(leftPanel, BorderLayout.WEST);

                // Create right panel for studio name and remaining stock
                JPanel rightPanel = new JPanel(new GridLayout(2, 2));
                rightPanel.add(new JLabel("Studio Name"));
                rightPanel.add(new JTextField(studioName));
                rightPanel.add(new JLabel("Remaining Stock"));
                rightPanel.add(new JTextField(String.valueOf(remainStock)));

                panel.add(rightPanel, BorderLayout.EAST);

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
            String countryorigin = JOptionPane.showInputDialog("Enter the name of the studio's origin country :");
            String stock = JOptionPane.showInputDialog("Enter the remaining stock :");

            String sql;

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

    private static boolean isStudioNameExists(Connection connection, String studioName) throws SQLException {
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
