package de.sb.broker.model;

import de.sb.java.validation.Inequal;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "Bid", schema = "broker", uniqueConstraints = @UniqueConstraint(columnNames = {"bidderReference", "auctionReference"}))
@DiscriminatorValue(value = "Bid")
@PrimaryKeyJoinColumn(name = "bidIdentity")
@Inequal(leftAccessPath = {"auction", "seller", "identity"}, rightAccessPath = {"bidder", "identity"}, operator = Inequal.Operator.NOT_EQUAL)
@Inequal(leftAccessPath = "price", rightAccessPath = {"auction", "askingPrice"}, operator = Inequal.Operator.GREATER)
public class Bid extends BaseEntity {

	@Min(0)
	@Column(nullable = false)
	private long price;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auctionReference")
	@NotNull
	private Auction auction;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bidderReference")
	@NotNull
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
