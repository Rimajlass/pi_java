package pi.interfaces;

import pi.entities.Expense;

import java.sql.SQLException;
import java.util.List;

public interface IExpenseService {
    void add(Expense expense) throws SQLException;

    void update(Expense expense) throws SQLException;

    void delete(int id) throws SQLException;

    Expense getById(int id) throws SQLException;

    List<Expense> getAll() throws SQLException;
}
