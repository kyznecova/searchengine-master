package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import static java.util.Comparator.comparing;

public class SiteMapCreator extends RecursiveAction /*RecursiveTask<String> */{
    private String url;
    private SiteEntity site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private static CopyOnWriteArraySet<String> links = new CopyOnWriteArraySet<>();

    public SiteMapCreator(String url, SiteEntity site, PageRepository pageRepository, SiteRepository siteRepository) {
        this.url = url;
        this.site = site;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    protected void compute() {

        StringBuffer result = new StringBuffer(url + "\n");
        Set<SiteMapCreator> subTask = new TreeSet<>(comparing(o -> o.url));

        getChildren(subTask);

        for (SiteMapCreator task : subTask) {
            result.append(task.join());
        }
    }

    private void getChildren(Set<SiteMapCreator> subTask) {
        try {
            Thread.sleep(150);
            Connection connection = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("https://www.google.com");
            Document document = connection.get();
            String newUrl = url.substring(site.getUrl().length());
            if ( newUrl.length() > 0) {
                PageEntity page = new PageEntity();
                page.setSiteId(site);
                page.setPath(newUrl);
                page.setContent(String.valueOf(document));
                page.setCode(connection.response().statusCode());
                pageRepository.save(page);
            }



            Elements elements = document.select("a");
            for (Element element : elements) {
                String link = element.absUrl("href");
                if (link.startsWith(url) && !links.contains(link)
                        && !link.contains("#") && !link.contains(".pdf")
                        && !link.contains(".jpeg") && !link.contains(".png")) {
                    SiteMapCreator siteMapCreator = new SiteMapCreator(link, site, pageRepository, siteRepository);
                    siteMapCreator.fork();
                    subTask.add(siteMapCreator);
                    links.add(link);
                }
            }
        } catch (InterruptedException | IOException ignored) {
        }
    }
}
