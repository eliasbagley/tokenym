package tokenym.server;

import tokenym.client.TokenymService;
import tokenym.shared.Utils;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class TokenymServiceImpl extends RemoteServiceServlet implements TokenymService
{
    private DatastoreService datastore;
    private Cache            cache;
    private static int       gridOption = 2; // direct keyboard to grid matching

    public TokenymServiceImpl ()
    {
        this.datastore = DatastoreServiceFactory.getDatastoreService();

        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put(GCacheFactory.EXPIRATION_DELTA, 60);

        try
        {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            this.cache = cacheFactory.createCache(props);
        }
        catch (CacheException e)
        {
            System.out.println("Cache exception" + e);
        }
    }

    public Boolean getToken (String passwordAndSecret, String token) throws IllegalArgumentException
    {
        String[] passwordAndSecretArr = passwordAndSecret.split("|");
        String password = passwordAndSecretArr[0]; // fix this, so that the shared secret stuff

        // check to see if it's a valid password
        if (!Utils.isValidPassword(password, true))
            return false; // so server knows that password is bad

        String sharedSecret = "";
        for (int i = 1; i < passwordAndSecretArr.length; i++)
        {
            sharedSecret = sharedSecret + passwordAndSecretArr[i];
        }

        // create a token
        // String token = Utils.createUniqueToken(); // token is now passed in
        String tokenAndSecret = "";
        if (sharedSecret.equals(""))
            tokenAndSecret = token;
        else
            tokenAndSecret = token + "|" + sharedSecret;

        String uniqueID = null;
        try
        {
            uniqueID = Utils.hash(password);
            // token = FieldVerifier.hash(token, 21); // gives us a longer token
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        // store the token with the uniqueID in the memcache (set it to expire
        // in 30 seconds)
        cache.put(tokenAndSecret, uniqueID);
        System.out.println("Putting " + tokenAndSecret + " " + uniqueID);

        return true;

    }

    // gets the id from the memcache if a token is present
    public String getID (String token) throws IllegalArgumentException
    {
        // if it's not in the memcache, the token isn't valid, so return null

        // otherwise, just pull the ID out of the cache
        String uniqueID = (String) cache.get(token);
        cache.remove(token); // should it be removed once changed, or only removed after 60 seconds?

        if (uniqueID == null)
            return "No ID associated with this token.";

        return uniqueID;
    }

    /**
     * Escape an html string. Escaping data received from the client helps to prevent cross-site script vulnerabilities.
     * 
     * @param html
     *            the html string to escape
     * @return the escaped string
     */

    @Override
    public String sendEmail (String emailAddress) throws IllegalArgumentException
    {
        // generate a new grid
        String grid = Utils.createGrid(gridOption);
        String formattedGrid = Utils.formatKeyboard(grid);

        // generate a new token
        String token = Utils.createUniqueToken();

        // store the grid into a permanent datastore, indexed by the token
        Entity gridEntity = new Entity("Grid", token);

        gridEntity.setProperty("grid", grid); // put the unformatted grid into the database

        datastore.put(gridEntity);

        // send the formatted grid to the user
        String msgBody = "Token:\n" + token + "\n\n\n" + "Grid:\n" + formattedGrid;
        String subject = "Tokenym Grid and Token";

        // for debugging (since email won't go through
        System.out.println(msgBody);
        
        sendEmail(emailAddress, subject, msgBody);

        // this doesn't do anything, can't figure out how to get async void callback
        return token;
    }

    // uses javamail to send an email to the specific address
    public void sendEmail (String emailAddress, String subject, String body)
    {
        
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        try
        {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("admin@tokenym.appspotmail.com", "Tokenym"));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress, "Tokenym User"));
            msg.setSubject(subject);
            msg.setText(body);
            Transport.send(msg);

        }
        catch (AddressException e)
        {
            e.printStackTrace();
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

    }

    public String generateNewKeyboard (String wat) throws IllegalArgumentException
    {
        return Utils.createKeyboard();
    }

    // also make it so that the keyboard is changed
    @Override
    public Boolean veridemPlusGetToken (String token, String encryptedTokenym, String keyboard, String email,
                                        String generatedToken) throws IllegalArgumentException
    {
        String uniqueID = "";
        String grid = "";
        String password = "";

        Key gridKey = KeyFactory.createKey("Grid", token);
        Entity gridEntity;
        try
        {
            gridEntity = datastore.get(gridKey);
            grid = (String) gridEntity.getProperty("grid");
        }
        catch (EntityNotFoundException e)
        {
            // possible do some other error checking here.
            System.out.println("Entity not found");
        }

        // System.out.println("Retrieved grid: " + grid);
        // decrypt the encrypted tokenym using the grid
        password = Utils.decrypt(encryptedTokenym, grid, keyboard, gridOption);

        // check to see if it's a valid password
        if (!Utils.isValidPassword(password, false))
            return false; // so server knows that password is bad

        // to prevent conflicts of people using the same "password", or "moniker"
        // concatenate the tokenym with the email address provided
        password = password + email;

        // create a new token
        // String newToken = Utils.createUniqueToken();

        // use the decrypted tokenuym to generate the user's ID
        try
        {
            uniqueID = Utils.hash(password);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        // store the new token and id in the cache
        // also need to update the permanent datastore to contain the new token, instead of the old token
        cache.put(generatedToken, uniqueID);
        datastore.delete(gridKey); // remove the old one
        gridEntity = new Entity("Grid", generatedToken);
        gridEntity.setProperty("grid", grid);
        // gridEntity.setProperty("token", newToken);
        datastore.put(gridEntity); // store the new one with the updated key
        // remove the old token and grid from the datastore

        // email the new token to us as a ledger
        sendEmail(email, "Token Ledger", generatedToken);

        return true;
    }
}
