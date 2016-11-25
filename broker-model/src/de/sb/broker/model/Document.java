package de.sb.broker.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name="Document", schema = "broker")
@DiscriminatorValue(value = "Document")
@PrimaryKeyJoinColumn(name = "documentIdentity")
@XmlType
@XmlRootElement
public class Document extends BaseEntity {

    @Column(name ="type", nullable = false)
    @NotNull
    @XmlElement
    @Size(min = 1, max=16)
    private String type;

    @Column(name ="content", nullable = false)
    @NotNull
    @XmlElement
    private byte[] content;

    @Column(name ="documentHash", nullable = false)
    @NotNull
    @XmlElement
    private int hash;

    @OneToMany(mappedBy = "document")
    private Set<Person> persons;

    protected Document() {
        super();
    }

    public Set<Person> getPersons() {
        return persons;
    }


    public Document(String type, byte[] content) {
        super();
        this.type = type;
        this.content = content;
        this.hash  = Document.documentHash(content);
        persons = new HashSet<Person>();
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public int getHash() {
        return hash;
    }

    static public int documentHash(byte[] content){
        return content.hashCode();
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
