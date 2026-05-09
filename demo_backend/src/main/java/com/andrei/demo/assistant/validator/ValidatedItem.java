package com.andrei.demo.assistant.validator;

import com.andrei.demo.model.Equipment;

/**
 * A single recommended item as it flows through the validator chain.
 *
 * <p>{@code originalLlmName} is preserved verbatim from the LLM output for
 * audit purposes — it stays unchanged even after {@link NameResolver}
 * resolves it to a catalog {@link Equipment}. {@code equipment} starts null
 * (raw output) and is set by {@link NameResolver}; if no resolution is
 * possible, the item is dropped by {@link HallucinationFilter}.
 *
 * <p>{@code availabilityStatus} is null until {@link StockAvailabilityAnnotator}
 * sets it. Other validators leave it alone.
 */
public record ValidatedItem(
        String originalLlmName,
        Equipment equipment,
        int quantity,
        String reason,
        AvailabilityStatus availabilityStatus
) {

    /** Canonical catalog name if resolved, else the raw LLM name. */
    public String effectiveName() {
        return equipment != null ? equipment.getName() : originalLlmName;
    }

    public ValidatedItem withEquipment(Equipment newEquipment) {
        return new ValidatedItem(originalLlmName, newEquipment, quantity, reason, availabilityStatus);
    }

    public ValidatedItem withQuantity(int newQuantity) {
        return new ValidatedItem(originalLlmName, equipment, newQuantity, reason, availabilityStatus);
    }

    public ValidatedItem withAvailabilityStatus(AvailabilityStatus status) {
        return new ValidatedItem(originalLlmName, equipment, quantity, reason, status);
    }
}