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

package eu.masconsult.bgbanking.banks.dskbank;

import java.io.IOException;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;

import eu.masconsult.bgbanking.banks.BankClient;
import eu.masconsult.bgbanking.banks.RawBankAccount;

public class DskClient implements BankClient {

    @Override
    public String authenticate(String username, String password) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<RawBankAccount> getBankAccounts(String authtoken) throws IOException,
            ParseException, AuthenticationException {
        // TODO Auto-generated method stub
        return null;
    }

}
