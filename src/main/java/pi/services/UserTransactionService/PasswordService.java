package pi.services.UserTransactionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class PasswordService {

    private static final String DEFAULT_PHP_PATH = "C:\\xampp\\php\\php.exe";

    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }

        return runPhp("echo password_hash(getenv('CODEX_PLAIN_PASSWORD'), PASSWORD_BCRYPT);",
                plainPassword,
                null);
    }

    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.isBlank() || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }

        String result = runPhp(
                "echo password_verify(getenv('CODEX_PLAIN_PASSWORD'), getenv('CODEX_HASHED_PASSWORD')) ? '1' : '0';",
                plainPassword,
                hashedPassword
        );
        return "1".equals(result);
    }

    private String runPhp(String code, String plainPassword, String hashedPassword) {
        ProcessBuilder builder = new ProcessBuilder();
        String[] command = new String[3];
        command[0] = resolvePhpBinary();
        command[1] = "-r";
        command[2] = code;
        builder.command(command);
        builder.environment().put("CODEX_PLAIN_PASSWORD", plainPassword);
        if (hashedPassword != null) {
            builder.environment().put("CODEX_HASHED_PASSWORD", hashedPassword);
        }

        try {
            Process process = builder.start();
            String stdout = read(process.getInputStream());
            String stderr = read(process.getErrorStream());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException("Erreur PHP password service: " + stderr);
            }

            return stdout.trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Execution PHP interrompue.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'executer php.exe pour les mots de passe.", e);
        }
    }

    private String resolvePhpBinary() {
        return DEFAULT_PHP_PATH;
    }

    private String read(java.io.InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }
}
