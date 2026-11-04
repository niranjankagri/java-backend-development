package jbdl.splitwise.expenses;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jbdl.splitwise.groups.ExpenseGroup;
import jbdl.splitwise.users.AppUser;

/**
 * One payment made by one member for the group, e.g. "Dinner, 900.00, paid by Asha". Its shares say
 * how much of it each person owes. Money is BigDecimal with 2 decimals, never double (Session 1).
 */
@Entity
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ExpenseGroup group;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2) // up to 9,999,999,999.99
    private BigDecimal amount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser paidBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SplitType splitType;

    @Column(nullable = false)
    private Instant createdAt;

    /**
     * The shares belong to the expense (they make no sense alone): cascade ALL saves and deletes them
     * with it, orphanRemoval deletes a share removed from this list.
     */
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseShare> shares = new ArrayList<>();

    protected Expense() {
    }

    public Expense(ExpenseGroup group, String description, BigDecimal amount, AppUser paidBy, SplitType splitType) {
        this.group = group;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.createdAt = Instant.now();
    }

    /** Adds a share and links it back to this expense (both sides of the relationship). */
    public void addShare(AppUser user, BigDecimal shareAmount) {
        shares.add(new ExpenseShare(this, user, shareAmount));
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public AppUser getPaidBy() {
        return paidBy;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ExpenseShare> getShares() {
        return shares;
    }
}
