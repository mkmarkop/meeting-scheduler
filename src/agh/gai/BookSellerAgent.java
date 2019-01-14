package agh.gai;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BookSellerAgent extends Agent {
    private Hashtable<String, Book> catalogue;
    private BookSellerGui myGui;

    protected void setup() {
        catalogue = new Hashtable();
        myGui = new BookSellerGui(this);
        myGui.display();

        //book selling service registration at DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-trading");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new OfferRequestsServer());

        addBehaviour(new PurchaseOrdersServer());

        addBehaviour(new DenialsOrdersServer());
    }

    protected void takeDown() {
        //book selling service deregistration at DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        myGui.dispose();
        System.out.println("Seller agent " + getAID().getName() + " terminated.");
    }

    //invoked from GUI, when a new book is added to the catalogue
    public void updateCatalogue(Book book) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogue.put(book.getTitle(), book);
                System.out.println(getAID().getLocalName() + ": " + book.getTitle() + " put into the catalogue. Price = " + book.getTotalPrice());
            }
        });
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            //proposals only template
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Book book = catalogue.get(title);

                if (book == null || book.getOffered()) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }

                if (book != null && !book.getOffered()) {
                    book.setOffered(true);
                    Integer price = book.getTotalPrice();
                    //title found in the catalogue, respond with its price as a proposal
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }


    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            //purchase order as proposal acceptance only template
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Book toRemove = catalogue.remove(title);
                //Integer price = (Integer) catalogue.remove(title);
                if (toRemove != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(getAID().getLocalName() + ": " + title + " sold to " + msg.getSender().getLocalName());
                } else {
                    //title not found in the catalogue, sold to another agent in the meantime (after proposal submission)
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class DenialsOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                catalogue.get(title).setOffered(false);
                System.out.println(getAID().getLocalName() + ": " + title + " is offered again");
            }
        }
    }

}
