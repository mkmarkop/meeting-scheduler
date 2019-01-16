package agh.gai;

import jade.core.AID;

import java.util.Set;
import java.util.HashMap;

public class Meeting {
	
	private static int nbOfMeetings = 0;
	private int id = 0;
	private Set<AID>  attendants;
	private HashMap possibleSlots;
	
	public Meeting(Set<AID>  attendants){
		nbOfMeetings ++;
		id = nbOfMeetings;
		this.attendants = attendants;
		possibleSlots = new HashMap();
	}
	
		public int getId(){
		return id;
	}
	
	public Set<AID> getAttendants(){
		return attendants;
	}
	
	public int getPossibleSlotSum(int slotNumber){
		return ((int) possibleSlots.get(slotNumber));
	}
	
	public void addPossibleSlot(int slotNumber, double slotSumInit){
		possibleSlots.put(slotNumber, slotSumInit);
	}
	
	public void updatePossibleSlotSum(int slotNumber, double slotSumUpdate){
		possibleSlots.put(slotNumber, ((int) possibleSlots.get(slotNumber)) + slotSumUpdate);
	}
	
	public void removePossibleShot(int slotNumber){
		possibleSlots.remove(slotNumber);
	}
}
