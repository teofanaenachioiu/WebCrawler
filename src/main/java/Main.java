import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
//https://www.linkedin.com/pulse/selenium-parallel-testing-using-java-threadlocal-testng-shargo/
//https://github.com/bonigarcia/webdrivermanager


public class Main {
    private static final String HTTPS = "https://";
    private static final String HTTP = "http://";
    private static final String BASE_URL = "tomblomfield.com/";
    private static final String BASE_URL_HTTPS = HTTPS + BASE_URL;
    private static final String BASE_URL_HTTP = HTTP + BASE_URL;
    private static final Set<String> foundLinks = new HashSet<>();
    private static final Queue<LinkNode> processingLinks = new ConcurrentLinkedQueue<>();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private static WebDriver driver;
    private static ThreadLocal<WebDriver> threadLocal = new ThreadLocal<>();

    private static boolean isLinkAccessibleFromBaseUrl(String link) {
        return link.startsWith(BASE_URL_HTTPS) || link.startsWith(BASE_URL_HTTP);
    }

    private static Set<String> getAllLinks(WebDriver webDriver) {
        return webDriver.findElements(By.tagName("a")).stream()
                .map(e -> e.getAttribute("href"))
                .filter(Objects::nonNull)
                .map(e -> {
                    int hashPosition = e.lastIndexOf("#");
                    if (hashPosition > -1) {
                        return e.substring(0, hashPosition);
                    }
                    return e;
                })
                .collect(Collectors.toSet());
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(options);

        foundLinks.add(BASE_URL_HTTPS);
        foundLinks.add(BASE_URL_HTTP);

        LinkNode root = new LinkNode(BASE_URL_HTTPS);
        processingLinks.add(root);

//        && Thread.getAllStackTraces().keySet().stream().filter(t -> t.equals(Thread.currentThread())).anyMatch(Thread::isAlive)
        while (!processingLinks.isEmpty()) {
            LinkNode currentLinkNode = processingLinks.poll();
            driver.get(currentLinkNode.getLink());

            System.out.println("processing " + currentLinkNode.getLink());

            for (String link : getAllLinks(driver)) {
                if (!isLinkAccessibleFromBaseUrl(link)) {
//                    currentLinkNode.addJsonProperty(link, new JsonObject());
                } else if (!foundLinks.contains(link)) {
                    LinkNode child = new LinkNode(link);
                    currentLinkNode.addJsonProperty(link, child.getJson());

                    foundLinks.add(link);
                    processingLinks.add(child);
                }
            }
        }

        driver.quit();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(root.getJson()));

    }

    private static class LinkNode {
        private String link;
        private JsonObject json = new JsonObject();

        public LinkNode(String link) {
            this.link = link;
        }

        public String getLink() {
            return link;
        }

        public JsonObject getJson() {
            return json;
        }

        public void addJsonProperty(String key, JsonObject json) {
            this.json.add(key, json);
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