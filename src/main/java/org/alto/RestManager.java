package org.alto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RestManager {

    private static final int MIN_TABLE_CNT = 8;
    private static final int MAX_TABLE_CNT = 16;

    private static final int MIN_CLIENTS_PER_TABLE = 2;
    private static final int MAX_CLIENTS_PER_TABLE = 6;

    private static final int MIN_ARRIVALS_PER_ITERATION = 0;
    private static final int MAX_ARRIVALS_PER_ITERATION = 3;

    private static final Random rnd = new Random();


    private final List<Table> tables;
    private final List<ClientsGroup> waitingClientsGroup;

    public RestManager() {
        this.tables = generateRandomTables();
        this.waitingClientsGroup = new ArrayList<>();
    }

    public RestManager(String tableSet) {
        this.tables = Main.processParams(tableSet).stream()
                .map(Table::new)
                .toList();
        this.waitingClientsGroup = new ArrayList<>();
    }

    /*
    Groups may share tables, however
        if at the same time you have an empty table with the required number of chairs
            and enough empty chairs at a larger one,
            you must always seat your client(s) at an empty table and not any partly seated one,
     even if the empty table is bigger than the size of the group.
     */

    // new client(s) show up
    public void onArrive(ClientsGroup group) {
        waitingClientsGroup.add(group);
    }

    public void seatGroups() {
        List<ClientsGroup> seatedClientGrp = new ArrayList<>();
        for (ClientsGroup currClientGrp : waitingClientsGroup) {
            Optional<Table> freeTableFound = tables.stream()
                    .filter(t -> t.occupiedSeats() == 0)
                    .filter(t -> currClientGrp.getGroupSize() <= t.getSeatNumber())
                    .min(Comparator.comparing(Table::getSeatNumber, Comparator.comparingInt(tsz -> tsz)));

            if (freeTableFound.isEmpty()) { // try to get partially full table
                freeTableFound = tables.stream()
                        .filter(t -> currClientGrp.getGroupSize() <= t.freeSeats())
                        .min(Comparator.comparing(Table::getSeatNumber, Comparator.comparingInt(tsz -> tsz)));
            }

            if (freeTableFound.isPresent()) {
                seatedClientGrp.add(currClientGrp);
                freeTableFound.get().addClient(currClientGrp);
            }
        }
        waitingClientsGroup.removeAll(seatedClientGrp);
    }

    // client(s) leave, either served or simply abandoning the queue
    public void onLeave(ClientsGroup leavingGroup) {
        Table occupiedTable = lookUp(leavingGroup);
        if (null != occupiedTable) {
            occupiedTable.removeAll(List.of(leavingGroup));
        }
        else {
            waitingClientsGroup.removeAll(List.of(leavingGroup));
        }
    }

    // return table where a given client group is seated,
    // or null if it is still queueing or has already left
    public Table lookUp(ClientsGroup group) {
        return tables.stream()
                .filter(t -> t.getClients().contains(group))
                .findFirst()
                .orElse(null);
    }

    public List<Table> getTables() {
        return this.tables;
    }

    public List<ClientsGroup> getWaitingClientsGroup() {
        return Collections.unmodifiableList(this.waitingClientsGroup);
    }

    public void markPassageOfTime() {
        waitingClientsGroup.forEach(ClientsGroup::markPassageOfTime);
        tables.stream()
                .filter(t -> 0 < t.occupiedSeats())
                .flatMap(t -> t.getClients().stream())
                .forEach(ClientsGroup::markPassageOfTime);
    }

    public List<ClientsGroup> cleanupWaitingClientGroups() {
        List<ClientsGroup> clientGroupsWhoAbandonedQueue = waitingClientsGroup.stream().filter(ClientsGroup::ranOutOfTime).toList();
        waitingClientsGroup.removeAll(clientGroupsWhoAbandonedQueue);
        return clientGroupsWhoAbandonedQueue;
    }

    public List<ClientsGroup> cleanupTables() {
        List<ClientsGroup> allCgWhoFinishedMeal = new ArrayList<>();
        for (Table cleanUpTable : tables) {
            List<ClientsGroup> cgWhoFinishedMeal = cleanUpTable.getClients().stream()
                    .filter(ClientsGroup::ranOutOfTime)
                    .toList();
            cleanUpTable.removeAll(cgWhoFinishedMeal);
            allCgWhoFinishedMeal.addAll(cgWhoFinishedMeal);
        }
        return allCgWhoFinishedMeal;
    }

    public void run(List<String> serializedArrivals) {
        List<List<String>> processedSerializedArrivals = serializedArrivals.stream()
                .map(String::trim)
                .map(Main::processParams)
                .toList();

        List<List<ClientsGroup>> arrivalsParam = new ArrayList<>();
        for (List<String> processedSerializedArrival : processedSerializedArrivals) {
            List<ClientsGroup> clientGroupsArrival = new ArrayList<>();
            for (String serializedClientGroups : processedSerializedArrival)
                if (!"{}".equals(serializedClientGroups))
                    clientGroupsArrival.add(new ClientsGroup(serializedClientGroups));
            arrivalsParam.add(clientGroupsArrival);
        }

        runAux(arrivalsParam);
    }

    public void run() {
        runAux(null);
    }

    private void runAux(List<List<ClientsGroup>> arrivalsParam) {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Press ENTER to iterate; any key + ENTER to stop;");
        String readString = scanner.nextLine(); // it's not redundant :)

        List<List<ClientsGroup>> arrivalsHistory = new ArrayList<>();
        int iter = 0;
        do {
            System.out.println("---------------> Iteration#" + ++iter);
            System.out.println("ClientGroups who finished meal: " + Arrays.toString(cleanupTables().toArray()));
            System.out.println("ClientGroups who abandoned queue: " + Arrays.toString(cleanupWaitingClientGroups().toArray()));

            List<ClientsGroup> arrivingCGs;
            if (null == arrivalsParam)
            	arrivingCGs = generateRandomClientGroups();
            else if(!arrivalsParam.isEmpty())
            	arrivingCGs = arrivalsParam.remove(0);
            else
            	break;
            
            arrivalsHistory.add(arrivingCGs);
            arrivingCGs.forEach(this::onArrive);
            this.seatGroups();

            System.out.println(this.getTables().stream().map(Objects::toString).collect(Collectors.joining("\n")));
            System.out.println("Waiting ClientsGroup: " + Arrays.toString(this.getWaitingClientsGroup().toArray()));

            this.markPassageOfTime();

            readString = scanner.nextLine();
        } while(readString.isEmpty());
        scanner.close();

        tables.forEach(t -> t.removeAll(t.getClients()));

        // how replicate this run
        System.out.printf("%n%n>>> If you want to re-execute this exact iteration set run the following command:%njava -Dfile.encoding=UTF-8 -classpath %s\\target\\classes org.alto.Main %s %s%n",
            System.getProperty("user.dir"),
            tables.stream().map(Table::serialize).collect(Collectors.joining(", ", "\"", "\"")),
            arrivalsHistory.stream()
                    .map(cgl -> cgl.stream().map(ClientsGroup::serialize).collect(Collectors.joining(", ", "{", "}")))
                    .collect(Collectors.joining(", ", "\"", "\""))
        );

    }

    private List<Table> generateRandomTables() {
        return IntStream.range(1, rnd.nextInt(MIN_TABLE_CNT, MAX_TABLE_CNT + 1) + 1) // create between MIN_TABLE_CNT and MAX_TABLE_CNT tables
                .mapToObj(i -> new Table(rnd.nextInt(MIN_CLIENTS_PER_TABLE, MAX_CLIENTS_PER_TABLE + 1))) // each table must accommodate between MIN_CLIENTS_PER_TABLE and MAX_CLIENTS_PER_TABLE
                .sorted(Comparator.comparingInt(Table::getSeatNumber))
                .toList();
    }

    private List<ClientsGroup> generateRandomClientGroups() {
        return IntStream.range(1, rnd.nextInt(MIN_ARRIVALS_PER_ITERATION, MAX_ARRIVALS_PER_ITERATION + 1) + 1) // between MIN_ARRIVALS and MAX_ARRIVALS arrive per iter
                .mapToObj(i -> new ClientsGroup()) // each table must accommodate between MIN_CLIENTS_PER_TABLE and MAX_CLIENTS_PER_TABLE
                .toList();
    }


}
