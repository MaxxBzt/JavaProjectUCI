import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Game {
    private int gameId;
    private String gameTitle;
    private int price;
    private Date releaseDate;
    private boolean exclusive;
    private String studioName;
    private float rating = -1;
    private String[] platforms;
    private int remainingStock;
    public Game(){}

    public Game(int gameId, String gameTitle, int price, Date releaseDate, boolean exclusive, String studioName, int remainingStock, float rating, String[] platforms) {
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.price = price;
        this.releaseDate = releaseDate;
        this.exclusive = exclusive;
        this.studioName = studioName;
        this.remainingStock = remainingStock;
        this.rating = rating;
        this.platforms = platforms;
    }

    public Game(ResultSet resultSet) {
        this();
        try{
            setGameId(resultSet.getInt("game_id"));
            setGameTitle(resultSet.getString("game_title"));
            setPrice(resultSet.getInt("price"));
            setReleaseDate(resultSet.getDate("release_date"));
            setExclusive(resultSet.getBoolean("exclusive"));
            setStudioName(resultSet.getString("studio_name"));
            setRemainingStock(resultSet.getInt("remaining_stock"));
            setRating((resultSet.getString("rating") == null) ? -1 : resultSet.getFloat("rating"));

            String platforms = resultSet.getString("platforms");
            setPlatforms(platforms == null ? null : platforms.split(" ,"));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getGameTitle() {
        return gameTitle==null ? null : String.valueOf(gameTitle);
    }

    public void setGameTitle(String gameTitle) {
        this.gameTitle = gameTitle;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public Date getReleaseDate() {
        return releaseDate==null ? null :  new Date(releaseDate.getTime());
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public String getStudioName() {
        return studioName==null ? null :String.valueOf(studioName);
    }

    public void setStudioName(String studioName) {
        this.studioName = studioName;
    }

    public int getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(int remainingStock) {
        this.remainingStock = remainingStock;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String[] getPlatforms() {
        return platforms==null ? null : Arrays.copyOf(platforms, platforms.length);
    }

    public void setPlatforms(String[] platforms) {
        this.platforms = platforms;
    }

    @Override
    public String toString() {
        return "Game{" +
                "gameId=" + gameId +
                ", gameTitle='" + gameTitle + '\'' +
                ", price=" + price +
                ", releaseDate=" + releaseDate +
                ", exclusive=" + exclusive +
                ", studioName='" + studioName + '\'' +
                ", rating=" + rating +
                ", platforms=" + Arrays.toString(platforms) +
                ", remainingStock=" + remainingStock +
                '}';
    }

    public Vector<String> toVector(){
        return new Vector<>(List.of(new String[]{
                String.valueOf(getGameId()),
                getGameTitle(),
                String.valueOf(getPrice()),
                String.valueOf(getReleaseDate()),
                isExclusive() ? "yes" : "no",
                getStudioName(),
                (getRating() == -1) ? "no rating": String.valueOf(getRating()),
                (getPlatforms() == null) ? "none" : Arrays.toString(getPlatforms()),
                String.valueOf(getRemainingStock())
        }));
    }
}
