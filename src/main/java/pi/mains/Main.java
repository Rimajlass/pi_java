package pi.mains;

import pi.entities.Revenue;
import pi.entities.User;
import pi.services.RevenueService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        RevenueService revenueService = new RevenueService();

        User user = new User();
        user.setId(1);

        try {
            Revenue revenue = new Revenue(
                    user,
                    1200.0,
                    "salary",
                    LocalDate.now(),
                    "simple revenue test",
                    LocalDateTime.now()
            );

            revenueService.add(revenue);
            System.out.println("Added revenue: " + revenue);

            Revenue fetchedRevenue = revenueService.getById(revenue.getId());
            System.out.println("Fetched revenue: " + fetchedRevenue);

            revenue.setAmount(1400.0);
            revenueService.update(revenue);
            System.out.println("Updated revenue: " + revenueService.getById(revenue.getId()));

            List<Revenue> revenues = revenueService.getAll();
            System.out.println("Revenue count: " + revenues.size());

            revenueService.delete(revenue.getId());
            System.out.println("Deleted revenue id: " + revenue.getId());
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        }
    }
}
