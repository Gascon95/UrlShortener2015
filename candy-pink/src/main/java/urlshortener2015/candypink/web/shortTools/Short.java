package urlshortener2015.candypink.web.shortTools;

import urlshortener2015.candypink.checker.web.ws.schema.GetCheckerRequest;
import urlshortener2015.candypink.checker.web.ws.schema.GetCheckerResponse;
import com.google.common.hash.Hashing;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.ws.client.core.WebServiceTemplate;
import urlshortener2015.candypink.domain.FishyURL;
import urlshortener2015.candypink.domain.ShortURL;
import urlshortener2015.candypink.repository.ShortURLRepository;
import io.jsonwebtoken.*;

import urlshortener2015.candypink.web.shortTools.Short;
import urlshortener2015.candypink.web.shortTools.Redirect;
import urlshortener2015.candypink.web.UrlShortenerController;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Random;
import java.util.UUID;

import urlshortener2015.candypink.repository.SecureTokenRepository;
import urlshortener2015.candypink.repository.SecureTokenRepositoryImpl;
import urlshortener2015.candypink.domain.SecureToken;

import org.springframework.util.Base64Utils;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class Short {


   private static final Logger logger = LoggerFactory.getLogger(Short.class);



  /**
   *
   */
   public static ResponseEntity<ShortURL> shorts(String url, String users, String time, String sponsor, String brand, 
					  HttpServletRequest request, HttpServletResponse response, ShortURLRepository 						  shortURLRepository, Jaxb2Marshaller marshaller) throws IOException{
	 logger.info("Requested new short for uri " + url);
        logger.info("Users who can redirect: " + users);
        logger.info("Time to be safe: " + time);
        // Obtain jwt
       final Claims claims = (Claims) request.getAttribute("claims");
       // Obtain username
        String username = claims.getSubject();
        // Obtain role
        String role = claims.get("role", String.class);
        if (role.equals("ROLE_NORMAL") && shortURLRepository.findByUserlast24h(username).size() >= 20) {
            logger.info("No more today");
            response.sendRedirect("noMore.html");
            // Can't redirect more today
            return new ResponseEntity<ShortURL>(HttpStatus.BAD_REQUEST);
        }
        boolean safe = !(users.equals("select") && time.equals("select"));
        if (users.equals("select")) {
            users = "All";
        }
        Integer timeToBeSafe;
        if (time.equals("select") || time.equals("Forever")) {
            timeToBeSafe = Integer.MAX_VALUE;
        }else if(time.equals("day")){
            timeToBeSafe = 1;
        }else if(time.equals("week")){
            timeToBeSafe = 7;
        }else{
            timeToBeSafe = 30;
        }
        ShortURL su = createAndSaveIfValid(url, username, safe, users, sponsor, brand, UUID
                .randomUUID().toString(), extractIP(request),timeToBeSafe, shortURLRepository);
        if (su != null) {
            HttpHeaders h = new HttpHeaders();
            h.setLocation(su.getUri());
            logger.info("Requesting to Checker service");
            GetCheckerRequest requestToWs = new GetCheckerRequest();
            requestToWs.setUrl(url);
            Object responseR = new WebServiceTemplate(marshaller).marshalSendAndReceive("http://localhost:"
                    + "8080" + "/ws", requestToWs);
            GetCheckerResponse checkerResponse = (GetCheckerResponse) responseR;
            String resultCode = checkerResponse.getResultCode();
            logger.info("respuesta recibida por el Web Service: " + resultCode);
            logger.info("Hay "+shortURLRepository.count()+ " urls en la bd");
            if (resultCode.equals("ok")) {
                return new ResponseEntity<ShortURL>(su, h, HttpStatus.CREATED);
            } else {
                return new ResponseEntity<ShortURL>(HttpStatus.BAD_REQUEST);
            }
        } else {
            logger.info("La uri es null");
            return new ResponseEntity<ShortURL>(HttpStatus.BAD_REQUEST);
        }
   }
   /**
     * Creates a random token of digits
     *
     * @param length - length of the token to return
     */
    private static String createToken(int length) {
        Random r = new Random();
        String token = "";
        for (int i = 0; i < length; i++) {
            // Only digits in the token
            token += r.nextInt(10);
        }
        return token;
    }

	protected static  ShortURL createAndSaveIfValid(String url, String username, boolean safe, String users,
                                            String sponsor, String brand, String owner, String ip,
                                            Integer timeToBeSafe, ShortURLRepository shortURLRepository) {
        UrlValidator urlValidator = new UrlValidator(new String[]{"http",
                "https"});
        // It is a valid URL
        if (urlValidator.isValid(url)) {
            // Hash
            logger.info("La url es valida");
            String id = Hashing.murmur3_32()
                    .hashString(url, StandardCharsets.UTF_8).toString();
            String token = null;
            // If Url is safe, we create the token, else token = null
            if (safe == true) {
                // Random token of ten digits
                token = createToken(10);
            }
            // ShortUrl
            ShortURL su = null;
            try {
                su = new ShortURL(id, url,
                        linkTo(
                                methodOn(UrlShortenerController.class).redirectToHTML(
                                        id, token, null, null, null)).toUri(), token, users,
                        sponsor, new Timestamp(System.currentTimeMillis()),
                        owner, HttpStatus.TEMPORARY_REDIRECT.value(),
                        safe,timeToBeSafe, null, null, null, null, ip, null, username,
                        0,0,0.0,0.0,true,0);
                logger.info("Se ha creado la uri");
            } catch (IOException e) {
                logger.info("Ha surgido una ioexception en create and safeifvalid");
            }
            if (su != null) {
                logger.info("Se va a guardar en la bd");
                return shortURLRepository.save(su);
            } else {
                return null;
            }
            // It is not a valid URL
        } else {
            logger.info("No es una url valida");
            return null;
        }
    }

     protected static String extractIP(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
