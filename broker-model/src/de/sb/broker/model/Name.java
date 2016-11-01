package de.sb.broker.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.validation.constraints.NotNull;

@Embeddable
public class Name {

	@Column(name ="familyName", nullable = false)
	@NotNull
	private String family;

	@Column(name = "givenName", nullable = false)
	@NotNull
	private String given;

	public String getFamily() {
		return family;
	}
	public void setFamily(String family) {
		this.family = family;
	}
	public String getGiven() {
		return given;
	}
	public void setGiven(String given) {
		this.given = given;
	}
	
}
