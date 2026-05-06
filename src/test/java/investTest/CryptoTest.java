package investTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import pi.entities.Crypto;
import pi.services.InvestissementService.CryptoService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(OrderAnnotation.class)
public class CryptoTest {

    private static CryptoService cryptoService;
    private static Connection cnx;
    private final List<String> insertedApiIds = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        cryptoService = new CryptoService();
        cnx = MyDatabase.getInstance().getCnx();

        assertNotNull(cryptoService);
        assertNotNull(cnx);
    }

    @AfterEach
    void cleanDatabase() throws SQLException {
        for (String apiId : insertedApiIds) {
            deleteCryptoByApiId(apiId);
        }
        insertedApiIds.clear();
    }

    @Test
    @Order(1)
    void testSaveOrUpdateInsertsCrypto() throws Exception {
        String uniqueApiId = "test-crypto-" + System.nanoTime();
        Crypto crypto = new Crypto("TestCoin", "TST", uniqueApiId, 100.0);

        cryptoService.saveOrUpdate(crypto);

        Crypto found = findCryptoByApiId(uniqueApiId);
        assertNotNull(found);
        assertEquals("TestCoin", found.getName());
        assertEquals("TST", found.getSymbol());
        assertEquals(100.0, found.getCurrentprice());

        insertedApiIds.add(uniqueApiId);
    }

    @Test
    @Order(2)
    void testSaveOrUpdateUpdatesCrypto() throws Exception {
        String uniqueApiId = "test-crypto-upd-" + System.nanoTime();
        Crypto crypto = new Crypto("UpdateCoin", "UPD", uniqueApiId, 50.0);
        cryptoService.saveOrUpdate(crypto);
        insertedApiIds.add(uniqueApiId);

        Crypto updated = new Crypto("UpdateCoin", "UPD", uniqueApiId, 250.0);
        cryptoService.saveOrUpdate(updated);

        Crypto found = findCryptoByApiId(uniqueApiId);
        assertNotNull(found);
        assertEquals(250.0, found.getCurrentprice());
    }

    @Test
    @Order(3)
    void testGetAllReturnsList() throws Exception {
        String uniqueApiId = "test-crypto-list-" + System.nanoTime();
        Crypto crypto = new Crypto("ListCoin", "LST", uniqueApiId, 75.0);
        cryptoService.saveOrUpdate(crypto);
        insertedApiIds.add(uniqueApiId);

        List<Crypto> list = cryptoService.getAll();

        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(c -> uniqueApiId.equals(c.getApiid())));
    }

    private Crypto findCryptoByApiId(String apiId) throws SQLException {
        String sql = "SELECT * FROM crypto WHERE apiid = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, apiId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Crypto(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("symbol"),
                            rs.getString("apiid"),
                            rs.getDouble("currentprice")
                    );
                }
            }
        }
        return null;
    }

    private void deleteCryptoByApiId(String apiId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM crypto WHERE apiid = ?")) {
            ps.setString(1, apiId);
            ps.executeUpdate();
        }
    }
}
