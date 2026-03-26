package com.knowgap.knowgap;

import com.knowgap.knowgap.model.User;
import com.knowgap.knowgap.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder encoder;

    @Value("${knowgap.app.resetDataOnStartup:false}")
    private boolean resetDataOnStartup;

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Ignore non-critical reset failures for tables that may not exist in all environments.
        }
    }

    private void hardResetData() {
        safeExecute("SET FOREIGN_KEY_CHECKS = 0");

        safeExecute("DELETE FROM user_answers");
        safeExecute("DELETE FROM quiz_attempts");
        safeExecute("DELETE FROM questions");
        safeExecute("DELETE FROM topics");
        safeExecute("DELETE FROM subjects");
        safeExecute("DELETE FROM users");
        safeExecute("DELETE FROM app_users");

        safeExecute("ALTER TABLE user_answers AUTO_INCREMENT = 1");
        safeExecute("ALTER TABLE quiz_attempts AUTO_INCREMENT = 1");
        safeExecute("ALTER TABLE questions AUTO_INCREMENT = 1");
        safeExecute("ALTER TABLE topics AUTO_INCREMENT = 1");
        safeExecute("ALTER TABLE subjects AUTO_INCREMENT = 1");
        safeExecute("ALTER TABLE users AUTO_INCREMENT = 1");
        safeExecute("ALTER TABLE app_users AUTO_INCREMENT = 1");

        safeExecute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private void ensureAdminUser() {
        userRepository.findByUsername("admin").ifPresent(existing -> {
            safeExecute("DELETE FROM user_answers WHERE attempt_id IN (SELECT id FROM quiz_attempts WHERE user_id = " + existing.getId() + ")");
            safeExecute("DELETE FROM quiz_attempts WHERE user_id = " + existing.getId());
            safeExecute("DELETE FROM app_users WHERE username = 'admin'");
            safeExecute("DELETE FROM users WHERE id = " + existing.getId());
        });

        User admin = new User(
                null,
                "admin",
                encoder.encode("admin123"),
                "admin@knowgap.com",
                "ROLE_ADMIN"
        );
        User saved = userRepository.save(admin);

        if (tableExists("app_users")) {
            safeExecute("DELETE FROM app_users WHERE username = 'admin'");
            jdbcTemplate.update(
                    "INSERT INTO app_users (id, username, password, email, role) VALUES (?, ?, ?, ?, ?)",
                    saved.getId(), saved.getUsername(), saved.getPassword(), saved.getEmail(), saved.getRole()
            );
        }
    }

    /**
     * Ensures quiz_attempts.user_id FK references `users`, not `app_users`.
     * Drops ALL existing FKs on that column then re-adds the correct one.
     * Idempotent — safe to run on every startup.
     */
    private void fixQuizAttemptsFk() {
        try {
            // Collect every FK constraint on quiz_attempts.user_id
            List<String> allFks = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quiz_attempts' " +
                    "AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL",
                    String.class
            );
            // Drop them all (use safeExecute so we survive if one is already gone)
            for (String fk : allFks) {
                safeExecute("ALTER TABLE quiz_attempts DROP FOREIGN KEY `" + fk + "`");
            }
            // Re-add the single correct FK pointing to users(id)
            jdbcTemplate.execute(
                    "ALTER TABLE quiz_attempts ADD CONSTRAINT fk_qa_users " +
                    "FOREIGN KEY (user_id) REFERENCES users(id)"
            );
            System.out.println("✅ quiz_attempts.user_id FK is now → users(id)");
        } catch (Exception e) {
            System.err.println("⚠️ Could not fix quiz_attempts FK: " + e.getMessage());
        }
    }

    // ── Entry point ──────────────────────────────────────────────────────────
    @Override
    public void run(String... args) {
        if (resetDataOnStartup) {
            hardResetData();
            System.out.println("✅ Database reset completed on startup.");
        }

        fixQuizAttemptsFk();
        ensureAdminUser();
        System.out.println("✅ Admin account ensured: admin / admin123");
        System.out.println("✅ AI-first mode enabled: subjects and quizzes will be generated by AI Tutor.");
    }
}
