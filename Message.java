class Subscriber {
    private final int id;
    private final String nameSurname;
    private final long subscriptionTime;
    private final long lastActiveTime;
    private final String[] topics;
    private final boolean isActive;

    public Subscriber(int id, String nameSurname, long subscriptionTime, long lastActiveTime, String[] topics, boolean isActive) {
        this.id = id;
        this.nameSurname = nameSurname;
        this.subscriptionTime = subscriptionTime;
        this.lastActiveTime = lastActiveTime;
        this.topics = topics != null ? topics.clone() : new String[0]; // Defensive copy
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public String getNameSurname() {
        return nameSurname;
    }

    public long getSubscriptionTime() {
        return subscriptionTime;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public String[] getTopics() {
        return topics.clone(); // Defensive copy
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "id=" + id +
                ", nameSurname='" + nameSurname + '\'' +
                ", subscriptionTime=" + subscriptionTime +
                ", lastActiveTime=" + lastActiveTime +
                ", topics=" + String.join(", ", topics) +
                ", isActive=" + isActive +
                '}';
    }
}

public class Message {
    public enum Demand {
        SUBS,
        DEL,
        CPCTY,
        STRT
    }

    private final Demand demand;
    private final Subscriber subscriber;
    private String response;

    public Message(Demand demand, Subscriber subscriber, String response) {
        this.demand = demand;
        this.subscriber = subscriber;
        this.response = response;
    }

    public Message(Demand demand, String response) {
        this(demand, null, response);
    }

    public Demand getDemand() {
        return demand;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "Message{" +
                "demand=" + demand +
                ", subscriber=" + (subscriber != null ? subscriber.getNameSurname() : "null") +
                ", response='" + response + '\'' +
                '}';
    }
}
