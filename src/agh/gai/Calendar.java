package agh.gai;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Calendar {
    private List<Slot> slots;
    private SecureRandom random;

    private static final String RNG_ALGORITHM = "SHA1PRNG";

    public Calendar(int size) {
        slots = new ArrayList<>();
        try {
            random = SecureRandom.getInstance(RNG_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        for (int i = 0; i < size; i++) {
            slots.add(new Slot(random.nextDouble()));
        }
    }

    public List<Slot> getSlots() {
        return Collections.unmodifiableList(this.slots);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (int n = slots.size(); i < (n - 1); i++) {
            sb.append(slots.get(i).getPreference());
            sb.append(", ");
        }

        sb.append(slots.get(i).getPreference());
        return sb.toString();
    }
}
