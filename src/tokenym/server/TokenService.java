package tokenym.server;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.json.JSONObject;
import tokenym.client.TokenymService;
import tokenym.client.TokenymServiceAsync;
import tokenym.shared.Utils;

import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;
public class TokenService extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2919906693452436323L;
	private Cache cache;
	
	public TokenService()
	{
		Map<String, Integer> props = new HashMap<String, Integer>();
		props.put(GCacheFactory.EXPIRATION_DELTA, 60);

		try {
			CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
			this.cache = cacheFactory.createCache(props);
			System.out.println("Creating cache factory");
		} catch (CacheException e) {
			System.out.println("Cache exception" + e);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		doGet(req,resp);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
	    // copied from grettingServiceImplementation. Should find a way to call that instead
	    String passwordAndSecret = (String) req.getParameter("password");
	    String[] passwordAndSecretArr = passwordAndSecret.split("|");
        String password = passwordAndSecretArr[0]; // fix this, so that the shared secret stuff

        String sharedSecret = "";
        for (int i = 1; i < passwordAndSecretArr.length; i++)
        {
            sharedSecret = sharedSecret + passwordAndSecretArr[i];
        }

        
        String token = Utils.createUniqueToken();
        String tokenAndSecret = "";
        
        // Verify that the input is valid.
        if (!Utils.isValidPassword(password, true))
        {
            token = null;
        }

        // create a token
        
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

	    
	    
	    try
        {
            JSONObject responseObj = new JSONObject();
            responseObj.put("token", token);
            PrintWriter writer = resp.getWriter();
            // just write the token for right now
            writer.write(responseObj.toString());
            writer.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ServletException(e);
        }
	
		
		
	}
	
	
}
