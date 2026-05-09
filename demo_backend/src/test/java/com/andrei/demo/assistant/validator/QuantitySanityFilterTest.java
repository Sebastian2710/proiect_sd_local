package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuantitySanityFilterTest {

    private final QuantitySanityFilter filter = new QuantitySanityFilter();

    @Test
    void normalQuantity_passesThrough() {
        ValidatedItem in = makeItem(3);
        ValidatedItem out = filter.validate(List.of(in), List.of()).get(0);
        assertEquals(3, out.quantity());
    }

    @Test
    void absurdQuantity_isClampedToMax() {
        ValidatedItem in = makeItem(500);
        ValidatedItem out = filter.validate(List.of(in), List.of()).get(0);
        assertEquals(QuantitySanityFilter.MAX_REASONABLE_QUANTITY, out.quantity());
    }

    @Test
    void zeroQuantity_isDropped() {
        assertTrue(filter.validate(List.of(makeItem(0)), List.of()).isEmpty());
    }

    @Test
    void negativeQuantity_isDropped() {
        assertTrue(filter.validate(List.of(makeItem(-5)), List.of()).isEmpty());
    }

    @Test
    void exactMaxQuantity_passesThroughUnchanged() {
        ValidatedItem in = makeItem(QuantitySanityFilter.MAX_REASONABLE_QUANTITY);
        ValidatedItem out = filter.validate(List.of(in), List.of()).get(0);
        assertEquals(QuantitySanityFilter.MAX_REASONABLE_QUANTITY, out.quantity());
    }

    private ValidatedItem makeItem(int qty) {
        Equipment eq = new Equipment();
        eq.setName("Test");
        eq.setDescription("");
        eq.setStockCount(100);
        return new ValidatedItem("Test", eq, qty, "test", null);
    }
}