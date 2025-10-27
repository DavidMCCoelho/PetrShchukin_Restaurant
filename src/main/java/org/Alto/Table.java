package org.Alto;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Table { // number of chairs

    private static int cnt = 0;
    private final int number;

    private final int size;

    private final List<ClientsGroup> clients;

    public Table(int size) {
        this.number = ++cnt;
        this.size = size;
        this.clients = new ArrayList<>();
    }

    public Table(String serializedTable) {
        Pattern p = Pattern.compile("\\{([0-9]+), ([0-9]+)}");
        Matcher m = p.matcher(serializedTable);
        if (!m.find())
            throw new RuntimeException(String.format("An error occurred while processing '%s'!", serializedTable));
        try {
            this.number = Integer.parseInt(m.group(1));
            this.size = Integer.parseInt(m.group(2));
        }
        catch (Exception ex) {
            throw new RuntimeException(String.format("An error occurred while processing '%s'!", serializedTable), ex);
        }
        this.clients = new ArrayList<>();
    }

    public int getSeatNumber() {
        return size;
    }

    @Override
    public String toString() {
        return String.format("Table#%d (%d seats): %s", number, size, getClients().isEmpty() ? "free" : Arrays.toString(getClients().toArray()));
    }

    public String serialize() {
        return String.format("{%d, %d}", number, size);
    }

    public int occupiedSeats() {
        return clients.stream().map(ClientsGroup::getGroupSize).reduce(0, Integer::sum);
    }

    public int freeSeats() {
        return getSeatNumber() - occupiedSeats();
    }

    public void addClient(ClientsGroup client) {
        this.clients.add(client);
    }

    public List<ClientsGroup> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public boolean removeAll(Collection<ClientsGroup> c) {
        return clients.removeAll(c);
    }

}
