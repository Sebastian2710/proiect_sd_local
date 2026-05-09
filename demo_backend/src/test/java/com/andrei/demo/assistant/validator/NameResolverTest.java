package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NameResolverTest {

    private NameResolver resolver;
    private List<Equipment> catalog;

    @BeforeEach
    void setUp() {
        resolver = new NameResolver();
        catalog = List.of(
                makeEquipment("ESP32 dev board"),
                makeEquipment("Arduino Uno R3"),
                makeEquipment("BME280 sensor")
        );
    }

    @Test
    void exactMatch_resolvesEquipment() {
        ValidatedItem out = resolver
                .validate(List.of(unresolved("ESP32 dev board")), catalog)
                .get(0);
        assertNotNull(out.equipment());
        assertEquals("ESP32 dev board", out.equipment().getName());
    }

    @Test
    void caseInsensitiveMatch_resolvesEquipment() {
        ValidatedItem out = resolver
                .validate(List.of(unresolved("esp32 DEV board")), catalog)
                .get(0);
        assertNotNull(out.equipment());
        assertEquals("ESP32 dev board", out.equipment().getName());
    }

    @Test
    void fuzzyMatch_resolvesNearTypos() {
        ValidatedItem out = resolver
                .validate(List.of(unresolved("Arduino Uno")), catalog)
                .get(0);
        assertNotNull(out.equipment(), "should fuzzy-resolve close name");
        assertEquals("Arduino Uno R3", out.equipment().getName());
    }

    @Test
    void unrelatedName_doesNotResolve() {
        ValidatedItem out = resolver
                .validate(List.of(unresolved("Quantum tunneling apparatus")), catalog)
                .get(0);
        assertNull(out.equipment(), "an unrelated name should not be force-matched");
    }

    @Test
    void preservesOriginalName_evenAfterMatching() {
        ValidatedItem out = resolver
                .validate(List.of(unresolved("esp32 DEV board")), catalog)
                .get(0);
        assertEquals("esp32 DEV board", out.originalLlmName());
        assertEquals("ESP32 dev board", out.effectiveName());
    }

    @Test
    void alreadyResolved_passesThroughUntouched() {
        Equipment esp32 = catalog.get(0);
        ValidatedItem in = new ValidatedItem("anything", esp32, 1, "r", null);
        ValidatedItem out = resolver.validate(List.of(in), catalog).get(0);
        assertSame(esp32, out.equipment(), "should not re-resolve an already-resolved item");
    }

    private ValidatedItem unresolved(String name) {
        return new ValidatedItem(name, null, 1, "test reason", null);
    }

    private Equipment makeEquipment(String name) {
        Equipment eq = new Equipment();
        eq.setName(name);
        eq.setDescription("test");
        eq.setStockCount(5);
        return eq;
    }
}