package de.sb.broker.model;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Entity
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlType
@Table(name = "BaseEntity", schema = "broker")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
@XmlSeeAlso({Person.class, Auction.class, Bid.class, Document.class})
public abstract class BaseEntity implements Comparable<BaseEntity> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@XmlElement
	private long identity;

	@Version
	@Min(1)
	@XmlElement
	private int version;

	@Column(nullable = false)
	@XmlElement
	private long creationTimestamp;

	public BaseEntity() {
		this.version = 1;
		this.creationTimestamp = System.currentTimeMillis();
	}

	public int compareTo(BaseEntity e) {
		return Long.compare(this.identity, e.identity);
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public long getIdentity() {
		return identity;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	static public byte[] getHash(byte[] bytes) {
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(bytes);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("NoSuchAlgorithmException: " + e.getStackTrace());
		}
		return messageDigest.digest();
	}
}
