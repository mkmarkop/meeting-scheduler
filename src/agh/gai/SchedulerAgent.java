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
    private HashMap<Integer, Meeting> organisedMeetings;
    private HashMap<Integer, List<Slot>> proposedMeetings;

    private static final int CALENDAR_SIZE = 24;

    private static final int MEETING_INTERVAL = 4000;

    public SchedulerAgent() {
        contacts = new HashSet<>();
        calendar = new Calendar(CALENDAR_SIZE);
        organisedMeetings = new HashMap<>();
        proposedMeetings = new HashMap<>();
    }

    @Override
    protected void setup() {
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
                        System.out.println("Agent " + myAgent.getAID().getLocalName() + " added Agent "
                                + description.getName().getLocalName() + " to contact list.");
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                    System.exit(-1);
                }
                System.out.println("Agent " + getAID().getLocalName() + " found " + contacts.size() + " contacts.");
            }
        });

        addBehaviour(new TickerBehaviour(this, MEETING_INTERVAL) {
            @Override
            public void onTick() {
                if (contacts.size() != 0) {

                    Set<AID> contactedAgents = new HashSet<>();
                    int nbOfAgentsToContact = new Random().nextInt(contacts.size()) + 1;
                    int contactedAgent;

                    List<AID> agentsList = new ArrayList<>(contacts);

                    for (int i = 0; i < nbOfAgentsToContact; i++) {
                        do {
                            contactedAgent = new Random().nextInt(contacts.size());
                        } while (contactedAgents.contains(agentsList.get(contactedAgent)));
                        contactedAgents.add(agentsList.get(contactedAgent));
                    }

                    Meeting newMeeting = new Meeting(contactedAgents);
                    organisedMeetings.put(newMeeting.getId(), newMeeting);
                    proposedMeetings.put(newMeeting.getId(), new ArrayList<>());

                    ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                    for (AID agent : newMeeting.getAttendants()) {
                        msg.addReceiver(agent);
                    }

                    Slot preferredSlotForMeeting = getPreferredAvailableSlot(proposedMeetings.get(newMeeting.getId()));

                    if (preferredSlotForMeeting != null) {

                        organisedMeetings.get(newMeeting.getId())
                                .addPossibleSlot(
                                        calendar.getSlots().indexOf(preferredSlotForMeeting),
                                        preferredSlotForMeeting.getPreference());

                        msg.setContent(newMeeting.getId() + " " + calendar.getSlots().indexOf(preferredSlotForMeeting));
                        myAgent.send(msg);

                        System.out.println("Agent " + getAID().getLocalName() +
                                " is scheduling a new meeting " + newMeeting.getId() + " with " + nbOfAgentsToContact + " contacts.");
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

    // Maybe index?
    private Slot getPreferredAvailableSlot(List<Slot> notPossibleSlots) {
        Slot preferredAvailableSlot = null;
        for (Slot slot : calendar.getSlots()) {
            if (!notPossibleSlots.contains(slot) && slot.getIsAvailable())
                if (preferredAvailableSlot == null || (preferredAvailableSlot.getPreference() < slot.getPreference()))
                    preferredAvailableSlot = slot;
        }

        return preferredAvailableSlot;
    }


    private class OrganiseMeeting extends Behaviour {

        private int contactedAgentsWithNoMoreSlot = 0;
        private int nbOfContactedAgents;

        public void action() {
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

                System.out.println(msg.getContent());
                String[] infos = msg.getContent().split(" ");

                int meetingId = Integer.parseInt(infos[0]);
                int slot = Integer.parseInt(infos[1]);
                double preferenceForSlot = Double.parseDouble(infos[2]);
                int proposedSlot = Integer.parseInt(infos[3]);
                double preferenceForProposedSlot = Double.parseDouble(infos[4]);

                nbOfContactedAgents = ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId).getAttendants().size();

                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId).updatePossibleSlotSum(slot, preferenceForSlot);
                    System.out.println("Agent " + msg.getSender() + " can come on slot " + slot + " for meeting " + meetingId);
                } else if (msg.getPerformative() == ACLMessage.FAILURE) {
                    ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId).removePossibleSlot(slot);
                    System.out.println("Agent " + msg.getSender() + " can not come on slot " + slot + " for meeting " + meetingId);

                    ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
                    for (AID agent : ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId).getAttendants()) {
                        newMsg.addReceiver(agent);
                    }

                    newMsg.setContent(meetingId + " " + slot);
                    myAgent.send(newMsg);

                }

                if (proposedSlot != -1)
                    if (((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot).getIsAvailable()) {
                        if (((SchedulerAgent) myAgent).organisedMeetings.get(meetingId) == null) {
                            ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId).addPossibleSlot(proposedSlot, preferenceForProposedSlot);
                            ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId).updatePossibleSlotSum(proposedSlot, ((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot).getPreference());


                            ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
                            for (AID agent : ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId).getAttendants()) {
                                reply.addReceiver(agent);
                            }

                            reply.setContent(meetingId + " " + proposedSlot);
                            myAgent.send(reply);

                        } else {
                            ((SchedulerAgent) myAgent).organisedMeetings.get(meetingId).updatePossibleSlotSum(proposedSlot, preferenceForProposedSlot);
                        }
                    }

                if (msg.getPerformative() == ACLMessage.INFORM) contactedAgentsWithNoMoreSlot++;

            } else {
                block();
            }
        }

        public boolean done() {
            return false; // this should be changed
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

                System.out.println("Agent " + myAgent.getAID().getLocalName() + " received proposal for meeting "
                        + meetingId + " from " + msg.getSender().getLocalName() );

                if (((SchedulerAgent) myAgent).proposedMeetings.get(meetingId) == null)
                    ((SchedulerAgent) myAgent).proposedMeetings.put(meetingId, new ArrayList<>());

                if (((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot).getIsAvailable()) {
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                } else {
                    reply.setPerformative(ACLMessage.FAILURE);
                }

                double preferenceForProposedSlot =
                        (((SchedulerAgent) myAgent).proposedMeetings.get(meetingId).contains(((SchedulerAgent) myAgent)
                                .calendar.getSlots().get(proposedSlot))) ?
                                0 : ((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot).getPreference();

                Slot preferredAvailableSlot = ((SchedulerAgent) myAgent).getPreferredAvailableSlot(((SchedulerAgent) myAgent).proposedMeetings.get(meetingId));

                if (preferredAvailableSlot != null) {
                    double preferenceForPreferredAvailableSlot = preferredAvailableSlot.getPreference();
                    reply.setContent(meetingId + " " + proposedSlot + " " + preferenceForProposedSlot + " " +
                            ((SchedulerAgent) myAgent).calendar.getSlots().indexOf(preferredAvailableSlot) + " "
                            + preferenceForPreferredAvailableSlot);
                } else {
                    reply.setContent(meetingId + " " + proposedSlot + " " +
                            preferenceForProposedSlot + " -1 -1");
                }

                myAgent.send(reply);

                ((SchedulerAgent) myAgent).proposedMeetings.get(meetingId).add(((SchedulerAgent) myAgent).calendar.getSlots().get(proposedSlot));
                ((SchedulerAgent) myAgent).proposedMeetings.get(meetingId).add(preferredAvailableSlot);
            } else {
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

                ((SchedulerAgent) myAgent).proposedMeetings.get(meetingId).add(((SchedulerAgent) myAgent).calendar.getSlots().get(notPossibleSlot));
            } else {
                block();
            }
        }
    }

}
