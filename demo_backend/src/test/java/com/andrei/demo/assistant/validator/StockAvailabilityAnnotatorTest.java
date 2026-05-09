package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockAvailabilityAnnotatorTest {

    private final StockAvailabilityAnnotator annotator = new StockAvailabilityAnnotator();

    @Test
    void quantityBelowStock_isAvailable() {
        ValidatedItem out = annotator.validate(List.of(item(2, 5)), List.of()).get(0);
        assertEquals(AvailabilityStatus.AVAILABLE, out.availabilityStatus());
    }

    @Test
    void quantityEqualsStock_isAvailable() {
        ValidatedItem out = annotator.validate(List.of(item(5, 5)), List.of()).get(0);
        assertEquals(AvailabilityStatus.AVAILABLE, out.availabilityStatus());
    }

    @Test
    void quantityExceedsStock_isInsufficient() {
        ValidatedItem out = annotator.validate(List.of(item(7, 5)), List.of()).get(0);
        assertEquals(AvailabilityStatus.INSUFFICIENT_STOCK, out.availabilityStatus());
    }

    @Test
    void zeroStock_isOutOfStock() {
        ValidatedItem out = annotator.validate(List.of(item(1, 0)), List.of()).get(0);
        assertEquals(AvailabilityStatus.OUT_OF_STOCK, out.availabilityStatus());
    }

    @Test
    void unmatchedItem_isLeftAlone() {
        // Defensive: HallucinationFilter normally strips these, but if one
        // reaches us we just don't annotate (no NPE).
        ValidatedItem in = new ValidatedItem("Ghost", null, 1, "r", null);
        ValidatedItem out = annotator.validate(List.of(in), List.of()).get(0);
        assertNull(out.availabilityStatus());
    }

    private ValidatedItem item(int qty, int stock) {
        Equipment eq = new Equipment();
        eq.setName("X");
        eq.setDescription("");
        eq.setStockCount(stock);
        return new ValidatedItem("X", eq, qty, "r", null);
    }
}