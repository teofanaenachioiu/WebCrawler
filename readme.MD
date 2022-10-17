# Web crawler

Simple application for finding all links on a website. 

# System specifications

- Java 14
- Maven 3.8.2

# Dependencies

The project has the following dependencies:

- **Selenium** (https://www.selenium.dev/) -> for web crawling
- **Selenium WebDriver** (https://github.com/bonigarcia/webdrivermanager) -> carries out the management (i.e., download, setup, and maintenance) of the drivers required by Selenium WebDriver 
- **Gson** (https://github.com/google/gson) -> for displaying JSONs in a more readable format

# Running

```mvn clean compile exec:java```

# Assumptions

1)  The app aims to find all the links reachable from a base URL, and not every link from every page. 

2)  "http://" and "https://" are considered two different links, even if the symbols after the protocol are the same.

3)  We consider that everything after a hash ("#") is an anchor in the page and we eliminate the part. The symbols after hash can be seen as "noise".

4)  Symbol "/" after the link is also considered as being "noise" and it is removed.

5)  If a link redirects to another link (e.g. when accessing "/random" we actually land on "/post/123"), both links are considered valid links and they are taken into account.
