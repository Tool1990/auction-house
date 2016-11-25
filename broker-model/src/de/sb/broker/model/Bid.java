package de.sb.broker.model;

import de.sb.java.validation.Inequal;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@Entity
@Table(name = "Bid", schema = "broker", uniqueConstraints = @UniqueConstraint(columnNames = {"bidderReference", "auctionReference"}))
@DiscriminatorValue(value = "Bid")
@PrimaryKeyJoinColumn(name = "bidIdentity")
@XmlType
@XmlRootElement
@Inequal(leftAccessPath = {"auction", "seller", "identity"}, rightAccessPath = {"bidder", "identity"}, operator = Inequal.Operator.NOT_EQUAL)
@Inequal(leftAccessPath = "price", rightAccessPath = {"auction", "askingPrice"}, operator = Inequal.Operator.GREATER)
public class Bid extends BaseEntity {

	@Min(1)
	@Column(nullable = false)
	@XmlElement
	private long price;

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(name = "auctionReference")
	private Auction auction;

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(name = "bidderReference")
	private Person bidder;

	public Bid(Auction auction, Person bidder) {
		super();

		this.price = 0;
		this.auction = auction;
		this.bidder = bidder;
	}

	protected Bid() {
		this(null, null);
	}

	public Auction getAuction() {
		return auction;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	public Person getBidder() {
		return bidder;
	}

	public long getAuctionReference() {
		return auction == null ? 0 : auction.getIdentity();
	}

	public long getBidderReference() {
		return bidder == null ? 0 : bidder.getIdentity();
	}
}
