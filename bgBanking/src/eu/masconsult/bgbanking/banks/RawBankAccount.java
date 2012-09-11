/*******************************************************************************
 * Copyright (c) 2012 MASConsult Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package eu.masconsult.bgbanking.banks;

/**
 * Represents a low-level bank account
 * 
 * @author Geno Roupsky
 */
final public class RawBankAccount {

    private String serverId;
    private String name;
    private String iban;
    private String currency;
    private float balance;
    private float availableBalance;
    private String lastTransaction;

    public String getServerId() {
        return serverId;
    }

    public RawBankAccount setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public String getName() {
        return name;
    }

    public RawBankAccount setName(String name) {
        this.name = name;
        return this;
    }

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

    public String getLastTransaction() {
        return lastTransaction;
    }

    public RawBankAccount setLastTransaction(String lastTransaction) {
        this.lastTransaction = lastTransaction;
        return this;
    }

    @Override
    public String toString() {
        return "RawBankAccount [serverId=" + serverId + ", name=" + name + ", iban=" + iban
                + ", currency=" + currency + ", balance=" + balance + ", availableBalance="
                + availableBalance + ", lastTransaction=" + lastTransaction + "]";
    }

}
