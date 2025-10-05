package dataAccess;

import java.io.File;
import java.net.NoRouteToHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import configuration.ConfigXML;
import configuration.UtilDate;
import domain.*;
import exceptions.*;

/**
 * It implements the data access to the objectDb database
 */
public class DataAccess  {
	public EntityManager  db;
	private  EntityManagerFactory emf;

	ConfigXML c=ConfigXML.getInstance();

     public DataAccess()  {
		if (c.isDatabaseInitialized()) {
			String fileName=c.getDbFilename();

			File fileToDelete= new File(fileName);
			if(fileToDelete.delete()){
				File fileToDeleteTemp= new File(fileName+"$");
				fileToDeleteTemp.delete();

				  System.out.println("File deleted");
				} else {
				  System.out.println("Operation failed");
				}
		}
		open();
		if  (c.isDatabaseInitialized())initializeDB();

		System.out.println("DataAccess created => isDatabaseLocal: "+c.isDatabaseLocal()+" isDatabaseInitialized: "+c.isDatabaseInitialized());

		close();

	}

    public DataAccess(EntityManager db) {
    	this.db=db;
    }

	/**
	 * This is the data access method that initializes the database with some events and questions.
	 * This method is invoked by the business logic (constructor of BLFacadeImplementation) when the option "initialize" is declared in the tag dataBaseOpenMode of resources/config.xml file
	 */
	public void initializeDB(){

		db.getTransaction().begin();

		try {

		   Calendar today = Calendar.getInstance();

		   int month=today.get(Calendar.MONTH);
		   int year=today.get(Calendar.YEAR);
		   if (month==12) { month=1; year+=1;}


		    //Create drivers
			Driver driver1=new Driver("driver1@gmail.com","Aitor Fernandez", "123");
			Driver driver2=new Driver("driver2@gmail.com","Ane Gaztañaga", "456");
			Driver driver3=new Driver("driver3@gmail.com","Test driver", "789");

			Car car1 = new Car("1234 ABC", 4, driver1, false);
			Car car2 = new Car("2345 DFG", 4, driver2, false);
			Car car3 = new Car("3456 HIJ", 6, driver3, true);
			Car car4 = new Car("4567 KLM", 9, driver2, true);
			Car car5 = new Car("5678 NÑO", 1, driver1, false);

			driver1.addCar(car1);
			driver1.addCar(car5);
			driver2.addCar(car4);
			driver2.addCar(car2);
			driver3.addCar(car3);


			//Create rides
			driver1.addRide("Donostia", "Bilbo", UtilDate.newDate(year,month,15), 7, car1);
			driver1.addRide("Donostia", "Gazteiz", UtilDate.newDate(year,month,6), 8, car1);
			driver1.addRide("Bilbo", "Donostia", UtilDate.newDate(year,month,25), 4, car5);
			driver1.addRide("Donostia", "Iruña", UtilDate.newDate(year,month,7), 8, car5);

			driver2.addRide("Donostia", "Bilbo", UtilDate.newDate(year,month,15), 3, car2);
			driver2.addRide("Bilbo", "Donostia", UtilDate.newDate(year,month,25), 5, car4);
			driver2.addRide("Eibar", "Gasteiz", UtilDate.newDate(year,month,6), 5, car2);

			driver3.addRide("Bilbo", "Donostia", UtilDate.newDate(year,month,14), 3, car3);

			Admin admin1 = new Admin("aitzol@gmail.com", "Aitzol", "123");
			Admin admin2 = new Admin("eneko@gmail.com", "Eneko", "123");

			db.persist(driver1);
			db.persist(driver2);
			db.persist(driver3);

			db.persist(admin1);
			db.persist(admin2);

			db.getTransaction().commit();
			System.out.println("Db initialized");
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * This method returns all the cities where rides depart
	 * @return collection of cities
	 */
	public List<String> getDepartCities(){
			TypedQuery<String> query = db.createQuery("SELECT DISTINCT r.from FROM Ride r ORDER BY r.from", String.class);
			List<String> cities = query.getResultList();
			return cities;

	}
	/**
	 * This method returns all the arrival destinations, from all rides that depart from a given city
	 *
	 * @param from the depart location of a ride
	 * @return all the arrival destinations
	 */
	public List<String> getArrivalCities(String from){
		TypedQuery<String> query = db.createQuery("SELECT DISTINCT r.to FROM Ride r WHERE r.from=?1 ORDER BY r.to",String.class);
		query.setParameter(1, from);
		List<String> arrivingCities = query.getResultList();
		return arrivingCities;

	}
	/**
	 * This method creates a ride for a driver
	 *
	 * @param from the origin location of a ride
	 * @param to the destination location of a ride
	 * @param date the date of the ride
	 * @param nPlaces available seats
	 * @param driverEmail to which ride is added
	 *
	 * @return the created ride, or null, or an exception
	 * @throws RideMustBeLaterThanTodayException if the ride date is before today
 	 * @throws RideAlreadyExistException if the same ride already exists for the driver
	 */
	public Ride createRide(String from, String to, Date date, float price, String driverEmail, String carPlate) throws  RideAlreadyExistException, RideMustBeLaterThanTodayException {
		System.out.println(">> DataAccess: createRide=> from= "+from+" to= "+to+" driver="+driverEmail+" date "+date);
		try {
			if(new Date().compareTo(date)>0) {
				throw new RideMustBeLaterThanTodayException(ResourceBundle.getBundle("Etiquetas").getString("CreateRideGUI.ErrorRideMustBeLaterThanToday"));
			}
			db.getTransaction().begin();

			Driver driver = db.find(Driver.class, driverEmail);
			if (driver.doesRideExists(from, to, date)) {
				db.getTransaction().commit();
				throw new RideAlreadyExistException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.RideAlreadyExist"));
			}

			Car car = db.find(Car.class, carPlate);
			Ride ride = driver.addRide(from, to, date, price, car);
			//next instruction can be obviated
			db.persist(driver);
			db.getTransaction().commit();

			return ride;
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			db.getTransaction().commit();
			return null;
		}
	}

	/**
	 * This method retrieves the rides from two locations on a given date
	 *
	 * @param from the origin location of a ride
	 * @param to the destination location of a ride
	 * @param date the date of the ride
	 * @return collection of rides
	 */
	public List<Ride> getRides(String from, String to, Date date) {
		System.out.println(">> DataAccess: getRides=> from= "+from+" to= "+to+" date "+date);

		TypedQuery<Ride> query = db.createQuery("SELECT r FROM Ride r WHERE r.from=?1 AND r.to=?2 AND r.date=?3",Ride.class);
		query.setParameter(1, from);
		query.setParameter(2, to);
		query.setParameter(3, date);
		List<Ride> rides = query.getResultList();
	 	return rides;
	}

	/**
	 * This method retrieves from the database the dates a month for which there are events
	 * @param from the origin location of a ride
	 * @param to the destination location of a ride
	 * @param date of the month for which days with rides want to be retrieved
	 * @return collection of rides
	 */
	public List<Date> getThisMonthDatesWithRides(String from, String to, Date date) {
		System.out.println(">> DataAccess: getEventsMonth");
		List<Date> res = new ArrayList<>();

		Date firstDayMonthDate= UtilDate.firstDayMonth(date);
		Date lastDayMonthDate= UtilDate.lastDayMonth(date);


		TypedQuery<Date> query = db.createQuery("SELECT DISTINCT r.date FROM Ride r WHERE r.from=?1 AND r.to=?2 AND r.date BETWEEN ?3 and ?4",Date.class);

		query.setParameter(1, from);
		query.setParameter(2, to);
		query.setParameter(3, firstDayMonthDate);
		query.setParameter(4, lastDayMonthDate);
		List<Date> dates = query.getResultList();
	 	 for (Date d:dates){
		   res.add(d);
		  }
	 	return res;
	}


	public void open(){

		String fileName=c.getDbFilename();
		if (c.isDatabaseLocal()) {
			emf = Persistence.createEntityManagerFactory("objectdb:"+fileName);
			db = emf.createEntityManager();
		} else {
			Map<String, String> properties = new HashMap<>();
			  properties.put("javax.persistence.jdbc.user", c.getUser());
			  properties.put("javax.persistence.jdbc.password", c.getPassword());

			  emf = Persistence.createEntityManagerFactory("objectdb://"+c.getDatabaseNode()+":"+c.getDatabasePort()+"/"+fileName, properties);
			  db = emf.createEntityManager();
    	   }
		System.out.println("DataAccess opened => isDatabaseLocal: "+c.isDatabaseLocal());


	}

	public void close(){
		db.close();
		System.out.println("DataAcess closed");
	}

	public Driver createDriver(String email, String name, String password) throws UserAlreadyExistException{
		System.out.println(">> DataAccess: createDriver=> email= "+email+" name= "+name+" password="+password);
		try {
			db.getTransaction().begin();

			Driver driver = db.find(Driver.class, email);
			if (driver!=null) {
				db.getTransaction().commit();
				throw new UserAlreadyExistException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.UserAlreadyExist"));
			}
			driver = new Driver (email, name, password);
			db.persist(driver);
			db.getTransaction().commit();

			return driver;
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			db.getTransaction().commit();
			return null;
		}
	}

	public Traveler createTraveler(String email, String name, String password) throws UserAlreadyExistException{
		System.out.println(">> DataAccess: createTraveler=> email= "+email+" name= "+name+" password="+password);
		try {
			db.getTransaction().begin();

			Traveler traveler = db.find(Traveler.class, email);
			if (traveler!=null) {
				db.getTransaction().commit();
				throw new UserAlreadyExistException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.UserAlreadyExist"));
			}
			traveler = new Traveler (email, name, password);
			db.persist(traveler);
			db.getTransaction().commit();

			return traveler;
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			db.getTransaction().commit();
			return null;
		}
	}

	public Reservation createReservation(int hm, Integer rideNumber, String travelerEmail) throws ReservationAlreadyExistException, NotEnoughAvailableSeatsException{
		System.out.println(">> DataAccess: createReservation=> how many seats= "+hm+" ride number= "+rideNumber+" traveler="+travelerEmail);
		try {
			db.getTransaction().begin();
			Ride r = db.find(Ride.class, rideNumber);
			if(r.getnPlaces()<hm) {
				throw new NotEnoughAvailableSeatsException(ResourceBundle.getBundle("Etiquetas").getString("MakeReservationGUI.jButtonError2"));
			}

			Traveler t = db.find(Traveler.class, travelerEmail);
		    Driver d = db.find(Driver.class, r.getDriver().getEmail());

			if (r.doesReservationExist(hm, t)) {
				db.getTransaction().commit();
				throw new ReservationAlreadyExistException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.ReservationAlreadyExist"));
			}
			Reservation res = t.makeReservation(r, hm);

			d.addReservation(res);
			r.addReservation(res);
			db.persist(d);
			db.persist(t);
			db.persist(r);
			db.getTransaction().commit();

			return res;
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			db.getTransaction().commit();
			return null;
		}
	}

	public Driver getDriverByEmail(String email, String password) throws UserDoesNotExistException, PasswordDoesNotMatchException{
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		db.getTransaction().commit();
		if(d==null) {
			this.close();
			throw new UserDoesNotExistException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.UserDoesNotExist"));
		}
		if(!d.getPassword().equals(password)) {
			this.close();
			throw new PasswordDoesNotMatchException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.PasswordDoesNotMatch"));
		}
		return d;
	}

	public Traveler getTravelerByEmail(String email, String password) throws UserDoesNotExistException, PasswordDoesNotMatchException{
		db.getTransaction().begin();
		Traveler t = db.find(Traveler.class, email);
		db.getTransaction().commit();
		if(t==null) {
			this.close();
			throw new UserDoesNotExistException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.UserDoesNotExist"));
		}
		if(!t.getPassword().equals(password)) {
			this.close();
			throw new PasswordDoesNotMatchException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.PasswordDoesNotMatch"));
		}
		return t;
	}

	public Admin getAdminByEmail(String email, String password) throws UserDoesNotExistException, PasswordDoesNotMatchException{
		db.getTransaction().begin();
		Admin a = db.find(Admin.class, email);
		db.getTransaction().commit();
		if(a==null) {
			this.close();
			throw new UserDoesNotExistException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.UserDoesNotExist"));
		}
		if(!a.getPassword().equals(password)) {
			this.close();
			throw new PasswordDoesNotMatchException(ResourceBundle.getBundle("Etiquetas").getString("DataAccess.PasswordDoesNotMatch"));
		}
		return a;
	}

	public void pay(Reservation res) throws NotEnoughMoneyException{
		db.getTransaction().begin();
		try {
			Traveler t = db.find(Traveler.class, res.getTraveler().getEmail());
			Driver d = db.find(Driver.class, res.getDriver().getEmail());
			float price = res.getHmTravelers()*res.getRide().getPrice();
			if(t.getMoney()-price<0){
				db.getTransaction().commit();
				throw new NotEnoughMoneyException();
			}
			Reservation r = db.find(Reservation.class, res.getReservationCode());
			r.setPayed(true);
			t.setMoney(t.getMoney()-price);
			d.setMoney(d.getMoney()+price);
			Transaction tr = new Transaction(price, d, t);
			d.addTransaction(tr);
			t.addTransaction(tr);
			db.persist(r);
			db.persist(tr);
			db.persist(d);
			db.persist(t);
			db.getTransaction().commit();
		}catch(NullPointerException e) {
			db.getTransaction().commit();
		}
	}

	public List<Reservation> getDriverReservations(String email){
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		db.getTransaction().commit();
		return d.getReservations();
	}

	public List<Reservation> getTravelerReservations(String email){
		db.getTransaction().begin();
		Traveler t = db.find(Traveler.class, email);
		db.getTransaction().commit();
		return t.getReservations();
	}

	public void takeMoneyDriver(String email, int hm) throws NotEnoughMoneyException{
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		if(d.getMoney()<hm) {
			db.getTransaction().commit();
			throw new NotEnoughMoneyException();
		}
		Transaction tr = new Transaction(hm, d);
		d.addTransaction(tr);
		d.setMoney(d.getMoney()-hm);
		db.persist(tr);
		db.persist(d);
		db.getTransaction().commit();
	}

	public void putMoneyTraveler(String email, int hm) {
		db.getTransaction().begin();
		Traveler t = db.find(Traveler.class, email);
		t.setMoney(t.getMoney()+hm);
		Transaction tr = new Transaction (hm, t);
		tr.setT(t);
		t.addTransaction(tr);
		db.persist(t);
		db.persist(tr);
		db.getTransaction().commit();
	}

	public void updateReservation(Reservation res) {
		db.getTransaction().begin();
		Traveler t = db.find(Traveler.class, res.getTraveler().getEmail());
		Driver d = db.find(Driver.class, res.getDriver().getEmail());
		Ride r = db.find(Ride.class, res.getRide().getRideNumber());
		t.updateReservation(res.getReservationCode());
		d.updateReservation(res.getReservationCode());
		r.updateReservation(res.getReservationCode());

		db.persist(d);
		db.persist(t);
		db.persist(r);
		db.getTransaction().commit();
	}

	public void cancelReservation (Reservation res) {
		db.getTransaction().begin();
		Traveler t = db.find(Traveler.class, res.getTraveler().getEmail());
		Driver d = db.find(Driver.class, res.getDriver().getEmail());
		Ride r = db.find(Ride.class, res.getRide().getRideNumber());
		t.cancelReservation(res);
		d.cancelReservation(res);
		r.cancelReservation(res);
		db.remove(db.find(Reservation.class, res.getReservationCode()));
		db.persist(d);
		db.persist(t);
		db.persist(r);
		db.getTransaction().commit();
	}

	public void addCarToDriver(String driverEmail, String carPlate, int nPlaces, boolean dis) throws CarAlreadyExistsException{
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, driverEmail);
		Car c = db.find(Car.class, carPlate);
		if(!(c == null)) {
			db.getTransaction().commit();
			throw new CarAlreadyExistsException();
		}
		Car car = new Car (carPlate, nPlaces, d, dis);
		d.addCar(car);
		db.persist(car);
		db.persist(d);
		db.getTransaction().commit();
	}

	public List<Car> getDriverCars(String email){
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		db.getTransaction().commit();
		return d.getCars();
	}

	public List<Transaction> getTravelerTransactions(String email){
		db.getTransaction().begin();
		Traveler t = db.find(Traveler.class, email);
		db.getTransaction().commit();
		return t.getTransactions();
	}

	public List<Transaction> getDriverTransactions(String email){
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		db.getTransaction().commit();
		return d.getTransactions();
	}

	public void removeRideDriver(Integer rideNumber, String email) {
		System.out.println(">> DataAccess: removeRideDriver=> ride number= "+rideNumber+" Driver="+email);
		try {
			db.getTransaction().begin();
			Ride r = db.find(Ride.class, rideNumber);
			List<Reservation>resList=r.getReservations();
			this.returnMoneyTravelers(resList, email);
		    Driver d = db.find(Driver.class, email);
		    d.removeRide(r.getFrom(), r.getTo(), r.getDate());
		    db.remove(r);
			db.persist(d);
			db.getTransaction().commit();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			db.getTransaction().commit();
		}
	}

	private void returnMoneyTravelers(List<Reservation>resList, String email) {
		try {
			Traveler t;
			Transaction trans;
			Driver d = db.find(Driver.class, email);
			for(Reservation res : resList) {
				if(res.isPayed()) {
					t = db.find(Traveler.class, res.getTraveler().getEmail());
					t.setMoney(t.getMoney()+res.getCost());
					trans = new Transaction(res.getCost(), res.getDriver(), res.getTraveler());
					t.addTransaction(trans);
					d.addTransaction(trans);
					d.setMoney(d.getMoney()-res.getCost());
					db.persist(t);
					db.persist(d);
					db.persist(trans);
				}
			}
		}catch(NullPointerException e) {
			db.getTransaction().commit();
		}
	}

	public List<Ride> getDriverRides(String email){
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		db.getTransaction().commit();
		return d.getRides();
	}

	public void deleteAccountDriver(String email) {
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		db.remove(d);
		db.getTransaction().commit();
	}

	public void deleteAccountTraveler(String email) {
		db.getTransaction().begin();
		Traveler t = db.find(Traveler.class, email);
		db.remove(t);
		db.getTransaction().commit();
	}

	public List<Complaint> getDriverComplaint(String email){
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		db.getTransaction().commit();
		return d.getComplaints();
	}

	public void createComplaintToDriver(String c, Reservation res) {
		db.getTransaction().begin();
		String driverEmail = res.getDriver().getEmail();
		String travelerEmail = res.getTraveler().getEmail();
		Integer rideId = res.getRide().getRideNumber();
		Driver d = db.find(Driver.class, driverEmail);
		Traveler t = db.find(Traveler.class, travelerEmail);
		Reservation r = db.find(Reservation.class, res.getReservationCode());
		r.setComplained(true);
		Complaint complaint = new Complaint(c, r);
		d.addComplaint(complaint);
		t.addComplaint(complaint);
		db.persist(r);
		db.persist(t);
		db.persist(d);
		db.persist(complaint);
		db.getTransaction().commit();
	}

	public void addRatingToTraveler(String e, int z, Integer resCode) {
		db.getTransaction().begin();
		Reservation r = db.find(Reservation.class, resCode);
		Traveler t = db.find(Traveler.class, e);
		r.setRatedD(true);
		t.addRating(z);
		db.persist(t);
		db.persist(r);
		db.getTransaction().commit();
	}

	public void addRatingToDriver(String email, int z, Integer resCode){
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		Reservation r = db.find(Reservation.class, resCode);
		d.addRating(z);
		r.setRatedT(true);
		db.persist(d);
		db.persist(r);
		db.getTransaction().commit();
	}
	public void createAlert(Traveler t, String jatorria, String helmuga) throws AlertAlreadyExistsException{
		db.getTransaction().begin();
		if(!this.doesAlertExist(t, jatorria, helmuga)) {
			Traveler traveler = db.find(Traveler.class, t.getEmail());
			Alert a = new Alert(jatorria, helmuga, t);
			traveler.addAlert(a);
			db.persist(traveler);
			db.persist(a);
		}else {
			db.getTransaction().commit();
			throw new AlertAlreadyExistsException();
		}
		db.getTransaction().commit();
	}

	private boolean doesAlertExist(Traveler tra, String jatorria, String helmuga) {
		Traveler t = db.find(Traveler.class, tra.getEmail());
		Alert momentukoa = new Alert(jatorria, helmuga, t);
		List<Alert>alertList = t.getAlerts();
		System.out.println(alertList.size());
		boolean found = false;
		int i = 0;
		while(!found && i<alertList.size()) {
			if(alertList.get(i).equals(momentukoa)) {
				found = true;
			}
			i++;
		}
		return found;
	}

	public List<Ride> isRideBeenCreated(Alert alert) {
		db.getTransaction().begin();
		String jatorria = alert.getJatorria();
		String helburua = alert.getHelburua();
		TypedQuery<Ride> query = db.createQuery("SELECT r FROM Ride r WHERE r.from=?1 AND r.to=?2", Ride.class);
		query.setParameter(1, jatorria);
		query.setParameter(2, helburua);
		db.getTransaction().commit();
		if(!query.getResultList().isEmpty()) return query.getResultList();
		else return null;
	}

	public List<Alert> getTravelerAlert(String email){
		db.getTransaction().begin();
		Traveler t = db.find(Traveler.class, email);
		db.getTransaction().commit();
		return t.getAlerts();
	}

	public void removeAlert(Integer z) {
		db.getTransaction().begin();
		Alert a = db.find(Alert.class, z);
		db.remove(a);
		db.getTransaction().commit();
	}

	public float getDriverRatings(String email) {
		db.getTransaction().begin();
		Driver d = db.find(Driver.class, email);
		db.getTransaction().commit();
		return d.calculateRating();
	}

	public float getTravelerRatings(String email) {
		db.getTransaction().begin();
		Traveler d = db.find(Traveler.class, email);
		db.getTransaction().commit();
		return d.calculateRating();
	}

	public List<Complaint> getAllComplaint(){
		db.getTransaction().begin();
		TypedQuery<Complaint> query = db.createQuery("SELECT c FROM Complaint c",Complaint.class);
		db.getTransaction().commit();
		return query.getResultList();
	}

	public void denyComplaint(Complaint co) {
		db.getTransaction().begin();
		Complaint c = db.find(Complaint.class, co.getId());
		c.setDenied(true);
		db.persist(c);
		db.getTransaction().commit();
	}

	public void acceptComplaint(Complaint co) {
		db.getTransaction().begin();
		Complaint c = db.find(Complaint.class, co.getId());
		Driver d = db.find(Driver.class, c.getDriverEmail());
		Traveler t = db.find(Traveler.class, c.getTravelerEmail());
		Reservation res = db.find(Reservation.class, co.getRes().getReservationCode());
		float price = res.getHmTravelers()*res.getRide().getPrice();
		d.setMoney(d.getMoney()-price);
		t.setMoney(t.getMoney()+price);
		d.removeComplaint(c);
		t.removeComplaint(c);
		Transaction tra = new Transaction(price, t, d);
		d.addTransaction(tra);
		t.addTransaction(tra);
		db.persist(tra);
		db.persist(t);
		db.persist(d);
		db.remove(c);
		db.getTransaction().commit();
	}

	public void denyComplaintAdmin(Complaint co) {
		db.getTransaction().begin();
		Complaint c = db.find(Complaint.class, co.getId());
		c.setDenied(true);
		Driver d = db.find(Driver.class, c.getDriverEmail());
		Traveler t = db.find(Traveler.class, c.getTravelerEmail());
		d.removeComplaint(c);
		t.removeComplaint(c);
		db.remove(c);
		db.persist(t);
		db.persist(d);
		db.getTransaction().commit();
	}

	public void updateAlerts(String jatorria, String helburua) {
		db.getTransaction().begin();
		TypedQuery<Alert> query = db.createQuery("SELECT a FROM Alert a WHERE a.jatorria=?1 AND a.helburua=?2", Alert.class);
		query.setParameter(1, jatorria);
		query.setParameter(2, helburua);
		List<Alert> aList = query.getResultList();
		if(!aList.isEmpty()) {
			for(Alert a: aList) {
				a.setSortuta(true);
			}
		}
		db.getTransaction().commit();
	}

    public void setEntityManager(Object db) {
        this.db = (EntityManager) db;
    }

    // Métodos auxiliares para pruebas unitarias
    public Traveler createTestTraveler(String email) {

        try {
            db.getTransaction().begin();
            Traveler traveler = db.find(Traveler.class, email);
            if (traveler == null) {
                traveler = new Traveler(email, "Test Traveler", "1234");
                db.persist(traveler);
            }
            db.getTransaction().commit();
            return traveler;
        } catch (Exception e) {
            db.getTransaction().rollback();
            throw e;
        }
    }

    public Ride createTestRide(String driverEmail, String from, String to, Date date, int nPlaces) {

        try {
            db.getTransaction().begin();
            Driver driver = db.find(Driver.class, driverEmail);
            if (driver == null) {
                driver = new Driver(driverEmail, "Test Driver", "1234");
                db.persist(driver);
            }
            // Buscar o crear un Car para el driver
            Car car = null;
            List<Car> cars = driver.getCars();

            if (cars == null || cars.isEmpty()) {
                car = new Car("CAR-" + driverEmail, nPlaces, driver, false);
                driver.addCar(car);
            } else {
                car = cars.get(0);
            }

            Ride ride = driver.addRide(from, to, date, 0f, car);
            db.persist(driver);
            db.getTransaction().commit();
            return ride;
        } catch (Exception e) {
            // db.getTransaction().rollback();
            throw e;
        }
    }

    public void removeDriver(String email) {
        db.getTransaction().begin();
        db.remove(db.find(Car.class, "CAR-" + email));
        Driver d = db.find(Driver.class, email);
        if (d != null) db.remove(d);
        db.getTransaction().commit();
    }

    public void removeTraveler(String email) {
        db.getTransaction().begin();
        Traveler t = db.find(Traveler.class, email);
        if (t != null) db.remove(t);
        db.getTransaction().commit();
    }

    public void removeRides() {
        db.getTransaction().begin();
        List<Ride> rides = db.createQuery("SELECT r FROM Ride r", Ride.class).getResultList();
        for (Ride r : rides) db.remove(r);
        db.getTransaction().commit();
    }

    public void removeReservations() {
        db.getTransaction().begin();
        List<Reservation> reservations = db.createQuery("SELECT r FROM Reservation r", Reservation.class).getResultList();
        for (Reservation r : reservations) db.remove(r);
        db.getTransaction().commit();
    }

    public Driver createTestDriver(String email) {
        try {
            db.getTransaction().begin();
            Driver driver = db.find(Driver.class, email);
            if (driver == null) {
                driver = new Driver(email, "Test Driver", "1234");
                db.persist(driver);
            }
            db.getTransaction().commit();
            return driver;
        } catch (Exception e) {
            throw e;
        }
    }

}
