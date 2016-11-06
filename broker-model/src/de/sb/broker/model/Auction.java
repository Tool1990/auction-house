package de.sb.broker.model;

import de.sb.java.validation.Inequal;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Auction", schema = "broker")
@DiscriminatorValue(value = "Auction")
@PrimaryKeyJoinColumn(name = "auctionIdentity")
@Inequal(leftAccessPath = "closureTimestamp", rightAccessPath = "creationTimestamp", operator = Inequal.Operator.GREATER)
public class Auction extends BaseEntity {

	@Column(nullable = false)
	@NotNull
	@Size(min=1, max=255)
	private String title;

	@Column(nullable = false)
	@Min(1)
	private short unitCount;

	@NotNull
	@Column(nullable = false)
	@Min(1)
	private long askingPrice;

	@NotNull
	@Valid
	@Column(nullable = false)
	private long closureTimestamp;

	@Column(nullable = false)
	@NotNull
	@Size(min=1, max=8189)
	private String description;

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(name = "sellerReference")
	@NotNull
	private Person seller;

	@OneToMany(mappedBy = "auction")
	private Set<Bid> bids;

	public Auction(Person seller) {
		super();

		this.title = "";
		this.unitCount = 1;
		this.askingPrice = 1;
		this.closureTimestamp = System.currentTimeMillis();
		this.description = "";
		this.seller = seller;
		bids = new HashSet<>();
	}

	protected Auction() {
		this(null);
	}

	public long getSellerReference() {
		return seller == null ? 0 : seller.getIdentity();
	}

	public boolean isClosed() {
		return System.currentTimeMillis() > closureTimestamp ? true : false;
	}

	public boolean isSealed() {
		return !bids.isEmpty() || isClosed();
	}

	public Bid getBid(Person bidder) {
		for (Bid bid : bids) {
			if (bid.getBidder().equals(bidder)) {
				return bid;
			}
		}
		return null;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public short getUnitCount() {
		return unitCount;
	}

	public void setUnitCount(short unitCount) {
		this.unitCount = unitCount;
	}

	public long getAskingPrice() {
		return askingPrice;
	}

	public void setAskingPrice(long askingPrice) {
		this.askingPrice = askingPrice;
	}

	public long getClosureTimestamp() {
		return closureTimestamp;
	}

	public void setClosureTimestamp(long closureTimestamp) {
		this.closureTimestamp = closureTimestamp;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Person getSeller() {
		return seller;
	}

}
