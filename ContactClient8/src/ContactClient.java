import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import javax.xml.ws.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import org.w3c.dom.*;

/**
 * RESTful web client to manage a list of courses.
 */
public class ContactClient {

	private static final String SERVICE_PATH = "/contacts/"; // initialise service path to match service
	private static final QName SERVICE_QNAME = new QName("urn:Contacts", ""); // set qualified name from xml specification
	private static final String SERVICE_URI = "http://localhost:8182" + SERVICE_PATH; // initialise service uri to match service

	static TextArea textArea = new TextArea(49,80); // set up text area to display results
	
	static JTextField phonetf = new JTextField(20); // set up text fields
	static JTextField firsttf = new JTextField(20);
	static JTextField lasttf = new JTextField(20);
	static JTextField streettf = new JTextField(20);
	static JTextField towntf = new JTextField(20);
	static JTextField posttf = new JTextField(20);
	
	static JFrame frame = new JFrame("Address book interface"); // initialise new window to house the gui

	// Creating the panel at bottom and adding components
	static JPanel panel = new JPanel(); //initialise new j panel to house the gui components

	static JLabel phone = new JLabel("Phone no"); // set up labels for text fields 
	static JLabel firstName = new JLabel("First name");
	static JLabel lastName = new JLabel("Last name");
	static JLabel street = new JLabel("Street");
	static JLabel town = new JLabel("Town");
	static JLabel post = new JLabel("Postcode");

	static JButton search = new JButton("Search"); // set up buttons for actions
	static JButton clear = new JButton("Clear");
	static JButton showAll = new JButton("Show all");
	static JButton add = new JButton("Add");
	static JButton update = new JButton("Update Contact");
	static JButton delete = new JButton("Delete");
	
	Transformer messageTransformer;
	private static Dispatch<Source> serviceDispatcher;

	public ContactClient() throws Exception { 
		Service service = Service.create(SERVICE_QNAME);
		service.addPort(SERVICE_QNAME, HTTPBinding.HTTP_BINDING, SERVICE_URI);
		serviceDispatcher = service.createDispatch(SERVICE_QNAME, Source.class, Service.Mode.MESSAGE);
		messageTransformer = TransformerFactory.newInstance().newTransformer();
	}

