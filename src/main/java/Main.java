import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {
    private static final int NUM_THREADS = (int) Math.floor(0.8 * Runtime.getRuntime().availableProcessors());
    private static final String HTTPS = "https://";
    private static final String HTTP = "http://";
    private static final String BASE_URL = "tomblomfield.com";
    private static final String BASE_URL_HTTPS = HTTPS + BASE_URL;
    private static final String BASE_URL_HTTP = HTTP + BASE_URL;

    private static final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS);
    private static final ThreadLocal<WebDriver> threadLocal = new ThreadLocal<>();
    private static final Semaphore semaphore = new Semaphore(1);

    private static volatile Queue<WebDriver> threadDrivers = new ConcurrentLinkedQueue<>();
    private static volatile Set<String> foundLinks = new ConcurrentSkipListSet<>();
    private static volatile Queue<LinkNode> processingLinks = new ConcurrentLinkedQueue<>();

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

    public static void main(String[] args) throws InterruptedException {
        System.out.println(NUM_THREADS + " threads");

        long startTime = System.nanoTime();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        foundLinks.add(BASE_URL_HTTPS);
        foundLinks.add(BASE_URL_HTTP);

        LinkNode root = new LinkNode(BASE_URL_HTTPS);
        processingLinks.add(root);

        while (!processingLinks.isEmpty() || threadPoolExecutor.getActiveCount() != 0) {
            if (threadPoolExecutor.getActiveCount() == NUM_THREADS) {
                semaphore.acquire();
            }

            threadPoolExecutor.submit(() -> {
                LinkNode currentLinkNode = processingLinks.poll();

                if (currentLinkNode == null) return;

                if (threadLocal.get() == null) {
                    WebDriver threadWebDriver = new ChromeDriver(options);
                    threadDrivers.add(threadWebDriver);
                    threadLocal.set(threadWebDriver);
                }
                WebDriver driver = threadLocal.get();
                driver.get(currentLinkNode.getLink());

                for (String link : getAllLinks(driver)) {
                    if (!isLinkAccessibleFromBaseUrl(link)) {
                        currentLinkNode.addJsonProperty(link, new JsonObject());
                    } else if (!foundLinks.contains(link)) {
                        LinkNode child = new LinkNode(link);
                        currentLinkNode.addJsonProperty(link, child.getJson());

                        foundLinks.add(link);
                        processingLinks.add(child);
                    }
                }

                semaphore.release();
            });

        }

        threadDrivers.forEach(WebDriver::quit);
        threadPoolExecutor.shutdown();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(root.getJson()));

        long stopTime = System.nanoTime();
        System.out.println(((double) (stopTime - startTime) / 1_000_000_000L) + " seconds");
    }

    private static class LinkNode {
        private final String link;
        private final JsonObject json = new JsonObject();

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
