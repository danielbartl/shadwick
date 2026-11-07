package dev.jbaby.shadwick.examples;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.time.LocalDate;
import java.util.zip.ZipInputStream;
import static org.assertj.core.api.Assertions.*;

class StudioWorkspaceTest {
    private StudioWorkspace workspace() { return new StudioWorkspace(LocalDate.of(2026, 11, 6)); }

    @Test void tasksHandoffAndPaymentsShareOneConsistentWorkspace() throws Exception {
        var w = workspace();
        assertThat(w.project(1).progress()).isEqualTo(50);
        assertThatThrownBy(() -> w.handoff(1)).hasMessageContaining("Complete every task");
        w.completeTask(1, 13, false); w.completeTask(1, 14, false);
        assertThat(w.project(1).progress()).isEqualTo(100);
        w.handoff(1);
        assertThat(w.active()).isEqualTo(2);
        assertThat(w.project(1).status()).isEqualTo("Delivered");
        assertThatThrownBy(() -> w.completeTask(1, 13, true)).hasMessageContaining("no longer open");
        try (var zip = new ZipInputStream(new ByteArrayInputStream(w.handoffZip(1)))) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("README.md");
            assertThat(new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).contains("Fern & Field", "50 estimated hours");
            assertThat(zip.getNextEntry().getName()).isEqualTo("completed-work.md");
            assertThat(new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).contains("[x] Review the mobile experience");
        }
        long open = w.outstanding(), collected = w.collected();
        assertThat(w.pay(2)).isTrue(); assertThat(w.pay(2)).isFalse();
        assertThat(w.outstanding()).isEqualTo(open - 620000); assertThat(w.collected()).isEqualTo(collected + 620000);
        assertThat(w.invoice(2).paidOn()).isEqualTo(w.today);
    }
    @Test void editsRejectConflictsAndValidateBeforeChangingData() {
        var w = workspace(); var p = w.project(1);
        w.completeTask(1, 13, false);
        assertThatThrownBy(() -> w.completeTask(1, 13, false)).hasMessageContaining("another tab");
        assertThatThrownBy(() -> w.saveProject(p.id(), p.revision(), "Changed", p.client(), p.category(), p.due(), p.budgetCents())).hasMessageContaining("Refresh");
        assertThat(w.project(1).name()).isEqualTo(p.name());
        assertThatThrownBy(() -> w.addTask(1, "Task", "Unknown", 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> w.saveProject(null, 0, " ", "Client", "Brand", w.today, 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> w.handoffZip(2)).hasMessageContaining("Prepare");
        assertThat(w.project(1).tasks()).hasSize(4);
    }
    @Test void newProjectsHaveIndependentTasksAndImmutableSnapshots() {
        var w = workspace(); var old = w.projects();
        var p = w.saveProject(null, 0, " New identity ", " Client ", " Brand ", w.today.plusDays(30), 500000);
        assertThat(p.name()).isEqualTo("New identity"); assertThat(p.tasks()).isEmpty();
        w.addTask(p.id(), "Design it", "Nora", 8);
        assertThat(w.project(p.id()).hours()).isEqualTo(8); assertThat(w.project(1).hours()).isEqualTo(50);
        assertThat(old).hasSize(4); assertThat(w.projects()).hasSize(5);
        assertThatThrownBy(() -> w.projects().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> w.project(p.id()).tasks().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void entireWorkspaceSurvivesSessionSerialization() throws Exception {
        var w = workspace(); w.pay(2); w.completeTask(1, 13, false);
        var bytes = new ByteArrayOutputStream(); try (var out = new ObjectOutputStream(bytes)) { out.writeObject(w); }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            var restored = (StudioWorkspace) in.readObject();
            assertThat(restored.generation).isEqualTo(w.generation); assertThat(restored.project(1).progress()).isEqualTo(75);
            assertThat(restored.invoice(2).paid()).isTrue(); assertThat(restored.activity()).containsExactlyElementsOf(w.activity());
        }
    }
}
