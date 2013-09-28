package tokenym.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface TokenymServiceAsync
{
    void getToken (String password, String generatedToken, AsyncCallback<Boolean> callback) throws IllegalArgumentException;

    void getID (String token, AsyncCallback<String> callback) throws IllegalArgumentException;

    void sendEmail (String email, AsyncCallback<String> callback) throws IllegalArgumentException;

    void generateNewKeyboard (String wat, AsyncCallback<String> callback) throws IllegalArgumentException;

    void veridemPlusGetToken (String token, String encryptedTokenym, String keyboard, String email, String generatedToken, AsyncCallback<Boolean> callback)
            throws IllegalArgumentException;
    
}
