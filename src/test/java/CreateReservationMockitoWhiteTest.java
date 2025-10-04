import testOperations.TestDataAccess;
import domain.*;
import exceptions.*;

import org.junit.*;
import org.mockito.*;

import javax.persistence.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CreateReservationMockitoWhiteTest {

    static TestDataAccess dataAccess;
    static Traveler t1;

    @Mock
    protected EntityManager db;
    @Mock
    protected EntityTransaction et;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        when(db.getTransaction()).thenReturn(et);

        // Traveler global
        t1 = new Traveler("t1@gmail.com", "T1", "pass");
        t1.setMoney(100);

        dataAccess = new TestDataAccess(db);
    }

    // Subclase de Ride para pruebas controladas
    private static class TestRide extends Ride {
        private boolean reservationExists;

        public TestRide(int nPlaces, boolean reservationExists) {
            super("A","B", new Date(), 25f,
                    new Driver("d@gmail.com", "Driver", "pass"),
                    new Car("1234ABC", nPlaces, new Driver("d@gmail.com", "Driver", "pass"), false));
            setRideNumber(1);
            setnPlaces(nPlaces);
            this.reservationExists = reservationExists;
            // Añadimos la ride al driver correctamente
            getDriver().addRide("A", "B", getDate(), 25f, getCar());
        }

        @Override
        public boolean doesReservationExist(int hm, Traveler t) {
            return reservationExists;
        }
    }

    private Ride createRide(int nPlaces, boolean reservationExists) {
        return new TestRide(nPlaces, reservationExists);
    }

    @Test
    public void test1_rideNotInDB() throws Exception {
        when(db.find(Ride.class, 999)).thenReturn(null); // ride no existe
        when(db.find(Traveler.class, "t1@gmail.com")).thenReturn(t1);

        Reservation res = dataAccess.createReservation(1, 999, "t1@gmail.com");
        assertNull(res);
    }

    @Test
    public void test2_notEnoughSeats() throws Exception {
        Ride r = createRide(2, false); // 2 plazas, reserva no existe
        when(db.find(Ride.class, 1)).thenReturn(r);
        when(db.find(Traveler.class, "t1@gmail.com")).thenReturn(t1);
        when(db.find(Driver.class, r.getDriver().getEmail())).thenReturn(r.getDriver());

        try {
            dataAccess.createReservation(3, 1, "t1@gmail.com"); // pide 3 plazas
            fail("Debió lanzar NotEnoughAvailableSeatsException");
        } catch (NotEnoughAvailableSeatsException e) {
            // Esperado
        }
    }

    @Test
    public void test3_reservationAlreadyExists() throws Exception {
        Ride r = createRide(2, true); // reserva ya existe
        when(db.find(Ride.class, 1)).thenReturn(r);
        when(db.find(Traveler.class, "t1@gmail.com")).thenReturn(t1);
        when(db.find(Driver.class, r.getDriver().getEmail())).thenReturn(r.getDriver());

        try {
            dataAccess.createReservation(1, 1, "t1@gmail.com");
            fail("Debió lanzar ReservationAlreadyExistException");
        } catch (ReservationAlreadyExistException e) {
            // Esperado
        }
    }

    @Test
    public void test4_successfulReservation() throws Exception {
        Ride r = createRide(2, false); // reserva no existe
        when(db.find(Ride.class, 1)).thenReturn(r);
        when(db.find(Traveler.class, "t1@gmail.com")).thenReturn(t1);
        when(db.find(Driver.class, r.getDriver().getEmail())).thenReturn(r.getDriver());

        Reservation res = dataAccess.createReservation(1, 1, "t1@gmail.com");
        assertNotNull(res);
        assertEquals(t1, res.getTraveler());
        assertEquals(r, res.getRide());
        assertEquals(2, r.getnPlaces()); // el valor original no cambia en este test
    }
}
