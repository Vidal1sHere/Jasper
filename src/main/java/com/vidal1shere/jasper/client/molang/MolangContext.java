package com.vidal1shere.jasper.client.molang;

import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class MolangContext {
    private static final Map<String, Integer> VARIABLE_SLOTS = new HashMap<>();
    private static final float[] EMPTY = new float[0];

    // Grown on use, since queries can be registered after a context exists
    private float[] queries = new float[0];
    private float[][] lists = new float[0][];
    private int[] listSizes = new int[0];
    private float[] variables = new float[8];
    private final RandomSource random = RandomSource.create();

    public static synchronized int variableSlot(String name) {
        return VARIABLE_SLOTS.computeIfAbsent(name, key -> VARIABLE_SLOTS.size());
    }

    public void set(MolangQuery query, float value) {
        if (query.index() >= queries.length) {
            queries = Arrays.copyOf(queries, query.index() + 1);
        }
        queries[query.index()] = value;
    }

    public void set(MolangQuery query, boolean value) {
        set(query, value ? 1.0F : 0.0F);
    }

    public float query(MolangQuery query) {
        return query.index() < queries.length ? queries[query.index()] : 0.0F;
    }

    public void setList(MolangQuery query, int[] values) {
        float[] list = list(query, values.length);
        for (int i = 0; i < values.length; i++) {
            list[i] = values[i];
        }
    }

    public void setList(MolangQuery query, float[] values) {
        System.arraycopy(values, 0, list(query, values.length), 0, values.length);
    }

    public float query(MolangQuery query, int index) {
        int slot = query.index();
        return slot < lists.length && index >= 0 && index < listSizes[slot] ? lists[slot][index] : 0.0F;
    }

    private float[] list(MolangQuery query, int size) {
        int slot = query.index();
        if (slot >= lists.length) {
            int length = slot + 1;
            int old = lists.length;
            lists = Arrays.copyOf(lists, length);
            listSizes = Arrays.copyOf(listSizes, length);
            Arrays.fill(lists, old, length, EMPTY);
        }
        if (lists[slot].length < size) {
            lists[slot] = new float[size];
        }
        listSizes[slot] = size;
        return lists[slot];
    }

    public float variable(int slot) {
        return slot < variables.length ? variables[slot] : 0.0F;
    }

    public void setVariable(int slot, float value) {
        if (slot >= variables.length) {
            variables = Arrays.copyOf(variables, Math.max(slot + 1, variables.length * 2));
        }
        variables[slot] = value;
    }

    public void resetVariables() {
        Arrays.fill(variables, 0.0F);
    }

    public RandomSource random() {
        return random;
    }
}
