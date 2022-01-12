import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.internal.logging.JULogging;
import org.neo4j.driver.types.Node;

import static org.neo4j.driver.Config.TrustStrategy.trustAllCertificates;

public class DriverIntroductionExample implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(DriverIntroductionExample.class.getName());
    private final Driver driver;

    public DriverIntroductionExample(String uri, String user, String password, Config config) {
        // The driver is a long living object and should be opened during the start of your application
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config);
    }

    @Override
    public void close() throws Exception {
        // The driver object should be closed before the application ends.
        driver.close();
    }

    public void createFriendshipAndNodes(String fstPer, final String person1Name, String secPer, final String person2Name, String frdName) {
        // To learn more about the Cypher syntax, see https://neo4j.com/docs/cypher-manual/current/
        // The Reference Card is also a good resource for keywords https://neo4j.com/docs/cypher-refcard/current/
        String createFriendshipQuery = "CREATE (p1:" + fstPer + "{ name: $person1_name })\n" +
                "CREATE (p2:" + secPer + "{ name: $person2_name })\n" +
                "CREATE (p1)-[:"+frdName+"]->(p2)\n" +
                "RETURN p1, p2";

        Map<String, Object> params = new HashMap<>();
        params.put("Test", fstPer);
        params.put("person1_name", person1Name);
        params.put("Test2", secPer);
        params.put("person2_name", person2Name);

        try (Session session = driver.session()) {
            // Write transactions allow the driver to handle retries and transient errors
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(createFriendshipQuery, params);
                return result.single();
            });

            System.out.println(String.format("Создано отношение между: %s, %s",
                    record.get("p1").get("name").asString(),
                    record.get("p2").get("name").asString()));
            // You should capture any errors along with the query and data for traceability
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, createFriendshipQuery + " raised an exception", ex);
            throw ex;
        }
    }

    public void removeFriendship(final String name_1, final String name_11, final String name_2, final String name_22, final String rel) {
        String removeFriendshipQuery = "MATCH(p1:" + name_1 + "{name: $name1})-[" + rel + "]->(p2:" + name_2 + "{name: $name2})\n" +
                "DELETE " + rel + "\n" +
                "RETURN p1, p2";

        Map<String, Object> params = new HashMap<>();
        params.put("name1", name_11);
        params.put("name2", name_22);

        try (Session session = driver.session()) {
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(removeFriendshipQuery, params);
                return result.single();
            });
            System.out.println(String.format("Removed friendship between: %s - %s -> %s",
                    name_11,
                    rel,
                    name_22
            ));
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, removeFriendshipQuery + " raised an exception", ex);
            throw ex;
        }
    }

    public void createNode(final String class_1, final String name_1) {
        String createNode = "CREATE (p1:" + class_1 + "{name: $name1})\n" +
                "RETURN p1";
        Map<String, Object> params = new HashMap<>();
        params.put("name1", name_1);

        try (Session session = driver.session()) {
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(createNode, params);
                return result.single();
            });
            System.out.println(String.format("Created Node - %s",
                    name_1
            ));
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, createNode + " raised an exception", ex);
            throw ex;
        }
    }

    public void removeNode(final String class_1, final String name_1) {
        String removeNode = "MATCH(p1:" + class_1 + "{name: $name1})\n" +
                "DELETE p1\n" +
                "RETURN p1";
        Map<String, Object> params = new HashMap<>();
        params.put("name1", name_1);

        try (Session session = driver.session()) {
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(removeNode, params);
                return result.single();
            });
            System.out.println(String.format("Removed Node - %s",
                    name_1
            ));
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, removeNode + " raised an exception", ex);
            throw ex;
        }
    }

    public void get4(String name1, String name2){
        String readPersonByNameQuery = "MATCH (:"+name1+"{name: '"+name2+ "'})-[r]-(p)\n" +
                "RETURN p.name, type(r) as d";
        try (Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run(readPersonByNameQuery);
                while (result.hasNext()) {
                    Record record = result.next();
                    System.out.println(String.format("Название связи:\"%s\" --> Наследуемая нода: \"%s\"", record.get("d").asString(), record.get("p.name").asString()));
                }
                return result;
            });
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, readPersonByNameQuery + " raised an exception", ex);
            throw ex;
        }
    }

    public void get1() {
        String readPersonByNameQuery = "MATCH path=shortestPath((p:Screen {name: 'MainView'})-[*]-(p2:Action {name: 'payOrder'}))\n" +
                "RETURN path";
        try (Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run(readPersonByNameQuery);
                while (result.hasNext()) {
                    Record record = result.next();
                    System.out.println(record.get("path"));
                    String d = record.toString();
                    Pattern pattern = Pattern.compile("[(\\[{](.*?)[)\\]}]");
                    Matcher matcher = pattern.matcher(d);
                    List<String> lst = new ArrayList<>();
                    while (matcher.find()) {
                        lst.add(matcher.group(1));
                    }
                    int z = lst.size();
                    for (int i = 0; i < z; i++){
                        lst.get(i).split("[(\\[{](.*?)[)\\]}]");
                    }
                    String mn = lst.get(0).replace("path: path[(","");
                    //System.out.println(mn);
                    lst.add(0,mn);
                    lst.remove(1);
                    int[] vowels = new int[lst.size()];
                    String[] tst = new String[lst.size()];
                    for (int i = 0; i < z; i++){
                        System.out.println(lst.get(i));
                    }
                    for (int i = 0;i<z;i++){
                        if(lst.get(i).length()<3){
                            vowels[i] = Integer.parseInt(lst.get(i));
                        } else if (lst.get(i).length()>3){
                            tst = lst.get(i).split(":");
                        }
                    }
                    int count = 1;
                    for (int i = 0; i < lst.size()-2;i = i+3) {
                        int first_id = vowels[i];
                        int second_id = vowels[i+2];
                        String testdrive = "MATCH (s)-[r]-(p)\n" +
                                "WHERE ID(s)="+first_id+" AND ID(p)="+second_id+"\n"+
                                "RETURN s.name, type(r) as d, p.name";
                        Result res = tx.run(testdrive);
                        while(res.hasNext()){
                            Record rec = res.next();
                            System.out.println(String.format("\"%d\") От экрана:\"%s\" --> По отношению: \"%s\" --> Переходим к экрану: \"%s\"", count, rec.get("s.name").asString(), rec.get("d").asString(),rec.get("p.name").asString()));
                            count++;
                        }
                    }
                    //System.out.println(lst.get(0));
                }
                return result;
            });
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, readPersonByNameQuery + " raised an exception", ex);
            throw ex;
        }

    }

    public void get5(String name1, String name2) {
        String readPersonByNameQuery = "MATCH (:"+name1+"{name: '"+name2+ "'})-[r]-(p)\n" +
                "RETURN p.name, type(r) as d";
        try (Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run(readPersonByNameQuery);
                while (result.hasNext()) {
                    Record record = result.next();
                    System.out.println(String.format("Название связи:\"%s\" --> Наследуемая нода: \"%s\"", record.get("d").asString(), record.get("p.name").asString()));
                }
                return result;
            });
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, readPersonByNameQuery + " raised an exception", ex);
            throw ex;
        }
    }

    public void renameNode(final String class_1, final String name_1, final String alias, final String descr, final String ne) {
        String renameNode = "MATCH (p1:" + class_1 + "{name:$name1, alias:$alias, description:$descr})\n" +
                "SET p1.name=\'" + ne + "\'\n" +
                "RETURN p1";
        Map<String, Object> params = new HashMap<>();
        params.put("name1", name_1);
        params.put("alias", alias);
        params.put("descr", descr);
        try (Session session = driver.session()) {
            Record record = session.writeTransaction(tx -> {
                Result result = tx.run(renameNode, params);
                return result.single();
            });
            System.out.println("Renamed_node");
        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, renameNode + " raised an exception", ex);
            throw ex;
        }
    }

