package jbdl.splitwise.expenses;

/**
 * How an expense is divided among the people sharing it. Stored by NAME in the database
 * (@Enumerated(EnumType.STRING)), so adding a new type later can't change the meaning of old rows.
 */
public enum SplitType {
    /** Everyone pays the same: 900 among 3 people = 300 each. */
    EQUAL,
    /** Exact amounts per person, which must add up to the total: 500 + 400 = 900. */
    EXACT,
    /** Percentages per person, which must add up to 100: 50 % + 50 %. */
    PERCENT
}
