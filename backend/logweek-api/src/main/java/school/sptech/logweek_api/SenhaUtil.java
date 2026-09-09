package school.sptech.logweek_api;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// Isola os detalhes do hash para manter os controllers simples.
public class SenhaUtil {
    private static String calcular(String senha, String salt) {
        try {
            PBEKeySpec chave = new PBEKeySpec(senha.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), 600000, 256);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(chave).getEncoded();
            chave.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception erro) {
            throw new IllegalStateException("Nao foi possivel proteger a senha", erro);
        }
    }

    public static String gerar(String senha) {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        String salt = Base64.getEncoder().encodeToString(bytes);
        return salt + ":" + calcular(senha, salt);
    }

    public static boolean conferir(String senha, String salvo) {
        String[] partes = salvo.split(":");
        if (partes.length != 2) {
            return false;
        }
        return MessageDigest.isEqual(calcular(senha, partes[0]).getBytes(StandardCharsets.UTF_8),
                partes[1].getBytes(StandardCharsets.UTF_8));
    }
}

