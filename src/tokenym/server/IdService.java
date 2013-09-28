package tokenym.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.json.JSONObject;

import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;
public class IdService extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2919906693452436323L;
	private Cache cache;
	
	
	public IdService()
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
		//System.out.println("Getting called");
			// otherwise, just pull the ID out of the cache
		//System.out.println("paramter: " + req.getParameter("token"));
		String uniqueID = (String) cache.get(req.getParameter("token"));
		
		System.out.println(uniqueID);
		//String uniqueID = "1234";
		try
		{
			JSONObject responseObj = new JSONObject();
			responseObj.put("id", uniqueID);
			PrintWriter writer = resp.getWriter();
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
