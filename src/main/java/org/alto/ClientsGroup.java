package org.alto;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientsGroup {

    private static final Random rnd = new Random();

    private static final int MIN_ITER = 6;
    private static final int MAX_ITER = 12;

    private static final int GROUP_MAX_SIZE = 6;

    private static int cnt = 0;
    private final int number;

    private final int size;
    private final int originalIterations;
    private int iterationsRemaining;

    public ClientsGroup() {
        this(rnd.nextInt(1, GROUP_MAX_SIZE + 1)); // Clients arrive alone or in groups, up to 6 persons
    }

    public ClientsGroup(int size) {
        this.number = ++cnt;
        this.size = size;
        this.iterationsRemaining =  rnd.nextInt(MIN_ITER, MAX_ITER +1);
        this.originalIterations =  iterationsRemaining;
    }

    private final Pattern p = Pattern.compile("\\{(\\d+), (\\d+), (\\d+)}");
    
    public ClientsGroup(String serializedClientGroups) {
        Matcher m = p.matcher(serializedClientGroups);
        if (!m.find())
            throw new RuntimeException(String.format("An error occurred while processing '%s'!", serializedClientGroups));
        try {
            this.number = Integer.parseInt(m.group(1));
            this.size = Integer.parseInt(m.group(2));
            this.iterationsRemaining =  Integer.parseInt(m.group(3));
            this.originalIterations =  iterationsRemaining;
        }
        catch (Exception ex) {
            throw new RuntimeException(String.format("An error occurred while processing '%s'!", serializedClientGroups), ex);
        }
    }

    public void markPassageOfTime() {
        --iterationsRemaining;
    }

    public boolean ranOutOfTime() {
        return iterationsRemaining == 0;
    }

    public int getGroupSize() {
        return size;
    }

    @Override
    public String toString() {
        return String.format("ClientsGroup#%d clients: %d, iter: %d", number, size, iterationsRemaining);
    }

    public String serialize() {
        return String.format("{%d, %d, %d}", number, size, originalIterations);
    }
}
