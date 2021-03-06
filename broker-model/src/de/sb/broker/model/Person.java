package de.sb.broker.model;

import org.eclipse.persistence.annotations.CacheIndex;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Person", schema = "broker")
@DiscriminatorValue(value = "Person")
@PrimaryKeyJoinColumn(name = "personIdentity")
public class Person extends BaseEntity {

	@Column(unique = true, nullable = false)
	@NotNull
	@Size(min = 1, max = 16)
	@XmlElement
	@CacheIndex(updateable = true)
	private String alias;

	@Column(nullable = false)
	@NotNull
	@Size(min = 32, max = 32)
	@XmlTransient
	private byte[] passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "GroupAlias", nullable = false)
	@NotNull
	@XmlElement
	@Valid
	private Group group;

	@Embedded
	@NotNull
	@Valid
	@XmlElement
	private Name name;

	@Embedded
	@NotNull
	@Valid
	@XmlElement
	private Contact contact;

	@Embedded
	@NotNull
	@Valid
	@XmlElement
	private Address address;

	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "documentReference")
	private Document document;

	@OneToMany(mappedBy = "seller")
	private Set<Auction> auctions;

	@OneToMany(mappedBy = "bidder")
	private Set<Bid> bids;

	public enum Group {
		ADMIN, USER
	}

	public Person() {
		super();
		this.alias = "";
		this.passwordHash = getHash("".getBytes());
		this.group = Group.USER;
		this.name = new Name();
		this.contact = new Contact();
		this.address = new Address();
		this.auctions = new HashSet<>();
		this.bids = new HashSet<>();
	}

	public Document getAvatar() {
		return document;
	}

	public void setAvatar(Document document) {
		this.document = document;
	}

	public byte[] getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(byte[] passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Name getName() {
		return name;
	}

	public Contact getContact() {
		return contact;
	}

	public Address getAddress() {
		return address;
	}

	public Set<Auction> getAuctions() {
		return auctions;
	}

	public Set<Bid> getBids() {
		return bids;
	}

	public long getDocumentReference() {
		return getAvatar() == null ? 0 : getAvatar().getIdentity();
	}
}
