package agh.gai;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class SchedulerAgent extends Agent {
    private Set<AID> contacts;
    private Calendar calendar;
		private HashMap organisedMeetings;
		private HashMap meetings;

    private static final int CALENDAR_SIZE = 24;

    public SchedulerAgent() {
      contacts = new HashSet<>();
      calendar = new Calendar(CALENDAR_SIZE);
			organisedMeetings = new HashMap<>();
			meetings = new HashMap();
    }

    @Override
    protected void setup() {

			int meetingInterval = 20000;

			System.out.println("Agent " + getAID().getLocalName() + " initialized.");
			DFAgentDescription template = new DFAgentDescription();
			template.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("scheduling");
			sd.setName("JADE-scheduling");
			template.addServices(sd);

			System.out.println("Agent " + getAID().getLocalName() + " calendar: " + calendar.toString());

			try {
					DFService.register(this, template);
			} catch (FIPAException fe) {
					fe.printStackTrace();
					System.exit(-1);
			}
			System.out.println("Agent " + getAID().getLocalName() + " registered itself.");

			addBehaviour(new OneShotBehaviour(this) {
				@Override
				public void action() {
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("scheduling");
					template.addServices(sd);

					try {
							DFAgentDescription[] results = DFService.search(myAgent, template);
							for (DFAgentDescription description : results) {
									if (description.getName().equals(myAgent.getAID()))
											continue;

									contacts.add(description.getName());
									System.out.println("Agent " + myAgent.getAID().getLocalName() + " added Agent " + description.getName().getLocalName() + " to contact list.");
							}
					} catch (FIPAException fe) {
							fe.printStackTrace();
							System.exit(-1);
						}
					System.out.println("Agent " + getAID().getLocalName() + " found " + contacts.size() + " contacts.");
				}
			});

			addBehaviour(new TickerBehaviour(this, meetingInterval) {
				@Override
				public void onTick() {
					if (contacts.size() != 0){
					
						Set<AID> contactedAgents = new HashSet<>();
						int nbOfcontactedAgents = new Random().nextInt(contacts.size()) + 1;
						int contactedAgent;
						
						List<AID> agentsList = new ArrayList<AID>();
						agentsList.addAll(contacts);
						
						for (int i = 0; i < nbOfcontactedAgents; i++){
							do{
								contactedAgent = new Random().nextInt(contacts.size());
							}while(contactedAgents.contains(agentsList.get(contactedAgent)));
							contactedAgents.add(agentsList.get(contactedAgent));
						}
						
						Meeting newMeeting = new Meeting(contactedAgents);
						organisedMeetings.put(newMeeting.getId(), newMeeting);
						meetings.put(newMeeting.getId(), new ArrayList<Slot>());
						
						ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
	      		for (AID agent : newMeeting.getAttendants()) {
	        		msg.addReceiver(agent);
	      		}

						Slot preferedSlotForMeeting = getPreferedAvailableSlot(((ArrayList<Slot>) meetings.get(newMeeting.getId())));

						if (preferedSlotForMeeting != null){
						
							((Meeting) organisedMeetings.get(newMeeting.getId())).addPossibleSlot(calendar.getSlots().indexOf(preferedSlotForMeeting), preferedSlotForMeeting.getPreference());
							
							msg.setContent(Integer.toString(newMeeting.getId()) + " " + Integer.toString(calendar.getSlots().indexOf(preferedSlotForMeeting)));
							myAgent.send(msg);

							System.out.println("Agent " + getAID().getLocalName() + " is scheduling a new meeting with " + nbOfcontactedAgents + " contacts.");
						}
					}
				}
			});
			
		addBehaviour(new OrganiseMeeting());
		addBehaviour(new RespondForMeeting());
		addBehaviour(new ImpossibleSlotForMeeting());

		}

    @Override
    protected void takeDown() {
        System.out.println("Agent " + getAID().getLocalName() + " terminated.");
    }

		private Slot getPreferedAvailableSlot(ArrayList<Slot> NotPossibleSlots){
			Slot preferedAvailableSlot = null;
			for (Slot slot : calendar.getSlots()){
				if ( !NotPossibleSlots.contains(slot) && slot.getIsAvailable() )
					if ( preferedAvailableSlot == null || ( preferedAvailableSlot.getPreference() < slot.getPreference() ) ) preferedAvailableSlot = slot;
			}

		 return preferedAvailableSlot;
		}
		
		
    private class OrganiseMeeting extends Behaviour {

			private int contactedAgentsWithNoMoreSlot = 0;
			private int nbOfcontactedAgents;
	
			public void action() {
				MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
																								MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
				mt = MessageTemplate.or(mt, MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				ACLMessage msg = myAgent.receive(mt);
				
				if (msg != null) {
					
					String[] infos = msg.getContent().split(" ");
					
					int meetingId = Integer.parseInt(infos[0]);
					int slot = Integer.parseInt(infos[1]);
					double preferenceForSlot = Double.parseDouble(infos[2]);
					int proposedSlot = Integer.parseInt(infos[3]);
					double preferenceForProposedSlot = Double.parseDouble(infos[4]);
					
					nbOfcontactedAgents = ((Meeting)((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).getAttendants().size();
				
					if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
						
						((Meeting)((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).updatePossibleSlotSum(slot, preferenceForSlot);
						
					}
					
					if (msg.getPerformative() == ACLMessage.FAILURE){
						
						((Meeting)((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).removePossibleShot(slot);
						
						ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
	      		for (AID agent : ((Meeting)((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).getAttendants()) {
	        		newMsg.addReceiver(agent);
	      		}
						
					  newMsg.setContent(Integer.toString(meetingId) + " " + Integer.toString(slot));
					  myAgent.send(newMsg);
						
					}
					
					if((Object)proposedSlot != null)
						if (((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot).getIsAvailable()){
							if (((Object)((Meeting)((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).getPossibleSlotSum(proposedSlot)) == null) {
								((Meeting) ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).addPossibleSlot(proposedSlot, preferenceForProposedSlot);
								((Meeting) ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).updatePossibleSlotSum(proposedSlot, ((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot).getPreference());
								
								
								ACLMessage reply =new ACLMessage(ACLMessage.PROPOSE);
								for (AID agent : ((Meeting)((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).getAttendants()) {
									reply.addReceiver(agent);
								}
								
								reply.setContent(Integer.toString(meetingId) + " " + Integer.toString(proposedSlot));
								myAgent.send(reply);
								
							}else{
								((Meeting)((SchedulerAgent) myAgent).organisedMeetings.get(meetingId)).updatePossibleSlotSum(proposedSlot, preferenceForProposedSlot);
							}
						}
					
				if (msg.getPerformative() == ACLMessage.INFORM) contactedAgentsWithNoMoreSlot++;
					
				}
				else {
					block();
				}
     }

			public boolean done() {

				return (contactedAgentsWithNoMoreSlot == nbOfcontactedAgents);
			}
	}
	
	private class RespondForMeeting extends CyclicBehaviour {
	  
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			ACLMessage msg = myAgent.receive(mt);
	    if (msg != null) {
				
				ACLMessage reply = msg.createReply();
				
				String[] infos = msg.getContent().split(" ");
				
				int meetingId = Integer.parseInt(infos[0]);
				int proposedSlot = Integer.parseInt(infos[1]);
				
				if ( ((Meeting)((SchedulerAgent) myAgent).meetings.get(meetingId)) == null ) ((SchedulerAgent) myAgent).meetings.put(meetingId, new ArrayList<Slot>());

				if ( ((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot).getIsAvailable() ){
					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				} else{
					reply.setPerformative(ACLMessage.FAILURE);
				}
				
				double preferenceForProposedSlot = (((ArrayList<Slot>) ((SchedulerAgent) myAgent).meetings.get(meetingId)).contains(((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot)))?0:((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot).getPreference();
				Slot preferedAvailableSlot = ((SchedulerAgent) myAgent).getPreferedAvailableSlot(((ArrayList<Slot>) ((SchedulerAgent) myAgent).meetings.get(meetingId)));
				
				if(preferedAvailableSlot != null){
				
					double preferenceForPreferedAvailableSlot = preferedAvailableSlot.getPreference();
					
					reply.setContent(Integer.toString(meetingId) + " " + Integer.toString(proposedSlot) + " " + Double.toString(preferenceForProposedSlot) + " " + Integer.toString(((SchedulerAgent) myAgent).calendar.getSlots().indexOf(preferedAvailableSlot)) + " " + Double.toString(preferenceForPreferedAvailableSlot));

				}else{
					
					ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
					newMsg.addReceiver(msg.getSender());
					newMsg.setContent(Integer.toString(meetingId) + " " + Integer.toString(proposedSlot));
					myAgent.send(newMsg);
					
					reply.setContent(Integer.toString(meetingId) + " " + Integer.toString(proposedSlot) + " " + Double.toString(preferenceForProposedSlot) + " " + null + " " + null);
					
				}
				
	      myAgent.send(reply);
				
				((ArrayList<Slot>) ((SchedulerAgent) myAgent).meetings.get(meetingId)).add(((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot));
				((ArrayList<Slot>) ((SchedulerAgent) myAgent).meetings.get(meetingId)).add(preferedAvailableSlot);
	    }
	    else {
	      block();
	    }
	  }
	}

	private class ImpossibleSlotForMeeting extends CyclicBehaviour {
	  
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
	    if (msg != null) {
				
				String[] infos = msg.getContent().split(" ");
				
				int meetingId = Integer.parseInt(infos[0]);
				int notPossibleSlot = Integer.parseInt(infos[1]);
				
				((ArrayList<Slot>) ((SchedulerAgent) myAgent).meetings.get(meetingId)).add(((SchedulerAgent) myAgent).calendar.getSlots().get(notPossibleSlot));
	    }
	    else {
	      block();
	    }
	  }
	}
	
}
