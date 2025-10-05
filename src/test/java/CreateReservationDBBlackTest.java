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

public class CreateReservationDBBlackTest {

    static DataAccess testDA = new DataAccess();

    private Traveler t1;
    private Ride r1;
    private Driver d1;

    @Before
    public void init() {
        testDA.open();

        try {
            testDA.removeDriver("d1");
            testDA.removeTraveler("t1@gmail.com");
            testDA.removeRides();
            testDA.removeReservations();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if (testDA.db.getTransaction().isActive())
            testDA.db.getTransaction().commit();

        d1 = testDA.createTestDriver("d1");
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
        testDA.removeTraveler("t1@gmail.com");
        testDA.removeRides();
        testDA.removeReservations();
        testDA.close();
    }

    @Test
    public void test1_validReservation() throws Exception {
        Reservation res = testDA.createReservation(1, r1.getRideNumber(), "t1@gmail.com");
        assertNotNull("La reserva debe crearse", res);
        assertEquals(t1.getEmail(), res.getTraveler().getEmail());
        assertEquals(r1.getRideNumber(), res.getRide().getRideNumber());
        assertTrue("La reserva debe guardarse en BD", testDA.getTravelerReservations("t1@gmail.com").contains(res));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test2_travelerEmailNull() throws Exception {
        testDA.createReservation(1, r1.getRideNumber(), null);
    }

    @Test
    public void test3_travelerNotInDB() {
        try {
            Reservation res = testDA.createReservation(1, r1.getRideNumber(), "noexiste@gmail.com");
            assertNull("Debe devolver null cuando traveler no está en BD", res);
        } catch (Exception e) {
            fail("No debería lanzar excepción: " + e);
        }
    }

    @Test
    public void test4_rideNotInDB() {
        try {
            Reservation res = testDA.createReservation(1, 9999, "t1@gmail.com");
            assertNull("Debe devolver null cuando el ride no existe", res);
        } catch (Exception e) {
            fail("No debería lanzar excepción: " + e);
        }
    }

    @Test
    public void test5_notEnoughSeats() {
        try {
            testDA.createReservation(3, r1.getRideNumber(), "t1@gmail.com");
            fail("Debió lanzar NotEnoughAvailableSeatsException");
        } catch (NotEnoughAvailableSeatsException e) {
        } catch (Exception e) {
            fail("Lanzó excepción incorrecta: " + e);
        }
    }

    @Test
    public void test6_invalidSeatNumber() {
        try {
            Reservation res = testDA.createReservation(0, r1.getRideNumber(), "t1@gmail.com");
            assertNull("hm <= 0 no debe crear la reserva", res);
        } catch (Exception e) {
            fail("No debería lanzar excepción: " + e);
        }
    }

    @Test
    public void test7_reservationAlreadyExists() {
        Reservation res1 = t1.makeReservation(r1, 1);
        testDA.db.getTransaction().begin();
        t1.addReservation(res1);
        r1.addReservation(res1);
        testDA.db.merge(t1);
        testDA.db.merge(r1);
        testDA.db.persist(res1);
        testDA.db.getTransaction().commit();

        try {
            Reservation res = testDA.createReservation(1, r1.getRideNumber(), "t1@gmail.com");
            assertNull("Debe devolver null o lanzar excepción si la reserva ya existe", res);
        } catch (ReservationAlreadyExistException e) {
        } catch (Exception e) {
            fail("Ha lanzado una excepción incorrecta: " + e);
        }
    }

    @Test
    public void testVL_hm0() {
        try {
            Reservation res = testDA.createReservation(0, r1.getRideNumber(), "t1@gmail.com");
            assertNull("Cuando hm=0 debe devolver null", res);
        } catch (Exception e) {
            fail("No debería lanzar excepción: " + e);
        }
    }

    @Test
    public void testVL_hm2() {
        try {
            Reservation res = testDA.createReservation(2, r1.getRideNumber(), "t1@gmail.com");
            assertNotNull("Cuando hm=2 debe crearse la reserva", res);
            assertEquals("El número de viajeros en la reserva debe ser 2", 2, res.getHmTravelers());
            assertTrue("Reserva debe guardarse en BD", testDA.getTravelerReservations("t1@gmail.com").contains(res));
        } catch (Exception e) {
            fail("No debería lanzar excepción: " + e);
        }
    }

    @Test(expected = NotEnoughAvailableSeatsException.class)
    public void testVL_hm3() throws Exception {
        testDA.createReservation(3, r1.getRideNumber(), "t1@gmail.com");
    }
}
