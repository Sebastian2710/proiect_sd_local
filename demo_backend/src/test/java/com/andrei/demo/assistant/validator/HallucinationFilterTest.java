package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HallucinationFilterTest {

    private final HallucinationFilter filter = new HallucinationFilter();

    @Test
    void dropsItemsWithoutEquipment() {
        Equipment eq = makeEquipment("Foo");
        List<ValidatedItem> items = List.of(
                new ValidatedItem("Foo", eq, 1, "ok", null),
                new ValidatedItem("Hallucinated", null, 1, "x", null)
        );
        List<ValidatedItem> out = filter.validate(items, List.of(eq));
        assertEquals(1, out.size());
        assertEquals("Foo", out.get(0).effectiveName());
    }

    @Test
    void preservesAllResolvedItems() {
        Equipment a = makeEquipment("A");
        Equipment b = makeEquipment("B");
        List<ValidatedItem> items = List.of(
                new ValidatedItem("A", a, 1, "r", null),
                new ValidatedItem("B", b, 2, "r", null)
        );
        assertEquals(2, filter.validate(items, List.of(a, b)).size());
    }

    @Test
    void emptyInput_returnsEmpty() {
        assertTrue(filter.validate(List.of(), List.of()).isEmpty());
    }

    @Test
    void allHallucinated_returnsEmpty() {
        List<ValidatedItem> items = List.of(
                new ValidatedItem("X", null, 1, "r", null),
                new ValidatedItem("Y", null, 1, "r", null)
        );
        assertTrue(filter.validate(items, List.of()).isEmpty());
    }

    private Equipment makeEquipment(String name) {
        Equipment eq = new Equipment();
        eq.setName(name);
        eq.setDescription("");
        eq.setStockCount(1);
        return eq;
    }
}