//    public void fourthTest(){
//        String fourthTest = "";
//        Map<String, Object> params = new HashMap<>();
//        params.put("name1", name_1);
//
//        try (Session session = driver.session()) {
//            Record record = session.writeTransaction(tx -> {
//                Result result = tx.run(removeNode, params);
//                return result.single();
//            });
//            System.out.println(String.format("Removed Node - %s",
//                    name_1
//            ));
//        } catch (Neo4jException ex) {
//            LOGGER.log(Level.SEVERE, removeNode + " raised an exception", ex);
//            throw ex;
//        }
//    }


    public static void main(String... args) throws Exception {
        // Aura queries use an encrypted connection using the "neo4j+s" protocol
        Scanner sc = new Scanner(System.in);
        Scanner sc2 = new Scanner(System.in);
        String uri = "neo4j+s://af26b91e.databases.neo4j.io:7687";

        String user = "neo4j";
        String password = "BSn0N2AXT_HPTGZRHTUhtF9nyy72VpsNxUItRkVnFvw";
        String fstPer = "";
        String secPer = "";
        String person1Name = "";
        String person2Name = "";
        String frdName = "";
        String name1 = "";
        String name2 = "";
        System.out.println("1 -- Создание двух нод и отношения между ними");
        System.out.println("2 -- Удаление отношения");
        System.out.println("3 -- Второй запрос");
        System.out.println("4 -- Третий запрос");
        System.out.println("Введите ключ меню: ");
        int choose_id = sc.nextInt();

        DriverIntroductionExample app = new DriverIntroductionExample(uri, user, password, Config.defaultConfig());
        switch (choose_id){
            case 1:
                System.out.println("Введите тип первой ноды:");
                fstPer = sc2.nextLine();
                System.out.println("Введите название первой ноды:");
                person1Name = sc2.nextLine();
                System.out.println("Введите тип второй ноды: ");
                secPer = sc2.nextLine();
                System.out.println("Введите название второй ноды: ");
                person2Name = sc2.nextLine();
                System.out.println("Введите название связи между ними: ");
                frdName = sc2.nextLine();
                app.createFriendshipAndNodes(fstPer,person1Name,secPer,person2Name,frdName);
                break;
            case 2:
                System.out.println("Введите тип первой ноды:");
                fstPer = sc2.nextLine();
                System.out.println("Введите название первой ноды:");
                person1Name = sc2.nextLine();
                System.out.println("Введите тип второй ноды: ");
                secPer = sc2.nextLine();
                System.out.println("Введите название второй ноды: ");
                person2Name = sc2.nextLine();
                System.out.println("Введите название связи между ними: ");
                frdName = sc2.nextLine();
                app.removeFriendship(fstPer,person1Name,secPer,person2Name,frdName);
                break;
            case 3:
                System.out.println("Введите тип ноды: ");
                name1 = sc2.nextLine();
                System.out.println("Введите название ноды: ");
                name2 = sc2.nextLine();
                app.get5(name1,name2);
                break;
            case 4:
                app.get1();
        }
    }
}