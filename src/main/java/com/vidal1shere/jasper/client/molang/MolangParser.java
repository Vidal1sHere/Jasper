package com.vidal1shere.jasper.client.molang;

import com.vidal1shere.jasper.client.keyframe.Easing;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public final class MolangParser {
    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float RAD = Mth.RAD_TO_DEG;

    private final String source;
    private final List<String> tokens;
    private int index;

    private MolangParser(String source) {
        this.source = source;
        this.tokens = tokenize(source);
    }

    public static MolangValue parse(String expression) {
        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            return MolangValue.ZERO;
        }
        try {
            return MolangValue.constant(Float.parseFloat(trimmed));
        } catch (NumberFormatException ignored) {
        }
        return new MolangParser(trimmed.toLowerCase(Locale.ROOT)).program();
    }

    private MolangValue program() {
        List<MolangValue> statements = new ArrayList<>();
        while (!atEnd()) {
            if (accept(";")) {
                continue;
            }
            if (accept("return")) {
                statements.add(expression());
                accept(";");
                if (!atEnd()) {
                    throw error("code after 'return'");
                }
                break;
            }
            statements.add(statement());
            if (!atEnd() && !accept(";")) {
                throw error("unexpected '" + peek() + "'");
            }
        }
        if (statements.isEmpty()) {
            return MolangValue.ZERO;
        }
        if (statements.size() == 1) {
            return statements.getFirst();
        }
        MolangValue[] all = statements.toArray(MolangValue[]::new);
        int last = all.length - 1;
        return context -> {
            for (int i = 0; i < last; i++) {
                all[i].get(context);
            }
            return all[last].get(context);
        };
    }

    private MolangValue statement() {
        if (index + 1 < tokens.size() && tokens.get(index + 1).equals("=")) {
            String name = next();
            int slot = variableSlot(name);
            if (slot < 0) {
                throw error("only variables can be assigned, got '" + name + "'");
            }
            next();
            MolangValue value = expression();
            return context -> {
                float result = value.get(context);
                context.setVariable(slot, result);
                return result;
            };
        }
        return expression();
    }

    private MolangValue expression() {
        MolangValue condition = or();
        if (!accept("?")) {
            return condition;
        }
        MolangValue then = expression();
        MolangValue otherwise = accept(":") ? expression() : MolangValue.ZERO;
        return fold(context -> condition.get(context) != 0.0F ? then.get(context) : otherwise.get(context), condition, then, otherwise);
    }

    private MolangValue or() {
        MolangValue left = and();
        while (accept("||")) {
            MolangValue a = left, b = and();
            left = fold(context -> a.get(context) != 0.0F || b.get(context) != 0.0F ? 1.0F : 0.0F, a, b);
        }
        return left;
    }

    private MolangValue and() {
        MolangValue left = equality();
        while (accept("&&")) {
            MolangValue a = left, b = equality();
            left = fold(context -> a.get(context) != 0.0F && b.get(context) != 0.0F ? 1.0F : 0.0F, a, b);
        }
        return left;
    }

    private MolangValue equality() {
        MolangValue left = comparison();
        while (true) {
            MolangValue a = left;
            if (accept("==")) {
                MolangValue b = comparison();
                left = fold(context -> a.get(context) == b.get(context) ? 1.0F : 0.0F, a, b);
            } else if (accept("!=")) {
                MolangValue b = comparison();
                left = fold(context -> a.get(context) != b.get(context) ? 1.0F : 0.0F, a, b);
            } else {
                return left;
            }
        }
    }

    private MolangValue comparison() {
        MolangValue left = additive();
        while (true) {
            MolangValue a = left;
            if (accept("<=")) {
                MolangValue b = additive();
                left = fold(context -> a.get(context) <= b.get(context) ? 1.0F : 0.0F, a, b);
            } else if (accept(">=")) {
                MolangValue b = additive();
                left = fold(context -> a.get(context) >= b.get(context) ? 1.0F : 0.0F, a, b);
            } else if (accept("<")) {
                MolangValue b = additive();
                left = fold(context -> a.get(context) < b.get(context) ? 1.0F : 0.0F, a, b);
            } else if (accept(">")) {
                MolangValue b = additive();
                left = fold(context -> a.get(context) > b.get(context) ? 1.0F : 0.0F, a, b);
            } else {
                return left;
            }
        }
    }

    private MolangValue additive() {
        MolangValue left = multiplicative();
        while (true) {
            MolangValue a = left;
            if (accept("+")) {
                MolangValue b = multiplicative();
                left = fold(context -> a.get(context) + b.get(context), a, b);
            } else if (accept("-")) {
                MolangValue b = multiplicative();
                left = fold(context -> a.get(context) - b.get(context), a, b);
            } else {
                return left;
            }
        }
    }

    private MolangValue multiplicative() {
        MolangValue left = unary();
        while (true) {
            MolangValue a = left;
            if (accept("*")) {
                MolangValue b = unary();
                left = fold(context -> a.get(context) * b.get(context), a, b);
            } else if (accept("/")) {
                MolangValue b = unary();
                left = fold(context -> a.get(context) / b.get(context), a, b);
            } else {
                return left;
            }
        }
    }

    private MolangValue unary() {
        if (accept("-")) {
            MolangValue value = unary();
            return fold(context -> -value.get(context), value);
        }
        if (accept("!")) {
            MolangValue value = unary();
            return fold(context -> value.get(context) == 0.0F ? 1.0F : 0.0F, value);
        }
        if (accept("+")) {
            return unary();
        }
        return primary();
    }

    private MolangValue primary() {
        if (atEnd()) {
            throw error("unexpected end of expression");
        }
        String token = next();
        if (token.equals("(")) {
            MolangValue value = expression();
            expect(")");
            return value;
        }
        char first = token.charAt(0);
        if (Character.isDigit(first) || first == '.') {
            try {
                return MolangValue.constant(Float.parseFloat(token));
            } catch (NumberFormatException e) {
                throw error("invalid number '" + token + "'");
            }
        }
        if (!Character.isLetter(first) && first != '_') {
            throw error("unexpected '" + token + "'");
        }
        if (accept("(")) {
            List<MolangValue> args = new ArrayList<>();
            if (!accept(")")) {
                do {
                    args.add(expression());
                } while (accept(","));
                expect(")");
            }
            return function(token, args);
        }
        return identifier(token);
    }

    private MolangValue identifier(String name) {
        if (name.equals("math.pi")) {
            return MolangValue.constant(Mth.PI);
        }
        if (name.equals("this")) {
            return MolangValue.ZERO;
        }
        int slot = variableSlot(name);
        if (slot >= 0) {
            return context -> context.variable(slot);
        }
        String query = strip(name, "query.", "q.");
        if (query != null) {
            MolangQuery resolved = MolangQuery.byName(query);
            if (resolved == null) {
                throw error("unknown query '" + name + "'");
            }
            if (resolved.isList()) {
                throw error("query '" + name + "' needs an index: " + name + "(i)");
            }
            return context -> context.query(resolved);
        }
        throw error("unknown identifier '" + name + "'");
    }

    private MolangValue function(String name, List<MolangValue> args) {
        String query = strip(name, "query.", "q.");
        if (query != null) {
            MolangQuery resolved = MolangQuery.byName(query);
            if (resolved == null || !resolved.isList()) {
                throw error("unknown function '" + name + "'");
            }
            arity(name, args, 1);
            MolangValue index = args.getFirst();
            return context -> context.query(resolved, Math.round(index.get(context)));
        }
        String function = strip(name, "math.");
        if (function == null) {
            throw error("unknown function '" + name + "'");
        }
        Easing easing = Easing.byName(function);
        if (easing != null) {
            return ease(name, easing, args);
        }
        return switch (function) {
            case "abs" -> unary(name, args, Math::abs);
            case "sin" -> unary(name, args, v -> Mth.sin(v * DEG));
            case "cos" -> unary(name, args, v -> Mth.cos(v * DEG));
            case "asin" -> unary(name, args, v -> (float) Math.asin(v) * RAD);
            case "acos" -> unary(name, args, v -> (float) Math.acos(v) * RAD);
            case "atan" -> unary(name, args, v -> (float) Math.atan(v) * RAD);
            case "sqrt" -> unary(name, args, v -> (float) Math.sqrt(v));
            case "floor" -> unary(name, args, v -> (float) Math.floor(v));
            case "ceil" -> unary(name, args, v -> (float) Math.ceil(v));
            case "round" -> unary(name, args, v -> (float) Math.round(v));
            case "trunc" -> unary(name, args, v -> v < 0.0F ? (float) Math.ceil(v) : (float) Math.floor(v));
            case "exp" -> unary(name, args, v -> (float) Math.exp(v));
            case "ln" -> unary(name, args, v -> (float) Math.log(v));
            case "hermite_blend" -> unary(name, args, v -> 3.0F * v * v - 2.0F * v * v * v);
            case "atan2" -> binary(name, args, (y, x) -> (float) Math.atan2(y, x) * RAD);
            case "pow" -> binary(name, args, (a, b) -> (float) Math.pow(a, b));
            case "mod" -> binary(name, args, (a, b) -> a % b);
            case "min" -> binary(name, args, Math::min);
            case "max" -> binary(name, args, Math::max);
            case "clamp" -> {
                arity(name, args, 3);
                MolangValue v = args.get(0), min = args.get(1), max = args.get(2);
                yield fold(context -> Mth.clamp(v.get(context), min.get(context), max.get(context)), v, min, max);
            }
            case "lerp" -> {
                arity(name, args, 3);
                MolangValue a = args.get(0), b = args.get(1), t = args.get(2);
                yield fold(context -> Mth.lerp(t.get(context), a.get(context), b.get(context)), a, b, t);
            }
            case "random" -> {
                arity(name, args, 2);
                MolangValue low = args.get(0), high = args.get(1);
                yield context -> Mth.lerp(context.random().nextFloat(), low.get(context), high.get(context));
            }
            case "random_integer" -> {
                arity(name, args, 2);
                MolangValue low = args.get(0), high = args.get(1);
                yield context -> {
                    int min = Math.round(low.get(context));
                    int max = Math.round(high.get(context));
                    return max <= min ? min : min + context.random().nextInt(max - min + 1);
                };
            }
            default -> throw error("unknown function '" + name + "'");
        };
    }

    @FunctionalInterface
    private interface FloatUnary {
        float apply(float value);
    }

    @FunctionalInterface
    private interface FloatBinary {
        float apply(float a, float b);
    }

    private MolangValue ease(String name, Easing easing, List<MolangValue> args) {
        if (args.isEmpty() || args.size() > 2) {
            throw error(name + " expects 1 or 2 arguments, got " + args.size());
        }
        MolangValue t = args.get(0);
        MolangValue argument = args.size() == 2 ? args.get(1) : MolangValue.constant(easing.defaultArgument());
        return fold(context -> easing.apply(t.get(context), argument.get(context)), t, argument);
    }

    private MolangValue unary(String name, List<MolangValue> args, FloatUnary operation) {
        arity(name, args, 1);
        MolangValue value = args.getFirst();
        return fold(context -> operation.apply(value.get(context)), value);
    }

    private MolangValue binary(String name, List<MolangValue> args, FloatBinary operation) {
        arity(name, args, 2);
        MolangValue a = args.get(0), b = args.get(1);
        return fold(context -> operation.apply(a.get(context), b.get(context)), a, b);
    }

    private void arity(String name, List<MolangValue> args, int expected) {
        if (args.size() != expected) {
            throw error(name + " expects " + expected + " argument(s), got " + args.size());
        }
    }

    private static MolangValue fold(MolangValue node, MolangValue... children) {
        for (MolangValue child : children) {
            if (!child.isConstant()) {
                return node;
            }
        }
        return MolangValue.constant(node.get(null));
    }

    private static int variableSlot(String name) {
        String variable = strip(name, "variable.", "v.", "temp.", "t.");
        return variable == null ? -1 : MolangContext.variableSlot(variable);
    }

    private static String strip(String name, String... prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix) && name.length() > prefix.length()) {
                return name.substring(prefix.length());
            }
        }
        return null;
    }

    private boolean atEnd() {
        return index >= tokens.size();
    }

    private String peek() {
        return tokens.get(index);
    }

    private String next() {
        if (atEnd()) {
            throw error("unexpected end of expression");
        }
        return tokens.get(index++);
    }

    private boolean accept(String token) {
        if (!atEnd() && tokens.get(index).equals(token)) {
            index++;
            return true;
        }
        return false;
    }

    private void expect(String token) {
        if (!accept(token)) {
            throw error("expected '" + token + "'" + (atEnd() ? " at the end" : ", got '" + peek() + "'"));
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("molang '" + source + "': " + message);
    }

    private static List<String> tokenize(String source) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || c == '.' && i + 1 < source.length() && Character.isDigit(source.charAt(i + 1))) {
                int start = i;
                while (i < source.length() && (Character.isDigit(source.charAt(i)) || source.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(source.substring(start, i));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_' || source.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(source.substring(start, i));
            } else if (i + 1 < source.length() && isPair(c, source.charAt(i + 1))) {
                tokens.add(source.substring(i, i + 2));
                i += 2;
            } else if ("+-*/()<>!=?:;,".indexOf(c) >= 0) {
                tokens.add(String.valueOf(c));
                i++;
            } else {
                throw new IllegalArgumentException("molang '" + source + "': unexpected character '" + c + "'");
            }
        }
        return tokens;
    }

    private static boolean isPair(char a, char b) {
        return a == '=' && b == '=' || a == '!' && b == '=' || a == '<' && b == '=' || a == '>' && b == '='
                || a == '&' && b == '&' || a == '|' && b == '|';
    }
}
