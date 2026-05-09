package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateMergerTest {

    private final DuplicateMerger merger = new DuplicateMerger();

    @Test
    void mergesSameEquipmentReferences_summingQuantities() {
        Equipment esp32 = makeEquipment("ESP32 dev board");
        List<ValidatedItem> items = List.of(
                new ValidatedItem("ESP32 dev board", esp32, 1, "for WiFi", null),
                new ValidatedItem("ESP32 dev board", esp32, 2, "for sensors", null)
        );
        List<ValidatedItem> out = merger.validate(items, List.of(esp32));
        assertEquals(1, out.size());
        assertEquals(3, out.get(0).quantity());
    }

    @Test
    void distinctEquipment_areNotMerged() {
        Equipment a = makeEquipment("A");
        Equipment b = makeEquipment("B");
        List<ValidatedItem> items = List.of(
                new ValidatedItem("A", a, 1, "r1", null),
                new ValidatedItem("B", b, 1, "r2", null)
        );
        assertEquals(2, merger.validate(items, List.of(a, b)).size());
    }

    @Test
    void preservesFirstSeenOrder_acrossMerge() {
        Equipment a = makeEquipment("A");
        Equipment b = makeEquipment("B");
        Equipment c = makeEquipment("C");
        List<ValidatedItem> items = List.of(
                new ValidatedItem("B", b, 1, "r", null),
                new ValidatedItem("A", a, 1, "r", null),
                new ValidatedItem("C", c, 1, "r", null),
                new ValidatedItem("A", a, 1, "r", null)   // duplicate of A
        );
        List<ValidatedItem> out = merger.validate(items, List.of(a, b, c));
        assertEquals(3, out.size());
        assertEquals("B", out.get(0).effectiveName());
        assertEquals("A", out.get(1).effectiveName());
        assertEquals("C", out.get(2).effectiveName());
        assertEquals(2, out.get(1).quantity());
    }

    @Test
    void mergedReason_combinesDistinctReasons() {
        Equipment esp32 = makeEquipment("ESP32 dev board");
        List<ValidatedItem> items = List.of(
                new ValidatedItem("ESP32 dev board", esp32, 1, "for WiFi", null),
                new ValidatedItem("ESP32 dev board", esp32, 1, "for sensor reading", null)
        );
        ValidatedItem merged = merger.validate(items, List.of(esp32)).get(0);
        assertTrue(merged.reason().contains("WiFi"));
        assertTrue(merged.reason().contains("sensor reading"));
    }

    @Test
    void identicalReasons_areDeduplicated() {
        Equipment esp32 = makeEquipment("ESP32 dev board");
        List<ValidatedItem> items = List.of(
                new ValidatedItem("ESP32 dev board", esp32, 1, "core MCU", null),
                new ValidatedItem("ESP32 dev board", esp32, 1, "core MCU", null)
        );
        ValidatedItem merged = merger.validate(items, List.of(esp32)).get(0);
        assertEquals("core MCU", merged.reason());
    }

    private Equipment makeEquipment(String name) {
        Equipment eq = new Equipment();
        eq.setName(name);
        eq.setDescription("");
        eq.setStockCount(5);
        return eq;
    }
}