package gator.lib.shiro;

import java.security.Key;
import java.util.Base64;
import org.apache.shiro.crypto.cipher.AesCipherService;

/**
 *
 * @author <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GappKeyMaker {
        /**
         * Generate AES keys.
         * @return AES keys as string representation encoded as base 64.
         */
        public String getNewKey() {
                AesCipherService aes = new AesCipherService();            
                Key key = aes.generateNewKey();
                return Base64.getEncoder().encodeToString(key.getEncoded());
        }
}
