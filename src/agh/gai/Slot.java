package agh.gai;

import jade.core.AID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Slot {
    private double preference;
    private Set<AID> attendants;

    public Slot() {
        this(0);
    }

    public Slot(double preference) {
        this.preference = preference;
        this.attendants = new HashSet<>();
    }

    public double getPreference() {
        return preference;
    }

    public void setPreference(double preference) {
        this.preference = preference;
    }

    public Set<AID> getAttendants() {
        return Collections.unmodifiableSet(this.attendants);
    }

    public void addAttendant(AID attendant) {
        this.attendants.add(attendant);
    }
}
