package jbdl.splitwise.expenses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import jbdl.splitwise.users.AppUser;

/** How much of one expense one person owes: "Ravi's share of Dinner is 300.00". */
@Entity
@Table(indexes = @Index(name = "idx_share_user", columnList = "user_id"))
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Expense expense;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected ExpenseShare() {
    }

    ExpenseShare(Expense expense, AppUser user, BigDecimal amount) {
        this.expense = expense;
        this.user = user;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public AppUser getUser() {
        return user;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
