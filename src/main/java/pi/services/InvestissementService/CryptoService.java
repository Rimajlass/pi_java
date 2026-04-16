package pi.services.InvestissementService;

import pi.entities.Crypto;
import pi.tools.MyDatabase;
import java.util.ArrayList;
import java.sql.Statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class CryptoService {

    private Connection cnx() {
        return MyDatabase.getInstance().getCnx();
    }

    public void saveOrUpdate(Crypto crypto) throws Exception {
        String checkSql = "SELECT id FROM crypto WHERE apiid = ?";
        PreparedStatement checkStmt = cnx().prepareStatement(checkSql);
        checkStmt.setString(1, crypto.getApiid());

        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            String updateSql = "UPDATE crypto SET name = ?, symbol = ?, currentprice = ? WHERE apiid = ?";
            PreparedStatement updateStmt = cnx().prepareStatement(updateSql);
            updateStmt.setString(1, crypto.getName());
            updateStmt.setString(2, crypto.getSymbol());
            updateStmt.setDouble(3, crypto.getCurrentprice());
            updateStmt.setString(4, crypto.getApiid());
            updateStmt.executeUpdate();
        } else {
            String insertSql = "INSERT INTO crypto(name, symbol, apiid, currentprice) VALUES (?, ?, ?, ?)";
            PreparedStatement insertStmt = cnx().prepareStatement(insertSql);
            insertStmt.setString(1, crypto.getName());
            insertStmt.setString(2, crypto.getSymbol());
            insertStmt.setString(3, crypto.getApiid());
            insertStmt.setDouble(4, crypto.getCurrentprice());
            insertStmt.executeUpdate();
        }
    }

    public void saveAllOrUpdate(List<Crypto> cryptos) throws Exception {
        for (Crypto crypto : cryptos) {
            saveOrUpdate(crypto);
        }
    }
    public List<Crypto> getAll() throws Exception {

        List<Crypto> list = new ArrayList<>();

        String sql = "SELECT * FROM crypto";

        Statement st = cnx().createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            Crypto c = new Crypto(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("symbol"),
                    rs.getString("apiid"),
                    rs.getDouble("currentprice")
            );

            list.add(c);
        }

        return list;
    }
}