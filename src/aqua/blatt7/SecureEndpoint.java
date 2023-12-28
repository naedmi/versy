package aqua.blatt7;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint extends Endpoint {
    private Endpoint endpoint;
    private SecretKeySpec key;
    private Cipher encryptCipher;
    private Cipher decryptCipher;

    public SecureEndpoint() {
        this.endpoint = new Endpoint();
        this.key = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(), "AES");
        try {
            encryptCipher = Cipher.getInstance("AES");
            decryptCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public SecureEndpoint(int port) {
        this.endpoint = new Endpoint(port);
        this.key = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(), "AES");
        try {
            encryptCipher = Cipher.getInstance("AES");
            decryptCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            encryptCipher.doFinal(payload.toString().getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        endpoint.send(receiver, payload);
    }

    public Message blockingReceive() {
        Message message = endpoint.blockingReceive();
        try {
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
            decryptCipher.doFinal(message.getPayload().toString().getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    public Message nonBlockingReceive() {
        Message message = endpoint.nonBlockingReceive();
        try {
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
            decryptCipher.doFinal(message.getPayload().toString().getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return message;
    }
}
