package net.dbd.demode.util.lang;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class Pair<L, R> {
    private final L left;
    private final R right;

    public L left() {
        return left;
    }

    public R right() {
        return right;
    }


    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }
}
