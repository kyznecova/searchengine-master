package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;

import static java.util.Comparator.comparing;

public class SiteMapCreator extends RecursiveAction /*RecursiveTask<String> */{
    private String url;
    private SiteEntity site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;


    private static CopyOnWriteArraySet<String> links = new CopyOnWriteArraySet<>();

    public SiteMapCreator(String url, SiteEntity site, PageRepository pageRepository,
                          SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.url = url;
        this.site = site;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
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

                LemmaFinder lemmaFinder = new LemmaFinder();
                Map<String, Integer> lemmas = lemmaFinder.collectLemmas(document.text());
                List<LemmaEntity> lemmaEntityList = new ArrayList<>();
                List<IndexEntity> indexList = new ArrayList<>();

                for (String lemma : lemmas.keySet()) {
                    LemmaEntity lemmaEntity = new LemmaEntity();
                    lemmaEntity.setSiteId(site);
                    lemmaEntity.setLemma(lemma);

                    lemmaEntity.setFrequency(lemmas.get(lemma));
                    lemmaEntityList.add(lemmaEntity);

                    IndexEntity index = new IndexEntity();
                    index.setPageId(page);
                    index.setLemmaId(lemmaEntity);
                    index.setRank(lemmas.get(lemma));
                    indexList.add(index);
                }
                lemmaRepository.saveAll(lemmaEntityList);
                indexRepository.saveAll(indexList);
            }

            Elements elements = document.select("a");
            for (Element element : elements) {
                String link = element.absUrl("href");
                if (link.startsWith(url) && !links.contains(link)
                        && !link.contains("#") && !link.contains(".pdf")
                        && !link.contains(".jpeg") && !link.contains(".png")) {
                    SiteMapCreator siteMapCreator = new SiteMapCreator(link, site, pageRepository, siteRepository, lemmaRepository, indexRepository);
                    siteMapCreator.fork();
                    subTask.add(siteMapCreator);
                    links.add(link);
                }
            }
        } catch (InterruptedException | IOException ignored) {
        }
    }
}
