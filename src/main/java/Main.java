import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
//https://www.linkedin.com/pulse/selenium-parallel-testing-using-java-threadlocal-testng-shargo/
//https://github.com/bonigarcia/webdrivermanager

public class Main {
    private static final String HTTPS = "https://";
    private static final String HTTP = "http://";
    private static final String BASE_URL = "tomblomfield.com";
    private static final String BASE_URL_HTTPS = HTTPS + BASE_URL;
    private static final String BASE_URL_HTTP = HTTP + BASE_URL;
    private static final Set<String> foundLinks = new HashSet<>();
    private static final Queue<LinkNode> processingLinks = new ConcurrentLinkedQueue<>();
    private static WebDriver driver;
    private static ThreadLocal<WebDriver> threadLocal = new ThreadLocal<>();

    private static Set<String> getRelativeLinks() {
        return driver.findElements(By.tagName("a")).stream()
                .map(e -> e.getAttribute("href"))
                .filter(elem -> {
                    if (elem != null) {
                        return elem.startsWith(BASE_URL_HTTPS) || elem.startsWith(BASE_URL_HTTP);
                    }
                    return false;
                })
                .map(e -> {
                    int hashPosition = e.lastIndexOf("#");
                    if (hashPosition > -1) {
                        return e.substring(0, hashPosition);
                    }
                    return e;
                })
                .collect(Collectors.toSet());
    }

    public static void main(String[] args) {
//        JsonObject jsonObject = new JsonObject();
//        JsonObject jsonObject2 = new JsonObject();
//        JsonObject jsonObject3 = new JsonObject();
//        JsonObject jsonObject4 = new JsonObject();
//        jsonObject2.add("root", jsonObject);
//        jsonObject.add("a", new JsonObject());
//        jsonObject.add("b", new JsonObject());
//        jsonObject.add("c", new JsonObject());
//
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        System.out.println(gson.toJson(jsonObject2));


        System.setProperty("webdriver.chrome.driver", "src\\main\\resources\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized"); // open Browser in maximized mode
        options.addArguments("--headless"); // without this it won't start on servers

        driver = new ChromeDriver(options);





        foundLinks.add(BASE_URL_HTTPS);
        foundLinks.add(BASE_URL_HTTP);
        LinkNode root = new LinkNode(BASE_URL_HTTPS);
        processingLinks.add(root);

        while (!processingLinks.isEmpty()) {
            LinkNode currentLinkNode = processingLinks.poll();
            driver.get(currentLinkNode.getLink());
            Set<String> links = getRelativeLinks();
//            System.out.println("currentLink: " + currentLinkNode.getLink());
//            System.out.println("foundLinks:");
            for (String link : links) {
                if (!foundLinks.contains(link)) {
                    foundLinks.add(link);
                    LinkNode child = new LinkNode(link);
                    currentLinkNode.getChildren().add(child);
                    currentLinkNode.getJson().add(link, child.getJson());
                    processingLinks.add(child);
//                    System.out.println("\t " + link);
                }
            }
//            System.out.println("--------------------");
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(root.getJson()));

        driver.quit();

    }

    private static class LinkNode {
        private String link;
        private List<LinkNode> children = new ArrayList<>();
        private JsonObject json = new JsonObject();

        public LinkNode(String link){
            this.link = link;
        }

        public List<LinkNode> getChildren(){
            return children;
        }

        public String getLink(){
            return link;
        }

        public JsonObject getJson(){
            return json;
        }
    }
}




//    public static WebDriver doBrowserSetup(String browserName){
//
//        WebDriver driver = null;
//        if (browserName.equalsIgnoreCase("chrome")){
//            //steup chrome browser
//            WebDriverManager.chromedriver().setup();
//
//
//            //Add options for --headed or --headless browser launch
//            ChromeOptions chromeOptions = new ChromeOptions();
//            chromeOptions.addArguments("-headed");
//
//            //initialize driver for chrome
//            driver = new ChromeDriver(chromeOptions);
//
//            //maximize window
//            driver.manage().window().maximize();
//
//            //add implicit timeout
//            driver.manage()
//                    .timeouts()
//                    .implicitlyWait(Duration.ofSeconds(30));
//
//        }
//        return driver;
//    }







//    ExecutorService executorService = Executors.newFixedThreadPool(5);


// for(String link : links) {
//         executorService.submit(() -> {
//         if (threadLocal.get() == null)
//         threadLocal.set(new ChromeDriver(options));
//         WebDriver driver2 = threadLocal.get();
//
//         driver2.get(link);
//         System.out.println("BBB:" + driver2.getCurrentUrl());
//
//         Set<String> link2 = driver2.findElements(By.tagName("a")).stream()
//        .map(e -> e.getAttribute("href"))
//        .filter(elem -> {
//        if (elem != null) {
//        return elem.startsWith(BASE_URL_HTTPS) || elem.startsWith(BASE_URL_HTTP);
//        }
//        return false;
//        })
//        .map(e -> {
//        int hashPosition = e.lastIndexOf("#");
//        if (hashPosition > -1) {
//        return e.substring(0, hashPosition);
//        }
//        return e;
//        })
//        .collect(Collectors.toSet());
//
//        link2.forEach(System.out::println);
//        System.out.println("---");
//
//        System.out.println();
//
//        driver.quit();
//        });
//        }
//        executorService.shutdown();