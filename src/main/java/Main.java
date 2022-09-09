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

    private static final Queue<WebDriver> threadDrivers = new ConcurrentLinkedQueue<>();
    private static final Set<String> foundLinks = new ConcurrentSkipListSet<>();
    private static final Queue<LinkNode> processingLinks = new ConcurrentLinkedQueue<>();

    private static boolean isLinkAccessibleFromBaseUrl(String link) {
        return link.startsWith(BASE_URL_HTTPS) || link.startsWith(BASE_URL_HTTP);
    }

    private static String removeNoiseFromLink(String linkWithNoise) {
        String link = linkWithNoise;

        int hashPosition = link.lastIndexOf("#");
        if (hashPosition > -1) {
            link = link.substring(0, hashPosition);
        }

        if (link.endsWith("/")) {
            link = link.substring(0, link.length() - 1);
        }

        return link;
    }

    private static Set<String> getAllLinks(WebDriver webDriver) {
        return webDriver.findElements(By.tagName("a")).stream()
                .map(e -> e.getAttribute("href"))
                .filter(Objects::nonNull)
                .map(Main::removeNoiseFromLink)
                .collect(Collectors.toSet());
    }

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.nanoTime();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        foundLinks.add(BASE_URL_HTTPS);
        foundLinks.add(BASE_URL_HTTP);

        LinkNode root = new LinkNode(BASE_URL_HTTPS);
        processingLinks.add(root);

        while (!processingLinks.isEmpty() || threadPoolExecutor.getActiveCount() != 0) {
            /*
                wait until at least one thread from threadPool is available
            */
            if (threadPoolExecutor.getActiveCount() == NUM_THREADS) {
                semaphore.acquire();
            }

            /*
                wait 0.5 seconds when the Queue is empty but we still have running threads,
                and there is a change that they may produce new processingLinks
            */
            if (processingLinks.isEmpty() && threadPoolExecutor.getActiveCount() != 0) {
                Thread.sleep(500L);
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
        threadPoolExecutor.shutdownNow();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(root.getJson()));

        long stopTime = System.nanoTime();

        System.out.println(((double) (stopTime - startTime) / 1_000_000_000L) + " seconds");
        System.exit(0);
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