	/**
	 * Call course service with given method for contact phone, first, last name, address including street, town, post code
	 * 
	 * @param method    HTTP method ("DELETE"/"GET"/"POST"/"PUT")
	 * @param arguments contact phone number, first, last name, street, town, post code with parameters used as follows:
	 *                  "DELETE",<phone> (delete contact)
	 *                  "GET" (list all contacts)
	 *                  "GET",<phone> (one contact) 
	 *                  "POST",<phone>,<firstname>,<lastname>,<street>,<town>,<postcode> (create contact)
	 *                  "PUT",<phone>,<firstname>,<lastname>,<street>,<town>,<postcode> (update contact)
	 */
	private void invoke(String method, String... arguments) {
		try {
			String phone = ""; // initialise empty string variables for details
			String first = "";
			String last = "";
			String street = "";
			String town = "";
			String post = "";
			
			switch (arguments.length) { // switch to assign method arguments to the variables
			case 6:
				post = arguments[5];
			case 5:
				town = arguments[4];
			case 4:
				street = arguments[3];
			case 3:
				last = arguments[2];
			case 2:
				first = arguments[1];
			case 1:
				phone = arguments[0];
			}
			
			String contacts = "<contacts/>"; // contacts closing tag
			
			if (method.equals("DELETE")) { // if method is DELETE print contact deleted with phone number
				System.out.println("delete contact:   " + phone);
				
			} else if (method.equals("GET")) { // if  only GET print all or if phone number given print that too
				System.out.println("retrieve contact: " + (phone.equals("") ? "All" : phone));
				
			} else if (method.equals("POST")) { // if POST, set output contacts string to formatted contacts details with xml tags
				System.out.println("create contact:   " + phone + ", " + first + " " + last + ", " + street + ", " + town + ", " + post);
				
				contacts = "<contacts><contact>" +
					"<phone>" + phone + "</phone>" +
					"<firstname>" + first + "</firstname>" +
					"<lastname>" + last + "</lastname>" +
					"<address>" + 
					"<street>" + street + "</street>" +
					"<town>" + town + "</town>" +
					"<postcode>" + post + "</postcode>" +
					"</address>" +
					"</contact></contacts>"; // contacts string to formatted contacts details with xml tags
				
				
			} else if (method.equals("PUT")) { // if PUT, set output contacts string to formatted contacts details with xml tags
				System.out.println("update contact:   " + phone + ", " + first + " " + last + ", " + street + ", " + town + ", " + post);
				
				contacts = "<contacts><contact>" +
						"<phone>" + phone + "</phone>" +
						"<firstname>" + first + "</firstname>" +
						"<lastname>" + last + "</lastname>" +
						"<address>" + 
						"<street>" + street + "</street>" +
						"<town>" + town + "</town>" +
						"<postcode>" + post + "</postcode>" +
						"</address>" + 
						"</contact></contacts>"; // contacts string to formatted contacts details with xml tags
			} else
				throw (new Exception("unrecognised method: " + method)); // invalid method throw exception

			Source requestSource = new StreamSource(new StringReader(contacts)); // initialise reader for XML source
			Map<String, Object> requestContext = serviceDispatcher.getRequestContext(); 
			requestContext.put(MessageContext.HTTP_REQUEST_METHOD, method);
			requestContext.put(MessageContext.PATH_INFO, SERVICE_PATH + phone);
			Source responseSource = serviceDispatcher.invoke(requestSource); 

			if (method.equals("GET")) { // if method = get
				DOMResult domXML = new DOMResult();
				messageTransformer.transform(responseSource, domXML);
				Node topNode = domXML.getNode();
				
				Element contactsNode = (Element) domXML.getNode().getFirstChild(); // get child of top node - <contacts>
				NodeList contactNodeList = contactsNode.getChildNodes(); // node list storing all contact nodes <contact>
				
				for (int i = 0; i < contactNodeList.getLength(); i++) { // for every contact do..
					
					Element contact = (Element) contactNodeList.item(i); // variable storing contact element i
					
					NodeList contactChildNodes = contact.getChildNodes(); // node list storing all contact details nodes e.g. <firstname>
					
					if(contactChildNodes.item(0).getNodeType() != Node.TEXT_NODE) { // if the first item in the node list is not a text node
					NodeList addressChildList = contactChildNodes.item(3).getChildNodes();
					String output = 	
							contactChildNodes.item(0).getFirstChild().getNodeValue() +", "+ // set position 0 of contact detail array to phone number, retrieved from the node list storing contact details
						contactChildNodes.item(1).getFirstChild().getNodeValue()+", "+  // same process repeated for the rest of entries 
						contactChildNodes.item(2).getFirstChild().getNodeValue() +", "+ 
						addressChildList.item(0).getFirstChild().getNodeValue()+", "+ 
						addressChildList.item(1).getFirstChild().getNodeValue()+", "+ 
						addressChildList.item(2).getFirstChild().getNodeValue() + "\n";
						
							
					if(i > 0) { // if there is more than one contact then append output string to text area
						textArea.append(output); 
						System.out.println(output);
					}
					else { // if only one contact then set text
						textArea.setText(output);
						System.out.println(output);
					}
					}
				}
			}
		} catch (Exception exception) {
			if (!(exception instanceof WebServiceException))
				exception.printStackTrace();
		}

		// analyse HTTP response
		Map<String, Object> responseContext = serviceDispatcher.getResponseContext();
		int httpResponse2 = (Integer) responseContext.get(MessageContext.HTTP_RESPONSE_CODE); // variable storing the http response of the current action, from the service

		
		if (method.equals("GET")) { // if method is GET
			if (httpResponse2 == 404) { // and if htttp response is 404
				textArea.setText("Contact not found."); // output not found to text area
			}
		}
		else if (method.equals("POST")) { //same as above but for post,  
			if (httpResponse2 == 405) {// checking 405, not allowed http response
				textArea.setText("A contact with this number already exists.");
			}
			else if(httpResponse2 == 201) { // if created successfully
				textArea.setText("Contact added.");
				phonetf.setText(""); // clear text fields 
				firsttf.setText("");
				lasttf.setText("");
				streettf.setText("");
				towntf.setText("");
				posttf.setText("");
			}
		}
		else if(method.equals("PUT")) { // if method PUT
			if (httpResponse2 == 405 || httpResponse2 == 404) { // if contact not found
				textArea.setText("Contact does not exist."); // error message to text area
			}
			else if (httpResponse2 == 200) { // if updated ok
				textArea.setText("Updated successfully."); // success message to text area
				phonetf.setText("");// clear text fields
				firsttf.setText("");
				lasttf.setText("");
				streettf.setText("");
				towntf.setText("");
				posttf.setText("");
			}
			
		}
		else if (method.equals("DELETE")) { // if method DELETE 
			if(httpResponse2 == 404) {// if contact not found 
				textArea.setText("Contact does not exist"); // error to text area
			}
			else if(httpResponse2 == 200) { // if removed ok
				textArea.setText("Contact deleted."); // success to text area
				phonetf.setText(""); // clear text fields 
				firsttf.setText("");
				lasttf.setText("");
				streettf.setText("");
				towntf.setText("");
				posttf.setText("");
			}
		}
	}

