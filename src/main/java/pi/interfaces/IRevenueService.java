package pi.interfaces;

import pi.entities.Revenue;

import java.sql.SQLException;
import java.util.List;

public interface IRevenueService {
    void add(Revenue revenue) throws SQLException;

    void update(Revenue revenue) throws SQLException;

    void delete(int id) throws SQLException;

    Revenue getById(int id) throws SQLException;

    List<Revenue> getAll() throws SQLException;
}
