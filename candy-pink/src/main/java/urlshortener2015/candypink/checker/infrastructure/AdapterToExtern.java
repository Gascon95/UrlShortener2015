package urlshortener2015.candypink.checker.infrastructure;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * Created by david on 2/01/16.
 */
public interface AdapterToExtern {

    public Map<String,Object> checkUrl(String url);

}
