package agh.gai;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.HashSet;
import java.util.Set;

public class SchedulerAgent extends Agent {
    private Set<AID> contacts;
    private Calendar calendar;

    public SchedulerAgent() {
        contacts = new HashSet<>();
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

        try {
            DFService.register(this, template);
        } catch (FIPAException fe) {
            fe.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Agent " + getAID().getLocalName() + " registered itself.");

        addBehaviour(new MeetingBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println("Agent " + getAID().getLocalName() + " terminated.");
    }

    private class MeetingBehaviour extends OneShotBehaviour {
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
    }
}
