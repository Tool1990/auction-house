package de.sb.broker.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "Document", schema = "broker")
@DiscriminatorValue(value = "Document")
@PrimaryKeyJoinColumn(name = "documentIdentity")
@XmlType
@XmlRootElement
public class Document extends BaseEntity {

    @Column(name = "type", nullable = false)
    @NotNull
    @XmlElement
    @Size(min = 1, max = 16)
    private String type;

    @Column(name = "content", nullable = false)
    @NotNull
    @XmlElement
    private byte[] content;

    @Column(name = "documentHash", nullable = false)
    @NotNull
    @XmlElement
    private byte[] hash;

    @OneToMany(mappedBy = "document")
    private Set<Person> persons;

    public Document() {
        super();
    }

    public Set<Person> getPersons() {
        return persons;
    }


    public Document(String type, byte[] content) {
        super();
        this.type = type;
        this.content = content;
        this.hash = getHash(content);
        persons = new HashSet<>();
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public byte[] getHash() {
        return hash;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
