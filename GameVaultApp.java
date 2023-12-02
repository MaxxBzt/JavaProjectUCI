import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class GameVaultApp {

    private static JTextArea displayArea;
    private static final String DB_URL = "jdbc:mariadb://localhost:3306/gamevault_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "x";

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

        displayArea = new JTextArea(10, 40);
        JScrollPane scrollPane = new JScrollPane(displayArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        addGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addGame();
                displayGamesList();
            }
        });

        displayArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    openNewFrame();
                }
            }
        });

        displayGamesList();
    }

    private static void openNewFrame() {
        JFrame newFrame = new JFrame("Game Details");
        newFrame.setSize(300, 200);
        newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Close only this frame on close

        JPanel panel = new JPanel();
        newFrame.add(panel);

        // Add your components to the newFrame as needed
        JLabel label = new JLabel("Game Details");
        panel.add(label);

        newFrame.setVisible(true);
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
                    "release_date TIMESTAMP," +
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
            displayArea.append("Error: " + ex.getMessage() + "\n");
        } catch (Exception ex) {
            ex.printStackTrace();
            displayArea.append("Unexpected error: " + ex.getMessage() + "\n");
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
            String sql = "SELECT game_title FROM game;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                displayArea.setText(""); // Clear the displayArea

                while (resultSet.next()) {
                    String title = resultSet.getString("game_title");
                    displayArea.append(title + "\n"); // Append each game title to the displayArea
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
