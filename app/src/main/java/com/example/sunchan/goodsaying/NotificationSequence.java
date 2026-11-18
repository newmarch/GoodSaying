package com.example.sunchan.goodsaying;

import java.util.List;

final class NotificationSequence {
    static final class Choice {
        final int id;
        final boolean newCycle;

        Choice(int id, boolean newCycle) {
            this.id = id;
            this.newCycle = newCycle;
        }
    }

    private NotificationSequence() {}

    static Choice next(List<Integer> ids, List<Boolean> seen, int lastId) {
        if (ids.isEmpty()) {
            return null;
        }

        // Follow the current order after the last delivery, skipping everyone already served.
        int start = ids.indexOf(lastId) + 1;
        for (int offset = 0; offset < ids.size(); offset++) {
            int index = (start + offset) % ids.size();
            if (!seen.get(index)) {
                return new Choice(ids.get(index), false);
            }
        }

        // Reordering or disabling rows may put the last delivery first in the next cycle.
        int first = ids.size() > 1 && ids.get(0) == lastId ? 1 : 0;
        return new Choice(ids.get(first), true);
    }
}
