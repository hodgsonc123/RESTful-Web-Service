
public class Person {

	private String phone;
	private String first;
	private String last;
	private String address;

	public Person(String phone, String first, String last, String address) {

		this.phone = phone;
		this.first = first;
		this.last = last;
		this.address = address;
	}

	public String getPhone() {

		return phone;
	}

	public String getFirst() {

		return first;
	}

	public String getLast() {

		return last;
	}

	public String getAddress() {

		return address;
	}
	
	public String getFormattedByPhone(String phone) {
		
		return phone + ", " + first + " " + last + ", " + address;
		
	}

}
