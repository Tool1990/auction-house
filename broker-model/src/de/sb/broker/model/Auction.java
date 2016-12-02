package de.sb.broker.model;

import de.sb.java.validation.Inequal;
import org.glassfish.jersey.message.filtering.EntityFiltering;

import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Entity
@Table(name = "Auction", schema = "broker")
@DiscriminatorValue(value = "Auction")
@PrimaryKeyJoinColumn(name = "auctionIdentity")
@XmlType
@XmlRootElement
@Inequal(leftAccessPath = "closureTimestamp", rightAccessPath = "creationTimestamp", operator = Inequal.Operator.GREATER)
public class Auction extends BaseEntity {

    @Column(nullable = false, updatable = true, insertable = true)
    @NotNull
    @Size(min = 1, max = 255)
    @XmlElement
    private String title;

    @Column(nullable = false, updatable = true, insertable = true)
    @Min(1)
    @XmlElement
    private short unitCount;


    @Column(nullable = false, updatable = true, insertable = true)
    @Min(1)
    @XmlElement
    private long askingPrice;

    @Column(nullable = false, updatable = true, insertable = true)
    @XmlElement
    private long closureTimestamp;

    @Column(nullable = false, updatable = true, insertable = true)
    @NotNull
    @Size(min = 1, max = 8189)
    @XmlElement
    private String description;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "sellerReference")
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

    public Auction() {
        this(null);
    }


    @XmlSellerAsReferenceFilter
    public long getSellerReference() {
        return seller == null ? 0 : seller.getIdentity();
    }

    @XmlElement(name = "closed")
    public boolean isClosed() {
        return System.currentTimeMillis() > closureTimestamp ? true : false;
    }

    @XmlElement(name = "sealed")
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

    @XmlBidsAsEntityFilter
    public Set<Bid> getBids() {
        return bids;
    }

    @XmlElement
    @XmlSellerAsEntityFilter
    public Person getSeller() {
        return seller;
    }

    /**
     * Filter annotation for associated sellers marshaled as entities.
     */
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    @EntityFiltering
    @SuppressWarnings("all")
    static public @interface XmlSellerAsEntityFilter {
        static final class Literal extends AnnotationLiteral<XmlSellerAsEntityFilter> implements XmlSellerAsEntityFilter {
        }
    }

    /**
     * Filter annotation for associated sellers marshaled as references.
     */
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    @EntityFiltering
    @SuppressWarnings("all")
    static public @interface XmlSellerAsReferenceFilter {
        static final class Literal extends AnnotationLiteral<XmlSellerAsReferenceFilter> implements XmlSellerAsReferenceFilter {
        }
    }

    /**
     * Filter annotation for associated bids marshaled as entities.
     */
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    @EntityFiltering
    @SuppressWarnings("all")
    static public @interface XmlBidsAsEntityFilter {
        static final class Literal extends AnnotationLiteral<XmlBidsAsEntityFilter> implements XmlBidsAsEntityFilter {
        }
    }

}
