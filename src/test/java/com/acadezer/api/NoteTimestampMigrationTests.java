package com.acadezer.api;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

class NoteTimestampMigrationTests {
    @Test void migrationAddsTimestampsToExistingNotesAndIsRepeatable() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:migration_notes;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("create table materias (id bigint primary key)");
                statement.execute("create table apuntes (id bigint primary key, titulo varchar(200) not null, contenido varchar(20000) not null, materia_id bigint not null)");
                statement.execute("insert into materias values (1)");
                statement.execute("insert into apuntes values (1, 'Apunte anterior', 'contenido anterior', 1)");
            }
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("database/add-note-timestamps.sql"));
            try (var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("select titulo, contenido, creado_en, actualizado_en from apuntes where id=1")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("Apunte anterior");
                    assertThat(result.getString(2)).isEqualTo("contenido anterior");
                    assertThat(result.getTimestamp(3)).isNotNull();
                    assertThat(result.getTimestamp(4)).isNotNull();
                }
                // La columna queda obligatoria y con valor, igual que en producción.
                statement.execute("insert into apuntes (id, titulo, contenido, materia_id) values (2, 'Nuevo', 'texto', 1)");
                try (var result = statement.executeQuery("select creado_en from apuntes where id=2")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getTimestamp(1)).isNotNull();
                }
            }
            // Ejecutar el script otra vez no debe borrar ni romper los apuntes.
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("database/add-note-timestamps.sql"));
            try (var statement = connection.createStatement(); var result = statement.executeQuery("select count(*) from apuntes")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(2);
            }
        }
    }
}
