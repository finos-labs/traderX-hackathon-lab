import java.sql.*;
import java.time.LocalDateTime;

public class CDMTradeInserter {
    public static void main(String[] args) {
        try {
            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection(
                "jdbc:h2:tcp://localhost:18082/traderx", "sa", "");
            
            String sql = "INSERT INTO CDMTRADES (ID, ACCOUNTID, CREATED, UPDATED, SECURITY, SIDE, QUANTITY, STATE, CDMTRADEOBJ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, "CDM-DEMO-001");
            stmt.setInt(2, 22214);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(5, "AAPL");
            stmt.setString(6, "Buy");
            stmt.setInt(7, 100);
            stmt.setString(8, "Settled");
            stmt.setString(9, "{\"cdmVersion\":\"6.0.0\",\"businessEventType\":\"EXECUTION\"}");
            
            int result = stmt.executeUpdate();
            System.out.println("CDM trade inserted: " + result + " row(s) affected");
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}