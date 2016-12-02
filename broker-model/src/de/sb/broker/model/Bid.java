package de.sb.broker.model;

import de.sb.java.validation.Inequal;
import org.glassfish.jersey.message.filtering.EntityFiltering;

import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Entity
@Table(name = "Bid", schema = "broker", uniqueConstraints = @UniqueConstraint(columnNames = {"bidderReference", "auctionReference"}))
@DiscriminatorValue(value = "Bid")
@PrimaryKeyJoinColumn(name = "bidIdentity")
@XmlType
@XmlRootElement
@Inequal(leftAccessPath = {"auction", "seller", "identity"}, rightAccessPath = {"bidder", "identity"}, operator = Inequal.Operator.NOT_EQUAL)
//@Inequal(leftAccessPath = "price", rightAccessPath = {"auction", "askingPrice"}, operator = Inequal.Operator.GREATER)
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

	public Bid() {
		this(null, null);
	}

	@XmlAuctionAsEntityFilter
	public Auction getAuction() {
		return auction;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	@XmlBidderAsEntityFilter
	public Person getBidder() {
		return bidder;
	}

	@XmlAuctionAsReferenceFilter
	public long getAuctionReference() {
		return auction == null ? 0 : auction.getIdentity();
	}

	@XmlBidderAsReferenceFilter
	public long getBidderReference() {
		return bidder == null ? 0 : bidder.getIdentity();
	}

	/**
	 * Filter annotation for associated bidders marshaled as entities.
	 */
	@Target({TYPE, METHOD, FIELD})
	@Retention(RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlBidderAsEntityFilter {
		static final class Literal extends AnnotationLiteral<XmlBidderAsEntityFilter> implements XmlBidderAsEntityFilter {
		}
	}

	/**
	 * Filter annotation for associated bidders marshaled as references.
	 */
	@Target({TYPE, METHOD, FIELD})
	@Retention(RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlBidderAsReferenceFilter {
		static final class Literal extends AnnotationLiteral<XmlBidderAsReferenceFilter> implements XmlBidderAsReferenceFilter {
		}
	}

	/**
	 * Filter annotation for associated auctions marshaled as entities.
	 */
	@Target({TYPE, METHOD, FIELD})
	@Retention(RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlAuctionAsEntityFilter {
		static final class Literal extends AnnotationLiteral<XmlAuctionAsEntityFilter> implements XmlAuctionAsEntityFilter {
		}
	}

	/**
	 * Filter annotation for associated auctions marshaled as references.
	 */
	@Target({TYPE, METHOD, FIELD})
	@Retention(RUNTIME)
	@EntityFiltering
	@SuppressWarnings("all")
	static public @interface XmlAuctionAsReferenceFilter {
		static final class Literal extends AnnotationLiteral<XmlAuctionAsReferenceFilter> implements XmlAuctionAsReferenceFilter {
		}
	}
}
