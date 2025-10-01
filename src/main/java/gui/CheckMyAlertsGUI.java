package gui;

import businessLogic.BLFacade;
import configuration.UtilDate;

import com.toedter.calendar.JCalendar;
import domain.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

import javax.swing.table.DefaultTableModel;


public class CheckMyAlertsGUI extends JFrame {
	private Traveler t;
	private static final long serialVersionUID = 1L;
	DefaultComboBoxModel<String> originLocations = new DefaultComboBoxModel<String>();
	DefaultComboBoxModel<String> destinationCities = new DefaultComboBoxModel<String>();
	private final JLabel jLabelEvents = new JLabel(ResourceBundle.getBundle("Etiquetas").getString("CreateRideGUI.Rides")); 
	private JLabel jLabelError = new JLabel("");

	private JButton jButtonClose = new JButton(ResourceBundle.getBundle("Etiquetas").getString("Close"));
	private JButton jButtonBook;
	
	private JScrollPane scrollPaneEvents = new JScrollPane();

	private JTable tableReservations= new JTable();

	private DefaultTableModel tableModelReservations;


	private String[] columnNamesRides = new String[] {
			ResourceBundle.getBundle("Etiquetas").getString("CheckReservationGUI.From"), 
			ResourceBundle.getBundle("Etiquetas").getString("CheckReservationGUI.To"),  
			ResourceBundle.getBundle("Etiquetas").getString("CheckReservationGUI.Created"), 
	};
	
	private void updateReservations(List<Alert> alertList) {
		tableModelReservations = new DefaultTableModel(null, columnNamesRides) {
			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
		};
		if(alertList.size()==0) {
			jLabelError.setText(ResourceBundle.getBundle("Etiquetas").getString("ChechReservationsGUI.jLabelError"));
		}else {
			for (Alert a: alertList){
				Vector<Object> row = new Vector<Object>();
				row.add(a.getJatorria());
				row.add(a.getHelburua());
				row.add(a.isSortuta());
				row.add(a); // ev object added in order to obtain it with tableModelEvents.getValueAt(i,3)
				tableModelReservations.addRow(row);		
			}
		}
		tableReservations.setModel(tableModelReservations);
	}


	public CheckMyAlertsGUI(Traveler t)
	{
		this.t=t;
		
		this.getContentPane().setLayout(null);
		this.setSize(new Dimension(700, 500));
		this.setTitle(ResourceBundle.getBundle("Etiquetas").getString("CheckReservationGUI.MainTitle"));
		jLabelEvents.setBounds(210, 62, 259, 16);
		this.getContentPane().add(jLabelEvents);
		jLabelEvents.setHorizontalAlignment(SwingConstants.CENTER);

		jButtonClose.setBounds(new Rectangle(274, 419, 130, 30));

		jButtonClose.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});
		BLFacade facade = MainGUI.getBusinessLogic();

		this.getContentPane().add(jButtonClose, null);

		scrollPaneEvents.setBounds(new Rectangle(139, 119, 398, 195));

		scrollPaneEvents.setViewportView(tableReservations);
		tableModelReservations = new DefaultTableModel(null, columnNamesRides);
		
		tableReservations.setModel(tableModelReservations);

		tableModelReservations.setDataVector(null, columnNamesRides);
		tableModelReservations.setColumnCount(4); // another column added to allocate ride objects

		tableReservations.getColumnModel().getColumn(0).setPreferredWidth(170);
		tableReservations.getColumnModel().getColumn(1).setPreferredWidth(170);
		tableReservations.getColumnModel().getColumn(1).setPreferredWidth(30);

		tableReservations.getColumnModel().removeColumn(tableReservations.getColumnModel().getColumn(3)); // not shown in JTable

		this.getContentPane().add(scrollPaneEvents, null);

		jLabelError = new JLabel(""); //$NON-NLS-1$ //$NON-NLS-2$
		jLabelError.setBounds(210, 77, 324, 30);
		getContentPane().add(jLabelError);
		jLabelError.setHorizontalAlignment(SwingConstants.CENTER);
		
		List<Alert> alertList = facade.getTravelerAlert(t.getEmail());
		this.updateReservations(alertList);
		
		jButtonBook = new JButton(ResourceBundle.getBundle("Etiquetas").getString("FindAndBookRidesGUI.jButtonBook")); //$NON-NLS-1$ //$NON-NLS-2$
		jButtonBook.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int i = tableReservations.getSelectedRow();
					if(!(i==-1)) {
						Alert al = alertList.get(i);
						if(!al.isSortuta()) {
							jLabelError.setText(ResourceBundle.getBundle("Etiquetas").getString("CheckMyAlertsGUI.jLabelError1"));
						}else {
							List<Ride> r = (List<Ride>) facade.isRideBeenCreated(al);
							JFrame a = new CheckMyAlertRidesGUI(t, r);
							a.setVisible(true);
							dispose();
						}
					}else {
						jLabelError.setText(ResourceBundle.getBundle("Etiquetas").getString("CheckMyAlertsGUI.jLabelError"));
					}
				}catch(Exception exc) {
					jLabelError.setText(ResourceBundle.getBundle("Etiquetas").getString("CheckMyAlertsGUI.jLabelError2"));
					System.out.println(exc.getMessage());				
				}
			}
		});
		jButtonBook.setBounds(254, 342, 183, 44);
		getContentPane().add(jButtonBook);

	}
}
