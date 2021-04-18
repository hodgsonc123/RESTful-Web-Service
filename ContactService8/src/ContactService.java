import java.io.StringReader;

import javax.annotation.Resource;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import javax.xml.ws.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.Service.Mode;

import org.w3c.dom.*;

/**
  RESTful web service to manage a list of contacts.
*/

@WebServiceProvider
@ServiceMode(Mode.MESSAGE)
public class ContactService implements Provider<Source>{

  HashMap<String,String[]> contactsMap = new HashMap<String,String[]>(); // HashMap storing list of contacts mapping unique phone number to an array of contact details
  private JAXBContext jaxbContext; 
  MessageContext requestContext;
  Transformer messageTransformer;

  @Resource(type=Object.class)
  protected WebServiceContext serviceContext;

  private static final String serviceURI = "http://localhost:8182/contacts/"; // initialise service uri

  /**
   * ContactService constructor
   * creates new 
  */
  public ContactService() throws Exception {
    jaxbContext = JAXBContext.newInstance(ContactService.class); // create new instance of the 
    messageTransformer = TransformerFactory.newInstance().newTransformer(); //initialise transformer to transform a source tree into a results tree
  }

  /**
    Return XML text for currently defined contacts in the form
    "<contacts>
    	<contact>
    		<phone>phone num</phone>
			<firstname>first</firstname>
			<lastname>last</lastname>
			<address>
				<street>street</street>
				<town>town</town>
				<postcode>post</postcode>
			</address>
		</contact>
	</contacts>", 
    
    @param phone - phone number of a contact
    @return		XML text for defined contacts
  */
	private String getContacts(String phone) {
		
		String contacts; // declare string to store contact details 
		if (phone != null) { // if value given for phone
			String[] contact = contactsMap.get(phone); // string array storing contact details retrieved from the hashmap
			if (contact != null) { // if there are contact details in the contact array
				String num = contact[0]; // assign individual contact details from array to variables
				String first = contact[1];
				String last = contact[2];
				String street = contact[3];
				String town = contact[4];
				String post = contact[5];
				
				contacts = "<contacts><contact>" 
						+ "<phone>" + num + "</phone>"
						+ "<firstname>" + first + "</firstname>"
						+ "<lastname>" + last + "</lastname>"
						+ "<address>"
						+ "<street>" + street + "</street>"
						+ "<town>" + town + "</town>"
						+ "<postcode>" + post + "</postcode>"
						+ "</address>"
						+ "</contact></contacts>"; // format output string with the details of the contact surrounded int he appropriate xml tags
			}else
				contacts = null; // if there are no contacts details in the array set output string to null
			
		} else { // if there is no phone number given in parameters, output all contacts using while loop
			contacts = "<contacts>"; // root contacts xml tag
			Iterator<String> contactIterator = contactsMap.keySet().iterator(); // set up hash map iterator
			while (contactIterator.hasNext()) { // while iterator has another contact
				phone = (String) contactIterator.next(); // move iterator to next
				
				String[] contact = contactsMap.get(phone); // get contact details associated with given phone number
				String num = contact[0]; // assign contact details to individual variables
				String first = contact[1];
				String last = contact[2];
				String street = contact[3];
				String town = contact[4];
				String post = contact[5];
				
				contacts = contacts + "<contact>" // add contact details to output string with xml tags
				+ "<phone>" + num + "</phone>"
						+ "<firstname>" + first + "</firstname>"
						+ "<lastname>" + last + "</lastname>"
						+ "<address>"
						+ "<street>" + street + "</street>"
						+ "<town>" + town + "</town>"
						+ "<postcode>" + post + "</postcode>"
						+ "</address>"
						+ "</contact>";
			}
			contacts += "</contacts>"; // contacts xml closing root tag
		}
		//System.out.println(contacts);
		return (contacts); // return output string storing results of get formatted in xml
	}

