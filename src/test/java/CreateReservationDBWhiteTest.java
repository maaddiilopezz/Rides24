import dataAccess.DataAccess;
import domain.Ride;
import domain.Traveler;
import domain.Reservation;
import domain.Driver;
import exceptions.NotEnoughAvailableSeatsException;
import exceptions.ReservationAlreadyExistException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class CreateReservationDBWhiteTest {

    static DataAccess testDA = new DataAccess();

    private Traveler t1;
    private Ride r1;

    @Before
    public void init() {
        testDA.open();

        try {
            testDA.removeDriver("d1");
            testDA.removeTraveler(String.valueOf(t1));
            testDA.removeRides();
            testDA.removeReservations();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if (testDA.db.getTransaction().isActive()) {
            testDA.db.getTransaction().commit();
        }

        Driver d1 = testDA.createTestDriver("d1");
        r1 = testDA.createTestRide(d1.getUsername(), "A", "B", Date.from(Instant.now()), 2);
        t1 = testDA.createTestTraveler("t1@gmail.com");
        testDA.db.getTransaction().begin();
        testDA.db.persist(t1);
        testDA.db.persist(r1);
        testDA.db.getTransaction().commit();
    }

    @After
    public void tearDown() {
        testDA.close();
        testDA.open();
        testDA.removeDriver("d1");
        testDA.removeTraveler(String.valueOf(t1));
        testDA.removeRides();
        testDA.removeReservations();
        testDA.close();
    }

    @Test
    public void test1_rideNotInDB() {
        try {
            Reservation res = testDA.createReservation(1, 999, "t1@gmail.com");
            assertNull(res);
            assertFalse(testDA.getTravelerReservations("t1@gmail.com").contains(res));
        } catch (Exception e) {
            fail("No debería lanzar excepción");
        }
    }

    @Test
    public void test2_notEnoughSeats() {
        Reservation res = null;
        try {
            res = testDA.createReservation(3, r1.getRideNumber(), "t1@gmail.com");
            fail("Debió lanzar NotEnoughAvailableSeatsException");
        } catch (NotEnoughAvailableSeatsException e) {
            if (testDA.db.getTransaction().isActive()) testDA.db.getTransaction().commit();
            assertFalse(testDA.getTravelerReservations("t1@gmail.com").contains(res));
        } catch (Exception e) {
            fail("Lanzó excepción incorrecta: " + e);
        }
    }

    @Test
    public void test3_reservationAlreadyExists() {
        Reservation res1 = t1.makeReservation(r1, 1);
        testDA.db.getTransaction().begin();
        t1.addReservation(res1);
        r1.addReservation(res1);
        testDA.db.merge(t1);
        testDA.db.merge(r1);
        testDA.db.merge(res1);
        testDA.db.getTransaction().commit();
        try {
            Reservation res = testDA.createReservation(1, r1.getRideNumber(), "t1@gmail.com");
            assertNull(res);
        } catch (ReservationAlreadyExistException e) {
        } catch (Exception e) {
            fail("Lanzó excepción incorrecta: " + e);
        }
    }

    @Test
    public void test4_successfulReservation() throws Exception {
        Reservation res = testDA.createReservation(1, r1.getRideNumber(), "t1@gmail.com");
        assertNotNull(res);
        assertEquals("t1@gmail.com", res.getTraveler().getEmail());
        assertEquals(r1.getRideNumber(), res.getRide().getRideNumber());
    }
}
