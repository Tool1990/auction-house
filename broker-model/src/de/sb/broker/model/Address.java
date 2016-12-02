package de.sb.broker.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Embeddable
@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class Address {

	@Column(nullable = true, updatable = true, insertable = true)
	@XmlElement
	@Size(min = 1, max = 63)
	private String street;

	@Column(nullable = true, updatable = true, insertable = true)
	@XmlElement
	@Size(min = 1, max = 15)
	private String postCode;

	@Column(nullable = true, updatable = true, insertable = true)
	@NotNull
	@XmlElement
	@Size(min = 1, max = 63)
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
