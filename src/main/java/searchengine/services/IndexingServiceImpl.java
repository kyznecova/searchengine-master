package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private volatile boolean indexingStarted;
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private SiteEntity siteEntity;
    private ForkJoinPool forkJoinPool;
    private boolean isContainsSite;

    @Override
    public IndexingResponse startIndexing() {
        if (indexingStarted) {
            return new IndexingResponse(false, "Индексация уже запущена");
        } else {
            indexingStarted = true;
        }

        pageRepository.deleteAll();
        siteRepository.deleteAll();
        lemmaRepository.deleteAll();
        indexRepository.deleteAll();

        for (Site site : sites.getSites()) {
            if (!indexingStarted) {
                forkJoinPool.shutdown();
                break;
            }

            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatus(StatusType.INDEXING);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);

            forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            SiteMapCreator siteMapCreator = new SiteMapCreator(
                    site.getUrl(),
                    siteEntity,
                    pageRepository,
                    siteRepository,
                    lemmaRepository,
                    indexRepository);
            forkJoinPool.execute(siteMapCreator);
            forkJoinPool.shutdown();

            siteEntity.setStatus(StatusType.INDEXED);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
        }
        indexingStarted = false;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        indexingStarted = false;

        forkJoinPool.shutdownNow();
        List<SiteEntity> siteList = siteRepository.findAll();
        for (SiteEntity site : siteList) {
            if (site.getStatus() == StatusType.INDEXING) {
                site.setStatus(StatusType.FAILED);
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
            }
        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse indexPage(String url) {
        isContainsSite = false;
        Site indexingSite = new Site();
        for (Site site : sites.getSites()) {
            if (url.contains(site.getUrl())) {
                isContainsSite = true;
                indexingSite = site;
                break;
            }
        }

        if (!isContainsSite) {
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов," +
                    " указанных в конфигурационном файле");
        } else {
            for (SiteEntity site : siteRepository.findAll()) {
                if (site.getName().equals(indexingSite.getName())) {
                    siteEntity = site;
                    System.out.println("Сайт был найден в репозитории. Запущена переиндексация страницы");
                    break;
                } else {
                    System.out.println("Сайт не был найден в репозитории. Создается новый объект");
                    siteEntity = new SiteEntity();
                    siteEntity.setUrl(indexingSite.getUrl());
                    siteEntity.setName(indexingSite.getName());
                    siteEntity.setStatus(StatusType.INDEXING);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteEntity);
                }
                PageMapCreator pageParser = new PageMapCreator(
                        url,
                        siteEntity,
                        pageRepository,
                        siteRepository,
                        lemmaRepository,
                        indexRepository);
                pageParser.parsePage();
                return new IndexingResponse(true);
            }
        }
        return null;
    }
}
