package urlshortener2015.candypink.checker.infraestructure;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * Created by david on 2/01/16.
 */
public interface AdapterToExtern {

    public Map<String,Boolean> checkUrl(String url);

}