	public static void main(String arguments[]) throws Exception {

		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // close program on window close
		frame.setSize(610, 800); //  set frame size
		frame.getContentPane().add(BorderLayout.NORTH, panel);
		frame.setVisible(true); // set windows visible
		
		// add all GUI components to the panel
		panel.add(phone); 
		panel.add(phonetf);

		panel.add(firstName);
		panel.add(firsttf);

		panel.add(lastName);
		panel.add(lasttf);
		
		panel.add(street);
		panel.add(streettf);
		
		panel.add(town);
		panel.add(towntf);
		
		panel.add(post);
		panel.add(posttf);

		panel.add(search);
		
		panel.add(showAll);
		panel.add(add);
		panel.add(update);
		panel.add(delete);
		panel.add(clear);
		
		textArea.setEditable(false); // set text area not editable
		panel.add(textArea);

		ContactClient client = new ContactClient(); // create new contact client object
		client.invoke("POST","1", "John", "Smith", "1 Street", "Town", "AB12 0XY");  // add some example data to hash map using POST
		client.invoke("POST","2", "first", "last", "st", "twn", "post"); 
		client.invoke("POST","3", "firstname", "lastname", "2 Street", "Town", "AB12 0XY"); 

		search.addActionListener(new ActionListener() { //  action listener for search button
			public void actionPerformed(ActionEvent e) {
				if (!phonetf.getText().equals("") ) { // if phone test field is not empty
					
					if (phonetf.getText().matches("[0-9]+")) { // if phone test field is only digits
						
						client.invoke("GET", phonetf.getText()); // get contact details relating to given phone number
					}
					else {
						textArea.setText("Phone number must use Digits 0-9 only."); // only digits warning message
					}
				}
				else {
					textArea.setText("Enter phone number."); // empty phone field warning message
				}
			}
		});

		showAll.addActionListener(new ActionListener() { // action listener for show all button
			public void actionPerformed(ActionEvent e) {
				
				client.invoke("GET"); // get all contacts
			}
		});

		add.addActionListener(new ActionListener() { // action listener for add button
			public void actionPerformed(ActionEvent e) {
				// if all text fields are filled in
				if (!phonetf.getText().equals("") && !firsttf.getText().equals("") && !lasttf.getText().equals("") && !streettf.getText().equals("") && !towntf.getText().equals("") && !posttf.getText().equals("")) {
					
					if(phonetf.getText().matches("[0-9]+")) {// if only digits in phone text field
						//post/create contact with given contact details gathered from texts fields 
						client.invoke("POST", phonetf.getText().toString(), firsttf.getText().toString(),lasttf.getText().toString(), streettf.getText().toString(), towntf.getText().toString(), posttf.getText().toString());
					
					} else {
						textArea.setText("Phone number must use Digits 0-9 only."); // only digits warning message
					}
				} else {
					textArea.setText("Please fill in all fields"); // empty field warning message

				}
			}
		});

		update.addActionListener(new ActionListener() { // action listener for update button
			public void actionPerformed(ActionEvent e) {
				// if all text fields are filled in
				if (!phonetf.getText().equals("") && !firsttf.getText().equals("") && !lasttf.getText().equals("") && !streettf.getText().equals("") && !towntf.getText().equals("") && !posttf.getText().equals("")) {
					//put/update given contact details gathered from texts fields 
					client.invoke("PUT", phonetf.getText().toString(), firsttf.getText().toString(),lasttf.getText().toString(), streettf.getText().toString(), towntf.getText().toString(), posttf.getText().toString());

				} else {
					textArea.setText("Please fill in all fields");// empty field warning message
				}
			}
		});

		delete.addActionListener(new ActionListener() { // action listener for delete button
			public void actionPerformed(ActionEvent e) {
				
				if (!phonetf.getText().equals("")) { // if phone text field filled in
					
					if(phonetf.getText().matches("[0-9]+")) { // if only digits used
						
						client.invoke("DELETE", phonetf.getText().toString()); // delete contact with given phone number
					
					}else {
						textArea.setText("Phone number must use Digits 0-9 only.");// only digits warning message
					}
				} else {
					textArea.setText("Enter phone number.");// empty phone field warning message
				}
			}
		});
		
		clear.addActionListener(new ActionListener() { // action listener for delete button
			public void actionPerformed(ActionEvent e) {
				
				phonetf.setText(""); // clear text fields 
				firsttf.setText("");
				lasttf.setText("");
				streettf.setText("");
				towntf.setText("");
				posttf.setText("");
			}
		});
	}
}
