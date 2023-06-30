package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PageMapCreator {

    private SiteEntity site;
    private PageEntity page;
    private final String url;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private boolean isContainsPage;

    public PageMapCreator(String url,
                          SiteEntity site,
                          PageRepository pageRepository,
                          SiteRepository siteRepository,
                          LemmaRepository lemmaRepository,
                          IndexRepository indexRepository) {
        this.url = url;
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public void parsePage() {

        for (PageEntity page : pageRepository.findAll()) {
            if (page.getPath().equalsIgnoreCase(url)) {
                this.page = page;
                isContainsPage = true;
                break;
            }
        }
        if (isContainsPage) {
            List<IndexEntity> indexList = new ArrayList<>();
            for (IndexEntity index : indexRepository.findAll()) {
                if (index.getPageId().getId() == page.getId()) {
                    indexList.add(index);
                }
            }
            indexRepository.deleteAll(indexList);
            pageRepository.delete(page);
        }

        Connection connection = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com");

        try {
            Document document = connection.get();
            String newUrl = url.substring(site.getUrl().length());
            if (newUrl.length() > 0) {
                PageEntity page = new PageEntity();
                page.setSiteId(site);
                page.setPath(newUrl);
                page.setContent(String.valueOf(document));
                page.setCode(connection.response().statusCode());
                pageRepository.save(page);
                site.setStatus(StatusType.INDEXED);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);


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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

