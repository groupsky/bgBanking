
package eu.masconsult.bgbanking.banks;

/**
 * Represents a low-level bank account
 * 
 * @author Geno Roupsky
 */
final public class RawBankAccount {

    private String iban;
    private String currency;
    private float balance;
    private float availableBalance;

    public String getIBAN() {
        return iban;
    }

    public RawBankAccount setIBAN(String iban) {
        this.iban = iban;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public RawBankAccount setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public float getBalance() {
        return balance;
    }

    public RawBankAccount setBalance(float balance) {
        this.balance = balance;
        return this;
    }

    public float getAvailableBalance() {
        return availableBalance;
    }

    public RawBankAccount setAvailableBalance(float availableBalance) {
        this.availableBalance = availableBalance;
        return this;
    }

    @Override
    public String toString() {
        return "RawBankAccount [iban=" + iban + ", currency=" + currency + ", balance=" + balance
                + ", availableBalance=" + availableBalance + "]";
    }

}
