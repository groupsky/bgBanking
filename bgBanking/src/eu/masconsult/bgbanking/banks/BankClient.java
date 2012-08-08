
package eu.masconsult.bgbanking.banks;

public interface BankClient {

    /**
     * Authenticates with the banking service and returns authToken/cookie for
     * use in following operations.
     * 
     * @param username username to use for authentication
     * @param password password to use for authentication
     * @return authToken that represents successful authentication. A
     *         <code>null</code> value means authentication failed.
     */
    String authenticate(String username, String password);

}
