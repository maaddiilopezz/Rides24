import testOperations.TestDataAccess;
import domain.*;
import exceptions.*;

import org.junit.*;
import org.mockito.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CreateReservationMockitoBlackTest {

    static TestDataAccess testDA;

    @Mock
    protected EntityManager db;
    @Mock
    protected EntityTransaction et;

    private Traveler t1;
    private Ride r1;
    private Driver d1;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        when(db.getTransaction()).thenReturn(et);

        testDA = new TestDataAccess(db);

        // Creamos objetos base
        t1 = new Traveler("t1@gmail.com", "T1", "pass");
        d1 = new Driver("d1@gmail.com", "Driver1", "pass");
        r1 = new Ride("A", "B", Date.from(Instant.now()), 2f, d1, new Car("1234ABC", 2, d1, false));
        r1.setRideNumber(1);

        // Mockeamos búsquedas en DB
        when(db.find(Traveler.class, "t1@gmail.com")).thenReturn(t1);
        when(db.find(Ride.class, 1)).thenReturn(r1);
        when(db.find(Driver.class, d1.getEmail())).thenReturn(d1);
    }

    // --------------------------------------------------------
    // CASOS DE PRUEBA
    // --------------------------------------------------------

    @Test
    public void test1_validReservation() throws Exception {
        Reservation res = testDA.createReservation(1, 1, "t1@gmail.com");
        assertNotNull("La reserva debe crearse", res);
        assertEquals(t1, res.getTraveler());
        assertEquals(r1, res.getRide());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test2_travelerEmailNull() throws Exception {
        testDA.createReservation(1, 1, null);
    }

    @Test
    public void test3_travelerNotInDB() throws Exception {
        when(db.find(Traveler.class, "noexiste@gmail.com")).thenReturn(null);
        Reservation res = testDA.createReservation(1, 1, "noexiste@gmail.com");
        assertNull("Debe devolver null cuando traveler no está en DB", res);
    }

    @Test
    public void test4_rideNotInDB() throws Exception {
        when(db.find(Ride.class, 9999)).thenReturn(null);
        Reservation res = testDA.createReservation(1, 9999, "t1@gmail.com");
        assertNull("Debe devolver null cuando el ride no existe", res);
    }

    @Test
    public void test5_notEnoughSeats() {
        try {
            testDA.createReservation(3, 1, "t1@gmail.com");
            fail("Debió lanzar NotEnoughAvailableSeatsException");
        } catch (NotEnoughAvailableSeatsException e) {
            // esperado
        } catch (Exception e) {
            fail("Lanzó excepción incorrecta: " + e);
        }
    }

    @Test
    public void test6_invalidSeatNumber() throws Exception {
        Reservation res = testDA.createReservation(0, 1, "t1@gmail.com");
        assertNull("hm <= 0 no debe crear la reserva", res);
    }

    @Test
    public void test7_reservationAlreadyExists() throws Exception {
        // Creamos reserva previa usando objetos reales
        Reservation res1 = t1.makeReservation(r1, 1);
        r1.addReservation(res1);
        t1.addReservation(res1);

        try {
            // Intentamos crear la misma reserva otra vez
            testDA.createReservation(1, 1, "t1@gmail.com");
            fail("Debió lanzar ReservationAlreadyExistException");
        } catch (ReservationAlreadyExistException e) {
            // Esperado
            assertEquals("Reservation already exists", e.getMessage());
        } catch (Exception e) {
            fail("Lanzó excepción incorrecta: " + e);
        }
    }


    // --------------------------------------------------------
    // VALORES LÍMITE
    // --------------------------------------------------------

    @Test
    public void testVL_hm0() throws Exception {
        Reservation res = testDA.createReservation(0, 1, "t1@gmail.com");
        assertNull("Cuando hm=0 debe devolver null", res);
    }

    @Test
    public void testVL_hm2() throws Exception {
        Reservation res = testDA.createReservation(2, 1, "t1@gmail.com");
        assertNotNull("Cuando hm=2 debe crearse la reserva", res);
        assertEquals(2, res.getHmTravelers());
    }

    @Test(expected = NotEnoughAvailableSeatsException.class)
    public void testVL_hm3() throws Exception {
        testDA.createReservation(3, 1, "t1@gmail.com");
    }
}
