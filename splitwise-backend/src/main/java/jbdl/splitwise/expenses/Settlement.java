package jbdl.splitwise.expenses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import jbdl.splitwise.groups.ExpenseGroup;
import jbdl.splitwise.users.AppUser;

/**
 * A repayment inside a group: "Ravi paid Asha 300.00". Recorded as its own row (never by editing
 * old expenses), so the history always explains every balance.
 */
@Entity
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ExpenseGroup group;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser fromUser;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser toUser;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant createdAt;

    protected Settlement() {
    }

    public Settlement(ExpenseGroup group, AppUser fromUser, AppUser toUser, BigDecimal amount) {
        this.group = group;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public AppUser getFromUser() {
        return fromUser;
    }

    public AppUser getToUser() {
        return toUser;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
