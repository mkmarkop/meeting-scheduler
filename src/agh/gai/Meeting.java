package agh.gai;

import jade.core.AID;

import java.util.Set;
import java.util.HashMap;

public class Meeting {

    private static int nbOfMeetings = 0;
    private int id = 0;
    private Set<AID> attendants;
    private HashMap<Integer, Double> possibleSlots;

    public Meeting(Set<AID> attendants) {
        nbOfMeetings++;
        id = nbOfMeetings;
        this.attendants = attendants;
        possibleSlots = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public Set<AID> getAttendants() {
        return attendants;
    }

    public double getPossibleSlotSum(int slotNumber) {
        return possibleSlots.get(slotNumber);
    }

    public void addPossibleSlot(int slotNumber, double slotSumInit) {
        possibleSlots.put(slotNumber, slotSumInit);
    }

    public void updatePossibleSlotSum(int slotNumber, double slotSumUpdate) {
        possibleSlots.put(slotNumber, possibleSlots.get(slotNumber) + slotSumUpdate);
    }

    public void removePossibleSlot(int slotNumber) {
        possibleSlots.remove(slotNumber);
    }
}
