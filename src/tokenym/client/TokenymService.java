package tokenym.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("greet")
public interface TokenymService extends RemoteService
{
    Boolean getToken (String password, String generatedToken) throws IllegalArgumentException;

    String getID (String token) throws IllegalArgumentException;

    String sendEmail (String emailAddress) throws IllegalArgumentException;

    String generateNewKeyboard (String wat) throws IllegalArgumentException;

    Boolean veridemPlusGetToken (String token, String encryptedTokenym, String keyboard, String email, String generatedToken) throws IllegalArgumentException;
    
}
