package urlshortener2015.candypink.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.filter.GenericFilterBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import io.jsonwebtoken.*;

import urlshortener2015.candypink.auth.support.AuthURI;

@Configurable
public class JWTokenFilter extends GenericFilterBean {
   
    private static final Logger log = LoggerFactory.getLogger(JWTokenFilter.class);

    private String key;

    private AuthURI[] uris;
    
    public JWTokenFilter(String key, AuthURI[] uris){
        this.key = key;
        this.uris = uris;
    }

    @Override
    public void doFilter(final ServletRequest req,
                         final ServletResponse res,
                         final FilterChain chain) throws IOException, ServletException {
	// Obtain servlets
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response  = (HttpServletResponse) res;
        final String authHeader = request.getHeader("Authorization");
	
		String permission = requiredPermission(request.getRequestURI(), request.getMethod());
		// All users
		if(permission == null) {
			log.info("Authentication not required");
			chain.doFilter(req, res); 
		}
		else {
			// No authenticated
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				log.info("No authenticate");
				// Error
			}
			// Authenticated
			else {
                String token = authHeader.substring(7);
                try {
                    //Parse claims from JWT
                    final Claims claims = Jwts.parser().setSigningKey(key)
                            .parseClaimsJws(token).getBody();
					String role = claims.get("role", String.class);
					log.info("Parsed");
					// Has not permission
					if(permission.equals("Admin") && !role.equals("Admin") ||
					   permission.equals("Premium") && role.equals("Normal")) {
						log.info("Not PErmission");
						//Error
					}
					// Has permission
					else {
						log.info("Yes Permission");
						request.setAttribute("claims",claims);	
                        chain.doFilter(req, res);
					}
                }
                catch(ExpiredJwtException expiredException){
                    // Token Expired
					log.info("Expired");
                }
                catch (final SignatureException  | NullPointerException  |MalformedJwtException ex) {
                    // Format incorrect
					log.info("Format incorrect");
                }
			}
		}
	}

	private String requiredPermission(String uri, String method) {
		log.info("URI PEDIDA: " + uri);
		log.info("METHOD: " + method);
		for(int i = 0; i < uris.length; i++) {
			if(uri.contains(uris[i].getUri())) {
				log.info("PREMIO: " + uris[i].getUri());
				log.info("PERMIO: " + uris[i].getPermission(method));	
				return uris[i].getPermission(method);
			}
		}
		return null;		
	}
}