  /**
    Respond to service invocation.
    @param source		service data source
  */
	public Source invoke(Source source) {

		String contacts = "<contacts/>";
		int httpResponse = 200; // assume success
		MessageContext requestContext = serviceContext.getMessageContext(); // variable storing request message context
		String method = (String) requestContext.get(MessageContext.HTTP_REQUEST_METHOD); // variable storing the method e.g. DELETE, GET etc
		String phone = (String) requestContext.get(MessageContext.PATH_INFO); // variable storing the phone number used for action

		// analyse service call
		try {
			if (method.equals("DELETE")) { // delete/remove contact from the hash map of the given phone number
				String[] deleted = contactsMap.remove(phone);
				if (deleted == null) // if contact doesnt exist, return http response code 404 not found
					httpResponse = 404;
			} else if (method.equals("GET")) { // get contact from hash map for given phone number
				String contactList = getContacts(phone);
				if (contactList != null) // if contacts exists, set contacts to the retrieved contact details
					contacts = contactList;
				else
					httpResponse = 404; // contact doesnt exist, return 404 not found http response
			} else if (method.equals("POST")) { // create new contact
				if (!contactsMap.containsKey(phone)) { // if phone number doesnt exist in hash map
					DOMResult domXML = new DOMResult(); //initialise new DOMResult object to store transformed tree
					messageTransformer.transform(source, domXML);
					Element contactsElement = (Element) domXML.getNode().getFirstChild(); // root node all contacts <contacts>
					Element contactElement = (Element) contactsElement.getFirstChild(); // a contact node <contact>
					NodeList contactChildList = contactElement.getChildNodes(); // contact detail node list e.g. <firstname>

					String[] contact = new String[6]; // declare string array to store contact details
					contact[0] = contactChildList.item(0).getFirstChild().getNodeValue(); // set position 0 of contact detail array to phone number, retrieved from the node list storing contact details
					contact[1] = contactChildList.item(1).getFirstChild().getNodeValue(); // same process repeated for the rest of entries 
					contact[2] = contactChildList.item(2).getFirstChild().getNodeValue();
					NodeList addressChildList = contactChildList.item(3).getChildNodes();
					contact[3] = addressChildList.item(0).getFirstChild().getNodeValue();
					contact[4] = addressChildList.item(1).getFirstChild().getNodeValue();
					contact[5] = addressChildList.item(2).getFirstChild().getNodeValue();

					
					
					System.out.println("contact added: " + contact[0] + ", " + contact[1] + " " + contact[2] + ", " + contact[3] + ", " + contact[4] + ", " + contact[5]);
					contactsMap.put(phone, contact); // enter contact into hash map

					httpResponse = 201; // set http response code to created
				} else {
					httpResponse = 405; // set http response to method not allowed as contact already exist
				}
			} else if (method.equals("PUT")) { // same process as post but with subtle difference 
				if (contactsMap.containsKey(phone)) { // if the hash map DOES contain the contact then update to given details
					DOMResult domXML = new DOMResult();
					messageTransformer.transform(source, domXML);
					Element contactsElement = (Element) domXML.getNode().getFirstChild();
					Element contactElement = (Element) contactsElement.getFirstChild();
					NodeList contactChildList = contactElement.getChildNodes();
					
					String[] contact = new String[6];
					contact[0] = contactChildList.item(0).getFirstChild().getNodeValue(); // set position 0 of contact detail array to phone number, retrieved from the node list storing contact details
					contact[1] = contactChildList.item(1).getFirstChild().getNodeValue(); // same process repeated for the rest of entries 
					contact[2] = contactChildList.item(2).getFirstChild().getNodeValue();
					NodeList addressChildList = contactChildList.item(3).getChildNodes();
					contact[3] = addressChildList.item(0).getFirstChild().getNodeValue();
					contact[4] = addressChildList.item(1).getFirstChild().getNodeValue();
					contact[5] = addressChildList.item(2).getFirstChild().getNodeValue();
					
					
					System.out.println("contact updated: " + contact[0] + ", " + contact[1] + " " + contact[2] + ", " + contact[3] + ", " + contact[4] + ", " + contact[5]);
					contactsMap.put(phone, contact);
					httpResponse = 200; // set http response OK
				} else {
					httpResponse = 404; // set http response to not found, contact not in hash map
				}
			} else
				throw new WebServiceException("unsupported method: " + method); // unsupported method given
		} catch (Exception exception) {
			System.err.println("service invocation exception: " + exception);
		}
		requestContext.put(MessageContext.HTTP_RESPONSE_CODE, httpResponse);
		Source responseSource = new StreamSource(new StringReader(contacts));
		return (responseSource);
	}

  /**
    Main method to initialise service.
  */
  public static void main(String arguments[]) throws Exception {
    ContactService service = new ContactService(); // create new contact service object
    System.out.println("Starting service ...");
    Endpoint endpoint =	Endpoint.create(HTTPBinding.HTTP_BINDING, service);
    endpoint.publish(serviceURI);
  }

}
