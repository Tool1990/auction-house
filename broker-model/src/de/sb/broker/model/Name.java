package de.sb.broker.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;

@Embeddable
public class Name {

    @Column(name = "familyName", nullable = false)
    @NotNull
    @XmlElement
    @Size(min = 1, max = 31)
    private String family;

    @Column(name = "givenName", nullable = false)
    @NotNull
    @XmlElement
    @Size(min = 1, max = 31)
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
