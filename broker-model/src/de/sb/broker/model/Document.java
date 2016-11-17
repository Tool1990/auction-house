package de.sb.broker.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * Created by koschall on 17/11/2016.
 */
@Entity
@Table(name="Document", schema = "broker")
@DiscriminatorValue(value = "Document")
@PrimaryKeyJoinColumn(name = "documentIdentity")
public class Document extends BaseEntity {

    @Column(name ="type", nullable = false)
    @NotNull
    @XmlElement
    private String type;

    @Column(name ="content", nullable = false)
    @NotNull
    @XmlElement
    private byte[] content;

    @Column(name ="documentHash", nullable = false)
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

    public void setPerson(Person person) {
        this.persons.add(person);
    }

    public Document(String type, byte[] content) {
        super();
        this.type = type;
        this.content = content;
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

    static public byte[] documentHash(byte[] content){
        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException: " + e.getStackTrace());
        }

        messageDigest.update(content);
        return messageDigest.digest();
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
