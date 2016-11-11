package de.sb.broker.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;

@Embeddable
public class Address {

	@Column(nullable = true)
	@XmlElement
	private String street;

	@Column(nullable = true)
	@XmlElement
	private String postCode;

	@Column(nullable = false)
	@NotNull
	@XmlElement
	private String city;
	
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getPostCode() {
		return postCode;
	}
	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
}
