import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
//https://www.linkedin.com/pulse/selenium-parallel-testing-using-java-threadlocal-testng-shargo/
//https://github.com/bonigarcia/webdrivermanager

public class Main {
    private static final String HTTPS = "https://";
    private static final String HTTP = "http://";
    private static final String BASE_URL = "tomblomfield.com";
    private static final String BASE_URL_HTTPS = HTTPS + BASE_URL;
    private static final String BASE_URL_HTTP = HTTP + BASE_URL;

    private static WebDriver driver;
    private static ThreadLocal<WebDriver> threadLocal = new ThreadLocal<>();

    private static final Set<String> foundLinks = new HashSet<>();
    private static final Queue<String> processingLinks = new ConcurrentLinkedQueue<>();

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
        System.setProperty("webdriver.chrome.driver", "src\\main\\resources\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized"); // open Browser in maximized mode
        options.addArguments("--headless"); // without this it won't start on servers

        driver = new ChromeDriver(options);

        foundLinks.add(BASE_URL_HTTPS);
        foundLinks.add(BASE_URL_HTTP);
        processingLinks.add(BASE_URL_HTTPS);

        while(!processingLinks.isEmpty()) {
            String currentLink = processingLinks.poll();
            driver.get(currentLink);
            Set<String> links = getRelativeLinks();
            System.out.println("currentLink: " + currentLink);
            System.out.println("foundLinks:");
            for (String link: links) {
                if(!foundLinks.contains(link)) {
                    foundLinks.add(link);
                    processingLinks.add(link);
                    System.out.println("\t " + link);
                }
            }
            System.out.println("--------------------");

        }

        driver.quit();
    }
}



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